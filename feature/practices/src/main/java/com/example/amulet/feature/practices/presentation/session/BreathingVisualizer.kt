package com.example.amulet.feature.practices.presentation.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amulet.core.design.foundation.color.AmuletPalette
import com.example.amulet.feature.practices.R
import com.example.amulet.shared.domain.practices.model.PracticeStep
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun BreathingVisualizer(
    currentStep: PracticeStep?,
    timeRemainingSec: Int,
    modifier: Modifier = Modifier
) {
    // Определяем фазу дыхания по заголовку
    val phase = remember(currentStep?.title) {
        val title = currentStep?.title?.lowercase().orEmpty()
        when {
            // "Активная фаза" и "Пауза" проверяем первыми -- они не содержат вдох/выдох.
            title.contains("актив") || title.contains("active") -> BreathingPhase.ACTIVE
            title.contains("пауза") || title.contains("pause") -> BreathingPhase.PAUSE
            title.contains("вдох") || title.contains("inhale") -> BreathingPhase.INHALE
            title.contains("выдох") || title.contains("exhale") -> BreathingPhase.EXHALE
            title.contains("задержка") || title.contains("hold") -> BreathingPhase.HOLD
            else -> BreathingPhase.DEFAULT
        }
    }

    // Улучшенные цвета для разных фаз с градиентами из темы
    val inhaleColors = listOf(
        AmuletPalette.InfoLight,
        AmuletPalette.Info,
        AmuletPalette.InfoDark
    )
    val exhaleColors = listOf(
        AmuletPalette.PrimaryLight,
        AmuletPalette.Primary,
        AmuletPalette.PrimaryVariant
    )
    val holdColors = listOf(
        AmuletPalette.SuccessLight,
        AmuletPalette.Success,
        AmuletPalette.SuccessDark
    )
    val defaultColors = listOf(
        AmuletPalette.InfoLight,
        AmuletPalette.Info,
        AmuletPalette.InfoDark
    )

    val targetColors = when (phase) {
        BreathingPhase.INHALE -> inhaleColors
        BreathingPhase.EXHALE -> exhaleColors
        BreathingPhase.HOLD -> holdColors
        BreathingPhase.ACTIVE -> exhaleColors // тёплые, энергичные тона
        BreathingPhase.PAUSE -> holdColors // спокойное удержание
        BreathingPhase.DEFAULT -> defaultColors
    }

    val animatedColor1 by animateColorAsState(
        targetValue = targetColors[0],
        animationSpec = tween(1500),
        label = "color1"
    )
    val animatedColor2 by animateColorAsState(
        targetValue = targetColors[1],
        animationSpec = tween(1500),
        label = "color2"
    )
    val animatedColor3 by animateColorAsState(
        targetValue = targetColors[2],
        animationSpec = tween(1500),
        label = "color3"
    )

    // Анимация размера
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val defaultScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "defaultScale"
    )

    // Быстрый пульс для активной фазы бодрящего дыхания.
    val activeScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "activeScale"
    )

    val stepDurationSec = (currentStep?.durationSec ?: 4).coerceAtLeast(1)
    val stepDurationMs = stepDurationSec * 1000

    val targetScale = when (phase) {
        BreathingPhase.INHALE -> 1.3f
        BreathingPhase.EXHALE -> 0.75f
        BreathingPhase.HOLD -> 1.0f
        BreathingPhase.PAUSE -> 0.7f // пустые лёгкие, удержание паузы
        BreathingPhase.ACTIVE -> activeScale
        BreathingPhase.DEFAULT -> defaultScale
    }

    // Для самоанимирующихся фаз (быстрый пульс / дефолт) мгновенно следуем за значением.
    val isSelfAnimating = phase == BreathingPhase.DEFAULT || phase == BreathingPhase.ACTIVE
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isSelfAnimating) {
            tween(durationMillis = 0, easing = FastOutSlowInEasing)
        } else {
            tween(durationMillis = stepDurationMs, easing = FastOutSlowInEasing)
        },
        label = "scale"
    )

    // Анимация вращения для частиц
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Пульсация свечения
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Внешнее размытое свечение (самый дальний слой)
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(animatedScale * 1.25f)
                .blur(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor1.copy(alpha = glowPulse * 0.5f),
                            animatedColor2.copy(alpha = glowPulse * 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Среднее свечение
        Box(
            modifier = Modifier
                .size(260.dp)
                .scale(animatedScale * 1.15f)
                .blur(20.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor2.copy(alpha = glowPulse * 0.6f),
                            animatedColor3.copy(alpha = glowPulse * 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Частицы вокруг основного круга
        ParticleSystem(
            animatedScale = animatedScale,
            rotation = rotation,
            color = animatedColor1,
            modifier = Modifier.size(240.dp)
        )

        // Основной круг с улучшенным градиентом
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(animatedScale)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            animatedColor1,
                            animatedColor2,
                            animatedColor3,
                            animatedColor2,
                            animatedColor1
                        )
                    )
                )
        )

        // Внутреннее кольцо с эффектом глубины
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(animatedScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor2.copy(alpha = 0.9f),
                            animatedColor3.copy(alpha = 0.95f),
                            animatedColor3
                        )
                    )
                )
        )

        // Центральный светлый участок
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(animatedScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            animatedColor1.copy(alpha = 0.8f),
                            animatedColor2.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Текст в центре
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = currentStep?.title ?: stringResource(id = R.string.practice_session_fallback_breathe),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
            if (timeRemainingSec > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatTime(timeRemainingSec),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}

@Composable
private fun ParticleSystem(
    animatedScale: Float,
    rotation: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Создаем частицы
    val particles = remember {
        List(12) { index ->
            Particle(
                angle = index * 30f,
                distance = 100f,
                size = Random.nextFloat() * 3f + 2f,
                alpha = Random.nextFloat() * 0.3f + 0.4f
            )
        }
    }

    Canvas(modifier = modifier.scale(animatedScale)) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        particles.forEach { particle ->
            val adjustedAngle = Math.toRadians((particle.angle + rotation).toDouble())
            val x = centerX + (particle.distance * cos(adjustedAngle)).toFloat()
            val y = centerY + (particle.distance * sin(adjustedAngle)).toFloat()

            drawCircle(
                color = color.copy(alpha = particle.alpha),
                radius = particle.size.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

private data class Particle(
    val angle: Float,
    val distance: Float,
    val size: Float,
    val alpha: Float
)

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "%d:%02d".format(m, s) else "$s"
}

private enum class BreathingPhase {
    INHALE, EXHALE, HOLD, PAUSE, ACTIVE, DEFAULT
}
