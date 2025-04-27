@file:Suppress("DEPRECATION")

package com.example.cnireader.nfc
import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.example.cnireader.util.PassportLogger

data class CniResult(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray
)

object IsoDepReader {

    suspend fun read(tag: Tag, logger: PassportLogger): CniResult {
        val isoDep = IsoDep.get(tag) ?: throw Exception("IsoDep non supporté")

        try {
            logger.log("Connexion NFC…")
            isoDep.connect()
            logger.log("Connexion réussie")

            isoDep.timeout = 5000

            // select MF
            val selectMF = byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0x3F, 0x00)
            logger.log("➡SELECT MF : ${selectMF.toHex()}")
            isoDep.transceive(selectMF).also { logger.log("⬅Réponse MF : ${it.toHex()}") }

            // SELECT IAS-ECC AID
            val selectIAS = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00, 0x0D,
                0xA0.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
                0x67.toByte(), 0x45.toByte(), 0x53.toByte(), 0x49.toByte(),
                0x41.toByte(), 0x53.toByte(), 0x43.toByte(), 0x45.toByte(),
                0x43.toByte()
            )
            logger.log("➡SELECT IAS-ECC AID : ${selectIAS.toHex()}")
            isoDep.transceive(selectIAS).also { logger.log("⬅Réponse AID : ${it.toHex()}") }

            // SELECT fichier identité 0500
            val selectIdentity = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x05, 0x00)
            logger.log("➡SELECT fichier 0500 (Identité)")
            isoDep.transceive(selectIdentity).also { logger.log("⬅Réponse 0500 : ${it.toHex()}") }

            // READ BINARY sur 0500
            val readBinaryIdentity = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)
            logger.log("➡READ BINARY fichier 0500")
            val identityData = isoDep.transceive(readBinaryIdentity)
            logger.log("⬅Données identité : ${identityData.toHex()}")

            // Analyse brute pour récupérer Nom/Prénom/DDN
            val textData = String(identityData.dropLast(2).toByteArray())
            logger.log("Infos brutes (UTF-8) : $textData")

            val (lastName, firstNames, birthDate) = parseIdentity(textData)

            // SELECT fichier photo 0600
            val selectPhoto = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x06, 0x00)
            logger.log("➡SELECT fichier 0600 (Photo)")
            isoDep.transceive(selectPhoto).also { logger.log("⬅Réponse 0600 : ${it.toHex()}") }

            // READ BINARY sur 0600
            val readBinaryPhoto = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)
            logger.log("➡READ BINARY fichier 0600")
            val photoBytes = isoDep.transceive(readBinaryPhoto)
            logger.log("Photo reçue (${photoBytes.size} octets)")

            // Résultat final prêt
            return CniResult(
                lastName = lastName,
                firstNames = firstNames,
                birthDate = birthDate,
                photoBytes = photoBytes
            )

        } catch (e: Exception) {
            logger.log("Exception : ${e.message}")
            throw e
        } finally {
            try { isoDep.close(); logger.log("🔒 Déconnexion NFC") } catch (_: Exception) {}
        }
    }

    private fun parseIdentity(text: String): Triple<String, String, String> {
        val lines = text.lines()
        val lastName = lines.getOrNull(0)?.trim() ?: "?"
        val firstNames = lines.getOrNull(1)?.trim() ?: "?"
        val birthDate = lines.getOrNull(2)?.trim() ?: "?"
        return Triple(lastName, firstNames, birthDate)
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02X".format(it) }
}
