package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.ludo.theme.*
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var alpha: Float = 1f,
    val size: Float
)

@Composable
fun ParticleEffect(modifier: Modifier = Modifier) {
    val particles = remember {
        val colors = listOf(
            LudoRed, LudoGreen, LudoYellow, LudoBlue,
            SafeZoneStar, Color.White
        )
        List(90) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.3f,
                vx = (Random.nextFloat() - 0.5f) * 0.012f,
                vy = Random.nextFloat() * 0.006f + 0.002f,
                color = colors.random(),
                alpha = Random.nextFloat() * 0.6f + 0.4f,
                size = Random.nextFloat() * 8f + 3f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            val px = ((p.x + p.vx * time) % 1f) * w
            val py = ((p.y + p.vy * time) % 1f) * h
            val alpha = (p.alpha * (1f - (py / h) * 0.5f)).coerceIn(0f, 1f)

            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size,
                center = Offset(px, py)
            )
        }
    }
}
