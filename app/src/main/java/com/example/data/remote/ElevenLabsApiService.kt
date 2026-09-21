package com.example.data.remote

import com.example.data.remote.model.ElevenLabsTtsRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ElevenLabsApiService {

    @Streaming
    @POST("v1/text-to-speech/{voice_id}/stream")
    suspend fun generateSpeechStream(
        @Path("voice_id") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body request: ElevenLabsTtsRequest
    ): ResponseBody
}
