package com.example.data.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale
import java.util.UUID

sealed interface TtsState {
    data object Idle : TtsState
    data object Initializing : TtsState
    data class Speaking(val text: String, val utteranceId: String) : TtsState
    data class Error(val message: String) : TtsState
}

class TextToSpeechManager(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Idle)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private var currentOnDoneCallback: (() -> Unit)? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        _ttsState.value = TtsState.Initializing
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                val trLocale = Locale.forLanguageTag("tr-TR")
                val langResult = textToSpeech?.setLanguage(trLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default locale if Turkish not installed
                    textToSpeech?.setLanguage(Locale.getDefault())
                    Timber.w("Turkish TTS language not supported, using default locale: %s", Locale.getDefault())
                } else {
                    Timber.i("TextToSpeech initialized with Turkish locale")
                }
                setupProgressListener()
                _ttsState.value = TtsState.Idle
            } else {
                isInitialized = false
                Timber.e("TextToSpeech initialization failed with status: %d", status)
                _ttsState.value = TtsState.Error("Metin seslendirme motoru başlatılamadı.")
            }
        }
    }

    private fun setupProgressListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Timber.d("TTS utterance started: %s", utteranceId)
            }

            override fun onDone(utteranceId: String?) {
                Timber.d("TTS utterance completed: %s", utteranceId)
                _ttsState.value = TtsState.Idle
                currentOnDoneCallback?.invoke()
                currentOnDoneCallback = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Timber.e("TTS utterance error: %s", utteranceId)
                _ttsState.value = TtsState.Error("Seslendirme sırasında hata oluştu.")
                currentOnDoneCallback?.invoke()
                currentOnDoneCallback = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Timber.e("TTS utterance error %d: %s", errorCode, utteranceId)
                _ttsState.value = TtsState.Error("Seslendirme hatası ($errorCode).")
                currentOnDoneCallback?.invoke()
                currentOnDoneCallback = null
            }
        })
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        onDone: () -> Unit = {}
    ) {
        if (!isInitialized || textToSpeech == null) {
            Timber.w("TTS not initialized yet")
            initializeTts()
            return
        }

        stop()
        currentOnDoneCallback = onDone

        try {
            textToSpeech?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
            textToSpeech?.setPitch(pitch.coerceIn(0.5f, 2.0f))

            val utteranceId = UUID.randomUUID().toString()
            _ttsState.value = TtsState.Speaking(text, utteranceId)

            val cleanText = sanitizeTextForSpeech(text)
            textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            Timber.d("TTS speaking text: %s", cleanText.take(50))
        } catch (e: Exception) {
            Timber.e(e, "Error during TTS speak")
            _ttsState.value = TtsState.Error("Seslendirme başlatılamadı: ${e.localizedMessage}")
        }
    }

    fun stop() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Timber.e(e, "Error stopping TTS")
        } finally {
            if (_ttsState.value is TtsState.Speaking) {
                _ttsState.value = TtsState.Idle
            }
            currentOnDoneCallback = null
        }
    }

    fun shutdown() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Timber.e(e, "Error shutting down TTS")
        } finally {
            textToSpeech = null
            isInitialized = false
            _ttsState.value = TtsState.Idle
        }
    }

    private fun sanitizeTextForSpeech(input: String): String {
        // Strip markdown stars, hashes, code ticks to sound pleasant in TTS
        return input
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
            .trim()
    }
}
