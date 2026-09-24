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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
            context = context
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

    @Test
    fun `initialization creates new conversation when none exist`() = runTest {
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNotNull("currentConversationId should not be null", state.currentConversationId)
        assertEquals("Yeni Sesli Sohbet", state.currentConversationTitle)
    }

    @Test
    fun `initialization loads existing conversation when available`() = runTest {
        val existingSession = ConversationSession(id = 42L, title = "Old Chat", lastUpdated = 12345L, messageCount = 2)
        fakeRepository.setInitialConversations(listOf(existingSession))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val newViewModel = VoiceAssistantViewModel(
            repository = fakeRepository,
            speechManager = speechManager,
            ttsManager = elevenLabsTtsManager,
            context = context
        )
        testScheduler.advanceUntilIdle()

        assertEquals(42L, newViewModel.uiState.value.currentConversationId)
        assertEquals("Old Chat", newViewModel.uiState.value.currentConversationTitle)
    }

    @Test
    fun `createNewConversation creates and selects new conversation`() = runTest {
        viewModel.createNewConversation()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Yeni Sesli Sohbet", state.currentConversationTitle)
        assertNotNull(state.currentConversationId)
    }

    @Test
    fun `sendTextMessage processes prompt successfully`() = runTest {
        viewModel.sendTextMessage("Hello AI")
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isProcessingAi)
        assertNull(viewModel.uiState.value.userErrorMessage)
    }

    @Test
    fun `deleteConversation clears active and selects fallback`() = runTest {
        viewModel.createNewConversation()
        testScheduler.advanceUntilIdle()
        val currentId = viewModel.uiState.value.currentConversationId!!

        viewModel.deleteConversation(currentId)
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.currentConversationId)
    }

    @Test
    fun `clearAllHistory resets and creates new default`() = runTest {
        viewModel.clearAllHistory()
        testScheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.currentConversationId)
        assertEquals("Yeni Sesli Sohbet", viewModel.uiState.value.currentConversationTitle)
    }
}

private class FakeVoiceRepository : VoiceAssistantRepository {
    private val conversationsFlow = MutableStateFlow<List<ConversationSession>>(emptyList())
    private val conversationsList = mutableListOf<ConversationSession>()
    private var nextId = 1L

    fun setInitialConversations(list: List<ConversationSession>) {
        conversationsList.clear()
        conversationsList.addAll(list)
        conversationsFlow.value = list.toList()
        if (list.isNotEmpty()) {
            nextId = list.maxOf { it.id } + 1L
        }
    }

    override fun getConversations(): Flow<List<ConversationSession>> = conversationsFlow

    override suspend fun getAllConversationsOnce(): List<ConversationSession> = conversationsList.toList()

    override fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> = flowOf(emptyList())

    override fun getConversationHistory(): Flow<List<ConversationHistoryEntity>> = flowOf(emptyList())

    override suspend fun getConversationById(id: Long): ConversationSession? = conversationsList.find { it.id == id }

    override suspend fun createNewConversation(title: String): Long {
        val id = nextId++
        val newSession = ConversationSession(id = id, title = title, lastUpdated = System.currentTimeMillis(), messageCount = 0)
        conversationsList.add(0, newSession)
        conversationsFlow.value = conversationsList.toList()
        return id
    }

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

    override suspend fun sendUserPromptStreaming(
        conversationId: Long,
        prompt: String,
        speechDurationMs: Long,
        persona: String,
        onSentenceReady: (String) -> Unit
    ): Result<ChatMessage> {
        onSentenceReady("Test streaming sentence")
        return Result.success(
            ChatMessage(
                id = 100L,
                conversationId = conversationId,
                sender = MessageSender.AI,
                text = "Test response"
            )
        )
    }

    override suspend fun deleteConversation(conversationId: Long) {
        conversationsList.removeAll { it.id == conversationId }
        conversationsFlow.value = conversationsList.toList()
    }

    override suspend fun clearAllHistory() {
        conversationsList.clear()
        conversationsFlow.value = emptyList()
    }

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
