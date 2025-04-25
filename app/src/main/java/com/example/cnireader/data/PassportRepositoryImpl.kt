package com.example.cnireader.data

import android.content.Context
import android.nfc.Tag
import android.util.Log
import com.example.cnireader.nfc.PassportReader
import com.example.cnireader.network.EmojiApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val emojiApi: EmojiApiService,
    private val accessKey: String
) : PassportRepository {

    override suspend fun scan(tag: Tag, can: String): ScanResult {
        val cscaBytes = context.assets.open("csca_france.crt").use { it.readBytes() }
        val data = PassportReader.read(tag, can, cscaBytes)

        Log.d("PassportRepo", "➡ Appel API Emoji en cours…")
        val allEmojis = emojiApi.getAllEmojis(accessKey)
        Log.d("PassportRepo", "✅ ${allEmojis.size} emojis reçus : ${allEmojis.take(3)}")

        val randomIndex = ThreadLocalRandom.current().nextInt(allEmojis.size)
        val emojiChar = allEmojis[randomIndex].character
        Log.d("PassportRepo", "🎲 Emoji tiré : $emojiChar (${allEmojis[randomIndex].unicodeName})")

        return ScanResult(
            lastName   = data.lastName,
            firstNames = data.firstNames,
            birthDate  = data.birthDate,
            photoBytes = data.photoBytes,
            emoji      = emojiChar
        )
    }
}
