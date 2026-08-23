package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.theme.*

@Composable
fun WinDialog(
    winner: Player?,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dialogScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "trophy")
    val trophyScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trophyPulse"
    )

    val winnerColor = when (winner?.color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
        null -> LudoGreen
    }

    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale),
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
                // Trophy
                Text(
                    text = "🏆",
                    fontSize = 72.sp,
                    modifier = Modifier.scale(trophyScale)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "GAME OVER!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Winner name with colored background
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(winnerColor.copy(alpha = 0.15f))
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

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Home", fontWeight = FontWeight.Medium)
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
