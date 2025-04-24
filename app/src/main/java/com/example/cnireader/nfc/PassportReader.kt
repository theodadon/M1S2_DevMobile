/** nfc/PassportReader */

package com.example.cnireader.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.PACESecretKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG1File
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

data class CniData(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray
)

object PassportReader {

    fun read(tag: Tag, can: String, cscaRaw: ByteArray): CniData {
        val isoDep = IsoDep.get(tag) ?: error("IsoDep non supporté")
        isoDep.timeout = 5000
        val cs = IsoDepCardService(isoDep).apply { open() }
        val ps = PassportService(cs, 256, 0, false, false).apply { open() }

        // PACE CAN
        val paceKey = PACESecretKeySpec(can.toByteArray(), "CAN", 0x01)
        ps.doPACE(paceKey, null, null)

        // Lecture DG1, DG2, SOD
        val dg1 = ps.getInputStream(PassportService.EF_DG1).use { it.readBytes() }
        val dg2 = ps.getInputStream(PassportService.EF_DG2).use { it.readBytes() }
        val sodStream = ps.getInputStream(PassportService.EF_SOD)
        val sod = SODFile(sodStream)

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
    }
}

