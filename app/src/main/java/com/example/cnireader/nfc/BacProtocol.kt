package com.example.cnireader.nfc

import android.nfc.tech.IsoDep
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class BacProtocol(private val isoDep: IsoDep) {

    private lateinit var kenc: ByteArray
    private lateinit var kmac: ByteArray

    fun doBAC(can: String) {
        // CAN => MRZ info format simulé
        val mrzInfo = can.padEnd(30, '<')

        val hash = MessageDigest.getInstance("SHA-1").digest(mrzInfo.toByteArray())

        kenc = hash.copyOfRange(0, 16)
        kmac = hash.copyOfRange(16, 32)

        // En vrai il faudrait faire du "Mutual Authentication" ici
        // Pour simplifier, on considère que si on arrive ici, on peut communiquer.
        // (ton projet CNI simplifié donc lecture directe)
    }

    fun getSecureIsoDep(): IsoDep {
        return isoDep
    }
}
