@file:Suppress("DEPRECATION")
package com.example.cnireader.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.example.cnireader.util.PassportLogger
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.PACESecretKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG1File
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class PassportReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class CniData(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray
)

private fun ByteArray.toHex(max: Int = 100): String =
    this.take(max).joinToString(" ") { "%02X".format(it) } + if (size > max) "…" else ""

/**
 * Fallback PassportReader qui force un PACE statique (ECDH-CAM + AES-CMAC128, P-256).
 */
object PassportReader {

    fun read(tag: Tag, can: String, cscaRaw: ByteArray, logger: PassportLogger): CniData {
        var cs: IsoDepCardService? = null
        var ps: PassportService? = null

        try {
            logger.log("🔵 Connexion NFC…")
            val isoDep = IsoDep.get(tag)
                ?: throw PassportReadException("IsoDep non supporté")
            isoDep.timeout = 10_000

            cs = IsoDepCardService(isoDep).apply { open() }
            logger.log("✅ IsoDepCardService ouvert")

            // Fallback : on désactive SFI et MAC pour éviter EF.CardAccess bloquant
            ps = PassportService(cs, 256, 0, /*isSFIEnabled=*/false, /*shouldCheckMAC=*/false).apply { open() }
            logger.log("✅ PassportService ouvert")

            // On saute EF.CardAccess et on force un PACE statique
            logger.log("⚙️ PACE statique (ECDH-CAM + AES-CMAC128, P-256)…")
            val oid = PACEInfo.ID_PACE_ECDH_CAM_AES_CBC_CMAC_128
            val paramSpec = PACEInfo.toParameterSpec(PACEInfo.PARAM_ID_ECP_NIST_P256_R1)
            runCatching {
                ps.doPACE(
                    PACESecretKeySpec(can.toByteArray(), "CAN", 0x01),
                    oid,
                    paramSpec
                )
            }.onFailure { t ->
                throw PassportReadException("PACE KO : ${t.message}", t)
            }
            logger.log("✅ PACE OK")

            // Lecture DG1
            logger.log("📥 Lecture DG1…")
            val dg1 = ps.getInputStream(PassportService.EF_DG1).use { it.readBytes() }
            logger.log("✅ DG1 lu (${dg1.size} bytes)")

            // Lecture DG2 (photo)
            logger.log("📥 Lecture DG2…")
            val dg2 = ps.getInputStream(PassportService.EF_DG2).use { it.readBytes() }
            logger.log("✅ DG2 lu (${dg2.size} bytes)")

            // Lecture SOD + PassiveAuth
            logger.log("📥 Lecture SOD…")
            val sodBytes = ps.getInputStream(PassportService.EF_SOD).use { it.readBytes() }
            logger.log("🔒 Vérification PassiveAuth…")
            val sod = SODFile(ByteArrayInputStream(sodBytes))
            val cscaCert = CertificateFactory
                .getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(cscaRaw)) as X509Certificate
            runCatching {
                PassiveAuth.verify(sod, mapOf(1 to dg1, 2 to dg2), cscaCert)
            }.onFailure { t ->
                throw PassportReadException("PassiveAuth KO : ${t.message}", t)
            }
            logger.log("✅ PassiveAuth OK")

            // Extraction MRZ
            val mrz = DG1File(ByteArrayInputStream(dg1)).mrzInfo
            return CniData(
                lastName   = mrz.primaryIdentifier,
                firstNames = mrz.secondaryIdentifier,
                birthDate  = mrz.dateOfBirth,
                photoBytes = dg2
            )

        } catch (e: PassportReadException) {
            logger.log("❌ ${e.message}")
            throw e
        } catch (t: Throwable) {
            logger.log("❌ Erreur inattendue : ${t.message}")
            throw PassportReadException("Erreur inattendue : ${t.message}", t)
        } finally {
            try { ps?.close() } catch (_: Throwable) { }
            try { cs?.close() } catch (_: Throwable) { }
        }
    }
}
