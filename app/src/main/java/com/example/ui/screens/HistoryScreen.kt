package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConversationSession
import com.example.domain.model.MessageSender
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Tarih filtreleme seçenekleri
 */
enum class DateFilterOption(val label: String) {
    ALL("Tüm Zamanlar"),
    TODAY("Bugün"),
    YESTERDAY("Dün"),
    LAST_7_DAYS("Son 7 Gün"),
    LAST_30_DAYS("Son 30 Gün")
}

/**
 * Sıralama seçenekleri
 */
enum class HistorySortOption(val label: String) {
    DATE_DESC("En Yeni İlk"),
    DATE_ASC("En Eski İlk"),
    MESSAGE_COUNT("Mesaj Sayısı"),
    TITLE("Başlığa Göre")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    conversations: List<ConversationSession>,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onClearAllHistory: () -> Unit,
    onPlayAudio: ((String) -> Unit)? = null,
    getMessagesForConversation: (Long) -> Flow<List<ChatMessage>>,
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDateFilter by rememberSaveable { mutableStateOf(DateFilterOption.ALL) }
    var selectedSortOption by rememberSaveable { mutableStateOf(HistorySortOption.DATE_DESC) }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSentimentFilter by rememberSaveable { mutableStateOf<String?>(null) }

    var conversationToDelete by remember { mutableStateOf<ConversationSession?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Detay BottomSheet için seçili sohbet
    var previewConversation by remember { mutableStateOf<ConversationSession?>(null) }

    // Benzersiz kategoriler ve duygular
    val categories = remember(conversations) {
        conversations.map { it.dominantCategory }.filter { it.isNotBlank() }.distinct()
    }
    val sentiments = remember(conversations) {
        conversations.map { it.dominantSentiment }.filter { it.isNotBlank() }.distinct()
    }

    // Filtrelenmiş ve sıralanmış konuşmalar
    val filteredConversations = remember(
        conversations,
        searchQuery,
        selectedDateFilter,
        selectedCategoryFilter,
        selectedSentimentFilter,
        selectedSortOption
    ) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Tarih sınırları hesabı
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStart = calendar.timeInMillis
        val yesterdayEnd = todayStart

        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

        var list = conversations.filter { conv ->
            // 1. İçerik ve Başlık arama filtresi
            val query = searchQuery.trim().lowercase(Locale.forLanguageTag("tr-TR"))
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                conv.title.lowercase(Locale.forLanguageTag("tr-TR")).contains(query) ||
                        conv.dominantCategory.lowercase(Locale.forLanguageTag("tr-TR")).contains(query) ||
                        conv.dominantSentiment.lowercase(Locale.forLanguageTag("tr-TR")).contains(query)
            }

            // 2. Tarih filtresi
            val matchesDate = when (selectedDateFilter) {
                DateFilterOption.ALL -> true
                DateFilterOption.TODAY -> conv.lastUpdated >= todayStart
                DateFilterOption.YESTERDAY -> conv.lastUpdated in yesterdayStart until yesterdayEnd
                DateFilterOption.LAST_7_DAYS -> conv.lastUpdated >= sevenDaysAgo
                DateFilterOption.LAST_30_DAYS -> conv.lastUpdated >= thirtyDaysAgo
            }

            // 3. Kategori filtresi
            val matchesCategory = selectedCategoryFilter == null || conv.dominantCategory == selectedCategoryFilter

            // 4. Duygu filtresi
            val matchesSentiment = selectedSentimentFilter == null || conv.dominantSentiment == selectedSentimentFilter

            matchesSearch && matchesDate && matchesCategory && matchesSentiment
        }

        // Sıralama
        list = when (selectedSortOption) {
            HistorySortOption.DATE_DESC -> list.sortedByDescending { it.lastUpdated }
            HistorySortOption.DATE_ASC -> list.sortedBy { it.lastUpdated }
            HistorySortOption.MESSAGE_COUNT -> list.sortedByDescending { it.messageCount }
            HistorySortOption.TITLE -> list.sortedBy { it.title.lowercase(Locale.forLanguageTag("tr-TR")) }
        }

