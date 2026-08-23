package com.example.ludo.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ludo.audio.SoundEffectManager
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
                        Color(0xFFFFF8E1),
                        Color(0xFFFFECB3),
                        Color(0xFFFFE082)
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
            // 1. TOP HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                ) {
                    Text("\uD83C\uDFE0", fontSize = 16.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("L", color = LudoRed, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("U", color = LudoGreen, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("D", color = LudoYellow, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("O", color = LudoBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }

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
                            .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                    ) {
                        Text(if (soundEnabled) "\uD83D\uDD0A" else "\uD83D\uDD07", fontSize = 15.sp)
                    }

                    IconButton(
                        onClick = { showRulesDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                    ) {
                        Text("\u2753", fontSize = 15.sp)
                    }

                    IconButton(
                        onClick = { showRestartDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE0E0E0), CircleShape)
                    ) {
                        Text("\uD83D\uDD04", fontSize = 15.sp)
                    }
                }
            }

            // 2. TOP CORNER DOCKS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

            // 3. CENTER BOARD
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

            // 4. LIVE GUIDANCE BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.5.dp, currentPlayerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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

            // 5. BOTTOM CORNER DOCKS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

        // Win Dialog
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

        // Rules Dialog
        if (showRulesDialog) {
            RulesDialog(onDismiss = { showRulesDialog = false })
        }

        // Restart Dialog
        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { showRestartDialog = false },
                containerColor = Color.White,
                titleContentColor = Color(0xFF3E2723),
                textContentColor = Color(0xFF616161),
                title = { Text("Restart Match?", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to restart this match?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showRestartDialog = false
                            viewModel.resetGame()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LudoRed)
                    ) {
                        Text("Restart", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showRestartDialog = false }
                    ) {
                        Text("Cancel", color = Color(0xFF616161))
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
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("\uD83D\uDCDC Game Rules", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleItem("\uD83C\uDFB2", "Tap your corner dice to roll. Each player has their own dice.")
                    RuleItem("\uD83D\uDEAA", "Roll a 6 to move a piece out of home base.")
                    RuleItem("\uD83D\uDD04", "Rolling 6 grants a bonus turn. Three 6s forfeits turn.")
                    RuleItem("\uD83D\uDCA5", "Land on an opponent to capture them + get bonus turn!")
                    RuleItem("\u2B50", "Stars and start squares are Safe Zones.")
                    RuleItem("\uD83D\uDC0D", "Snakes slide you backward on the track!")
                    RuleItem("\uD83E\uDE9C", "Ladders boost you forward on the track!")
                    RuleItem("\u2699\uFE0F", "Auto-move triggers when only 1 piece can move.")
                    RuleItem("\uD83C\uDFE0", "Guide all 4 pieces to center Home to win!")
                }

                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LudoGreen)
                ) {
                    Text("Got It!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RuleItem(icon: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(icon, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFF616161), lineHeight = 18.sp)
    }
}
