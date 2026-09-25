package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ludo.theme.TextDark
import com.example.ludo.theme.TextMuted

@Composable
fun WinDialog(
    standings: List<Player>,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val winner = standings.firstOrNull()
    val appear = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val trophyScale = rememberPulse(active = true, from = 1f, to = 1.15f, durationMs = 600)
    val winnerColor = PlayerColorUtils.getComposeColor(winner?.color)

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🏆",
                    fontSize = 64.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = trophyScale.value
                        scaleY = trophyScale.value
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "GAME OVER!", fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextDark)

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(winnerColor.copy(alpha = 0.15f))
                        .border(1.dp, winnerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${winner?.name ?: "Player"} Wins!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = winnerColor,
                        textAlign = TextAlign.Center
                    )
                }

                if (standings.size > 1) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (player in standings.drop(1)) {
                            Text(
                                text = "${rankLabel(player.rank)}  ${player.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PlayerColorUtils.getComposeColor(player.color)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Home", fontWeight = FontWeight.Medium, color = TextMuted)
                    }

                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = winnerColor)
                    ) {
                        Text("Play Again", fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
            }
        }
    }
}
