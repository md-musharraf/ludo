package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.theme.*

@Composable
fun DiceView(
    diceValue: Int,
    isRolling: Boolean,
    enabled: Boolean,
    playerColor: Color = LudoGreen,
    size: Dp = 54.dp,
    showPromptBadge: Boolean = false,
    onClick: () -> Unit
) {
    // 1. Continuous rotation during roll
    val infiniteTransition = rememberInfiniteTransition(label = "diceAnim")
    val rollingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rollingRotation"
    )

    // 2. Pulse for waiting to roll
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val promptScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "promptScale"
    )

    // 3. Bounce on landing
    val landingScale by animateFloatAsState(
        targetValue = if (isRolling) 1.15f else if (enabled) 1.04f else 1f,
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
                kotlinx.coroutines.delay(50)
            }
        } else {
            displayedNumber = diceValue
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Optional prompt badge
        if (showPromptBadge && enabled && !isRolling) {
            Box(
                modifier = Modifier
                    .scale(promptScale)
                    .clip(RoundedCornerShape(8.dp))
                    .background(playerColor.copy(alpha = 0.18f))
                    .border(1.dp, playerColor.copy(alpha = glowAlpha), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ROLL 🎲",
                    color = playerColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
        }

        // Dice Box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .scale(landingScale)
                .rotate(if (isRolling) rollingRotation else 0f)
                .then(
                    if (enabled) {
                        Modifier.shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = playerColor.copy(alpha = glowAlpha),
                            spotColor = playerColor
                        )
                    } else {
                        Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
                    }
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF8F8F8),
                            Color(0xFFEBEBEB)
                        )
                    )
                )
                .then(
                    if (enabled) {
                        Modifier.border(
                            width = 2.dp,
                            color = playerColor.copy(alpha = glowAlpha),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = Color(0xFFD6D6D6),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                )
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            // Dice Canvas Drawing Pips
            Canvas(modifier = Modifier.size(size * 0.75f)) {
                val dotRadius = this.size.width * 0.09f
                val margin = this.size.width * 0.22f
                val center = Offset(this.size.width / 2, this.size.height / 2)
                val topLeft = Offset(margin, margin)
                val topRight = Offset(this.size.width - margin, margin)
                val midLeft = Offset(margin, this.size.height / 2)
                val midRight = Offset(this.size.width - margin, this.size.height / 2)
                val bottomLeft = Offset(margin, this.size.height - margin)
                val bottomRight = Offset(this.size.width - margin, this.size.height - margin)

                val pipColor = if (displayedNumber == 6) LudoRed else Color(0xFF222222)

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
                        color = Color.White.copy(alpha = 0.5f),
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
