package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Kullanıcı konuşurken dışarıya doğru genişleyen çoklu eşmerkezli nabız dalgaları yayan
 * yüksek kaliteli mikrofon aksiyon butonu.
 */
@Composable
fun PulsingMicOrb(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    rmsDb: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 96.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse_motion")

    // Dalga 1: Hızlı iç halka nabzı
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1_scale"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1_alpha"
    )

    // Dalga 2: Gecikmeli dış halka nabzı
    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2_scale"
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2_alpha"
    )

    // Dalga 3: Çok daha geniş ve yumuşak aura
    val wave3Scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 3.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3_scale"
    )
    val wave3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave3_alpha"
    )

    // Çekirdek nefes alma (breathing core)
    val coreBreathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_breathing"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    val activeColor = when {
        isListening -> errorColor
        isSpeaking -> tertiaryColor
        isProcessing -> secondaryColor
        else -> primaryColor
    }

    val dynamicVoiceBoost = if (isListening) (rmsDb / 12f).coerceIn(0f, 0.4f) else 0f

    Box(
        modifier = modifier.size(orbSize * 2.2f),
        contentAlignment = Alignment.Center
    ) {
        // Çok katmanlı dışa yayılan pulsing dalgalar (yalnızca aktif dinleme veya konuşmada)
        if (isListening || isSpeaking) {
            // Dış Katman 3
            Box(
                modifier = Modifier
                    .size(orbSize)
                    .scale(wave3Scale + dynamicVoiceBoost)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = wave3Alpha))
            )

            // Orta Katman 2
            Box(
                modifier = Modifier
                    .size(orbSize)
                    .scale(wave2Scale + dynamicVoiceBoost)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                activeColor.copy(alpha = wave2Alpha),
                                Color.Transparent
                            )
                        )
                    )
            )

            // İç Katman 1
            Box(
                modifier = Modifier
                    .size(orbSize)
                    .scale(wave1Scale + dynamicVoiceBoost * 0.5f)
                    .clip(CircleShape)
                    .border(2.dp, activeColor.copy(alpha = wave1Alpha), CircleShape)
            )
        }

        // Ana Parlayan Çekirdek Orb
        val coreScale = if (isListening || isSpeaking) (coreBreathingScale + dynamicVoiceBoost) else 1.0f

        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(orbSize)
                .scale(coreScale)
                .clip(CircleShape)
                .testTag("pulsing_voice_orb_button"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = activeColor,
                contentColor = Color.White
            )
        ) {
            when {
                isListening -> {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Dinlemeyi Durdur",
                        modifier = Modifier.size(orbSize * 0.42f)
                    )
                }
                isSpeaking -> {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Seslendirmeyi Durdur",
                        modifier = Modifier.size(orbSize * 0.42f)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Konuşmaya Başla",
                        modifier = Modifier.size(orbSize * 0.42f)
                    )
                }
            }
        }
    }
}
