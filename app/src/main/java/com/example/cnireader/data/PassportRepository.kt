package com.example.cnireader.data

import android.nfc.Tag
import com.example.cnireader.nfc.CniData
import com.example.cnireader.util.PassportLogger

data class ScanResult(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray,
    val emoji: String
)

interface PassportRepository {
    suspend fun scan(tag: Tag, can: String, logger: PassportLogger): ScanResult
}
