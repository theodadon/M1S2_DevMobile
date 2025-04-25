package com.example.cnireader.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.PACESecretKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG1File
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/** Exception métier pour tout problème NFC */
class PassportReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

private fun ByteArray.toHex(): String =
    joinToString(" ") { "%02X".format(it) }

/** Données extraites de la CNI */
data class CniData(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray
)

object PassportReader {

    /**
     * Lit DG1, DG2, SOD et renvoie CniData.
     * Gère PACE null-OID en fallback.
     */
    fun read(tag: Tag, can: String, cscaRaw: ByteArray): CniData {
        try {
            val isoDep = IsoDep.get(tag)
                ?: throw PassportReadException("IsoDep non supporté")
            isoDep.timeout = 5000

            val cs = IsoDepCardService(isoDep).apply { open() }
            val ps = PassportService(cs, 256, 0, false, false).apply { open() }

            // PACE avec CAN, try/catch pour null-OID
            try {
                ps.doPACE(PACESecretKeySpec(can.toByteArray(), "CAN", 0x01), null, null)
            } catch (e: Exception) {
                Log.w("PassportReader", "PACE échoué (${e.message}), on continue sans PACE", e)
            }

            // Lecture DG1
            val dg1 = ps.getInputStream(PassportService.EF_DG1).use { it.readBytes() }
            Log.d("PassportReader", "DG1 RAW (${dg1.size} bytes): ${dg1.toHex().take(200)}…")

            // Lecture DG2 (photo)
            val dg2 = ps.getInputStream(PassportService.EF_DG2).use { it.readBytes() }
            Log.d("PassportReader", "DG2 RAW (${dg2.size} bytes): ${dg2.toHex().take(200)}…")

            // Lecture SOD
            val sodBytes = ps.getInputStream(PassportService.EF_SOD).use { it.readBytes() }
            val sod = SODFile(ByteArrayInputStream(sodBytes))

            // Passive Auth
            val cf = CertificateFactory.getInstance("X.509")
            val cscaCert = cf.generateCertificate(ByteArrayInputStream(cscaRaw)) as X509Certificate
            PassiveAuth.verify(sod, mapOf(1 to dg1, 2 to dg2), cscaCert)

            // Extraction MRZ
            val mrz = DG1File(ByteArrayInputStream(dg1)).mrzInfo

            return CniData(
                lastName   = mrz.primaryIdentifier,
                firstNames = mrz.secondaryIdentifier,
                birthDate  = mrz.dateOfBirth,
                photoBytes = dg2
            )
        } catch (e: Exception) {
            Log.e("PassportReader", "Erreur lecture CNI", e)
            throw PassportReadException("Échec lecture NFC : ${e.message}", e)
        }
    }
}
