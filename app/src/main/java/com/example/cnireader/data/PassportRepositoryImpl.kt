package com.example.cnireader.data

import android.nfc.Tag
import com.example.cnireader.nfc.IsoDepReader
import com.example.cnireader.util.PassportLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassportRepositoryImpl @Inject constructor() : PassportRepository {

    override suspend fun scan(tag: Tag, logger: PassportLogger) {
        IsoDepReader.read(tag, logger)
    }
}
