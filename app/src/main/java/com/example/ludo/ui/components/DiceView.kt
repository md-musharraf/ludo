package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
    val infiniteTransition = rememberInfiniteTransition(label = "diceAnim")

    val rollingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rollingRotation"
    )

    val rollingScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rollingScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val promptScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "promptScale"
    )

    val landingScale by animateFloatAsState(
        targetValue = if (isRolling) 1f else if (enabled) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "landingScale"
    )

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
        if (showPromptBadge && enabled && !isRolling) {
            Box(
                modifier = Modifier
                    .scale(promptScale)
                    .clip(RoundedCornerShape(8.dp))
                    .background(playerColor.copy(alpha = 0.15f))
                    .border(1.dp, playerColor.copy(alpha = glowAlpha), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ROLL \uD83C\uDFB2",
                    color = playerColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
        }

        // Classic White 3D Dice Box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .scale(if (isRolling) rollingScale * landingScale else landingScale)
                .rotate(if (isRolling) rollingRotation else 0f)
                .shadow(
                    elevation = if (enabled) 10.dp else 3.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = if (enabled) playerColor.copy(alpha = 0.4f) else Color(0x33000000),
                    spotColor = if (enabled) playerColor else Color(0x44000000)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF5F5F5),
                            Color(0xFFEEEEEE)
                        )
                    )
                )
                .border(
                    width = if (enabled) 2.5.dp else 1.dp,
                    color = if (enabled) playerColor.copy(alpha = glowAlpha) else Color(0xFFE0E0E0),
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            Canvas(modifier = Modifier.size(size * 0.76f)) {
                val dotRadius = this.size.width * 0.10f
                val margin = this.size.width * 0.22f
                val center = Offset(this.size.width / 2, this.size.height / 2)
                val topLeft = Offset(margin, margin)
                val topRight = Offset(this.size.width - margin, margin)
                val midLeft = Offset(margin, this.size.height / 2)
                val midRight = Offset(this.size.width - margin, this.size.height / 2)
                val bottomLeft = Offset(margin, this.size.height - margin)
                val bottomRight = Offset(this.size.width - margin, this.size.height - margin)

                val pipColor = if (displayedNumber == 6) LudoRed else Color(0xFF212121)

                fun drawPip(pos: Offset) {
                    // Pip shadow
                    drawCircle(
                        color = Color(0x22000000),
                        radius = dotRadius * 1.15f,
                        center = Offset(pos.x + 0.8f, pos.y + 0.8f)
                    )
                    // Pip body
                    drawCircle(color = pipColor, radius = dotRadius, center = pos)
                    // Pip gloss highlight
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = dotRadius * 0.35f,
                        center = Offset(pos.x - dotRadius * 0.25f, pos.y - dotRadius * 0.25f)
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
