package com.example.cnireader.nfc

import android.nfc.tech.IsoDep
import com.example.cnireader.data.ScanResult

class LDSReader(private val bacSession: BacProtocol) {

    fun readDG1AndDG2(): ScanResult {
        val isoDep = bacSession.getSecureIsoDep()

        // Simulation simple : envoi de commandes NFC (SELECT + READ_BINARY)
        // (Normalement on ferait SELECT FILE, READ BINARY, etc)

        // (Ici simplifié pour ton test)
        // DG1 => Info identité (nom, prénom, date de naissance)
        val lastName = "DUPONT"
        val firstNames = "Jean"
        val birthDate = "1992-05-14"

        // DG2 => Photo (fictive ici)
        val photoBytes = ByteArray(0) // Remplacer par vraie lecture DG2 plus tard

        // Pour ton test, on met un tableau vide ou une image stockée dans l'app

        return ScanResult(
            lastName = lastName,
            firstNames = firstNames,
            birthDate = birthDate,
            photoBytes = photoBytes,
            emoji = "📇"
        )
    }
}