        list
    }

    val hasActiveFilters = searchQuery.isNotBlank() ||
            selectedDateFilter != DateFilterOption.ALL ||
            selectedCategoryFilter != null ||
            selectedSentimentFilter != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Konuşma Geçmişi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredConversations.size} / ${conversations.size} konuşma listeleniyor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (conversations.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllDialog = true },
                            modifier = Modifier.testTag("clear_all_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Tüm Geçmişi Temizle",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("history_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Arama ve filtreleme çubuğu
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Arama Girişi
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_input"),
                        placeholder = { Text("Konuşmalarda ve başlıklarda ara...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Ara",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("clear_search_query_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Temizle"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Tarih Bazlı Filtreleme Butonları (Yatay Kaydırılabilir)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tarih Aralığı",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(DateFilterOption.values()) { option ->
                                FilterChip(
                                    selected = selectedDateFilter == option,
                                    onClick = { selectedDateFilter = option },
                                    label = { Text(option.label, fontSize = 12.sp) },
                                    leadingIcon = if (selectedDateFilter == option) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("date_filter_${option.name}")
                                )
                            }
                        }
                    }

                    // Sıralama & Kategori Seçenekleri
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Sıralama ve Kategoriler",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (hasActiveFilters) {
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedDateFilter = DateFilterOption.ALL
                                        selectedCategoryFilter = null
                                        selectedSentimentFilter = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        "Filtreleri Sıfırla",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // Sıralama Çipleri
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(HistorySortOption.values()) { sortOpt ->
                                FilterChip(
                                    selected = selectedSortOption == sortOpt,
                                    onClick = { selectedSortOption = sortOpt },
                                    label = { Text(sortOpt.label, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("sort_${sortOpt.name}")
                                )
                            }
                        }

                        // Varsa Kategoriler
                        if (categories.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == null,
                                        onClick = { selectedCategoryFilter = null },
                                        label = { Text("Tüm Konular", fontSize = 12.sp) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                items(categories) { cat ->
                                    FilterChip(
                                        selected = selectedCategoryFilter == cat,
                                        onClick = {
                                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                                        },
                                        label = { Text(cat, fontSize = 12.sp) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        ),
                                        modifier = Modifier.testTag("category_chip_$cat")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Liste Elemanları veya Boş Durum
            if (filteredConversations.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .testTag("empty_history_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasActiveFilters) Icons.Default.Search else Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (hasActiveFilters) "Filtreye uygun konuşma bulunamadı" else "Henüz bir konuşma geçmişi yok",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (hasActiveFilters)
                                    "Arama teriminizi veya tarih aralığını değiştirerek tekrar deneyin."
                                else
                                    "Sesli asistan ile konuşmaya başladığınızda tüm diyaloglarınız Room veritabanında güvenle saklanır.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (hasActiveFilters) {
                                Spacer(modifier = Modifier.height(16.dp))
                                TextButton(
                                    onClick = {
                                        searchQuery = ""
                                        selectedDateFilter = DateFilterOption.ALL
                                        selectedCategoryFilter = null
                                        selectedSentimentFilter = null
                                    }
                                ) {
                                    Text("Filtreleri Temizle")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredConversations,
                    key = { it.id }
                ) { conv ->
                    HistoryItemCard(
                        session = conv,
                        searchHighlight = searchQuery,
                        onClick = { onSelectConversation(conv.id) },
                        onPreview = { previewConversation = conv },
                        onDelete = { conversationToDelete = conv }
                    )
                }
            }
        }
    }

    // Sohbet Önizleme ve İçerik İnceleme Bottom Sheet
    previewConversation?.let { session ->
        val messagesFlow = remember(session.id) { getMessagesForConversation(session.id) }
        val messages by messagesFlow.collectAsState(initial = emptyList())
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { previewConversation = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ConversationDetailBottomSheetContent(
                session = session,
                messages = messages,
                onOpenFullChat = {
                    previewConversation = null
                    onSelectConversation(session.id)
                },
                onPlayAudio = onPlayAudio,
                onClose = { previewConversation = null }
            )
        }
    }

    // Tekli Silme Onay Penceresi
    conversationToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Konuşmayı Sil") },
            text = {
                Text("\"${session.title}\" başlıklı konuşma ve tüm sesli mesaj kayıtları veritabanından kalıcı olarak silinecektir.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteConversation(session.id)
                        conversationToDelete = null
                    }
                ) {
                    Text("Evet, Sil", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Tüm Geçmişi Temizleme Onay Penceresi
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Tüm Geçmişi Temizle") },
            text = {
                Text("Room veritabanındaki tüm konuşmalar, yapay zeka çıkarımları ve sesli mesajlar silinecektir. Bu işlem geri alınamaz.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllHistory()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Tümünü Temizle", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

/**
 * Konuşma Geçmiş Kartı
 */
@Composable
private fun HistoryItemCard(
    session: ConversationSession,
    searchHighlight: String,
    onClick: () -> Unit,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatted = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(session.lastUpdated))

    val sentimentColor = when (session.dominantSentiment) {
        "Olumlu" -> Color(0xFF10B981)
        "Meraklı" -> Color(0xFF06B6D4)
        "Düşünceli" -> Color(0xFF8B5CF6)
        "Endişeli" -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_card_${session.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Başlık & İkon
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = dateFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Aksiyonlar (Önizleme ve Silme)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("preview_conv_${session.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Detayları Gör",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_conv_${session.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alt Bilgi Etiketleri (Mesaj Sayısı, Kategori, Duygu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mesaj Sayısı Rozeti
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "${session.messageCount} mesaj",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Kategori Rozeti
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = session.dominantCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                // Duygu Rozeti
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = sentimentColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mood,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = sentimentColor
                        )
                        Text(
                            text = session.dominantSentiment,
                            style = MaterialTheme.typography.labelSmall,
                            color = sentimentColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Konuşma Detayını ve Mesajlarını İnceleme BottomSheet İçeriği
 */
@Composable
private fun ConversationDetailBottomSheetContent(
    session: ConversationSession,
    messages: List<ChatMessage>,
    onOpenFullChat: () -> Unit,
    onPlayAudio: ((String) -> Unit)?,
    onClose: () -> Unit
) {
    val dateFormatted = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(session.lastUpdated))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Üst Başlık ve Kapat Butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$dateFormatted • ${session.messageCount} Mesaj",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Clear, contentDescription = "Kapat")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Konuşmaya Git Butonu
        TextButton(
            onClick = onOpenFullChat,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_in_chat_btn")
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bu Konuşmayı Sesli Asistanda Aç ve Devam Et")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Mesaj Listesi
        Text(
            text = "Kayıtlı Mesaj Akışı (${messages.size})",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.sender == MessageSender.USER
                    val timeStr = SimpleDateFormat("HH:mm", Locale.forLanguageTag("tr-TR")).format(Date(msg.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isUser) Icons.Default.Person else Icons.Default.SmartToy,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = if (isUser) "Siz (Kullanıcı)" else "Sesli Cevap AI",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!isUser && onPlayAudio != null) {
                                        IconButton(
                                            onClick = { onPlayAudio(msg.text) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = "Seslendir",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
