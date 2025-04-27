package com.example.cnireader.network

import retrofit2.http.GET
import retrofit2.http.Query

data class EmojiApiResponse(
    val slug: String,
    val character: String,
    val unicodeName: String,
    val codePoint: String,
    val group: String,
    val subGroup: String
)

interface EmojiApiService {
    @GET("emojis")
    suspend fun getAllEmojis(@Query("access_key") accessKey: String): List<EmojiApiResponse>
}
