package com.example.data.speech

import android.content.Context
import android.os.Bundle
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

    // Google TTS'in paket adı — çoğu Android cihazda varsayılan olarak yüklü gelir
    private val googleTtsPackage = "com.google.android.tts"

    init {
        initializeTts()
    }

    private fun initializeTts() {
        _ttsState.value = TtsState.Initializing

        // Önce Google TTS motoruyla başlatmayı dene
        textToSpeech = TextToSpeech(context, { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                applyTurkishLocale()
                setupProgressListener()
                _ttsState.value = TtsState.Idle
                Timber.i("TextToSpeech initialized with Google TTS engine")
            } else {
                // Google TTS başarısız olursa varsayılan motorla tekrar dene
                Timber.w("Google TTS engine failed, falling back to default engine")
                initializeWithDefaultEngine()
            }
        }, googleTtsPackage) // ← Google TTS motorunu zorla
    }

    private fun initializeWithDefaultEngine() {
        textToSpeech?.shutdown()
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                applyTurkishLocale()
                setupProgressListener()
                _ttsState.value = TtsState.Idle
                Timber.i("TextToSpeech initialized with default engine")
            } else {
                isInitialized = false
                Timber.e("TextToSpeech initialization failed with status: %d", status)
                _ttsState.value = TtsState.Error("Metin seslendirme motoru başlatılamadı.")
            }
        }
    }

    private fun applyTurkishLocale() {
        val trLocale = Locale.forLanguageTag("tr-TR")
        val langResult = textToSpeech?.setLanguage(trLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA ||
            langResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            textToSpeech?.language = Locale.getDefault()
            Timber.w("Turkish TTS not supported, using default: %s", Locale.getDefault())
        } else {
            Timber.i("Turkish locale applied to TTS")
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
            Timber.w("TTS not initialized yet, retrying...")
            initializeTts()
            return
        }

        stop()
        currentOnDoneCallback = onDone

        try {
            // Biraz yavaş ve alçak pitch daha insansı hissettiriyor
            textToSpeech?.setSpeechRate((speechRate * 0.9f).coerceIn(0.5f, 2.0f))
            textToSpeech?.setPitch((pitch * 0.95f).coerceIn(0.5f, 2.0f))

            val utteranceId = UUID.randomUUID().toString()
            val cleanText = sanitizeTextForSpeech(text)

            _ttsState.value = TtsState.Speaking(cleanText, utteranceId)

            // Bundle ile ses akışını müzik kanalına yönlendir — kalite artar
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                putInt(
                    TextToSpeech.Engine.KEY_PARAM_STREAM,
                    android.media.AudioManager.STREAM_MUSIC
                )
            }

            textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Timber.d("TTS speaking: %s", cleanText.take(60))
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

    /**
     * Metni TTS için temizler ve daha doğal okunmasını sağlar.
     * - Markdown işaretlerini kaldırır
     * - Kısa duraklamalar için virgül ekler
     * - Uzun cümleleri nefes alanlarına böler
     */
    private fun sanitizeTextForSpeech(input: String): String {
        return input
            // Markdown temizleme
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\[.*?]\\(.*?\\)"), "")
            // Madde işaretlerini virgüle çevir — liste gibi okumak yerine akıcı konuş
            .replace(Regex("^[-•·]\\s*", RegexOption.MULTILINE), "")
            // Birden fazla boşluğu tek boşluğa indir
            .replace(Regex("\\s{2,}"), " ")
            // Satır sonlarını kısa duraklama virgülüne çevir
            .replace(Regex("\\n+"), ", ")
            // "..." gibi üç noktalı duraklamaları koru ama normalize et
            .replace(Regex("\\.{2,}"), "...")
            // Sondaki gereksiz noktalama temizle
            .trimEnd(',', ' ')
            .trim()
    }
}