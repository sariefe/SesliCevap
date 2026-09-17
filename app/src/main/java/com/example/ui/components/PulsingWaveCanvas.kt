package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Yüksek kaliteli, organik ve akıcı Çok Katmanlı Sinüs Dalgası Görselleştiricisi (Pulsing Wave Visualizer).
 * Kullanıcı konuştuğunda veya yapay zeka seslendirdiğinde aktif dinlemeyi ve ses gücünü (rmsDb)
 * dinamik sinüs dalgaları, parıltılı parçacıklar ve degrade dolgular ile temsil eder.
 */
@Composable
fun PulsingWaveCanvas(
    isListening: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_wave_motion")

    // Dalga fazı kayması (sürekli akış)
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_primary"
    )

    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_secondary"
    )

    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_tertiary"
    )

    // Nefes alıp veren nabız ölçeği (Pulsing breath effect)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_pulse_breath"
    )

    // Parçacık parıltısı
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    // Temaya göre dinamik renkler
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val activeColor = if (isListening) primaryColor else tertiaryColor

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // Kullanıcı konuşurken mikrofon dB seviyesine göre dinamik genlik (0..1)
        val normalizedRms = (rmsDb / 10f).coerceIn(0.1f, 1.2f)
        val activityMultiplier = when {
            isListening -> (normalizedRms * 1.4f * pulseScale).coerceIn(0.35f, 2.2f)
            isSpeaking -> (0.85f * pulseScale)
            else -> 0.15f
        }

        val baseAmplitude = (height * 0.22f) * activityMultiplier

        // 1. Arka Plan Degrade Dolgulu Dalga (Layer 1 - Derinlik ve Aura)
        drawWaveFill(
            width = width,
            centerY = centerY,
            amplitude = baseAmplitude * 0.75f,
            frequency = 1.2f,
            phase = phase2,
            brush = Brush.verticalGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = if (isListening || isSpeaking) 0.25f else 0.05f),
                    activeColor.copy(alpha = if (isListening || isSpeaking) 0.12f else 0.02f),
                    Color.Transparent
                ),
                startY = centerY - baseAmplitude,
                endY = height
            )
        )

        // 2. İkincil Sinüs Dalgası (Layer 2 - Cyan & Tertiary Kontur)
        drawWaveLine(
            width = width,
            centerY = centerY,
            amplitude = baseAmplitude * 0.9f,
            frequency = 1.6f,
            phase = phase3,
            strokeWidth = 3f,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = 0.4f),
                    tertiaryColor.copy(alpha = if (isListening || isSpeaking) 0.85f else 0.2f),
                    secondaryColor.copy(alpha = 0.4f)
                )
            )
        )

        // 3. Ana Parlak Sinüs Dalgası (Layer 3 - Primary Vurgu Dalgası)
        drawWaveLine(
            width = width,
            centerY = centerY,
            amplitude = baseAmplitude,
            frequency = 2.0f,
            phase = phase1,
            strokeWidth = if (isListening || isSpeaking) 5f else 2.5f,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.3f),
                    activeColor,
                    secondaryColor,
                    activeColor.copy(alpha = 0.3f)
                )
            )
        )

        // 4. Parıltılı Işıma Noktaları (Crest Sparkles)
        if (isListening || isSpeaking) {
            val pointCount = 9
            for (i in 1..pointCount) {
                val x = width * (i / (pointCount + 1f))
                val normalizedX = (x / width) * 2 * PI.toFloat() * 2.0f + phase1
                val y = centerY + (sin(normalizedX.toDouble()) * baseAmplitude).toFloat()

                val crestGlowRadius = (4.5f + 3f * shimmerAlpha * activityMultiplier)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = shimmerAlpha),
                            activeColor.copy(alpha = shimmerAlpha * 0.5f),
                            Color.Transparent
                        ),
                        center = Offset(x, y),
                        radius = crestGlowRadius * 2
                    ),
                    radius = crestGlowRadius * 2,
                    center = Offset(x, y)
                )

                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Sinüs eğrisi konturu çizer.
 */
private fun DrawScope.drawWaveLine(
    width: Float,
    centerY: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    strokeWidth: Float,
    brush: Brush
) {
    val path = Path()
    val steps = 80
    val stepX = width / steps

    for (i in 0..steps) {
        val x = i * stepX
        // Ekran kenarlarına doğru dalganın sönümlenmesi (envelope function / pencereli sinüs)
        val envelope = sin((i.toFloat() / steps) * PI).toFloat()
        val angle = (i.toFloat() / steps) * 2 * PI.toFloat() * frequency + phase
        val y = centerY + (sin(angle.toDouble()) * amplitude * envelope).toFloat()

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        brush = brush,
        style = Stroke(width = strokeWidth)
    )
}

/**
 * Sinüs eğrisinin altını dolduran degrade çizer.
 */
private fun DrawScope.drawWaveFill(
    width: Float,
    centerY: Float,
    amplitude: Float,
    frequency: Float,
    phase: Float,
    brush: Brush
) {
    val path = Path()
    val steps = 60
    val stepX = width / steps

    path.moveTo(0f, centerY)

    for (i in 0..steps) {
        val x = i * stepX
        val envelope = sin((i.toFloat() / steps) * PI).toFloat()
        val angle = (i.toFloat() / steps) * 2 * PI.toFloat() * frequency + phase
        val y = centerY + (sin(angle.toDouble()) * amplitude * envelope).toFloat()
        path.lineTo(x, y)
    }

    path.lineTo(width, centerY + amplitude * 1.5f)
    path.lineTo(0f, centerY + amplitude * 1.5f)
    path.close()

    drawPath(
        path = path,
        brush = brush
    )
}
