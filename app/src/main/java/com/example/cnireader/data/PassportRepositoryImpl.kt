package com.example.cnireader.data

import android.content.Context
import android.nfc.Tag
import android.util.Log
import com.example.cnireader.nfc.CniData
import com.example.cnireader.nfc.PassportReadException
import com.example.cnireader.nfc.PassportReader
import com.example.cnireader.network.EmojiApiService
import com.example.cnireader.util.PassportLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import javax.inject.Singleton

class ScanException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Singleton
class PassportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val emojiApi: EmojiApiService,
    private val accessKey: String
) : PassportRepository {

    override suspend fun scan(tag: Tag, can: String, logger: PassportLogger): ScanResult {
        try {
            val csca = context.assets.open("csca_france.crt").use { it.readBytes() }

            val data: CniData = try {
                PassportReader.read(tag, can, csca, logger)
            } catch (e: PassportReadException) {
                logger.log("Lecture CNI échouée : ${e.message}")
                throw ScanException("Lecture CNI échouée : ${e.message}", e)
            }

            logger.log("➤ Appel API Emoji…")
            val list = try {
                emojiApi.getAllEmojis(accessKey)
            } catch (e: Exception) {
                logger.log("API Emoji KO : ${e.message}")
                throw ScanException("API Emoji KO : ${e.message}", e)
            }
            logger.log("✅ ${list.size} emojis reçus")

            val emoji = list[ThreadLocalRandom.current().nextInt(list.size)].character
            logger.log("Emoji tiré : $emoji")

            return ScanResult(
                lastName = data.lastName,
                firstNames = data.firstNames,
                birthDate = data.birthDate,
                photoBytes = data.photoBytes,
                emoji = emoji
            )

        } catch (e: ScanException) {
            throw e
        } catch (t: Throwable) {
            Log.e("PassportRepo", "Erreur inattendue", t)
            throw ScanException("Erreur inconnue : ${t.message}", t)
        }
    }
}
