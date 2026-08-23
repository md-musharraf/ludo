package com.example.ludo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    // Map players to 4 corners
    // Red -> TOP_LEFT (0)
    // Green -> TOP_RIGHT (1)
    // Yellow -> BOTTOM_RIGHT (2)
    // Blue -> BOTTOM_LEFT (3)
    val redPlayer = gameState.players.firstOrNull { it.color == PlayerColor.RED }
    val greenPlayer = gameState.players.firstOrNull { it.color == PlayerColor.GREEN }
    val yellowPlayer = gameState.players.firstOrNull { it.color == PlayerColor.YELLOW }
    val bluePlayer = gameState.players.firstOrNull { it.color == PlayerColor.BLUE }

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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ================= 1. TOP HEADER & CONTROLS =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Button
                IconButton(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .shadow(2.dp, CircleShape)
                ) {
                    Text("🏠", fontSize = 16.sp)
                }

                // Title Banner
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L", color = LudoRed, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("U", color = LudoGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("D", color = LudoYellow, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("O", color = LudoBlue, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }

                // Action Controls: Sound, Rules, Restart
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            soundEnabled = !soundEnabled
                            SoundEffectManager.isSoundEnabled = soundEnabled
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Text(if (soundEnabled) "🔊" else "🔇", fontSize = 15.sp)
                    }

                    IconButton(
                        onClick = { showRulesDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Text("❓", fontSize = 15.sp)
                    }

                    IconButton(
                        onClick = { showRestartDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Text("🔄", fontSize = 15.sp)
                    }
                }
            }

            // ================= 2. TOP CORNER PLAYER DOCKS =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left Dock (RED)
                CornerPlayerDock(
                    player = redPlayer,
                    isCurrentTurn = currentPlayer?.color == PlayerColor.RED,
                    isRolling = gameState.isDiceRollingForPlayer == redPlayer?.id,
                    diceValue = gameState.diceResult?.value ?: 1,
                    corner = DockCorner.TOP_LEFT,
                    onDiceClick = {
                        if (currentPlayer?.color == PlayerColor.RED) {
                            viewModel.rollDice()
                        }
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Top-Right Dock (GREEN)
                CornerPlayerDock(
                    player = greenPlayer,
                    isCurrentTurn = currentPlayer?.color == PlayerColor.GREEN,
                    isRolling = gameState.isDiceRollingForPlayer == greenPlayer?.id,
                    diceValue = gameState.diceResult?.value ?: 1,
                    corner = DockCorner.TOP_RIGHT,
                    onDiceClick = {
                        if (currentPlayer?.color == PlayerColor.GREEN) {
                            viewModel.rollDice()
                        }
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // ================= 3. CENTER 15x15 BOARD CANVAS =================
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .padding(vertical = 2.dp),
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

            // ================= 4. LIVE GUIDANCE BANNER =================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentPlayerColor.copy(alpha = 0.15f))
                    .border(1.5.dp, currentPlayerColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gameState.moveMessage.ifEmpty { "${currentPlayer?.name ?: "Player"}'s turn" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentPlayerColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            // ================= 5. BOTTOM CORNER PLAYER DOCKS =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom-Left Dock (BLUE)
                CornerPlayerDock(
                    player = bluePlayer,
                    isCurrentTurn = currentPlayer?.color == PlayerColor.BLUE,
                    isRolling = gameState.isDiceRollingForPlayer == bluePlayer?.id,
                    diceValue = gameState.diceResult?.value ?: 1,
                    corner = DockCorner.BOTTOM_LEFT,
                    onDiceClick = {
                        if (currentPlayer?.color == PlayerColor.BLUE) {
                            viewModel.rollDice()
                        }
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Bottom-Right Dock (YELLOW)
                CornerPlayerDock(
                    player = yellowPlayer,
                    isCurrentTurn = currentPlayer?.color == PlayerColor.YELLOW,
                    isRolling = gameState.isDiceRollingForPlayer == yellowPlayer?.id,
                    diceValue = gameState.diceResult?.value ?: 1,
                    corner = DockCorner.BOTTOM_RIGHT,
                    onDiceClick = {
                        if (currentPlayer?.color == PlayerColor.YELLOW) {
                            viewModel.rollDice()
                        }
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        // Win Dialog & Confetti Celebration
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
                title = { Text("Restart Match?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to restart this Ludo match?") },
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
                    RuleItem("🎲", "Each player has their own corner dice dock. Tap your personal dice to roll.")
                    RuleItem("🚪", "Roll a 6 to move a piece out of your home base to the track.")
                    RuleItem("🔄", "Rolling a 6 grants you a bonus turn!")
                    RuleItem("⚠️", "Three consecutive 6s forfeits your turn.")
                    RuleItem("💥", "Landing on an opponent sends their piece flying back home + grants an extra turn!")
                    RuleItem("⭐", "Star positions and colored start squares are Safe Zones.")
                    RuleItem("🏠", "Guide all 4 pieces into the center Home triangle to win!")
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
