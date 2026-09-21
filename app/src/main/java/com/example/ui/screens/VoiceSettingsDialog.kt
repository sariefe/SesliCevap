package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

@Composable
fun VoiceSettingsDialog(
    initialIsVoiceEnabled: Boolean,
    initialAutoSpeak: Boolean,
    initialSpeechRate: Float,
    initialSpeechPitch: Float,
    initialElevenLabsApiKey: String = "",
    onSave: (isVoiceEnabled: Boolean, autoSpeak: Boolean, speechRate: Float, speechPitch: Float, elevenLabsApiKey: String) -> Unit,
    onTestVoice: (speechRate: Float, speechPitch: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var isVoiceEnabled by remember { mutableStateOf(initialIsVoiceEnabled) }
    var autoSpeak by remember { mutableStateOf(initialAutoSpeak) }
    var speechRate by remember { mutableFloatStateOf(initialSpeechRate) }
    var speechPitch by remember { mutableFloatStateOf(initialSpeechPitch) }
    var elevenLabsApiKey by remember { mutableStateOf(initialElevenLabsApiKey) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Asistan Etkileşim Ayarları", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Main Toggle: Text-Only vs Full Voice-Enabled Mode
                Text(
                    text = "Etkileşim Modu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Text-Only Mode Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isVoiceEnabled = false }
                            .testTag("text_only_mode_toggle"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isVoiceEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        border = BorderStroke(
                            1.5.dp,
                            if (!isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = if (!isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Sadece Metin",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!isVoiceEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isVoiceEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sessiz / Yazışma",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Full Voice-Enabled Mode Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isVoiceEnabled = true }
                            .testTag("full_voice_mode_toggle"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isVoiceEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        border = BorderStroke(
                            1.5.dp,
                            if (isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tam Sesli",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isVoiceEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (isVoiceEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Konuşma & Dinleme",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switch row explaining the active mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isVoiceEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = if (isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (isVoiceEnabled) "Sesli Etkileşim Aktif" else "Metin Odaklı Mod Aktif",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isVoiceEnabled) {
                                    "Mikrofon girişi ve sesli okuma özellikleri kullanılabilir."
                                } else {
                                    "Mikrofon ve otomatik seslendirme devre dışı bırakılır."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isVoiceEnabled,
                        onCheckedChange = { isVoiceEnabled = it },
                        modifier = Modifier.testTag("voice_enabled_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Voice Options (enabled only when full voice is turned on)
                AnimatedVisibility(
                    visible = isVoiceEnabled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        // Auto speak switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Otomatik Sesli Yanıt",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Yapay zeka cevabı geldiğinde otomatik seslendirilsin.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoSpeak,
                                onCheckedChange = { autoSpeak = it },
                                modifier = Modifier.testTag("auto_speak_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // ElevenLabs API Key Optional Input
                        OutlinedTextField(
                            value = elevenLabsApiKey,
                            onValueChange = { elevenLabsApiKey = it },
                            label = { Text("ElevenLabs API Anahtarı (Opsiyonel Nöral Ses)") },
                            placeholder = { Text("sk_1234567890...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Speech rate
                        Text(
                            text = "Konuşma Hızı: ${"%.1f".format(Locale.US, speechRate)}x",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = speechRate,
                            onValueChange = { speechRate = it },
                            valueRange = 0.5f..2.0f,
                            steps = 14,
                            modifier = Modifier.testTag("speech_rate_slider")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Speech pitch
                        Text(
                            text = "Ses Perdesi (Ton): ${"%.1f".format(Locale.US, speechPitch)}x",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = speechPitch,
                            onValueChange = { speechPitch = it },
                            valueRange = 0.5f..2.0f,
                            steps = 14,
                            modifier = Modifier.testTag("speech_pitch_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Test voice button
                        FilledTonalButton(
                            onClick = { onTestVoice(speechRate, speechPitch) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Sesi Test Et")
                        }
                    }
                }

                if (!isVoiceEnabled) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Metin modunda asistanla yalnızca klavye ile yazışırsınız. Sesli okuma ve mikrofon kullanımı kapalıdır.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(isVoiceEnabled, autoSpeak, speechRate, speechPitch, elevenLabsApiKey)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_settings_btn")
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun VoiceSettingsDialogPreview() {
    MyApplicationTheme {
        VoiceSettingsDialog(
            initialIsVoiceEnabled = true,
            initialAutoSpeak = true,
            initialSpeechRate = 1.0f,
            initialSpeechPitch = 1.0f,
            onSave = { _, _, _, _, _ -> },
            onTestVoice = { _, _ -> },
            onDismiss = {}
        )
    }
}
