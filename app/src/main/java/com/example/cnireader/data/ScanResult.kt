package com.example.cnireader.data

data class ScanResult(
    val lastName: String,
    val firstNames: String,
    val birthDate: String,
    val photoBytes: ByteArray,
    val emoji: String
)
