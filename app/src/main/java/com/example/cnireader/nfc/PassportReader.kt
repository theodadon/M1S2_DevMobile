@file:Suppress("DEPRECATION")

package com.example.cnireader.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.example.cnireader.util.PassportLogger
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.BACKey
import org.jmrtd.PACESecretKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.CardAccessFile
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

private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

object PassportReader {

    private val AID_CNI: ByteArray = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x0C,
        0x07,
        0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x02
    )

    fun read(tag: Tag, can: String, cscaRaw: ByteArray, logger: PassportLogger): CniData {
        var cs: IsoDepCardService? = null
        var ps: PassportService? = null

        try {
            logger.log("🔵 Connexion IsoDep…")
            val isoDep = IsoDep.get(tag)
                ?: throw PassportReadException("IsoDep non supporté")
            isoDep.timeout = 10_000

            cs = IsoDepCardService(isoDep).apply { open() }
            logger.log("✅ IsoDepCardService ouvert")

            logger.log("🔹 Sélect AID CNIe : ${AID_CNI.toHex()}")
            val resp = isoDep.transceive(AID_CNI)
            logger.log("🔹 Réponse SELECT AID : ${resp.toHex()}")

            ps = PassportService(cs, 256, 0, true, true).apply { open() }
            logger.log("✅ PassportService ouvert")

            val fixedCan = "066424"
            logger.log("✏️ CAN forcé = $fixedCan")

            logger.log("📥 getInputStream(EF_CARD_ACCESS)…")
            val caBytes = runCatching {
                ps.getInputStream(PassportService.EF_CARD_ACCESS).use { it.readBytes() }
            }.getOrElse { t ->
                logger.log("❌ Lecture EF_CARD_ACCESS échouée : ${t.message}")
                throw PassportReadException("Impossible lire EF_CARD_ACCESS", t)
            }
            logger.log("✅ EF_CARD_ACCESS lu (${caBytes.size} octets)")

            val paceInfos = runCatching {
                val cardAccess = CardAccessFile(ByteArrayInputStream(caBytes))
                cardAccess.getSecurityInfos().filterIsInstance<PACEInfo>()
            }.getOrElse { t ->
                logger.log("❌ Parsing CardAccessFile KO : ${t.message}")
                throw PassportReadException("CardAccessFile invalide", t)
            }
            logger.log("✅ PACEInfo trouvés : ${paceInfos.size}")

            var paceOk = false
            for ((i, paceInfo) in paceInfos.withIndex()) {
                logger.log("⚡ Essai PACE #${i+1} (OID=${paceInfo.objectIdentifier})…")
                val spec = runCatching {
                    PACEInfo.toParameterSpec(paceInfo.parameterId)
                }.getOrElse { t ->
                    logger.log("❌ toParameterSpec KO: ${t.message}")
                    null
                } ?: continue

                if (runCatching {
                        ps.doPACE(
                            PACESecretKeySpec(fixedCan.toByteArray(), "CAN", 0x01),
                            paceInfo.objectIdentifier,
                            spec
                        )
                    }.isSuccess) {
                    logger.log("✅ PACE réussi (OID=${paceInfo.objectIdentifier})")
                    paceOk = true
                    break
                } else {
                    logger.log("❌ PACE échoué (OID=${paceInfo.objectIdentifier})")
                }
            }

            if (!paceOk) {
                logger.log("🔁 Fallback BAC…")
                runCatching {
                    val bacKey = BACKey(fixedCan, fixedCan, fixedCan)
                    ps.doBAC(bacKey)
                }.onSuccess {
                    logger.log("✅ BAC réussi")
                }.onFailure { t ->
                    logger.log("❌ BAC échoué: ${t.message}")
                    throw PassportReadException("PACE et BAC échoués", t)
                }
            }

            logger.log("📥 getInputStream(EF_DG1)…")
            val dg1 = runCatching {
                ps.getInputStream(PassportService.EF_DG1).use { it.readBytes() }
            }.getOrElse { t ->
                logger.log("❌ Lecture DG1 KO: ${t.message}")
                throw PassportReadException("Impossible lire DG1", t)
            }
            logger.log("✅ DG1 lu (${dg1.size} octets)")

            logger.log("📥 getInputStream(EF_DG2)…")
            val dg2 = runCatching {
                ps.getInputStream(PassportService.EF_DG2).use { it.readBytes() }
            }.getOrElse { t ->
                logger.log("❌ Lecture DG2 KO: ${t.message}")
                throw PassportReadException("Impossible lire DG2", t)
            }
            logger.log("✅ DG2 lu (${dg2.size} octets)")

            logger.log("📥 getInputStream(EF_SOD)…")
            val sodBytes = runCatching {
                ps.getInputStream(PassportService.EF_SOD).use { it.readBytes() }
            }.getOrElse { t ->
                logger.log("❌ Lecture SOD KO: ${t.message}")
                throw PassportReadException("Impossible lire SOD", t)
            }
            logger.log("✅ SOD lu (${sodBytes.size} octets)")

            logger.log("🔒 PassiveAuth…")
            val sodFile = SODFile(ByteArrayInputStream(sodBytes))
            val cscaCert = CertificateFactory
                .getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(cscaRaw)) as X509Certificate
            runCatching {
                PassiveAuth.verify(sodFile, mapOf(1 to dg1, 2 to dg2), cscaCert)
            }.onSuccess {
                logger.log("✅ PassiveAuth OK")
            }.onFailure { t ->
                logger.log("❌ PassiveAuth KO: ${t.message}")
                throw PassportReadException("PassiveAuth échouée", t)
            }

            val mrz = DG1File(ByteArrayInputStream(dg1)).mrzInfo
            logger.log("✅ Lecture terminée")

            return CniData(
                lastName = mrz.primaryIdentifier,
                firstNames = mrz.secondaryIdentifier,
                birthDate = mrz.dateOfBirth,
                photoBytes = dg2
            )

        } catch (e: PassportReadException) {
            logger.log("❌ PassportReadException: ${e.message}")
            throw e
        } catch (t: Throwable) {
            logger.log("❌ Erreur inattendue: ${t.message}")
            throw PassportReadException("Erreur inattendue: ${t.message}", t)
        } finally {
            try { ps?.close() } catch (_: Throwable) {}
            try { cs?.close() } catch (_: Throwable) {}
        }
    }
}
