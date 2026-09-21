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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceAssistantRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val geminiApiService: GeminiApiService,
    private val conversationHistoryDao: ConversationHistoryDao? = null,
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
                dominantSentiment = "Nötr",
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

            // 1. Kullanıcı mesajını kaydet
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
            Timber.i(
                "Saved user message to DB: %s (Category: %s, Sentiment: %s)",
                maskSensitive(prompt), userCategory, userSentiment
            )

            // Konuşma başlığını güncelle (ilk mesajsa)
            val currentConv = conversationDao.getConversationByIdOnce(conversationId)
            currentConv?.let { conv ->
                val isFirstMsg = conv.messageCount == 0 || conv.title.startsWith("Yeni Sesli")
                if (isFirstMsg) {
                    val newTitle = if (prompt.length > 32) "${prompt.take(30)}..." else prompt
                    conversationDao.updateConversation(
                        conv.copy(
                            title = newTitle,
                            lastUpdated = System.currentTimeMillis(),
                            messageCount = conv.messageCount + 1,
                            dominantCategory = userCategory,
                            dominantSentiment = userSentiment
                        )
                    )
                } else {
                    conversationDao.updateConversation(
                        conv.copy(
                            lastUpdated = System.currentTimeMillis(),
                            messageCount = conv.messageCount + 1
                        )
                    )
                }
            }

            // 2. Bu konuşmanın tüm geçmişini çek — Gemini'ye bağlam olarak gönderilecek
            val conversationHistory = messageDao
                .getMessagesForConversation(conversationId)
                .first()
                .takeLast(10) // Son 10 mesaj yeterli, token limitini aşmamak için

            // 3. Yapay zekadan cevap al (geçmişle birlikte)
            val aiResponseText = fetchAiAnswer(prompt, conversationHistory)
            val aiSentiment = analyzeSentiment(aiResponseText)

            // 4. Yapay zeka mesajını kaydet
            val aiMessageEntity = MessageEntity(
                conversationId = conversationId,
                sender = MessageSender.AI.name,
                text = aiResponseText,
                timestamp = System.currentTimeMillis(),
                sentiment = aiSentiment,
                category = userCategory
            )
            val aiMsgId = messageDao.insertMessage(aiMessageEntity)
            Timber.i("Saved AI message to DB: id %d", aiMsgId)

            // Konuşma geçmişine kaydet
            conversationHistoryDao?.insertHistory(
                ConversationHistoryEntity(
                    conversationId = conversationId,
                    userInputText = prompt,
                    aiResponse = aiResponseText,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Konuşma istatistiklerini güncelle
            val updatedConv = conversationDao.getConversationByIdOnce(conversationId)
            if (updatedConv != null) {
                conversationDao.updateConversation(
                    updatedConv.copy(
                        lastUpdated = System.currentTimeMillis(),
                        messageCount = updatedConv.messageCount + 1,
                        dominantCategory = userCategory,
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

            val sentimentGroups = messages.groupBy { it.sentiment }
            val sentimentStats = sentimentGroups.asSequence().map { (sentiment, list) ->
                val percentage =
                    if (totalMsgs > 0) (list.size.toFloat() / totalMsgs) * 100f else 0f
                SentimentStat(sentiment = sentiment, count = list.size, percentage = percentage)
            }.sortedByDescending { it.count }.toList()

            val categoryGroups = messages.groupBy { it.category }
            val categoryStats = categoryGroups.asSequence().map { (category, list) ->
                val percentage =
                    if (totalMsgs > 0) (list.size.toFloat() / totalMsgs) * 100f else 0f
                CategoryStat(category = category, count = list.size, percentage = percentage)
            }.sortedByDescending { it.count }.toList()

            val dominantCategory = categoryStats.firstOrNull()?.category ?: "Genel"
            val dominantSentiment = sentimentStats.firstOrNull()?.sentiment ?: "Nötr"

            AnalyticsSummary(
                totalConversations = totalConvs,
                totalMessages = totalMsgs,
                totalUserQueries = totalUserQueries,
                mostUsedCategory = dominantCategory,
                mostUsedSentiment = dominantSentiment,
                dominantCategory = dominantCategory,
                dominantSentiment = dominantSentiment,
                sentimentDistribution = sentimentStats,
                categoryDistribution = categoryStats,
                recentSessions = conversations.asSequence().take(5).map { it.toDomain() }.toList()
            )
        }
    }

    // -------------------------------------------------------------------------
    // Gemini API
    // -------------------------------------------------------------------------

    private suspend fun fetchAiAnswer(
        prompt: String,
        history: List<MessageEntity> = emptyList()
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidApiKey = apiKey.isNotBlank() && !apiKey.contains("placeholder")

        Timber.d("Gemini API key present: %b (masked: %s)", hasValidApiKey, maskSensitive(apiKey))

        if (hasValidApiKey) {
            try {
                // Geçmiş mesajları Gemini formatına çevir
                val historyContents = history.map { msg ->
                    Content(
                        role = if (msg.sender == MessageSender.USER.name) "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                }

                // Son kullanıcı mesajını da ekle
                val allContents = historyContents + Content(
                    role = "user",
                    parts = listOf(Part(text = prompt))
                )

                val request = GeminiRequest(
                    contents = allContents,
                    systemInstruction = Content(
                        parts = listOf(
                            Part(
                                text = """
                                    Sen samimi, sıcak, empatik ve canlı bir Türkçe sesli asistansın.
                                    
                                    ÇOK ÖNEMLİ KONUŞMA KURALLARI:
                                    1. Asla resmi, robotik veya kitabi dil kullanma. Yakın bir arkadaş gibi günlük konuşma dilinde yaz.
                                    2. Cümlelerine insansı düşünce geçişleri ve günlük bağlaçlar ekle:
                                       ("Yani...", "Hımm...", "Aslında bakarsan", "Anladım seni", "Açıkçası...", "Şöyle söyleyeyim")
                                    3. Asla maddeler (* / -), başlıklar (#) veya kalın/eğik kelimeler (** / __) kullanma.
                                    4. Cümleleri kısa tut ve aralara virgül (,) koy. Bu, seslendirilerken doğal nefes duraklaması yaratır.
                                    5. Kullanıcının duygusunu yakala: mutluysa sevin, meraklıysa heyecanla anlat, üzgünse empati kur.
                                    6. Yanıtların en fazla 2-3 akıcı cümleden oluşsun; sesli dinlemeye mükemmel uygunlukta olsun.
                                """.trimIndent()
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.9f,
                        topP = 0.95f,
                        maxOutputTokens = 400
                    )
                )

                val response = geminiApiService.generateContent(apiKey, request)

                // API hata döndürdüyse akıllı yerel yanıta düş
                response.error?.let { err ->
                    Timber.w("Gemini API error %d: %s, falling back to inteligente response", err.code, err.message)
                    return generateIntelligentFallback(prompt)
                }

                val candidateText = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text

                if (!candidateText.isNullOrBlank()) {
                    return candidateText.trim()
                }
            } catch (e: Exception) {
                Timber.w(e, "Gemini API call failed (%s), falling back to inteligente response", e.localizedMessage)
                return generateIntelligentFallback(prompt)
            }
        }

        return generateIntelligentFallback(prompt)
    }

    // -------------------------------------------------------------------------
    // Yerel yedek cevaplar (API yokken veya hata anında)
    // -------------------------------------------------------------------------

    private fun generateIntelligentFallback(prompt: String): String {
        val lower = prompt.lowercase().trim()
        return when {
            lower.contains("merhaba") || lower.contains("selam") ||
                    lower.contains("günaydın") ->
                "Merhaba! Seninle konuşmak çok güzel. Bugün sana nasıl yardımcı olabilirim?"

            lower.contains("nasılsın") || lower.contains("ne haber") ->
                "İyiyim, teşekkürler! Seninle sohbet etmek her zaman keyifli. Sen nasılsın, bugün nasıl geçiyor?"

            lower.contains("kimsin") || lower.contains("sen kimsin") ||
                    lower.contains("nesin") ->
                "Ben sesli konuşmalarını dinleyen ve sana anında yanıt veren bir yapay zeka asistanınım. Sorularını yanıtlamak, fikir üretmek veya sadece sohbet etmek için buradayım."

            lower.contains("saat") || lower.contains("tarih") -> {
                val now = SimpleDateFormat(
                    "HH:mm, dd MMMM yyyy",
                    Locale.forLanguageTag("tr-TR")
                ).format(Date())
                "Şu an saat $now."
            }

            lower.contains("hava") ->
                "Hava durumu için güncel konumuna ihtiyacım var, ama genel olarak söyleyeyim: dışarı çıkmadan önce bir telefona bakmak her zaman iyi fikir!"

            lower.contains("teşekkür") || lower.contains("sağol") ->
                "Ne demek, her zaman! Başka bir şey sormak istersen buradayım."

            lower.contains("görüşürüz") || lower.contains("hoşçakal") ||
                    lower.contains("bay bay") ->
                "Görüşmek üzere! İyi günler dilerim, istediğin zaman tekrar konuşabiliriz."

            else ->
                "Hmm, ilginç bir konu bu. Aslında $prompt hakkında düşününce, pratik adımlarla başlamak her zaman en iyisi. Bu konuda sana daha iyi yardımcı olabilmem için biraz daha anlatır mısın?"
        }
    }

    // -------------------------------------------------------------------------
    // Sınıflandırma & duygu analizi
    // -------------------------------------------------------------------------

    private fun classifyCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("kod") || lower.contains("yazılım") ||
                    lower.contains("fonksiyon") || lower.contains("android") ||
                    lower.contains("kotlin") || lower.contains("api") ||
                    lower.contains("bilgisayar") || lower.contains("yapay zeka") ->
                "Teknoloji & Yazılım"

            lower.contains("nedir") || lower.contains("nasıl") ||
                    lower.contains("öğren") || lower.contains("tarih") ||
                    lower.contains("fizik") || lower.contains("neden") ||
                    lower.contains("kitap") || lower.contains("bilim") ->
                "Bilgi & Öğrenme"

            lower.contains("hedef") || lower.contains("plan") ||
                    lower.contains("görev") || lower.contains("yapılacak") ||
                    lower.contains("iş") || lower.contains("zaman") ||
                    lower.contains("üretkenlik") || lower.contains("not") ->
                "Üretkenlik & Planlama"

            lower.contains("sağlık") || lower.contains("spor") ||
                    lower.contains("diyet") || lower.contains("su") ||
                    lower.contains("egzersiz") || lower.contains("uyku") ||
                    lower.contains("doktor") || lower.contains("beslenme") ->
                "Sağlık & Yaşam"

            lower.contains("merhaba") || lower.contains("selam") ||
                    lower.contains("nasılsın") || lower.contains("günaydın") ||
                    lower.contains("iyi akşamlar") || lower.contains("sohbet") ->
                "Günlük Sohbet"

            else -> "Genel"
        }
    }

    private fun analyzeSentiment(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("harika") || lower.contains("süper") ||
                    lower.contains("güzel") || lower.contains("teşekkür") ||
                    lower.contains("mutlu") || lower.contains("başardım") ||
                    lower.contains("sevindim") || lower.contains("iyi") ||
                    lower.contains("mükemmel") -> "Olumlu"

            lower.contains("nedir") || lower.contains("nasıl") ||
                    lower.contains("merak") || lower.contains("acaba") ||
                    lower.contains("kim") || lower.contains("neden") -> "Meraklı"

            lower.contains("düşün") || lower.contains("mantık") ||
                    lower.contains("fikir") || lower.contains("analiz") ||
                    lower.contains("strateji") || lower.contains("önemli") -> "Düşünceli"

            lower.contains("kötü") || lower.contains("zor") ||
                    lower.contains("endişe") || lower.contains("stres") ||
                    lower.contains("hata") || lower.contains("problem") -> "Endişeli"

            else -> "Nötr"
        }
    }

    // -------------------------------------------------------------------------
    // Güvenlik: hassas log maskeleme
    // -------------------------------------------------------------------------

    private fun maskSensitive(value: String): String {
        if (value.isBlank()) return "***"
        val visible = value.take(4)
        return "$visible***"
    }

    // -------------------------------------------------------------------------
    // Entity → Domain dönüşümleri
    // -------------------------------------------------------------------------

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
