package com.example.ui.screens

import android.annotation.SuppressLint
package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.speech.SpeechState
import com.example.data.speech.TtsState
import com.example.domain.model.ChatMessage
import com.example.domain.model.ConversationSession
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.PulsingWaveCanvas
import com.example.ui.components.VoiceMicButton
import com.example.ui.viewmodel.VoiceUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantScreen(
    uiState: VoiceUiState,
    conversations: List<ConversationSession>,
    onMicClick: () -> Unit,
    onToggleVoiceMode: () -> Unit = {},
    onSendTextMessage: (String) -> Unit,
    onPlayAudio: (ChatMessage) -> Unit,
    onStopAudio: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVoiceInteraction: () -> Unit = {},
    onDismissError: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var textInput by remember { mutableStateOf("") }
    var isKeyboardMode by remember { mutableStateOf(false) }
    var showConversationsDropdown by remember { mutableStateOf(false) }

    val isListening = uiState.speechState is SpeechState.Listening || uiState.speechState is SpeechState.Ready
    val isSpeaking = uiState.ttsState is TtsState.Speaking
    val isProcessing = uiState.isProcessingAi

    // Auto-scroll to latest message
    LaunchedEffect(uiState.messages.size, isListening, isProcessing) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showConversationsDropdown = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.currentConversationTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Sohbetleri Değiştir",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = when {
                                    !uiState.isVoiceEnabled -> "Sadece Metin Modu (Sessiz)"
                                    isListening -> "Dinleniyor..."
                                    isSpeaking -> "Seslendiriliyor..."
                                    isProcessing -> "Analiz ediliyor..."
                                    else -> "Tam Sesli Etkileşim Hazır"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    !uiState.isVoiceEnabled -> MaterialTheme.colorScheme.tertiary
                                    isListening -> MaterialTheme.colorScheme.error
                                    isSpeaking -> MaterialTheme.colorScheme.tertiary
                                    isProcessing -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = showConversationsDropdown,
                            onDismissRequest = { showConversationsDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("+ Yeni Sesli Sohbet", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showConversationsDropdown = false
                                    onNewConversation()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            )
                            conversations.forEach { conv ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            conv.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        showConversationsDropdown = false
                                        onSelectConversation(conv.id)
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleVoiceMode,
                        modifier = Modifier.testTag("toggle_voice_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (uiState.isVoiceEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (uiState.isVoiceEnabled) "Metin Moduna Geç" else "Sesli Moda Geç",
                            tint = if (uiState.isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = onOpenVoiceInteraction,
                        modifier = Modifier.testTag("open_voice_interaction_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = "Sesli Etkileşim Ekranı",
                            tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNewConversation,
                        modifier = Modifier.testTag("new_conversation_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Yeni Sohbet")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings_btn")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Ayarlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Pulsing Audio Wave Visualizer Banner
            AnimatedVisibility(
                visible = isListening || isSpeaking,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // High-fidelity pulsing sine waves
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            PulsingWaveCanvas(
                                isListening = isListening,
                                isSpeaking = isSpeaking,
                                rmsDb = uiState.micRmsDb,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isListening) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = if (isListening) "Aktif Dinleme: Sizi dinliyorum, konuşun..." else "Cevap seslendiriliyor (Durdurmak için dokunun)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Error Banner if any
            AnimatedVisibility(visible = uiState.userErrorMessage != null) {
                uiState.userErrorMessage?.let { errMsg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = errMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Messages LazyColumn
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    // Empty State with Starter Prompt Chips
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Sesli Soru Sorun",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mikrofon butonuna dokunup konuşun. Yapay zeka anında analiz ederek size sesli yanıt verecektir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Veya bu sorulardan birine dokunun:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val samplePrompts = listOf(
                                "Bugün nasılsın?",
                                "Günün özeti nedir?",
                                "Bana bir fikir ver",
                                "Zaman yönetimi tavsiyesi",
                                "Sağlıklı yaşam önerileri"
                            )
                            samplePrompts.forEach { prompt ->
                                FilterChip(
                                    selected = false,
                                    onClick = { onSendTextMessage(prompt) },
                                    label = { Text(prompt) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { msg ->
                            val isThisSpeaking = isSpeaking && uiState.ttsState.text == msg.text
                            ChatMessageItem(
                                message = msg,
                                isSpeakingThis = isThisSpeaking,
                                onPlayAudio = { onPlayAudio(it) }
                            )
                        }

                        // Live partial transcript preview while user is actively speaking
                        if (uiState.speechState is SpeechState.Listening) {
                            val partial = uiState.speechState.partialText
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = partial.ifBlank { "Dinleniyor..." },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }

                        // AI Processing / analyzing indicator
                        if (isProcessing) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Yapay zeka analiz ediyor ve sesli yanıt hazırlıyor...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Voice & Text Dock
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val showKeyboardInput = isKeyboardMode || !uiState.isVoiceEnabled

                    if (showKeyboardInput) {
                        if (!uiState.isVoiceEnabled) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clickable { onToggleVoiceMode() }
                                    .testTag("text_only_mode_banner")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Chat,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Sadece Metin Modu Aktif (Sessiz)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Sesli Moda Geç",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                placeholder = {
                                    Text(if (!uiState.isVoiceEnabled) "Metinle soru sorun..." else "Mesajınızı yazın...")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("text_prompt_input"),
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (textInput.isNotBlank()) {
                                        onSendTextMessage(textInput)
                                        textInput = ""
                                    }
                                },
                                modifier = Modifier.testTag("send_text_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Gönder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (uiState.isVoiceEnabled) {
                                IconButton(onClick = { isKeyboardMode = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Ses Moduna Geç"
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = onOpenSettings,
                                    modifier = Modifier.testTag("settings_shortcut_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MicOff,
                                        contentDescription = "Ses Kapalı (Ayarlar)",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Voice Mic Center Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isKeyboardMode = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("keyboard_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Klavyeyle Yaz",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            VoiceMicButton(
                                isListening = isListening,
                                isSpeaking = isSpeaking,
                                isProcessing = isProcessing,
                                onClick = {
                                    if (isSpeaking) {
                                        onStopAudio()
                                    } else {
                                        onMicClick()
                                    }
                                }
                            )

                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = "Ses Ayarları",
                                    tint = if (uiState.autoSpeak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                isListening -> "Dinleniyor... Bitirmek için dokunun"
                                isSpeaking -> "Seslendiriliyor... Durdurmak için dokunun"
                                isProcessing -> "Yapay zeka yanıtlıyor..."
                                else -> "Konuşmak için mikrofona dokunun"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isListening -> MaterialTheme.colorScheme.error
                                isSpeaking -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
