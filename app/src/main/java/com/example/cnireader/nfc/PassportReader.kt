// File: app/src/main/java/com/example/cnireader/nfc/PassportReader.kt
package com.example.cnireader.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.PACESecretKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.icao.DG1File
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** Exception métier pour tout problème NFC */
class PassportReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Données extraites de la CNI */
data class CniData(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray
)

/** Helper pour dump hex, limité à [max] octets */
private fun ByteArray.toHex(max: Int = 100): String =
    this.take(max).joinToString(" ") { "%02X".format(it) } + if (size > max) "…" else ""

/**
 * Lecteur ICAO 9303 / PACE + PassiveAuth adapté CNIe
 */
object PassportReader {

    fun read(tag: Tag, can: String, cscaRaw: ByteArray): CniData {
        try {
            // 1️⃣ connexion NFC
            val isoDep = IsoDep.get(tag)
                ?: throw PassportReadException("IsoDep non supporté")
            isoDep.timeout = 5000

            // 2️⃣ CardService
            val cs = IsoDepCardService(isoDep).apply { open() }

            // 3️⃣ PassportService : SFI **désactivé**, MAC **désactivé**
            val ps = PassportService(cs, 256, 0, true, true).apply { open() }
            // 4️⃣ lecture EF.CardAccess pour récupérer les infos PACE
            val caBytes = try {
                ps.getInputStream(PassportService.EF_CARD_ACCESS).use { it.readBytes() }
            } catch (e: Exception) {
                throw PassportReadException("Impossible de lire EF.CardAccess : ${e.message}", e)
            }
            val cardAccess = CardAccessFile(ByteArrayInputStream(caBytes))
            val paceInfo = cardAccess.securityInfos
                .filterIsInstance<org.jmrtd.lds.PACEInfo>()
                .firstOrNull()
                ?: throw PassportReadException("EF.CardAccess sans PACEInfo")

            Log.d("PassportReader", "💡 PACEInfo trouvé : OID=${paceInfo.objectIdentifier}")

            // ⚡ FORCAGE CAN en dur pour debug
            val debugCan = "066424" // ← ton CAN à 6 chiffres
            Log.w("PassportReader", "⚡ CAN forcé : $debugCan")

            // 5️⃣ PACE avec le paramètre issu de EF.CardAccess
            try {
                val paramSpec = org.jmrtd.lds.PACEInfo.toParameterSpec(paceInfo.parameterId)
                ps.doPACE(
                    PACESecretKeySpec(debugCan.toByteArray(), "CAN", 0x01),
                    paceInfo.objectIdentifier,
                    paramSpec
                )
                Log.d("PassportReader", "✅ PACE OK")
            } catch (e: Exception) {
                throw PassportReadException("PACE KO : ${e.message}", e)
            }

            // 6️⃣ Lecture DG1
            val dg1 = try {
                ps.getInputStream(PassportService.EF_DG1).use { it.readBytes() }
            } catch (e: Exception) {
                throw PassportReadException("Impossible de lire DG1 : ${e.message}", e)
            }
            Log.d("PassportReader", "DG1 (${dg1.size} bytes): ${dg1.toHex(50)}")

            // 7️⃣ Lecture DG2 (photo)
            val dg2 = try {
                ps.getInputStream(PassportService.EF_DG2).use { it.readBytes() }
            } catch (e: Exception) {
                throw PassportReadException("Impossible de lire DG2 : ${e.message}", e)
            }
            Log.d("PassportReader", "DG2 (${dg2.size} bytes)")

            // 8️⃣ Lecture SOD + Passive Auth
            val sodBytes = try {
                ps.getInputStream(PassportService.EF_SOD).use { it.readBytes() }
            } catch (e: Exception) {
                throw PassportReadException("Impossible de lire SOD : ${e.message}", e)
            }
            val sod = SODFile(ByteArrayInputStream(sodBytes))
            val cf = CertificateFactory.getInstance("X.509")
            val cscaCert = cf.generateCertificate(ByteArrayInputStream(cscaRaw)) as X509Certificate

            try {
                PassiveAuth.verify(sod, mapOf(1 to dg1, 2 to dg2), cscaCert)
                Log.d("PassportReader", "✅ Passive Auth OK")
            } catch (e: Exception) {
                throw PassportReadException("Passive Auth échouée : ${e.message}", e)
            }

            // 9️⃣ extraction MRZ
            val mrz = DG1File(ByteArrayInputStream(dg1)).mrzInfo
            return CniData(
                lastName   = mrz.primaryIdentifier,
                firstNames = mrz.secondaryIdentifier,
                birthDate  = mrz.dateOfBirth,
                photoBytes = dg2
            )

        } catch (e: PassportReadException) {
            Log.e("PassportReader", "PassportReadException", e)
            throw e
        } catch (e: Exception) {
            Log.e("PassportReader", "Erreur inattendue", e)
            throw PassportReadException("Échec lecture NFC : ${e.message}", e)
        }
    }
}
