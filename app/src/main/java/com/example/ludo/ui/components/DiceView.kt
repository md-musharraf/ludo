package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.theme.*

@Composable
fun DiceView(
    diceValue: Int,
    isRolling: Boolean,
    enabled: Boolean,
    playerColor: Color = LudoGreen,
    onClick: () -> Unit
) {
    // 1. Continuous rotation during roll
    val infiniteTransition = rememberInfiniteTransition(label = "diceAnim")
    val rollingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rollingRotation"
    )

    // 2. Pulse for waiting to roll
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val promptScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "promptScale"
    )

    // 3. Bounce on landing
    var bounceTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(diceValue) {
        if (!isRolling) {
            bounceTrigger++
        }
    }

    val landingScale by animateFloatAsState(
        targetValue = if (isRolling) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "landingScale"
    )

    // Cycle numbers rapidly during roll
    var displayedNumber by remember { mutableIntStateOf(diceValue) }
    LaunchedEffect(isRolling) {
        if (isRolling) {
            while (true) {
                displayedNumber = (1..6).random()
                kotlinx.coroutines.delay(60)
            }
        } else {
            displayedNumber = diceValue
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // Tap prompt badge
        if (enabled && !isRolling) {
            Box(
                modifier = Modifier
                    .scale(promptScale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(playerColor.copy(alpha = 0.18f))
                    .border(1.dp, playerColor.copy(alpha = glowAlpha), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "TAP TO ROLL 🎲",
                    color = playerColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dice Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .scale(landingScale)
                .rotate(if (isRolling) rollingRotation else 0f)
                .then(
                    if (enabled) {
                        Modifier.shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = playerColor.copy(alpha = glowAlpha),
                            spotColor = playerColor
                        )
                    } else {
                        Modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
                    }
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF9F9F9),
                            Color(0xFFEDEDED)
                        )
                    )
                )
                .then(
                    if (enabled) {
                        Modifier.border(
                            width = 2.5.dp,
                            color = playerColor.copy(alpha = glowAlpha),
                            shape = RoundedCornerShape(20.dp)
                        )
                    } else {
                        Modifier.border(
                            width = 1.5.dp,
                            color = Color(0xFFDCDCDC),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                )
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            // Dice Canvas Drawing Pips
            Canvas(modifier = Modifier.size(56.dp)) {
                val dotRadius = size.width * 0.088f
                val margin = size.width * 0.22f
                val center = Offset(size.width / 2, size.height / 2)
                val topLeft = Offset(margin, margin)
                val topRight = Offset(size.width - margin, margin)
                val midLeft = Offset(margin, size.height / 2)
                val midRight = Offset(size.width - margin, size.height / 2)
                val bottomLeft = Offset(margin, size.height - margin)
                val bottomRight = Offset(size.width - margin, size.height - margin)

                val pipColor = if (displayedNumber == 6) LudoRed else Color(0xFF212121)

                fun drawPip(pos: Offset) {
                    // Pip shadow
                    drawCircle(
                        color = Color(0x30000000),
                        radius = dotRadius,
                        center = Offset(pos.x + 1f, pos.y + 1f)
                    )
                    // Pip fill
                    drawCircle(color = pipColor, radius = dotRadius, center = pos)
                    // Pip highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = dotRadius * 0.4f,
                        center = Offset(pos.x - dotRadius * 0.3f, pos.y - dotRadius * 0.3f)
                    )
                }

                when (displayedNumber) {
                    1 -> drawPip(center)
                    2 -> {
                        drawPip(topRight)
                        drawPip(bottomLeft)
                    }
                    3 -> {
                        drawPip(topRight)
                        drawPip(center)
                        drawPip(bottomLeft)
                    }
                    4 -> {
                        drawPip(topLeft)
                        drawPip(topRight)
                        drawPip(bottomLeft)
                        drawPip(bottomRight)
                    }
                    5 -> {
                        drawPip(topLeft)
                        drawPip(topRight)
                        drawPip(center)
                        drawPip(bottomLeft)
                        drawPip(bottomRight)
                    }
                    6 -> {
                        drawPip(topLeft)
                        drawPip(topRight)
                        drawPip(midLeft)
                        drawPip(midRight)
                        drawPip(bottomLeft)
                        drawPip(bottomRight)
                    }
                }
            }
        }
    }
}
