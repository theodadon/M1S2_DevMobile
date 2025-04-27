package com.example.cnireader.util

/**
 * Permet de logger chaque étape du scan NFC,
 * et renvoyer tout le texte construit au fur et à mesure.
 */
class PassportLogger(
    private val onLog: (String) -> Unit
) {
    private val builder = StringBuilder()

    fun log(message: String) {
        builder.appendLine(message)
        onLog(builder.toString())
    }
}
