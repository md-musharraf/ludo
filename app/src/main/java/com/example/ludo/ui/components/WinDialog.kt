package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.Player
import com.example.ludo.theme.*

@Composable
fun WinDialog(
    standings: List<Player>,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val winner = standings.firstOrNull()
    val appear = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val bob = rememberPulse(active = true, from = 0f, to = 1f, durationMs = 700)
    val winnerColor = PlayerColorUtils.getComposeColor(winner?.color)

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        LudoCard(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                },
            elevation = 16.dp,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Winner's pawn, gently bouncing on its colour tint.
                if (winner != null) {
                    Canvas(
                        Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(PlayerColorUtils.getLightColor(winner.color))
                    ) {
                        val s = size.minDimension
                        drawPawn(
                            center = Offset(size.width / 2, size.height / 2 + s * 0.04f),
                            radius = s * 0.28f,
                            color = winner.color,
                            lift = bob.value * s * 0.08f
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                SectionLabel("Winner")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${winner?.name ?: "Player"} wins!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = winnerColor,
                    textAlign = TextAlign.Center
                )

                if (standings.size > 1) {
                    Spacer(Modifier.height(18.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ControlShape)
                            .background(SubtleFill)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (player in standings) {
                            StandingRow(player)
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SecondaryButton("Home", onClick = onHome, icon = Icons.Rounded.Home, modifier = Modifier.weight(1f))
                    PrimaryButton(
                        "Play Again",
                        onClick = onPlayAgain,
                        icon = Icons.Rounded.Refresh,
                        color = InkDark,
                        height = 50.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StandingRow(player: Player) {
    val color = PlayerColorUtils.getComposeColor(player.color)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (player.rank == 1) color else SurfaceWhite)
        ) {
            Text(
                "${player.rank}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (player.rank == 1) Color.White else InkDark
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(player.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkDark, modifier = Modifier.weight(1f))
        Text(rankLabel(player.rank), fontSize = 12.sp, color = InkMuted)
    }
}
