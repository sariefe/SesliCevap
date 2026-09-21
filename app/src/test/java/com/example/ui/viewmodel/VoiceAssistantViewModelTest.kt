package com.example.ui.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.ConversationHistoryEntity
import com.example.data.remote.ElevenLabsApiService
import com.example.data.remote.model.ElevenLabsTtsRequest
import com.example.data.repository.VoiceAssistantRepository
import com.example.data.speech.ElevenLabsTtsManager
import com.example.data.speech.SpeechRecognitionManager
import com.example.data.speech.TextToSpeechManager
import com.example.domain.model.AnalyticsSummary
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConversationSession
import com.example.domain.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceAssistantViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeVoiceRepository
    private lateinit var speechManager: SpeechRecognitionManager
    private lateinit var elevenLabsTtsManager: ElevenLabsTtsManager
    private lateinit var viewModel: VoiceAssistantViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakeRepository = FakeVoiceRepository()
        speechManager = SpeechRecognitionManager(context)
        val nativeTts = TextToSpeechManager(context)
        val fakeElevenLabsApi = object : ElevenLabsApiService {
            override suspend fun generateSpeechStream(
                voiceId: String,
                apiKey: String,
                request: ElevenLabsTtsRequest
            ): ResponseBody {
                throw UnsupportedOperationException("Fake ElevenLabs API for test")
            }
        }
        elevenLabsTtsManager = ElevenLabsTtsManager(context, fakeElevenLabsApi, nativeTts)

        viewModel = VoiceAssistantViewModel(
            repository = fakeRepository,
            speechManager = speechManager,
            ttsManager = elevenLabsTtsManager,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState has default voice settings`() {
        val state = viewModel.uiState.value
        assertTrue("isVoiceEnabled should default to true", state.isVoiceEnabled)
        assertTrue("autoSpeak should default to true", state.autoSpeak)
        assertEquals(1.0f, state.speechRate, 0.01f)
        assertEquals(1.0f, state.speechPitch, 0.01f)
        assertNull("userErrorMessage should be null initially", state.userErrorMessage)
    }

    @Test
    fun `toggleVoiceEnabled toggles isVoiceEnabled`() {
        viewModel.toggleVoiceEnabled()
        assertFalse("isVoiceEnabled should be false after toggle", viewModel.uiState.value.isVoiceEnabled)

        viewModel.toggleVoiceEnabled()
        assertTrue("isVoiceEnabled should be true after second toggle", viewModel.uiState.value.isVoiceEnabled)
    }

    @Test
    fun `updateSettings updates settings correctly`() {
        viewModel.updateSettings(
            isVoiceEnabled = false,
            autoSpeak = false,
            rate = 1.2f,
            pitch = 0.8f
        )

        val state = viewModel.uiState.value
        assertFalse(state.isVoiceEnabled)
        assertFalse(state.autoSpeak)
        assertEquals(1.2f, state.speechRate, 0.01f)
        assertEquals(0.8f, state.speechPitch, 0.01f)
    }

    @Test
    fun `dismissError clears error message`() {
        viewModel.dismissError()
        assertNull(viewModel.uiState.value.userErrorMessage)
    }
}

private class FakeVoiceRepository : VoiceAssistantRepository {
    private val conversationsFlow = MutableStateFlow<List<ConversationSession>>(emptyList())

    override fun getConversations(): Flow<List<ConversationSession>> = conversationsFlow

    override fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> = flowOf(emptyList())

    override fun getConversationHistory(): Flow<List<ConversationHistoryEntity>> = flowOf(emptyList())

    override suspend fun getConversationById(id: Long): ConversationSession? = null

    override suspend fun createNewConversation(title: String): Long = 1L

    override suspend fun sendUserPrompt(
        conversationId: Long,
        prompt: String,
        speechDurationMs: Long
    ): Result<ChatMessage> {
        return Result.success(
            ChatMessage(
                id = 100L,
                conversationId = conversationId,
                sender = MessageSender.AI,
                text = "Test response"
            )
        )
    }

    override suspend fun deleteConversation(conversationId: Long) {}

    override suspend fun clearAllHistory() {}

    override fun getAnalytics(): Flow<AnalyticsSummary> {
        return flowOf(
            AnalyticsSummary(
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
    }
}
