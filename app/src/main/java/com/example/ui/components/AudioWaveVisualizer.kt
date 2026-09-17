package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase3"
    )

    val waveGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    val idleGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.outlineVariant,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 18
        val baseRmsFactor = (rmsDb / 10f).coerceIn(0f, 1f)

        for (i in 0 until barCount) {
            val distFromCenter = kotlin.math.abs(i - (barCount / 2f)) / (barCount / 2f)
            val bellCurve = 1f - (distFromCenter * 0.6f)

            val animatedHeightFraction = when {
                isListening -> {
                    val phaseVal = if (i % 3 == 0) phase1 else if (i % 3 == 1) phase2 else phase3
                    ((baseRmsFactor * 0.7f + phaseVal * 0.3f) * bellCurve).coerceIn(0.15f, 1.0f)
                }
                isSpeaking -> {
                    val phaseVal = if (i % 2 == 0) phase2 else phase1
                    (phaseVal * bellCurve).coerceIn(0.2f, 0.9f)
                }
                else -> {
                    0.1f
                }
            }

            val barHeight = (48.dp * animatedHeightFraction).coerceAtLeast(6.dp)

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isListening || isSpeaking) waveGradient else idleGradient)
            )
        }
    }
}
