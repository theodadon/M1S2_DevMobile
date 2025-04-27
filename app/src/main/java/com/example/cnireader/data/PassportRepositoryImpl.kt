// File: app/src/main/java/com/example/cnireader/data/PassportRepositoryImpl.kt
package com.example.cnireader.data

import com.example.cnireader.data.PassportRepository
import android.content.Context
import android.nfc.Tag
import android.util.Log
import com.example.cnireader.nfc.CniData
import com.example.cnireader.nfc.PassportReadException
import com.example.cnireader.nfc.PassportReader
import com.example.cnireader.network.EmojiApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import javax.inject.Singleton

/** Exception métier pour la couche Repository */
class ScanException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Singleton
class PassportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val emojiApi: EmojiApiService,
    private val accessKey: String
) : PassportRepository() {

    override suspend fun scan(tag: Tag, can: String): ScanResult {
        try {
            // 1) Charger le certificat CSCA
            val csca = context.assets.open("csca_france.crt").use { it.readBytes() }

            // 2) Lecture NFC + PACE + PassiveAuth
            val data: CniData = try {
                PassportReader.read(tag, can, csca)
            } catch (e: PassportReadException) {
                Log.e("PassportRepo", "Lecture CNI échouée", e)
                throw ScanException(
                    "Erreur lecture CNI : ${e.message}\n\n${e.stackTraceToString()}",
                    e
                )
            }

            // 3) Appel API Emoji
            Log.d("PassportRepo", "➤ Appel API Emoji…")
            val all = try {
                emojiApi.getAllEmojis(accessKey)
            } catch (e: Exception) {
                Log.e("PassportRepo", "API Emoji échouée", e)
                throw ScanException(
                    "Erreur API Emoji : ${e.message}\n\n${e.stackTraceToString()}",
                    e
                )
            }
            Log.d("PassportRepo", "✅ ${all.size} emojis reçus")

            // 4) Choix aléatoire d'un emoji
            val idx = ThreadLocalRandom.current().nextInt(all.size)
            val emoji = all[idx].character
            Log.d("PassportRepo", "🎲 Emoji tiré : $emoji")

            // 5) Retour du résultat
            return ScanResult(
                lastName   = data.lastName,
                firstNames = data.firstNames,
                birthDate  = data.birthDate,
                photoBytes = data.photoBytes,
                emoji      = emoji
            )

        } catch (e: ScanException) {
            // Erreur fonctionnelle déjà formatée
            throw e
        } catch (e: Exception) {
            // Erreur technique inconnue
            Log.e("PassportRepo", "Erreur inattendue", e)
            throw ScanException(
                "Erreur inconnue : ${e.message}\n\n${e.stackTraceToString()}",
                e
            )
        }
    }
}
