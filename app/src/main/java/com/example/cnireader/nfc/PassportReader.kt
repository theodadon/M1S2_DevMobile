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

/** Exception métier pour tout problème NFC */
class PassportReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Données extraites du passeport */
data class CniData(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray
)

object PassportReader {

    /**
     * Lit les données d’un passeport biométrique via PACE (ou BAC en fallback)
     * puis vérifie la Passive Authentication.
     */
    fun read(
        tag: Tag,
        can: String,
        cscaRaw: ByteArray,
        logger: PassportLogger
    ): CniData {
        var cs: IsoDepCardService? = null
        var ps: PassportService? = null

        try {
            logger.log("Connexion IsoDep")
            val isoDep = IsoDep.get(tag)
                ?: throw PassportReadException("IsoDep non supporté")
            isoDep.timeout = 10000

            cs = IsoDepCardService(isoDep).apply { open() }
            logger.log("IsoDepCardService ouvert")

            ps = PassportService(cs, 256, 0, false, false).apply { open() }
            logger.log("PassportService ouvert")

            // Forçage CAN en dur pour debug (remplacer par can en prod)
            val fixedCan = can

            // lecture EF.CardAccess
            logger.log("Lecture EF.CardAccess")
            val caBytes = try {
                ps.getInputStream(PassportService.EF_CARD_ACCESS).use { it.readBytes() }
            } catch (t: Throwable) {
                throw PassportReadException("Lecture EF.CardAccess impossible: ${t.message}", t)
            }
            logger.log("EF.CardAccess lu (${caBytes.size} octets)")

            // récupération des PACEInfo
            val paceInfos = try {
                CardAccessFile(ByteArrayInputStream(caBytes))
                    .getSecurityInfos()
                    .filterIsInstance<PACEInfo>()
            } catch (t: Throwable) {
                throw PassportReadException("Parsing CardAccessFile impossible: ${t.message}", t)
            }

            // tentative PACE
            var paceOk = false
            for (paceInfo in paceInfos) {
                logger.log("Tentative PACE OID=${paceInfo.objectIdentifier}")
                val spec = PACEInfo.toParameterSpec(paceInfo.parameterId)
                try {
                    ps.doPACE(
                        PACESecretKeySpec(fixedCan.toByteArray(), "CAN", 0x01),
                        paceInfo.objectIdentifier,
                        spec
                    )
                    logger.log("PACE réussi")
                    paceOk = true
                    break
                } catch (_: Throwable) {
                    logger.log("PACE échoué pour OID=${paceInfo.objectIdentifier}")
                }
            }

            // fallback BAC si PACE KO
            if (!paceOk) {
                logger.log("Tentative BAC")
                try {
                    val bacKey = BACKey(fixedCan, fixedCan, fixedCan)
                    ps.doBAC(bacKey)
                    logger.log("BAC réussi")
                } catch (t: Throwable) {
                    throw PassportReadException("PACE et BAC ont échoué", t)
                }
            }

            // lecture DG1
            logger.log("Lecture DG1")
            val dg1 = try {
                ps.getInputStream(PassportService.EF_DG1).use { it.readBytes() }
            } catch (t: Throwable) {
                throw PassportReadException("Lecture DG1 impossible: ${t.message}", t)
            }
            logger.log("DG1 lu (${dg1.size} octets)")

            // lecture DG2 (photo)
            logger.log("Lecture DG2")
            val dg2 = try {
                ps.getInputStream(PassportService.EF_DG2).use { it.readBytes() }
            } catch (t: Throwable) {
                throw PassportReadException("Lecture DG2 impossible: ${t.message}", t)
            }
            logger.log("DG2 lu (${dg2.size} octets)")

            // lecture SOD
            logger.log("Lecture SOD")
            val sodBytes = try {
                ps.getInputStream(PassportService.EF_SOD).use { it.readBytes() }
            } catch (t: Throwable) {
                throw PassportReadException("Lecture SOD impossible: ${t.message}", t)
            }
            logger.log("SOD lu (${sodBytes.size} octets)")

            // Passive Authentication
            logger.log("Vérification Passive Authentication")
            val sod = SODFile(ByteArrayInputStream(sodBytes))
            val cf = CertificateFactory.getInstance("X.509")
            val cscaCert = cf.generateCertificate(ByteArrayInputStream(cscaRaw)) as X509Certificate
            try {
                PassiveAuth.verify(sod, mapOf(1 to dg1, 2 to dg2), cscaCert)
                logger.log("Passive Authentication réussie")
            } catch (t: Throwable) {
                throw PassportReadException("Passive Authentication échouée: ${t.message}", t)
            }

            // extraction MRZ
            val mrz = DG1File(ByteArrayInputStream(dg1)).mrzInfo
            return CniData(
                lastName   = mrz.primaryIdentifier,
                firstNames = mrz.secondaryIdentifier,
                birthDate  = mrz.dateOfBirth,
                photoBytes = dg2
            )
        } catch (e: PassportReadException) {
            logger.log("Erreur NFC: ${e.message}")
            throw e
        } finally {
            try { ps?.close() } catch (_: Throwable) {}
            try { cs?.close() } catch (_: Throwable) {}
        }
    }
}
