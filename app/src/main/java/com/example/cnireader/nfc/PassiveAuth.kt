package com.example.cnireader.nfc

import org.jmrtd.lds.SODFile
import java.security.MessageDigest
import java.security.cert.X509Certificate

private fun mdName(oid: String) = when (oid) {
    "2.16.840.1.101.3.4.2.1"  -> "SHA-256"   // sha256
    "2.16.840.1.101.3.4.2.2"  -> "SHA-384"
    "2.16.840.1.101.3.4.2.3"  -> "SHA-512"
    "1.3.14.3.2.26"           -> "SHA-1"
    else -> error("OID hash inconnu : $oid")
}

/** Passive Auth compatible JMRTD 0.8.1 (hash + chaîne CSCA→DS). */
object PassiveAuth {

    fun verify(sod: SODFile, dgMap: Map<Int, ByteArray>, csca: X509Certificate) {

        /* 1 ▸ contrôle des hash */
        val md = MessageDigest.getInstance(mdName(sod.digestAlgorithm))
        for ((dg, expectedHash) in sod.dataGroupHashes) {
            val actual = dgMap[dg] ?: error("DG$dg manquant")
            if (!md.digest(actual).contentEquals(expectedHash)) {
                throw IllegalStateException("Hash DG$dg invalide")
            }
        }

        /* 2 ▸ vérifier la signature du certificat DS avec la clé CSCA  */
        sod.docSigningCertificate?.verify(csca.publicKey)
            ?: throw IllegalStateException("Certificat DS absent du SOD")
    }
}
