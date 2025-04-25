package com.example.cnireader.network

import retrofit2.http.GET
import retrofit2.http.Query

interface EmojiApiService {
    @GET("emojis")
    suspend fun getAllEmojis(
        @Query("access_key") accessKey: String
    ): List<EmojiApiResponse>
}
