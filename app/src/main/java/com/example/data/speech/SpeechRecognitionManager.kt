package com.example.data.speech

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

sealed interface SpeechState {
    data object Idle : SpeechState
    data object Ready : SpeechState
    data class Listening(val partialText: String = "") : SpeechState
    data class Success(val finalText: String) : SpeechState
    data class Error(val errorMessage: String) : SpeechState
}

class SpeechRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private fun requestAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            stopListening()
                        }
                    }
                    .build()

                audioFocusRequest = focusReq
                am.requestAudioFocus(focusReq)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    { focusChange -> if (focusChange == AudioManager.AUDIOFOCUS_LOSS) stopListening() },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }

    fun startListening(locale: Locale = Locale.forLanguageTag("tr-TR")) {
        if (!isRecognitionAvailable) {
            _speechState.value = SpeechState.Error("Cihazınızda ses tanıma servisi bulunamadı.")
            Timber.w("Speech recognition service not available on this device")
            return
        }

        try {
            stopListening()
            requestAudioFocus()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
            _speechState.value = SpeechState.Listening()
            Timber.d("Started listening for speech in language: %s", locale)
        } catch (e: Exception) {
            Timber.e(e, "Error starting speech recognition")
            abandonAudioFocus()
            _speechState.value = SpeechState.Error("Ses dinleme başlatılamadı: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Timber.e(e, "Error stopping speech recognition")
        } finally {
            abandonAudioFocus()
            speechRecognizer = null
            _rmsDb.value = 0f
            if (_speechState.value is SpeechState.Listening) {
                _speechState.value = SpeechState.Idle
            }
        }
    }

    fun resetState() {
        abandonAudioFocus()
        _speechState.value = SpeechState.Idle
        _rmsDb.value = 0f
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Timber.d("SpeechRecognizer ready for speech")
                _speechState.value = SpeechState.Ready
            }

            override fun onBeginningOfSpeech() {
                Timber.d("SpeechRecognizer speech begun")
                _speechState.value = SpeechState.Listening("")
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = (rmsdB.coerceIn(0f, 10f))
                _rmsDb.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Timber.d("SpeechRecognizer end of speech")
                _rmsDb.value = 0f
            }

            override fun onError(error: Int) {
                _rmsDb.value = 0f
                abandonAudioFocus()
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Ses kaydı hatası oluştu."
                    SpeechRecognizer.ERROR_CLIENT -> "İstemci hatası."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon izni verilmedi."
                    SpeechRecognizer.ERROR_NETWORK -> "Ağ bağlantısı hatası."
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Ağ zaman aşımına uğradı."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Konuşma anlaşılamadı. Lütfen tekrar deneyin."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Ses tanıyıcı meşgul, lütfen bekleyin."
                    SpeechRecognizer.ERROR_SERVER -> "Sunucu hatası oluştu."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Ses algılanamadı."
                    else -> "Bilinmeyen bir ses hatası ($error)."
                }
                Timber.w("SpeechRecognizer error: %s (code %d)", message, error)
                _speechState.value = SpeechState.Error(message)
            }

            override fun onResults(results: Bundle?) {
                _rmsDb.value = 0f
                abandonAudioFocus()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                Timber.d("SpeechRecognizer result: %s", text)
                if (text.isNotBlank()) {
                    _speechState.value = SpeechState.Success(text)
                } else {
                    _speechState.value = SpeechState.Error("Hiçbir konuşma algılanamadı.")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank()) {
                    _speechState.value = SpeechState.Listening(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
