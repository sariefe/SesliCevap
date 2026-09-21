package com.example.ui.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.VoiceAssistantRepository
import com.example.data.speech.ElevenLabsTtsManager
import com.example.data.speech.SpeechRecognitionManager
import com.example.data.speech.SpeechState
import com.example.data.speech.TtsState
import com.example.domain.model.AnalyticsSummary
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConversationSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class VoiceUiState(
    val currentConversationId: Long? = null,
    val currentConversationTitle: String = "Yeni Sohbet",
    val messages: List<ChatMessage> = emptyList(),
    val speechState: SpeechState = SpeechState.Idle,
    val ttsState: TtsState = TtsState.Idle,
    val micRmsDb: Float = 0f,
    val isProcessingAi: Boolean = false,
    // true → tam sesli mod, false → yalnızca metin modu
    val isVoiceEnabled: Boolean = true,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val elevenLabsApiKey: String = "",
    val userErrorMessage: String? = null
)

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    private val repository: VoiceAssistantRepository,
    private val speechManager: SpeechRecognitionManager,
    private val ttsManager: ElevenLabsTtsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<ConversationSession>> = repository.getConversations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val analytics: StateFlow<AnalyticsSummary> = repository.getAnalytics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsSummary(
                totalConversations = 0,
                totalMessages = 0,
                totalUserQueries = 0,
                dominantSentiment = "—",
                dominantCategory = "—",
                mostUsedCategory = "",
                mostUsedSentiment = "",
                sentimentDistribution = emptyList(),
                categoryDistribution = emptyList()
            )
        )

    private var messagesJob: Job? = null
    private var listeningStartTime: Long = 0L

    // Kullanıcı konuşmayı bitirince otomatik gönderim için sessizlik sayacı
    private var silenceTimerJob: Job? = null

    // Kaç ms sessizlik sonra otomatik gönderilsin (1.5 saniye)
    private val autoSubmitSilenceMs = 1_500L

    init {
        observeSpeechState()
        observeTtsState()
        initializeDefaultConversation()
    }

    // -------------------------------------------------------------------------
    // Gözlemciler
    // -------------------------------------------------------------------------

    private fun observeSpeechState() {
        viewModelScope.launch {
            speechManager.speechState.collectLatest { state ->
                _uiState.update { it.copy(speechState = state) }
                when (state) {
                    is SpeechState.Listening -> {
                        if (listeningStartTime == 0L) {
                            listeningStartTime = System.currentTimeMillis()
                        }
                        // Partial text gelince sessizlik sayacını sıfırla
                        if (state.partialText.isNotBlank()) {
                            resetSilenceTimer()
                        }
                    }

                    is SpeechState.Success -> {
                        silenceTimerJob?.cancel()
                        val duration = if (listeningStartTime > 0L) {
                            System.currentTimeMillis() - listeningStartTime
                        } else 0L
                        listeningStartTime = 0L
                        handleRecognizedSpeech(state.finalText, duration)
                        speechManager.resetState()
                    }

                    is SpeechState.Error -> {
                        silenceTimerJob?.cancel()
                        listeningStartTime = 0L
                        // Sessizlik zaman aşımı ve eşleşme bulunamadı hatalarını gizle —
                        // bunlar normal kullanımda sıkça olur, kullanıcıyı rahatsız etmesin
                        val shouldShow = !state.errorMessage.contains("anlaşılamadı") &&
                                !state.errorMessage.contains("algılanamadı") &&
                                !state.errorMessage.contains("zaman aşımı")
                        if (shouldShow) {
                            _uiState.update { it.copy(userErrorMessage = state.errorMessage) }
                        }
                    }

                    SpeechState.Idle -> {
                        silenceTimerJob?.cancel()
                        listeningStartTime = 0L
                    }

                    SpeechState.Ready -> { /* Mikrofon hazır, bekliyoruz */ }
                }
            }
        }

        viewModelScope.launch {
            speechManager.rmsDb.collectLatest { rms ->
                _uiState.update { it.copy(micRmsDb = rms) }
            }
        }
    }

    private fun observeTtsState() {
        viewModelScope.launch {
            ttsManager.ttsState.collectLatest { ttsState ->
                _uiState.update { it.copy(ttsState = ttsState) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sessizlik sayacı — kullanıcı konuşmayı bitirince otomatik gönderim
    // -------------------------------------------------------------------------

    private fun resetSilenceTimer() {
        silenceTimerJob?.cancel()
        silenceTimerJob = viewModelScope.launch {
            delay(autoSubmitSilenceMs.milliseconds)
            // Süre doldu ve hâlâ dinleme modundayız → durdur, sistem sonucu gönderecek
            val currentState = _uiState.value.speechState
            if (currentState is SpeechState.Listening && currentState.partialText.isNotBlank()) {
                Timber.d("Silence detected, stopping recognition to trigger result")
                speechManager.stopListening()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Konuşma yönetimi
    // -------------------------------------------------------------------------

    private fun initializeDefaultConversation() {
        viewModelScope.launch {
            val list = conversations.first()
            if (_uiState.value.currentConversationId == null) {
                if (list.isNotEmpty()) {
                    selectConversation(list.first().id)
                } else {
                    val newId = repository.createNewConversation("Yeni Sesli Sohbet")
                    selectConversation(newId)
                }
            }
        }
    }

    fun selectConversation(conversationId: Long) {
        _uiState.update { it.copy(currentConversationId = conversationId) }
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesForConversation(conversationId).collectLatest { msgs ->
                _uiState.update { current ->
                    val title = conversations.value
                        .find { it.id == conversationId }
                        ?.title
                        ?: current.currentConversationTitle
                    current.copy(messages = msgs, currentConversationTitle = title)
                }
            }
        }
    }

    fun createNewConversation() {
        viewModelScope.launch {
            stopVoiceInput()
            stopAudio()
            val newId = repository.createNewConversation("Yeni Sesli Sohbet")
            selectConversation(newId)
        }
    }

    // -------------------------------------------------------------------------
    // Ses girişi
    // -------------------------------------------------------------------------

    fun toggleVoiceListening() {
        when (_uiState.value.speechState) {
            is SpeechState.Listening, SpeechState.Ready -> stopVoiceInput()
            else -> startVoiceInput()
        }
    }

    fun startVoiceInput() {
        if (!_uiState.value.isVoiceEnabled) {
            _uiState.update {
                it.copy(userErrorMessage = "Sesli etkileşim kapalı. Ayarlardan sesli modu açabilirsiniz.")
            }
            return
        }
        stopAudio()
        listeningStartTime = System.currentTimeMillis()
        speechManager.startListening()
    }

    fun stopVoiceInput() {
        silenceTimerJob?.cancel()
        speechManager.stopListening()
    }

    // -------------------------------------------------------------------------
    // Mesaj işleme
    // -------------------------------------------------------------------------

    fun sendTextMessage(prompt: String) {
        if (prompt.isBlank()) return
        stopAudio()
        handleRecognizedSpeech(prompt.trim(), 0L)
    }

    private fun handleRecognizedSpeech(text: String, durationMs: Long) {
        val convId = _uiState.value.currentConversationId
        if (convId == null) {
            viewModelScope.launch {
                val newId = repository.createNewConversation("Yeni Sesli Sohbet")
                selectConversation(newId)
                processUserPrompt(newId, text, durationMs)
            }
        } else {
            processUserPrompt(convId, text, durationMs)
        }
    }

    private fun processUserPrompt(conversationId: Long, prompt: String, durationMs: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingAi = true, userErrorMessage = null) }
            val result = repository.sendUserPrompt(conversationId, prompt, durationMs)
            _uiState.update { it.copy(isProcessingAi = false) }

            result.onSuccess { aiMessage ->
                Timber.i(
                    "AI response received, isVoiceEnabled=%b, autoSpeak=%b",
                    _uiState.value.isVoiceEnabled, _uiState.value.autoSpeak
                )
                if (_uiState.value.isVoiceEnabled && _uiState.value.autoSpeak) {
                    playAudio(aiMessage.text)
                }
            }.onFailure { err ->
                Timber.e(err, "Failed to get AI answer")
                // Hata zaten Türkçe geliyor (repository'de çevrildi)
                _uiState.update {
                    it.copy(userErrorMessage = err.localizedMessage ?: "Yapay zeka yanıt veremedi.")
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Ses çıkışı
    // -------------------------------------------------------------------------

    fun playAudio(text: String, force: Boolean = false) {
        if (!_uiState.value.isVoiceEnabled && !force) {
            _uiState.update {
                it.copy(userErrorMessage = "Sesli oynatma kapalı. Ayarlardan sesli modu açabilirsiniz.")
            }
            return
        }
        ttsManager.speak(
            text = text,
            speechRate = _uiState.value.speechRate,
            pitch = _uiState.value.speechPitch,
            elevenLabsApiKey = _uiState.value.elevenLabsApiKey
        )
    }

    fun stopAudio() {
        ttsManager.stop()
    }

    // -------------------------------------------------------------------------
    // Ayarlar
    // -------------------------------------------------------------------------

    fun toggleVoiceEnabled() {
        val newVoiceEnabled = !_uiState.value.isVoiceEnabled
        if (!newVoiceEnabled) {
            stopVoiceInput()
            stopAudio()
        }
        _uiState.update { it.copy(isVoiceEnabled = newVoiceEnabled) }
    }

    fun updateSettings(
        isVoiceEnabled: Boolean,
        autoSpeak: Boolean,
        rate: Float,
        pitch: Float,
        elevenLabsApiKey: String = _uiState.value.elevenLabsApiKey
    ) {
        if (!isVoiceEnabled) {
            stopVoiceInput()
            stopAudio()
        }
        _uiState.update {
            it.copy(
                isVoiceEnabled = isVoiceEnabled,
                autoSpeak = autoSpeak,
                speechRate = rate,
                speechPitch = pitch,
                elevenLabsApiKey = elevenLabsApiKey
            )
        }
    }

    // -------------------------------------------------------------------------
    // Geçmiş yönetimi
    // -------------------------------------------------------------------------

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_uiState.value.currentConversationId == id) {
                _uiState.update { it.copy(currentConversationId = null, messages = emptyList()) }
                val remaining = conversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first().id)
                } else {
                    val newId = repository.createNewConversation("Yeni Sesli Sohbet")
                    selectConversation(newId)
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _uiState.update { it.copy(currentConversationId = null, messages = emptyList()) }
            val newId = repository.createNewConversation("Yeni Sesli Sohbet")
            selectConversation(newId)
        }
    }

    fun getMessagesForConversation(conversationId: Long) =
        repository.getMessagesForConversation(conversationId)

    fun dismissError() {
        _uiState.update { it.copy(userErrorMessage = null) }
    }

    @SuppressLint("EmptySuperCall")
    override fun onCleared() {
        super.onCleared()
        silenceTimerJob?.cancel()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
