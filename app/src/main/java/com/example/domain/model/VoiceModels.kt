package com.example.domain.model

data class ChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sentiment: String = "Nötr",
    val category: String = "Genel",
    val audioDurationMs: Long = 0L
)

enum class MessageSender {
    USER,
    AI
}

data class ConversationSession(
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val messageCount: Int = 0,
    val dominantCategory: String = "Genel",
    val dominantSentiment: String = "Nötr",
    val summary: String = ""
)

data class AnalyticsSummary(
    val totalConversations: Int = 0,
    val totalMessages: Int = 0,
    val totalUserQueries: Int = 0,
    val dominantCategory: String = "Genel",
    val dominantSentiment: String = "Nötr",
    val sentimentDistribution: List<SentimentStat> = emptyList(),
    val categoryDistribution: List<CategoryStat> = emptyList(),
    val recentSessions: List<ConversationSession> = emptyList()
)

data class SentimentStat(
    val sentiment: String,
    val count: Int,
    val percentage: Float
)

data class CategoryStat(
    val category: String,
    val count: Int,
    val percentage: Float
)
