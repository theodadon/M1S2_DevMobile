package com.example.cnireader.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.example.cnireader.util.PassportLogger

object IsoDepReader {

    suspend fun read(tag: Tag, logger: PassportLogger) {
        val isoDep = IsoDep.get(tag)
            ?: throw Exception("IsoDep non supporté")

        try {
            logger.log("Connexion NFC...")
            isoDep.connect()
            logger.log("Connexion réussie")
            isoDep.timeout = 5000

            val selectMf = byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0x3F, 0x00)
            logger.log("Commande : SELECT MF")
            logger.log(selectMf.toHex())
            val responseMf = isoDep.transceive(selectMf)
            logger.log("Réponse :")
            logger.log(responseMf.toHex())

            val selectIasAid = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00,
                0x0D,
                0xA0.toByte(), 0x00, 0x00, 0x01,
                0x67, 0x45, 0x53, 0x49,
                0x41, 0x53, 0x43, 0x45,
                0x43
            )
            logger.log("Commande : SELECT IAS-ECC AID")
            logger.log(selectIasAid.toHex())
            val responseAid = isoDep.transceive(selectIasAid)
            logger.log("Réponse :")
            logger.log(responseAid.toHex())

        } catch (e: Exception) {
            logger.log("Erreur : ${e.message}")
            throw e
        } finally {
            try {
                isoDep.close()
                logger.log("Déconnexion NFC")
            } catch (_: Exception) { }
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02X".format(it) }
}
