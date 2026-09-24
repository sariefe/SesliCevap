package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSettingsDialog(
    initialIsVoiceEnabled: Boolean,
    initialAutoSpeak: Boolean,
    initialSpeechRate: Float,
    initialSpeechPitch: Float,
    initialElevenLabsApiKey: String = "",
    initialSelectedVoiceId: String = "21m00Tcm4TlvDq8ikWAM",
    initialSelectedPersona: String = "Genel Dostane Asistan",
    onSave: (isVoiceEnabled: Boolean, autoSpeak: Boolean, speechRate: Float, speechPitch: Float, elevenLabsApiKey: String, selectedVoiceId: String, selectedPersona: String) -> Unit,
    onTestVoice: (speechRate: Float, speechPitch: Float, selectedVoiceId: String, elevenLabsApiKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isVoiceEnabled by remember { mutableStateOf(initialIsVoiceEnabled) }
    var autoSpeak by remember { mutableStateOf(initialAutoSpeak) }
    var speechRate by remember { mutableFloatStateOf(initialSpeechRate) }
    var speechPitch by remember { mutableFloatStateOf(initialSpeechPitch) }
    var elevenLabsApiKey by remember { mutableStateOf(initialElevenLabsApiKey) }
    var selectedVoiceId by remember { mutableStateOf(initialSelectedVoiceId) }
    var selectedPersona by remember { mutableStateOf(initialSelectedPersona) }

    val voices = listOf(
        "Rachel (Kadın)" to "21m00Tcm4TlvDq8ikWAM",
        "Bella (Sıcak Kadın)" to "EXAVITQu4vr4xnSDxMaL",
        "Adam (Tok Erkek)" to "pNInz6obpgDQGcFmaJgB",
        "Antoni (Sakin Erkek)" to "ErXwobaYiN019PkySvjV"
    )

    val personas = listOf(
        "Genel Dostane Asistan",
        "Teknik Yazılım Uzmanı",
        "Motivasyon ve Yaşam Koçu"
    )

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
                Text("Asistan Etkileşim ve Kişilik Ayarları", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Main Toggle: Text-Only vs Full Voice-Enabled Mode
                Text(
                    text = "Etkileşim Modu",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

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
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = if (!isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sadece Metin",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (!isVoiceEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isVoiceEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isVoiceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tam Sesli",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isVoiceEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (isVoiceEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Voice Options (enabled only when full voice is turned on)
                AnimatedVisibility(
                    visible = isVoiceEnabled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    "Yapay zeka cevabı otomatik seslendirilsin.",
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

                        // AI Persona Selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Asistan Kişiliği (Persona)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                personas.forEach { persona ->
                                    FilterChip(
                                        selected = selectedPersona == persona,
                                        onClick = { selectedPersona = persona },
                                        label = { Text(persona, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        // ElevenLabs Voice ID Selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "ElevenLabs Ses Karakteri",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                voices.forEach { (label, id) ->
                                    FilterChip(
                                        selected = selectedVoiceId == id,
                                        onClick = { selectedVoiceId = id },
                                        label = { Text(label, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        // ElevenLabs API Key Optional Input
                        OutlinedTextField(
                            value = elevenLabsApiKey,
                            onValueChange = { elevenLabsApiKey = it },
                            label = { Text("ElevenLabs API Anahtarı") },
                            placeholder = { Text("sk_1234567890...") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

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

                        // Test voice button
                        FilledTonalButton(
                            onClick = { onTestVoice(speechRate, speechPitch, selectedVoiceId, elevenLabsApiKey) },
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(isVoiceEnabled, autoSpeak, speechRate, speechPitch, elevenLabsApiKey, selectedVoiceId, selectedPersona)
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
            onSave = { _, _, _, _, _, _, _ -> },
            onTestVoice = { _, _, _, _ -> },
            onDismiss = {}
        )
    }
}
