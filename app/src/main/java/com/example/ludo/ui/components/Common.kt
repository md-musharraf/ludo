package com.example.ludo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.example.ludo.theme.LudoBlue
import com.example.ludo.theme.LudoGreen
import com.example.ludo.theme.LudoRed
import com.example.ludo.theme.LudoYellow
import com.example.ludo.theme.TextDark
import com.example.ludo.theme.TextMuted

/**
 * A value that ping-pongs between [from] and [to] only while [active]; otherwise it rests at
 * [idle] and no frames are scheduled, so idle widgets cost nothing.
 *
 * Read the returned state inside draw/graphicsLayer lambdas to keep updates out of composition.
 */
@Composable
fun rememberPulse(
    active: Boolean,
    from: Float,
    to: Float,
    durationMs: Int,
    idle: Float = from,
    easing: Easing = FastOutSlowInEasing,
    repeatMode: RepeatMode = RepeatMode.Reverse
): State<Float> {
    val anim = remember { Animatable(idle) }
    LaunchedEffect(active, from, to, durationMs) {
        if (active) {
            anim.snapTo(from)
            anim.animateTo(to, infiniteRepeatable(tween(durationMs, easing = easing), repeatMode))
        } else {
            anim.snapTo(idle)
        }
    }
    return anim.asState()
}

private val TitleColors = listOf('L' to LudoRed, 'U' to LudoGreen, 'D' to LudoYellow, 'O' to LudoBlue)

/** The four-colour "LUDO" wordmark. */
@Composable
fun LudoTitle(fontSize: TextUnit, modifier: Modifier = Modifier) {
    Row(modifier) {
        for ((letter, color) in TitleColors) {
            Text(letter.toString(), color = color, fontSize = fontSize, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColor: Color = LudoRed
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = TextDark,
        textContentColor = TextMuted,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = confirmColor)) {
                Text(confirmLabel, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
