package com.example.data.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.example.BuildConfig
import com.example.data.remote.ElevenLabsApiService
import com.example.data.remote.model.ElevenLabsTtsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ElevenLabsTtsManager(
    private val context: Context,
    private val elevenLabsApiService: ElevenLabsApiService,
    private val nativeTtsManager: TextToSpeechManager
) {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var mediaPlayer: MediaPlayer? = null

    // Varsayılan doğal Türkçe uyumlu ses ID'si (21m00Tcm4TlvDq8ikWAM - Rachel)
    private val defaultVoiceId = "21m00Tcm4TlvDq8ikWAM"

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    init {
        // Yerel TTS durumlarını da dinle
        scope.launch {
            nativeTtsManager.ttsState.collect { state ->
                if (mediaPlayer == null) {
                    _ttsState.value = state
                }
            }
        }
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        elevenLabsApiKey: String = "",
        onDone: () -> Unit = {}
    ) {
        stop()

        val keyToUse = elevenLabsApiKey.ifBlank {
            BuildConfig.ELEVENLABS_API_KEY.ifBlank {
                System.getenv("ELEVENLABS_API_KEY") ?: ""
            }
        }

        if (keyToUse.isNotBlank()) {
            scope.launch {
                try {
                    _ttsState.value = TtsState.Initializing
                    Timber.d("ElevenLabs TTS request started")
                    val request = ElevenLabsTtsRequest(text = text)
                    val responseBody = elevenLabsApiService.generateSpeechStream(
                        voiceId = defaultVoiceId,
                        apiKey = keyToUse,
                        request = request
                    )

                    val tempFile = File(context.cacheDir, "elevenlabs_${UUID.randomUUID()}.mp3")
                    FileOutputStream(tempFile).use { output ->
                        responseBody.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }

                    Timber.i("ElevenLabs TTS request succeeded")
                    withContext(Dispatchers.Main) {
                        playAudioFile(tempFile, text, onDone)
                    }
                } catch (e: Exception) {
                    // Do not log API keys or request payloads — status only.
                    Timber.w("ElevenLabs TTS failed, falling back to Native Android TTS: %s", e.javaClass.simpleName)
                    withContext(Dispatchers.Main) {
                        nativeTtsManager.speak(text, speechRate, pitch, onDone)
                    }
                }
            }
        } else {
            Timber.d("ElevenLabs API key missing, using Native Android TTS")
            nativeTtsManager.speak(text, speechRate, pitch, onDone)
        }
    }

    private fun playAudioFile(file: File, text: String, onDone: () -> Unit) {
        try {
            stopMediaPlayer()
            val usage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioAttributes.USAGE_ASSISTANT
            } else {
                AudioAttributes.USAGE_MEDIA
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _ttsState.value = TtsState.Idle
                    file.delete()
                    onDone()
                    stopMediaPlayer()
                }
                setOnErrorListener { _, _, _ ->
                    _ttsState.value = TtsState.Idle
                    file.delete()
                    onDone()
                    stopMediaPlayer()
                    true
                }
                start()
            }
            _ttsState.value = TtsState.Speaking(text, file.name)
            Timber.i("Playing ElevenLabs audio stream successfully")
        } catch (e: Exception) {
            Timber.e(e, "Error playing ElevenLabs audio file, falling back")
            file.delete()
            nativeTtsManager.speak(text, onDone = onDone)
        }
    }

    fun stop() {
        stopMediaPlayer()
        nativeTtsManager.stop()
        _ttsState.value = TtsState.Idle
    }

    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error stopping MediaPlayer")
        } finally {
            mediaPlayer = null
        }
    }

    fun shutdown() {
        stop()
        nativeTtsManager.shutdown()
    }
}
