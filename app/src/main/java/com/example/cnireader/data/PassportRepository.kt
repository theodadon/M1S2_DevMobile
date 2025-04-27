package com.example.cnireader.data

import android.nfc.Tag
import com.example.cnireader.nfc.CniData
import com.example.cnireader.util.PassportLogger

/**
 * Résultat de scan : données CNI + emoji.
 */
data class ScanResult(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray,
    val emoji: String
)

interface PassportRepository {
    /**
     * Lance la lecture NFC + journalisation via [logger], puis récupère un emoji.
     */
    suspend fun scan(tag: Tag, can: String, logger: PassportLogger): ScanResult
}
