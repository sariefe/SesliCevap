package com.example.data.repository

import com.example.data.local.entity.ConversationHistoryEntity
import com.example.domain.model.AnalyticsSummary
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConversationSession
import kotlinx.coroutines.flow.Flow

interface VoiceAssistantRepository {

    fun getConversations(): Flow<List<ConversationSession>>

    suspend fun getAllConversationsOnce(): List<ConversationSession>

    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>>

    fun getConversationHistory(): Flow<List<ConversationHistoryEntity>>

    suspend fun getConversationById(id: Long): ConversationSession?

    suspend fun createNewConversation(title: String = "Yeni Sesli Sohbet"): Long

    suspend fun sendUserPrompt(
        conversationId: Long,
        prompt: String,
        speechDurationMs: Long = 0L
    ): Result<ChatMessage>

    suspend fun sendUserPromptStreaming(
        conversationId: Long,
        prompt: String,
        speechDurationMs: Long = 0L,
        persona: String = "Genel Dostane Asistan",
        onSentenceReady: (String) -> Unit
    ): Result<ChatMessage>

    suspend fun deleteConversation(conversationId: Long)

    suspend fun clearAllHistory()

    fun getAnalytics(): Flow<AnalyticsSummary>
}
