package com.example.data.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
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

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

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
                if (_ttsState.value is TtsState.Initializing) {
                    _ttsState.value = TtsState.Idle
                }
                Timber.i("TextToSpeech initialized with Google TTS engine")
            } else {
                // Google TTS başarısız olursa varsayılan motorla tekrar dene
                Timber.w("Google TTS engine failed, falling back to default engine")
                initializeWithDefaultEngine()
            }
        }, googleTtsPackage)
    }

    private fun initializeWithDefaultEngine() {
        textToSpeech?.shutdown()
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                applyTurkishLocale()
                setupProgressListener()
                if (_ttsState.value is TtsState.Initializing) {
                    _ttsState.value = TtsState.Idle
                }
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
        when (langResult) {
            TextToSpeech.LANG_MISSING_DATA -> {
                textToSpeech?.language = Locale.getDefault()
                _ttsState.value = TtsState.Error("Türkçe ses verisi cihazınızda bulunamadı. Lütfen TTS ayarlarından Türkçe ses paketini indirin.")
                Timber.w("Turkish TTS missing data, falling back to default")
            }
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                textToSpeech?.language = Locale.getDefault()
                Timber.w("Turkish TTS not supported, using default: %s", Locale.getDefault())
            }
            else -> {
                Timber.i("Turkish locale applied to TTS")
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
                abandonAudioFocus()
                _ttsState.value = TtsState.Idle
                currentOnDoneCallback?.invoke()
                currentOnDoneCallback = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Timber.e("TTS utterance error: %s", utteranceId)
                abandonAudioFocus()
                _ttsState.value = TtsState.Error("Seslendirme sırasında hata oluştu.")
                currentOnDoneCallback?.invoke()
                currentOnDoneCallback = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Timber.e("TTS utterance error %d: %s", errorCode, utteranceId)
                abandonAudioFocus()
                _ttsState.value = TtsState.Error("Seslendirme hatası ($errorCode).")
                currentOnDoneCallback?.invoke()
                currentOnDoneCallback = null
            }
        })
    }

    private fun requestAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                            stop()
                        }
                    }
                    .build()

                audioFocusRequest = focusReq
                am.requestAudioFocus(focusReq)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    { focusChange -> if (focusChange == AudioManager.AUDIOFOCUS_LOSS) stop() },
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
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
        requestAudioFocus()
        currentOnDoneCallback = onDone

        try {
            // 0.92f hız ve 0.96f pitch insansı temposuna mükemmel uyar
            textToSpeech?.setSpeechRate((speechRate * 0.92f).coerceIn(0.5f, 2.0f))
            textToSpeech?.setPitch((pitch * 0.96f).coerceIn(0.5f, 2.0f))

            val utteranceId = UUID.randomUUID().toString()
            val cleanText = sanitizeTextForSpeech(text)

            _ttsState.value = TtsState.Speaking(cleanText, utteranceId)

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                putInt(
                    TextToSpeech.Engine.KEY_PARAM_STREAM,
                    AudioManager.STREAM_MUSIC
                )
            }

            textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Timber.d("TTS speaking: %s", cleanText.take(60))
        } catch (e: Exception) {
            Timber.e(e, "Error during TTS speak")
            abandonAudioFocus()
            _ttsState.value = TtsState.Error("Seslendirme başlatılamadı: ${e.localizedMessage}")
        }
    }

    fun stop() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            Timber.e(e, "Error stopping TTS")
        } finally {
            abandonAudioFocus()
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
            abandonAudioFocus()
            textToSpeech = null
            isInitialized = false
            _ttsState.value = TtsState.Idle
        }
    }

    /**
     * Metni TTS için temizler ve daha doğal nefes duraklamaları ekler.
     */
    private fun sanitizeTextForSpeech(input: String): String {
        return input
            .replace(Regex("[*#_`~]"), "")
            .replace(Regex("\\[.*?]\\(.*?\\)"), "")
            .replace(Regex("^[-•·]\\s*", RegexOption.MULTILINE), "")
            .replace(Regex("\\s{2,}"), " ")
            .replace(Regex("\\n+"), ", ")
            .replace(Regex("\\.{2,}"), "...")
            .replace(". ", ", ")
            .trimEnd(',', ' ')
            .trim()
    }
}
