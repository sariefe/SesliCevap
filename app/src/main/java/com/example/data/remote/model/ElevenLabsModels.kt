package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ElevenLabsTtsRequest(
    @Json(name = "text") val text: String,
    @Json(name = "model_id") val modelId: String = "eleven_turbo_v2_5",
    @Json(name = "voice_settings") val voiceSettings: ElevenLabsVoiceSettings? = ElevenLabsVoiceSettings()
)

@JsonClass(generateAdapter = true)
data class ElevenLabsVoiceSettings(
    @Json(name = "stability") val stability: Float = 0.5f,
    @Json(name = "similarity_boost") val similarityBoost: Float = 0.75f
)
