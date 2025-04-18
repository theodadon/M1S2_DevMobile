package com.example.cnireader.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import net.sf.scuba.smartcards.IsoDepCardService
import org.jmrtd.PassportService
import org.jmrtd.PACESecretKeySpec
import org.jmrtd.lds.SODFile
import org.jmrtd.lds.icao.DG1File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

data class CniData(val lastName: String, val firstNames: String, val birthDate: String)

object PassportReader {

    /** Lit DG1 + SOD, exécute PACE‑CAN, effectue la Passive Auth (JMRTD 0.8.1). */
    fun read(tag: Tag, can: String, cscaRaw: ByteArray): CniData {

        /* ◉ 1 Connexion NFC */
        val isoDep = IsoDep.get(tag) ?: error("Tag IsoDep non supporté")
        isoDep.timeout = 5000
        val cardService = IsoDepCardService(isoDep).apply { open() }
        /* ◉ 2 PassportService (JMRTD 0.8.1)  */
        val ps = PassportService(cardService, 256, 0, false, false).apply { open() }
        /* ◉ 3 PACE CAN  – keyReference 0x01 */
        val paceKey = PACESecretKeySpec(can.toByteArray(), "CAN", 0x01)
        ps.doPACE(paceKey, null, null)

        /* ◉ 4 Lecture DG1 & SOD  */
        val dg1Bytes = ps.getInputStream(PassportService.EF_DG1).use { it.readBytes() }
        val sodBytes = ps.getInputStream(PassportService.EF_SOD).use { it.readBytes() }

        val mrz = DG1File(dg1Bytes.inputStream()).mrzInfo
        val sod = SODFile(sodBytes.inputStream())

        /* ◉ 5 Passive Auth  */
        val csca = CertificateFactory.getInstance("X.509")
            .generateCertificate(cscaRaw.inputStream()) as X509Certificate
        PassiveAuth.verify(sod, mapOf(1 to dg1Bytes), csca)

        /* ◉ 6 Retour  */
        return CniData(
            lastName   = mrz.primaryIdentifier,
            firstNames = mrz.secondaryIdentifier,
            birthDate  = mrz.dateOfBirth
        )
    }
}
