package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ludo.theme.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor

private val DieFaceTop = Color(0xFFFFFFFF)
private val DieFaceBottom = Color(0xFFF3EEE5)
private val DieEdge = Color(0xFFE2DACC)
private val PipColor = Color(0xFF2A2320)

private const val FLIP_MS = 120
private const val TUMBLE_FACES = 32

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
 * Classic white die with rounded corners and a soft contact shadow.
 * - Rolling: tumbles over and over (squeezing edge-on, new face each turn), hops and wobbles,
 *   then springs flat onto the result.
 * - [enabled]: a pulsing player-colour ring invites a tap.
 * All animation is read in draw/layer lambdas, so it never recomposes.
 */
@Composable
fun DiceView(
    diceValue: Int,
    isRolling: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    playerColor: Color = LudoGreen,
    size: Dp = 50.dp
) {
    // `turn` counts face-overs: face-on at whole numbers, edge-on at .5. The die squeezes along
    // one axis as it turns over, and the face only changes while edge-on, like a real tumble.
    val turn = remember { Animatable(0f) }
    val wobble = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val tumbleFaces = remember { IntArray(TUMBLE_FACES) { (1..6).random() } }
    var settledFace by remember { mutableIntStateOf(diceValue.coerceIn(1, 6)) }

    LaunchedEffect(isRolling, diceValue) {
        if (isRolling) {
            coroutineScope {
                launch { while (true) turn.animateTo(turn.value + 1f, tween(FLIP_MS, easing = LinearEasing)) }
                launch {
                    while (true) {
                        hop.animateTo(1f, tween(FLIP_MS, easing = FastOutSlowInEasing))
                        hop.animateTo(0.15f, tween(FLIP_MS, easing = FastOutLinearInEasing))
                    }
                }
                launch {
                    while (true) {
                        wobble.animateTo(12f, tween(FLIP_MS * 2, easing = FastOutSlowInEasing))
                        wobble.animateTo(-12f, tween(FLIP_MS * 2, easing = FastOutSlowInEasing))
                    }
                }
            }
        } else {
            settledFace = diceValue.coerceIn(1, 6)
            val wasTumbling = turn.value % 1f != 0f
            val settle = spring<Float>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
            coroutineScope {
                launch { turn.animateTo(ceil(turn.value), spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)) }
                launch { hop.animateTo(0f, settle) }
                launch { wobble.animateTo(0f, settle) }
                launch {
                    if (wasTumbling) {
                        pop.snapTo(1.14f)
                        pop.animateTo(1f, settle)
                    }
                }
            }
        }
    }

    val ring = rememberPulse(active = enabled && !isRolling, from = 0f, to = 1f, durationMs = 800)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(stiffness = Spring.StiffnessMedium), label = "press")
    val haptics = LocalHapticFeedback.current

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Dice showing $diceValue" }
            .clickable(
                enabled = enabled && !isRolling,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = "Roll dice"
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        val full = this.size.minDimension
        val die = full * 0.74f
        val lift = hop.value * full * 0.12f
        val left = (this.size.width - die) / 2
        val top = (this.size.height - die) / 2 - lift
        val corner = CornerRadius(die * 0.22f)

        // Invitation ring.
        if (enabled && !isRolling) {
            val grow = full * 0.04f * ring.value
            val ringInset = (full - die) / 2 - full * 0.06f - grow
            drawRoundRect(
                color = playerColor.copy(alpha = 0.35f + 0.55f * (1f - ring.value)),
                topLeft = Offset(ringInset, ringInset),
                size = Size(full - ringInset * 2, full - ringInset * 2),
                cornerRadius = CornerRadius(die * 0.3f),
                style = Stroke(width = full * 0.045f)
            )
        }

        // Contact shadow: tightens and darkens as the die comes down.
        val shadowSpread = 1f + hop.value * 0.25f
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.16f * (1.2f - hop.value * 0.6f)),
            topLeft = Offset(this.size.width / 2 - die * shadowSpread / 2, (this.size.height - die) / 2 + die * 0.14f),
            size = Size(die * shadowSpread, die * 0.95f),
            cornerRadius = corner
        )

        // Tumble: squeeze along X or Y (alternating per face-over) as the die turns edge-on.
        val t = turn.value
        val faceIndex = floor(t + 0.5f).toInt()
        val edgeOn = abs(cos((t - floor(t)) * PI.toFloat()))
        val squeeze = 0.35f + 0.65f * edgeOn
        val alongX = floor(t).toInt() % 2 == 0 // Fixed for the whole turn, so no mid-turn jump
        val face = if (isRolling) tumbleFaces[faceIndex.mod(TUMBLE_FACES)] else settledFace
        val scale = pop.value * pressScale
        val pivot = Offset(this.size.width / 2, top + die / 2)
        withTransform({
            rotate(wobble.value, pivot)
            scale(scale * (if (alongX) squeeze else 1f), scale * (if (alongX) 1f else squeeze), pivot)
        }) {
            // Body: faint vertical shading, darker bottom edge for thickness.
            drawRoundRect(DieEdge, Offset(left, top + die * 0.04f), Size(die, die), corner)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(DieFaceTop, DieFaceBottom), startY = top, endY = top + die),
                topLeft = Offset(left, top),
                size = Size(die, die * 0.97f),
                cornerRadius = corner
            )
            drawRoundRect(DieEdge, Offset(left, top), Size(die, die * 0.97f), corner, style = Stroke(width = die * 0.025f))

            // Pips on an exact 3x3 grid inside a padded face.
            val pad = die * 0.24f
            val step = (die - pad * 2) / 2
            val isOne = face == 1
            val pipRadius = die * (if (isOne) 0.12f else 0.085f)
            val color = if (isOne) LudoRed else PipColor
            for (cell in PipLayouts[face - 1]) {
                val center = Offset(left + pad + (cell % 3) * step, top + pad + (cell / 3) * step)
                drawCircle(color, pipRadius, center)
                drawCircle(Color.White.copy(alpha = 0.22f), pipRadius * 0.4f, Offset(center.x - pipRadius * 0.3f, center.y - pipRadius * 0.3f))
            }
            // The face darkens slightly as it turns away from the light.
            if (edgeOn < 1f) {
                drawRoundRect(Color.Black.copy(alpha = 0.18f * (1f - edgeOn)), Offset(left, top), Size(die, die * 0.97f), corner)
            }
        }
    }
}
