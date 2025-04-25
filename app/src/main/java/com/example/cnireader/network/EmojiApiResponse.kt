package com.example.cnireader.network

/**
 * Champ "character" contient l'emoji lui-même.
 */
data class EmojiApiResponse(
    val slug: String,
    val character: String,
    val unicodeName: String,
    val codePoint: String,
    val group: String,
    val subGroup: String
)
