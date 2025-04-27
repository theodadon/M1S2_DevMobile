@file:Suppress("DEPRECATION")

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

            isoDep.timeout = 5000

            // 1️⃣ SELECT Master File
            val selectMF = byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0x3F, 0x00)
            logger.log("➡️ SELECT MF : ${selectMF.toHex()}")
            isoDep.transceive(selectMF).also { logger.log("⬅️ Réponse MF : ${it.toHex()}") }

            // 2️⃣ SELECT IAS-ECC AID
            val selectIAS = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00, 0x0D,
                0xA0.toByte(), 0x00, 0x00, 0x01, 0x67, 0x45, 0x53, 0x49, 0x41, 0x53, 0x43, 0x45, 0x43
            )
            logger.log("➡️ SELECT IAS-ECC AID : ${selectIAS.toHex()}")
            isoDep.transceive(selectIAS).also { logger.log("⬅️ Réponse AID : ${it.toHex()}") }

            // 3️⃣ SELECT le fichier Identity (0500)
            val selectIdentity = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x05, 0x00)
            logger.log("➡️ SELECT fichier 0500 (identité)")
            isoDep.transceive(selectIdentity).also { logger.log("⬅️ Réponse 0500 : ${it.toHex()}") }

            // 4️⃣ READ BINARY (on lit les infos personnelles)
            val readBinary = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)
            logger.log("➡️ READ BINARY sur 0500")
            val dataIdentity = isoDep.transceive(readBinary)
            logger.log("⬅️ Données identité : ${dataIdentity.toHex()}")

            // 5️⃣ Parse grossier des données (debug)
            val text = String(dataIdentity.dropLast(2).toByteArray())
            logger.log("📄 Infos brutes :\n$text")

            // (bonus plus tard : découper proprement en Nom / Prénom / DDN)

            // 6️⃣ Lire aussi la photo : SELECT 0600 + READ BINARY
            val selectPhoto = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x06, 0x00)
            logger.log("➡️ SELECT fichier 0600 (photo)")
            isoDep.transceive(selectPhoto).also { logger.log("⬅️ Réponse 0600 : ${it.toHex()}") }

            val readPhoto = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)
            logger.log("➡️ READ BINARY sur 0600")
            val dataPhoto = isoDep.transceive(readPhoto)
            logger.log("⬅️ Photo reçue (${dataPhoto.size} octets)")

            // Ensuite à toi de décoder JPEG2000 (option à ajouter)

        } catch (e: Exception) {
            logger.log("❌ Exception : ${e.message}")
            throw e
        } finally {
            try { isoDep.close(); logger.log("🔒 Déconnexion NFC") } catch (_: Exception) {}
        }
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02X".format(it) }
}
