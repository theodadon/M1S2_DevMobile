package com.example.cnireader.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Docs: List all emojis
 * GET https://emoji-api.com/emojis?access_key=KEY
 * :contentReference[oaicite:0]{index=0}
 */
interface EmojiApiService {
    @GET("emojis")
    suspend fun getAllEmojis(
        @Query("access_key") accessKey: String
    ): List<EmojiApiResponse>
}
