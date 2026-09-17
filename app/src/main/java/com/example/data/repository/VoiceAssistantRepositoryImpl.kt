package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.ConversationHistoryDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.ConversationHistoryEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.remote.GeminiApiService
import com.example.data.remote.model.Content
import com.example.data.remote.model.GeminiRequest
import com.example.data.remote.model.GenerationConfig
import com.example.data.remote.model.Part
import com.example.domain.model.AnalyticsSummary
import com.example.domain.model.CategoryStat
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConversationSession
import com.example.domain.model.MessageSender
import com.example.domain.model.SentimentStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

class VoiceAssistantRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val geminiApiService: GeminiApiService,
    private val conversationHistoryDao: ConversationHistoryDao? = null
) : VoiceAssistantRepository {

    override fun getConversations(): Flow<List<ConversationSession>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getConversationHistory(): Flow<List<ConversationHistoryEntity>> {
        return conversationHistoryDao?.getAllHistory() ?: flowOf(emptyList())
    }

    override suspend fun getConversationById(id: Long): ConversationSession? {
        return withContext(Dispatchers.IO) {
            conversationDao.getConversationByIdOnce(id)?.toDomain()
        }
    }

    override suspend fun createNewConversation(title: String): Long {
        return withContext(Dispatchers.IO) {
            val newEntity = ConversationEntity(
                title = title,
                createdAt = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis(),
                messageCount = 0,
                dominantCategory = "Genel",
                dominantSentiment = "Nötr"
            )
            val id = conversationDao.insertConversation(newEntity)
            Timber.d("Created new conversation session with id: %d", id)
            id
        }
    }

    override suspend fun sendUserPrompt(
        conversationId: Long,
        prompt: String,
        speechDurationMs: Long
    ): Result<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val userCategory = classifyCategory(prompt)
            val userSentiment = analyzeSentiment(prompt)

            // 1. Save User message to Room
            val userMessageEntity = MessageEntity(
                conversationId = conversationId,
                sender = MessageSender.USER.name,
                text = prompt,
                timestamp = System.currentTimeMillis(),
                sentiment = userSentiment,
                category = userCategory,
                audioDurationMs = speechDurationMs
            )
            messageDao.insertMessage(userMessageEntity)
            Timber.i("Saved user message to DB: %s (Category: %s, Sentiment: %s)", prompt.take(30), userCategory, userSentiment)

            // Update conversation title if first message
            val currentConv = conversationDao.getConversationByIdOnce(conversationId)
            if (currentConv != null && (currentConv.messageCount == 0 || currentConv.title.startsWith("Yeni Sesli"))) {
                val newTitle = if (prompt.length > 32) "${prompt.take(30)}..." else prompt
                conversationDao.updateConversation(
                    currentConv.copy(
                        title = newTitle,
                        lastUpdated = System.currentTimeMillis(),
                        messageCount = currentConv.messageCount + 1,
                        dominantCategory = userCategory,
                        dominantSentiment = userSentiment
                    )
                )
            } else if (currentConv != null) {
                conversationDao.updateConversation(
                    currentConv.copy(
                        lastUpdated = System.currentTimeMillis(),
                        messageCount = currentConv.messageCount + 1
                    )
                )
            }

            // 2. Fetch AI Response
            val aiResponseText = fetchAiAnswer(prompt)
            val aiCategory = userCategory
            val aiSentiment = analyzeSentiment(aiResponseText)

            // 3. Save AI message to Room
            val aiMessageEntity = MessageEntity(
                conversationId = conversationId,
                sender = MessageSender.AI.name,
                text = aiResponseText,
                timestamp = System.currentTimeMillis(),
                sentiment = aiSentiment,
                category = aiCategory
            )
            val aiMsgId = messageDao.insertMessage(aiMessageEntity)
            Timber.i("Saved AI message to DB: id %d", aiMsgId)

            // Save paired user prompt, AI response, and timestamp in ConversationHistoryEntity
            conversationHistoryDao?.insertHistory(
                ConversationHistoryEntity(
                    conversationId = conversationId,
                    userInputText = prompt,
                    aiResponse = aiResponseText,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Update conversation stats
            val updatedConv = conversationDao.getConversationByIdOnce(conversationId)
            if (updatedConv != null) {
                conversationDao.updateConversation(
                    updatedConv.copy(
                        lastUpdated = System.currentTimeMillis(),
                        messageCount = updatedConv.messageCount + 1,
                        dominantCategory = aiCategory,
                        dominantSentiment = aiSentiment
                    )
                )
            }

            val resultDomain = aiMessageEntity.copy(id = aiMsgId).toDomain()
            Result.success(resultDomain)
        } catch (e: Exception) {
            Timber.e(e, "Failed to process user prompt")
            Result.failure(e)
        }
    }

    override suspend fun deleteConversation(conversationId: Long) {
        withContext(Dispatchers.IO) {
            conversationHistoryDao?.deleteHistoryForConversation(conversationId)
            messageDao.deleteMessagesForConversation(conversationId)
            conversationDao.deleteConversationById(conversationId)
            Timber.d("Deleted conversation id: %d", conversationId)
        }
    }

    override suspend fun clearAllHistory() {
        withContext(Dispatchers.IO) {
            conversationHistoryDao?.clearAllHistory()
            messageDao.clearAllMessages()
            conversationDao.clearAllConversations()
            Timber.d("Cleared all conversation history")
        }
    }

    override fun getAnalytics(): Flow<AnalyticsSummary> {
        return combine(
            conversationDao.getAllConversations(),
            messageDao.getAllMessages()
        ) { conversations, messages ->
            val totalConvs = conversations.size
            val totalMsgs = messages.size
            val userMessages = messages.filter { it.sender == MessageSender.USER.name }
            val totalUserQueries = userMessages.size

            // Sentiment distribution
            val sentimentGroups = messages.groupBy { it.sentiment }
            val sentimentStats = sentimentGroups.map { (sentiment, list) ->
                val percentage = if (totalMsgs > 0) (list.size.toFloat() / totalMsgs) * 100f else 0f
                SentimentStat(
                    sentiment = sentiment,
                    count = list.size,
                    percentage = percentage
                )
            }.sortedByDescending { it.count }

            // Category distribution
            val categoryGroups = messages.groupBy { it.category }
            val categoryStats = categoryGroups.map { (category, list) ->
                val percentage = if (totalMsgs > 0) (list.size.toFloat() / totalMsgs) * 100f else 0f
                CategoryStat(
                    category = category,
                    count = list.size,
                    percentage = percentage
                )
            }.sortedByDescending { it.count }

            val dominantCategory = categoryStats.firstOrNull()?.category ?: "Genel"
            val dominantSentiment = sentimentStats.firstOrNull()?.sentiment ?: "Nötr"

            AnalyticsSummary(
                totalConversations = totalConvs,
                totalMessages = totalMsgs,
                totalUserQueries = totalUserQueries,
                dominantCategory = dominantCategory,
                dominantSentiment = dominantSentiment,
                sentimentDistribution = sentimentStats,
                categoryDistribution = categoryStats,
                recentSessions = conversations.take(5).map { it.toDomain() }
            )
        }
    }

    private suspend fun fetchAiAnswer(prompt: String): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidApiKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidApiKey) {
            try {
                val request = GeminiRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(Part(text = prompt)),
                            role = "user"
                        )
                    ),
                    systemInstruction = Content(
                        parts = listOf(
                            Part(
                                text = "Sen Türkçe konuşan samimi, akıllı, net ve sesli yanıt için optimize edilmiş bir yapay zeka asistanısın. " +
                                        "Cevapların anlaşılır, akıcı ve sesli dinlemeye uygun olsun. Karmaşık semboller ve aşırı uzun listelerden kaçın."
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.7f,
                        topP = 0.95f,
                        maxOutputTokens = 800
                    )
                )
                val response = geminiApiService.generateContent(apiKey, request)
                val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!candidateText.isNullOrBlank()) {
                    return candidateText.trim()
                }
            } catch (e: Exception) {
                Timber.w(e, "Gemini API call failed, falling back to smart local response engine")
            }
        }

        // Smart local assistant fallback if API key is not yet configured or network unreachable
        return generateIntelligentFallback(prompt)
    }

    private fun generateIntelligentFallback(prompt: String): String {
        val lower = prompt.lowercase().trim()
        return when {
            lower.contains("merhaba") || lower.contains("selam") || lower.contains("günaydın") -> {
                "Merhaba! Sesli asistanınıza hoş geldiniz. Size nasıl yardımcı olabilirim? Merak ettiğiniz bir konuyu sorabilir veya fikir danışabilirsiniz."
            }
            lower.contains("nasılsın") || lower.contains("ne haber") -> {
                "Harikayım, teşekkür ederim! Sizinle konuşmak çok güzel. Bugün hangi konuda birlikte çalışmak veya sohbet etmek istersiniz?"
            }
            lower.contains("kimsin") || lower.contains("sen kimsin") || lower.contains("nesin") -> {
                "Ben sesli ve yazılı olarak size anında yanıt verebilen, konuşmalarınızı analiz edip geçmişe dönük özetleyen akıllı yapay zeka asistanınızım."
            }
            lower.contains("saat") || lower.contains("tarih") -> {
                val now = java.text.SimpleDateFormat("HH:mm, dd MMMM yyyy", java.util.Locale.forLanguageTag("tr-TR")).format(java.util.Date())
                "Şu anki zaman: $now."
            }
            lower.contains("hava") -> {
                "Bulunduğunuz bölgede hava durumu hakkında güncel bilgi almak için lütfen konumunuzu kontrol edin. Genel olarak ılık ve güzel bir gün görünüyor!"
            }
            lower.contains("nedir") || lower.contains("nasıl") || lower.contains("açıkla") || lower.contains("bilgi") -> {
                "Sorduğunuz konu oldukça ilgi çekici! $prompt hakkında temel olarak şunları söyleyebilirim: Konunun özünde sistematik bir yaklaşım ve doğru analiz yatmaktadır. Daha derin bir inceleme için spesifik detayları konuşabiliriz."
            }
            lower.contains("teşekkür") || lower.contains("sağol") -> {
                "Rica ederim! Her zaman yardıma hazırım. Başka bir sorunuz veya konuşmak istediğiniz bir konu olursa dinliyorum."
            }
            lower.contains("görüşürüz") || lower.contains("hoşçakal") || lower.contains("bay bay") -> {
                "Görüşmek üzere! Kendinize çok iyi bakın, dilediğiniz an tekrar konuşabiliriz."
            }
            else -> {
                "\"$prompt\" ifadenizi dikkatle analiz ettim. Düşünceniz oldukça değerli. Bu doğrultuda adımlarınızı planlayabilir, gerektiğinde daha detaylı analiz yapabiliriz. Size bu konuda nasıl yardımcı olmamı istersiniz?"
            }
        }
    }

    private fun classifyCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("kod") || lower.contains("yazılım") || lower.contains("fonksiyon") ||
                    lower.contains("android") || lower.contains("kotlin") || lower.contains("api") ||
                    lower.contains("bilgisayar") || lower.contains("yapay zeka") -> "Teknoloji & Yazılım"
            lower.contains("nedir") || lower.contains("nasıl") || lower.contains("öğren") ||
                    lower.contains("tarih") || lower.contains("fizik") || lower.contains("neden") ||
                    lower.contains("kitap") || lower.contains("bilim") -> "Bilgi & Öğrenme"
            lower.contains("hedef") || lower.contains("plan") || lower.contains("görev") ||
                    lower.contains("yapılacak") || lower.contains("iş") || lower.contains("zaman") ||
                    lower.contains("üretkenlik") || lower.contains("not") -> "Üretkenlik & Planlama"
            lower.contains("sağlık") || lower.contains("spor") || lower.contains("diyet") ||
                    lower.contains("su") || lower.contains("egzersiz") || lower.contains("uyku") ||
                    lower.contains("doktor") || lower.contains("beslenme") -> "Sağlık & Yaşam"
            lower.contains("merhaba") || lower.contains("selam") || lower.contains("nasılsın") ||
                    lower.contains("günaydın") || lower.contains("iyi akşamlar") || lower.contains("sohbet") -> "Günlük Sohbet"
            else -> "Genel"
        }
    }

    private fun analyzeSentiment(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("harika") || lower.contains("süper") || lower.contains("güzel") ||
                    lower.contains("teşekkür") || lower.contains("mutlu") || lower.contains("başardım") ||
                    lower.contains("sevindim") || lower.contains("iyi") || lower.contains("mükemmel") -> "Olumlu"
            lower.contains("nedir") || lower.contains("nasıl") || lower.contains("merak") ||
                    lower.contains("acaba") || lower.contains("kim") || lower.contains("neden") -> "Meraklı"
            lower.contains("düşün") || lower.contains("mantık") || lower.contains("fikir") ||
                    lower.contains("analiz") || lower.contains("strateji") || lower.contains("önemli") -> "Düşünceli"
            lower.contains("kötü") || lower.contains("zor") || lower.contains("endişe") ||
                    lower.contains("stres") || lower.contains("hata") || lower.contains("problem") -> "Endişeli"
            else -> "Nötr"
        }
    }

    private fun ConversationEntity.toDomain() = ConversationSession(
        id = id,
        title = title,
        createdAt = createdAt,
        lastUpdated = lastUpdated,
        messageCount = messageCount,
        dominantCategory = dominantCategory,
        dominantSentiment = dominantSentiment,
        summary = summary
    )

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        conversationId = conversationId,
        sender = if (sender == MessageSender.USER.name) MessageSender.USER else MessageSender.AI,
        text = text,
        timestamp = timestamp,
        sentiment = sentiment,
        category = category,
        audioDurationMs = audioDurationMs
    )
}
