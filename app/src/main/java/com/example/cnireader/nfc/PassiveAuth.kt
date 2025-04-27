package com.example.cnireader.nfc

import org.jmrtd.lds.SODFile
import java.security.MessageDigest
import java.security.cert.X509Certificate

/** Passive Authentication (hash + signature DS) */
object PassiveAuth {
    private fun mdName(oid: String?): String = when (oid) {
        "2.16.840.1.101.3.4.2.1" -> "SHA-256"
        "2.16.840.1.101.3.4.2.2" -> "SHA-384"
        "2.16.840.1.101.3.4.2.3" -> "SHA-512"
        "1.3.14.3.2.26"          -> "SHA-1"
        else                     -> "SHA-256"
    }

    fun verify(sod: SODFile, dgMap: Map<Int, ByteArray>, csca: X509Certificate) {
        val md = MessageDigest.getInstance(mdName(sod.digestAlgorithm))
        for ((dg, expected) in sod.dataGroupHashes) {
            val actual = dgMap[dg] ?: throw IllegalStateException("DG$dg manquant")
            if (!md.digest(actual).contentEquals(expected)) {
                throw IllegalStateException("Hash DG$dg invalide")
            }
        }
        sod.docSigningCertificate
            ?.verify(csca.publicKey)
            ?: throw IllegalStateException("Certificat DS absent du SOD")
    }
}
