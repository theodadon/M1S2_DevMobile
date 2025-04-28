package com.example.cnireader.data

import android.nfc.Tag
import com.example.cnireader.util.PassportLogger

interface PassportRepository {
    /**
     * Lance la lecture du Tag NFC, loggue tout avec [PassportLogger].
     */
    suspend fun scan(tag: Tag, logger: PassportLogger)
}
