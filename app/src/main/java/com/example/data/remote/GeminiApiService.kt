package com.example.data.remote

import com.example.data.remote.model.GeminiRequest
import com.example.data.remote.model.GeminiResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface GeminiApiService {

    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @Streaming
    @POST("v1beta/models/gemini-2.0-flash:streamGenerateContent?alt=sse")
    suspend fun streamGenerateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): ResponseBody
}
