package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun VoiceMicButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    val activeColor by animateColorAsState(
        targetValue = when {
            isListening -> MaterialTheme.colorScheme.error
            isSpeaking -> MaterialTheme.colorScheme.tertiary
            isProcessing -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.primary
        },
        label = "button_color",
    )

    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer pulsing ring when listening or speaking
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(activeColor.copy(alpha = pulseAlpha)),
            )
        }

        FilledIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(68.dp)
                .testTag("voice_mic_button"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = activeColor,
                contentColor = Color.White,
            ),
        ) {
            when {
                isListening -> {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Dinlemeyi Durdur",
                        modifier = Modifier.size(32.dp),
                    )
                }
                isSpeaking -> {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Seslendirmeyi Durdur",
                        modifier = Modifier.size(32.dp),
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Konuşmaya Başla",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceMicButtonPreview() {
    MaterialTheme {
        VoiceMicButton(
            isListening = false,
            isSpeaking = false,
            isProcessing = false,
            onClick = {},
        )
    }
}
