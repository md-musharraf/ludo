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
import kotlinx.coroutines.delay

/**
 * Classic Physical Dice Cup & 3D Ivory Dice Component.
 * Features:
 * - Rich mahogany leather/wood shaker cup with golden brass rim
 * - Deep velvet felt cup interior
 * - 3D Ivory porcelain dice with beveled edges and realistic indented pips (Ruby Red 1 & 6)
 * - Authentic physics animations: cup shaker oscillation, 3D tumbling rotation, spring-damped landing bounce
 */
@Composable
fun DiceView(
    diceValue: Int,
    isRolling: Boolean,
    enabled: Boolean,
    playerColor: Color = LudoGreen,
    size: Dp = 56.dp,
    showPromptBadge: Boolean = true,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "classicDiceCupAnim")

    // Cup shaker tilt oscillation during rolling
    val cupTilt by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(80, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cupTilt"
    )

    // Dice rapid tumbling rotation
    val rollingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rollingRotation"
    )

    // Elastic scaling while shaking
    val rollingScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rollingScale"
    )

    // Smooth breathing glow for active turn
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Prompt badge gentle floating pulse
    val promptScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "promptScale"
    )

    // Spring-damped landing bounce when roll finishes
    val landingScale by animateFloatAsState(
        targetValue = if (isRolling) 1f else if (enabled) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "landingScale"
    )

    var displayedNumber by remember { mutableIntStateOf(diceValue) }
    LaunchedEffect(isRolling, diceValue) {
        if (isRolling) {
            while (true) {
                displayedNumber = (1..6).random()
                delay(45)
            }
        } else {
            displayedNumber = diceValue
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(2.dp)
    ) {
        // Active "ROLL" Prompt Badge
        if (showPromptBadge && enabled && !isRolling) {
            Box(
                modifier = Modifier
                    .scale(promptScale)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                playerColor.copy(alpha = 0.25f),
                                Color(0xFFFFD54F).copy(alpha = 0.35f),
                                playerColor.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .border(1.2.dp, playerColor.copy(alpha = glowAlpha), RoundedCornerShape(8.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ROLL \uD83C\uDFB2",
                    color = if (playerColor == LudoYellow) Color(0xFFE65100) else playerColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
        }

        // Classic Leather/Wood Shaker Cup & 3D Dice Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .rotate(if (isRolling) cupTilt else 0f)
                .scale(if (isRolling) rollingScale * landingScale else landingScale)
                .shadow(
                    elevation = if (enabled) 10.dp else 3.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = if (enabled) playerColor.copy(alpha = 0.5f) else Color(0x33000000),
                    spotColor = if (enabled) playerColor else Color(0x44000000)
                )
                .clip(RoundedCornerShape(16.dp))
                // Outer Mahogany Cup Body
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF5D4037),
                            Color(0xFF3E2723),
                            Color(0xFF271510)
                        )
                    )
                )
                // Brass/Gold Outer Rim Frame
                .border(
                    width = if (enabled) 2.5.dp else 1.2.dp,
                    color = if (enabled) playerColor.copy(alpha = glowAlpha) else Color(0xFF8D6E63),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    enabled = enabled && !isRolling,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(3.5.dp)
        ) {
            // Inner Cup Felt Well
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2C241E),
                                Color(0xFF1E1814),
                                Color(0xFF120E0C)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFF3E2723), RoundedCornerShape(12.dp))
            ) {
                // 3D Ivory Porcelain Dice
                val diceBoxSize = size * 0.72f
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(diceBoxSize)
                        .rotate(if (isRolling) rollingRotation else 0f)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(10.dp),
                            ambientColor = Color(0x88000000),
                            spotColor = Color.Black
                        )
                        .clip(RoundedCornerShape(10.dp))
                        // Ivory surface with multi-stop top-light gradient
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFFCFAF7),
                                    Color(0xFFF4EFE6),
                                    Color(0xFFE8E0D2)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f)
                            )
                        )
                        .border(1.2.dp, Color(0xFFD7CCC8), RoundedCornerShape(10.dp))
                ) {
                    // Realistic Indented Pips Canvas
                    Canvas(modifier = Modifier.size(diceBoxSize * 0.78f)) {
                        val dotRadius = this.size.width * 0.11f
                        val margin = this.size.width * 0.22f
                        val center = Offset(this.size.width / 2, this.size.height / 2)
                        val topLeft = Offset(margin, margin)
                        val topRight = Offset(this.size.width - margin, margin)
                        val midLeft = Offset(margin, this.size.height / 2)
                        val midRight = Offset(this.size.width - margin, this.size.height / 2)
                        val bottomLeft = Offset(margin, this.size.height - margin)
                        val bottomRight = Offset(this.size.width - margin, this.size.height - margin)

                        // Classic Ludo Pip Colors: Jewel Ruby Red for 1 & 6, Obsidian Black for 2, 3, 4, 5
                        val isRedFace = displayedNumber == 1 || displayedNumber == 6
                        val pipColor = if (isRedFace) LudoRed else Color(0xFF1E1E1E)

                        fun drawPip(pos: Offset) {
                            // Pip bevel depth shadow
                            drawCircle(
                                color = Color(0x33000000),
                                radius = dotRadius * 1.15f,
                                center = Offset(pos.x + 0.8f, pos.y + 0.8f)
                            )
                            // Pip core body
                            drawCircle(
                                color = pipColor,
                                radius = dotRadius,
                                center = pos
                            )
                            // Pip specular highlight
                            drawCircle(
                                color = Color.White.copy(alpha = 0.65f),
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
    }
}
