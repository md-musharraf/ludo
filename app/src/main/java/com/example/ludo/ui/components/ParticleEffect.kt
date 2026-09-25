package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ludo.theme.*
import kotlin.random.Random

private class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val alpha: Float,
    val size: Float
)

private val ConfettiColors = listOf(LudoRed, LudoGreen, LudoYellow, LudoBlue, SafeZoneStar)

/** Positive modulo so particles drifting left wrap to the right edge instead of vanishing. */
private fun wrap(v: Float): Float = ((v % 1f) + 1f) % 1f

@Composable
fun ParticleEffect(modifier: Modifier = Modifier) {
    val particles = remember {
        List(45) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.3f,
                vx = (Random.nextFloat() - 0.5f) * 0.012f,
                vy = Random.nextFloat() * 0.006f + 0.002f,
                color = ConfettiColors.random(),
                alpha = Random.nextFloat() * 0.6f + 0.4f,
                size = Random.nextFloat() * 6f + 3f
            )
        }
    }

    val time = rememberPulse(
        active = true, from = 0f, to = 1000f, durationMs = 30_000,
        easing = LinearEasing, repeatMode = RepeatMode.Restart
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = time.value
        for (p in particles) {
            val px = wrap(p.x + p.vx * t) * w
            val py = wrap(p.y + p.vy * t) * h
            val alpha = (p.alpha * (1f - (py / h) * 0.5f)).coerceIn(0f, 1f)
            drawCircle(color = p.color.copy(alpha = alpha), radius = p.size, center = Offset(px, py))
        }
    }
}
