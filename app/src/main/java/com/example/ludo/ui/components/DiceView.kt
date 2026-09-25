package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ludo.theme.*
import kotlinx.coroutines.delay

private val CupShape = RoundedCornerShape(14.dp)
private val WellShape = RoundedCornerShape(10.dp)
private val DieShape = RoundedCornerShape(8.dp)

private val CupBrush = Brush.verticalGradient(listOf(Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFF271510)))
private val WellBrush = Brush.radialGradient(listOf(Color(0xFF2C241E), Color(0xFF1E1814), Color(0xFF120E0C)))
private val DieBrush = Brush.linearGradient(
    colors = listOf(Color.White, Color(0xFFFCFAF7), Color(0xFFF4EFE6), Color(0xFFE8E0D2)),
    start = Offset.Zero,
    end = Offset(100f, 100f)
)

/** Pip cells (0..8 on a 3x3 grid, row-major) for faces 1..6. */
private val PipLayouts = arrayOf(
    intArrayOf(4),
    intArrayOf(2, 6),
    intArrayOf(2, 4, 6),
    intArrayOf(0, 2, 6, 8),
    intArrayOf(0, 2, 4, 6, 8),
    intArrayOf(0, 2, 3, 5, 6, 8)
)

/**
 * Dice cup with a 3D ivory die. All animated values are read in draw/layer lambdas, so rolling
 * and glowing never recompose, and every animation is idle unless it is needed.
 */
@Composable
fun DiceView(
    diceValue: Int,
    isRolling: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playerColor: Color = LudoGreen,
    size: Dp = 52.dp
) {
    val cupTilt = rememberPulse(active = isRolling, from = -12f, to = 12f, durationMs = 80, idle = 0f)
    val spin = rememberPulse(
        active = isRolling, from = 0f, to = 720f, durationMs = 450,
        easing = LinearEasing, repeatMode = RepeatMode.Restart
    )
    val shake = rememberPulse(active = isRolling, from = 0.92f, to = 1.08f, durationMs = 120, idle = 1f)
    val glow = rememberPulse(active = enabled && !isRolling, from = 0.4f, to = 1f, durationMs = 750)

    // Spring-damped landing bounce when roll finishes
    val landingScale = animateFloatAsState(
        targetValue = if (!isRolling && enabled) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "landingScale"
    )

    var displayedNumber by remember { mutableIntStateOf(diceValue.coerceIn(1, 6)) }
    LaunchedEffect(isRolling, diceValue) {
        if (isRolling) {
            while (true) {
                displayedNumber = (1..6).random()
                delay(45)
            }
        } else {
            displayedNumber = diceValue.coerceIn(1, 6)
        }
    }

    val haptics = LocalHapticFeedback.current
    val borderWidth = if (enabled) 2.2.dp else 1.2.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Dice showing $diceValue" }
            .graphicsLayer {
                rotationZ = cupTilt.value
                val scale = shake.value * landingScale.value
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (enabled) 8.dp else 2.dp,
                shape = CupShape,
                ambientColor = if (enabled) playerColor.copy(alpha = 0.5f) else Color(0x33000000),
                spotColor = if (enabled) playerColor else Color(0x44000000)
            )
            .clip(CupShape)
            .background(CupBrush)
            .drawWithContent {
                drawContent()
                val stroke = borderWidth.toPx()
                drawRoundRect(
                    color = if (enabled) playerColor.copy(alpha = glow.value) else Color(0xFF8D6E63),
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    style = Stroke(width = stroke)
                )
            }
            .clickable(
                enabled = enabled && !isRolling,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Roll dice"
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(3.dp)
    ) {
        // Inner felt well
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(WellShape)
                .background(WellBrush)
                .border(1.dp, Color(0xFF3E2723), WellShape)
        ) {
            val dieSize = size * 0.72f
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(dieSize)
                    .graphicsLayer { rotationZ = spin.value }
                    .shadow(elevation = 4.dp, shape = DieShape, ambientColor = Color(0x88000000), spotColor = Color.Black)
                    .clip(DieShape)
                    .background(DieBrush)
                    .border(1.2.dp, Color(0xFFD7CCC8), DieShape)
            ) {
                Canvas(modifier = Modifier.size(dieSize * 0.78f)) {
                    val w = this.size.width
                    val dotRadius = w * 0.11f
                    val margin = w * 0.22f
                    val step = w / 2 - margin
                    val face = displayedNumber
                    val pipColor = if (face == 1 || face == 6) LudoRed else Color(0xFF1E1E1E)

                    for (cell in PipLayouts[face - 1]) {
                        val pos = Offset(margin + (cell % 3) * step, margin + (cell / 3) * step)
                        drawCircle(Color(0x33000000), dotRadius * 1.15f, Offset(pos.x + 0.8f, pos.y + 0.8f))
                        drawCircle(pipColor, dotRadius, pos)
                        drawCircle(
                            Color.White.copy(alpha = 0.65f),
                            dotRadius * 0.35f,
                            Offset(pos.x - dotRadius * 0.25f, pos.y - dotRadius * 0.25f)
                        )
                    }
                }
            }
        }
    }
}
