package com.example.cnireader.util

class PassportLogger(
    private val onLog: (String) -> Unit
) {
    private val builder = StringBuilder()

    fun log(message: String) {
        builder.appendLine(message)
        onLog(builder.toString())
    }
}
