package com.example.ludo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ludo.audio.SoundEffectManager
import com.example.ludo.model.GamePhase
import com.example.ludo.model.PlayerColor
import com.example.ludo.theme.*
import com.example.ludo.ui.components.*
import com.example.ludo.viewmodel.GameViewModel

@Composable
fun GameScreen(
    playerCount: Int,
    isVsAI: Boolean,
    aiDifficulty: String,
    onNavigateHome: () -> Unit,
) {
    val viewModel = remember { GameViewModel(playerCount, isVsAI, aiDifficulty) }
    val gameState by viewModel.gameState.collectAsState()

    var showRulesDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(SoundEffectManager.isSoundEnabled) }

    val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex)
    val currentPlayerColor = when (currentPlayer?.color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
        null -> LudoGreen
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF9E6),
                        Color(0xFFFFF3D6),
                        Color(0xFFFFECB3)
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar with Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Button
                IconButton(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(2.dp, CircleShape)
                ) {
                    Text("🏠", fontSize = 18.sp)
                }

                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L", color = LudoRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("U", color = LudoGreen, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("D", color = LudoYellow, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("O", color = LudoBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }

                // Action Buttons: Sound, Rules, Restart
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Sound Toggle
                    IconButton(
                        onClick = {
                            soundEnabled = !soundEnabled
                            SoundEffectManager.isSoundEnabled = soundEnabled
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Text(if (soundEnabled) "🔊" else "🔇", fontSize = 16.sp)
                    }

                    // Rules Button
                    IconButton(
                        onClick = { showRulesDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Text("❓", fontSize = 16.sp)
                    }

                    // Restart Button
                    IconButton(
                        onClick = { showRestartDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Text("🔄", fontSize = 16.sp)
                    }
                }
            }

            // 2. Player Status Panel
            PlayerPanel(
                players = gameState.players,
                currentPlayerIndex = gameState.currentPlayerIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // 3. Turn Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentPlayerColor.copy(alpha = 0.15f))
                    .border(1.5.dp, currentPlayerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gameState.moveMessage.ifEmpty { "${currentPlayer?.name ?: "Player"}'s turn" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = currentPlayerColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // 4. Main 15x15 Ludo Board Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                LudoBoard(
                    gameState = gameState,
                    onTokenClick = { tokenId ->
                        viewModel.selectToken(tokenId)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 5. Bottom Dice Controls Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DiceView(
                    diceValue = gameState.diceResult?.value ?: 1,
                    isRolling = gameState.gamePhase == GamePhase.ANIMATING_MOVE,
                    enabled = gameState.gamePhase == GamePhase.WAITING_FOR_ROLL &&
                            !(currentPlayer?.isAI ?: false) && !gameState.isGameOver,
                    playerColor = currentPlayerColor,
                    onClick = { viewModel.rollDice() }
                )
            }
        }

        // Win Dialog & Confetti Overlay
        if (gameState.isGameOver) {
            val winner = gameState.players.firstOrNull { it.id == gameState.winnerId }
                ?: gameState.players.firstOrNull { it.hasFinished }
            ParticleEffect(modifier = Modifier.fillMaxSize())
            WinDialog(
                winner = winner,
                onPlayAgain = { viewModel.resetGame() },
                onHome = onNavigateHome
            )
        }

        // Rules Dialog Modal
        if (showRulesDialog) {
            RulesDialog(onDismiss = { showRulesDialog = false })
        }

        // Restart Confirmation Modal
        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { showRestartDialog = false },
                title = { Text("Restart Game?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to restart this match?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showRestartDialog = false
                            viewModel.resetGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LudoRed)
                    ) {
                        Text("Restart")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showRestartDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun RulesDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📜 Ludo Rules", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleItem("🎲", "Roll a 6 to move a piece out of your home base.")
                    RuleItem("🔄", "Rolling a 6 grants you an extra turn!")
                    RuleItem("⚠️", "Three consecutive 6s forfeits your turn.")
                    RuleItem("💥", "Landing on an opponent's piece sends it back home and gives an extra turn!")
                    RuleItem("⭐", "Star positions and colored start squares are Safe Zones.")
                    RuleItem("🏠", "Guide all 4 pieces around the board into the center Home to win!")
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LudoGreen)
                ) {
                    Text("Got It!")
                }
            }
        }
    }
}

@Composable
private fun RuleItem(icon: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 18.sp)
    }
}
