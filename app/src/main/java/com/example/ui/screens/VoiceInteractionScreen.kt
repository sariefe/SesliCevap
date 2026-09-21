package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.speech.SpeechState
import com.example.data.speech.TtsState
import com.example.domain.model.MessageSender
import com.example.ui.components.PulsingMicOrb
import com.example.ui.components.PulsingWaveCanvas
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VoiceUiState

/**
 * Yüksek Sadakatli 'Voice Interaction' (Sesli Etkileşim) Tam Ekran Arayüzü.
 * Kullanıcı konuşurken aktif dinlemeyi gösteren parıltılı, çok katmanlı sinüs dalgaları (pulsing wave animation),
 * eşmerkezli nabız atan mikrofon küresi, canlı konuşma transkripsiyonu ve yapay zeka durum göstergelerini içerir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceInteractionScreen(
    uiState: VoiceUiState,
    onMicClick: () -> Unit,
    onStopAudio: () -> Unit,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectPrompt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = uiState.speechState is SpeechState.Listening || uiState.speechState is SpeechState.Ready
    val isSpeaking = uiState.ttsState is TtsState.Speaking
    val isProcessing = uiState.isProcessingAi

    // Dinamik koyu aura degrade arka planı
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            when {
                isListening -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                isSpeaking -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                isProcessing -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                else -> MaterialTheme.colorScheme.surface
            },
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
    ) {
        // Üst Araç Çubuğu (Kapat butonu, başlık ve ayarlar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .testTag("voice_interaction_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Geri Dön",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sesli Etkileşim",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        isListening -> "Aktif Dinleme Modu"
                        isSpeaking -> "Sesli Yanıt Veriliyor"
                        isProcessing -> "Yapay Zeka Analiz Ediyor"
                        else -> "Mikrofona Dokunarak Konuşun"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isListening -> MaterialTheme.colorScheme.error
                        isSpeaking -> MaterialTheme.colorScheme.tertiary
                        isProcessing -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .testTag("voice_interaction_settings_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Ses Ayarları",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Merkez İçerik: Canlı Transkripsiyon, Çok Katmanlı Sinüs Dalgası ve Nabız Atan Mikrofon Küresi
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Canlı Transkripsiyon veya Son Yapay Zeka Cevabı Kartı
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = when {
                            isListening -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            isSpeaking -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val currentText = when {
                            uiState.speechState is SpeechState.Listening -> {
                                val partial = uiState.speechState.partialText
                                if (partial.isNotBlank()) "\"$partial\"" else "Sizi dinliyorum, lütfen konuşun..."
                            }
                            isProcessing -> "Söyledikleriniz analiz ediliyor..."
                            isSpeaking -> {
                                val speakingText = uiState.ttsState.text
                                speakingText
                            }
                            uiState.messages.isNotEmpty() -> {
                                val lastMsg = uiState.messages.last()
                                val isUserSender = lastMsg.sender == MessageSender.USER
                                "${if (isUserSender) "Siz: " else "AI: "}${lastMsg.text}"
                            }
                            else -> "Mikrofona dokunup dilediğiniz soruyu sorun veya aşağıdaki örneklerden birini seçin."
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isListening) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = when {
                                    isListening -> "CANLI DİNLENİYOR"
                                    isProcessing -> "ANALİZ EDİLİYOR"
                                    isSpeaking -> "SESLENDİRİLİYOR"
                                    else -> "HAZIR"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isListening -> MaterialTheme.colorScheme.error
                                    isSpeaking -> MaterialTheme.colorScheme.tertiary
                                    isProcessing -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentText,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // Ortadaki Pulsing Wave (Sinüs Dalgası) Canlı Dalgalanma Alanı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Arka plandaki çok katmanlı sinüs dalgası
                PulsingWaveCanvas(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    rmsDb = uiState.micRmsDb,
                    modifier = Modifier.fillMaxSize()
                )

                // Dalganın merkezinde duran parlayan ikon veya işlemci indikatörü
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Alt Kısım: Nabız Atan Mikrofon Küresi ve Hızlı Örnek Komutlar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Nabız Atan Mikrofon Küresi (Concentric Pulsing Waves)
                PulsingMicOrb(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    isProcessing = isProcessing,
                    rmsDb = uiState.micRmsDb,
                    onClick = {
                        if (isSpeaking) {
                            onStopAudio()
                        } else {
                            onMicClick()
                        }
                    },
                    orbSize = 88.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        isListening -> "Dinleniyor... Tamamlamak için dokunun"
                        isSpeaking -> "Seslendiriliyor... Durdurmak için dokunun"
                        isProcessing -> "Yapay zeka yanıt hazırlıyor..."
                        else -> "Konuşmak için mikrofona dokunun"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isListening -> MaterialTheme.colorScheme.error
                        isSpeaking -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Kullanıcı boşta iken gösterilen hızlı konuşma önerileri
                if (!isListening && !isSpeaking && !isProcessing) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val prompts = listOf(
                            "Bugün hava nasıl?",
                            "Bana motivasyon ver",
                            "Önemli bir bilimsel gerçek"
                        )
                        prompts.forEach { prompt ->
                            FilterChip(
                                selected = false,
                                onClick = { onSelectPrompt(prompt) },
                                label = { Text(prompt, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceInteractionScreenPreview() {
    MyApplicationTheme {
        VoiceInteractionScreen(
            uiState = VoiceUiState(
                currentConversationTitle = "Sesli Etkileşim"
            ),
            onMicClick = {},
            onStopAudio = {},
            onClose = {},
            onOpenSettings = {},
            onSelectPrompt = {}
        )
    }
}
