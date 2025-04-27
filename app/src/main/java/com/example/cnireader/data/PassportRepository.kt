package com.example.cnireader.data


import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.example.cnireader.data.ScanResult
import com.example.cnireader.nfc.BacProtocol
import com.example.cnireader.nfc.LDSReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PassportRepository @Inject constructor() {

    open suspend fun scan(tag: Tag, can: String): ScanResult {
        val isoDep = IsoDep.get(tag) ?: throw Exception("Pas de IsoDep sur ce Tag")
        isoDep.connect()

        val bacSession = BacProtocol(isoDep)
        bacSession.doBAC(can)

        val ldsReader = LDSReader(bacSession)
        val result = ldsReader.readDG1AndDG2()

        isoDep.close()
        return result
    }
}