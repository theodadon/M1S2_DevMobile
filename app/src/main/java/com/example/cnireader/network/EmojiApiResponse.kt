package com.example.cnireader.network

data class EmojiApiResponse(
    val slug: String,
    val character: String,
    val unicodeName: String,
    val codePoint: String,
    val group: String,
    val subGroup: String
)
