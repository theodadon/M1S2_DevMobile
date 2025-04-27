package com.example.cnireader.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.example.cnireader.util.PassportLogger

object IsoDepReader {

    suspend fun read(tag: Tag, logger: PassportLogger) {
        val isoDep = IsoDep.get(tag)
            ?: throw Exception("IsoDep non supporté")

        try {
            logger.log("🔵 Connexion NFC...")
            isoDep.connect()
            logger.log("✅ Connexion réussie")

            // Allonge un peu les timeouts
            isoDep.timeout = 5000

            // Première commande : SELECT MF (Master File)
            val selectMf = byteArrayOf(0x00.toByte(), 0xA4.toByte(), 0x00.toByte(), 0x0C.toByte(), 0x02.toByte(), 0x3F.toByte(), 0x00.toByte())
            logger.log("➡️ SELECT MF : ${selectMf.toHex()}")
            val responseMf = isoDep.transceive(selectMf)
            logger.log("⬅️ Réponse MF : ${responseMf.toHex()}")

            // (optionnel) Tester SELECT AID IAS-ECC (application ID pour les cartes françaises)
            // Ex : "A000000167455349415343454343" = AID IAS-ECC
            val selectIasAid = byteArrayOf(
                0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
                0x0D.toByte(), // longueur 13
                0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
                0x67.toByte(), 0x45.toByte(), 0x53.toByte(), 0x49.toByte(),
                0x41.toByte(), 0x53.toByte(), 0x43.toByte(), 0x45.toByte(),
                0x43.toByte()
            )
            logger.log("➡️ SELECT IAS-ECC AID : ${selectIasAid.toHex()}")
            val responseAid = isoDep.transceive(selectIasAid)
            logger.log("⬅️ Réponse AID : ${responseAid.toHex()}")

        } catch (e: Exception) {
            logger.log("❌ Exception : ${e.message}")
            throw e
        } finally {
            try {
                isoDep.close()
                logger.log("🔒 Déconnexion NFC")
            } catch (_: Exception) { }
        }
    }

    // Extension utilitaire pour afficher proprement en hexadécimal
    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02X".format(it) }
}
