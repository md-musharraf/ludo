package com.example.ludo.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ludo.audio.SoundEffectManager
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.AiDifficulty
import com.example.ludo.model.GamePhase
import com.example.ludo.model.GameState
import com.example.ludo.model.PlayerColor
import com.example.ludo.theme.*
import com.example.ludo.ui.components.*
import com.example.ludo.viewmodel.GameViewModel

/** Board corner each colour's dock sits beside, and which side its dice cup is on. */
private val DockSides = mapOf(
    PlayerColor.RED to DiceSide.START,
    PlayerColor.GREEN to DiceSide.END,
    PlayerColor.BLUE to DiceSide.START,
    PlayerColor.YELLOW to DiceSide.END
)

@Composable
fun GameScreen(
    playerCount: Int,
    isVsAI: Boolean,
    aiDifficulty: AiDifficulty,
    onNavigateHome: () -> Unit,
) {
    // Scoped to the navigation entry: survives rotation, cleared (with its engine) on exit.
    val viewModel = viewModel { GameViewModel(playerCount, isVsAI, aiDifficulty) }
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()

    var showRulesDialog by remember { mutableStateOf(false) }
    var showRestartDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(SoundEffectManager.isSoundEnabled) }

    // Each cup keeps showing its owner's last roll instead of every cup mirroring the current one.
    val lastRolls = remember { mutableStateMapOf<Int, Int>() }
    LaunchedEffect(gameState.diceResult, gameState.currentPlayerIndex) {
        val roll = gameState.diceResult ?: return@LaunchedEffect
        gameState.currentPlayer?.let { lastRolls[it.id] = roll.value }
    }

    KeepScreenOn()

    val requestLeave: () -> Unit = {
        if (gameState.isGameOver) onNavigateHome() else showLeaveDialog = true
    }
    BackHandler(onBack = requestLeave)

    val restart = {
        lastRolls.clear()
        viewModel.resetGame()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackgroundBrush)
            .safeDrawingPadding()
    ) {
        val isLandscape = maxWidth > maxHeight
        val isCompact = if (isLandscape) maxHeight < 480.dp else maxHeight < 680.dp
        val metrics = ScreenMetrics(isCompact)

        val dock: @Composable (PlayerColor, Modifier) -> Unit = { color, modifier ->
            PlayerDock(gameState, color, lastRolls, viewModel::rollDice, modifier)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = metrics.contentPadding, vertical = 2.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(
                metrics = metrics,
                soundEnabled = soundEnabled,
                onHome = requestLeave,
                onToggleSound = {
                    soundEnabled = !soundEnabled
                    SoundEffectManager.isSoundEnabled = soundEnabled
                },
                onRules = { showRulesDialog = true },
                onRestart = { showRestartDialog = true }
            )

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockColumn(metrics) {
                        dock(PlayerColor.RED, it)
                        dock(PlayerColor.BLUE, it)
                    }
                    Board(gameState, viewModel::selectToken, Modifier.fillMaxHeight())
                    DockColumn(metrics) {
                        dock(PlayerColor.GREEN, it)
                        dock(PlayerColor.YELLOW, it)
                    }
                }
            } else {
                DockRow(metrics) {
                    dock(PlayerColor.RED, it)
                    dock(PlayerColor.GREEN, it)
                }
                Board(gameState, viewModel::selectToken, Modifier.weight(1f, fill = false))
            }

            GuidanceBanner(gameState, metrics)

            if (!isLandscape) {
                DockRow(metrics) {
                    dock(PlayerColor.BLUE, it)
                    dock(PlayerColor.YELLOW, it)
                }
            }
        }

        if (gameState.isGameOver) {
            ParticleEffect(modifier = Modifier.fillMaxSize())
            WinDialog(standings = gameState.standings, onPlayAgain = restart, onHome = onNavigateHome)
        }

        if (showRulesDialog) {
            RulesDialog(onDismiss = { showRulesDialog = false })
        }

        if (showRestartDialog) {
            ConfirmDialog(
                title = "Restart Match?",
                message = "Are you sure you want to restart this match?",
                confirmLabel = "Restart",
                onConfirm = {
                    showRestartDialog = false
                    restart()
                },
                onDismiss = { showRestartDialog = false }
            )
        }

        if (showLeaveDialog) {
            ConfirmDialog(
                title = "Leave Match?",
                message = "Your current match progress will be lost.",
                confirmLabel = "Leave",
                onConfirm = {
                    showLeaveDialog = false
                    onNavigateHome()
                },
                onDismiss = { showLeaveDialog = false }
            )
        }
    }
}

private class ScreenMetrics(isCompact: Boolean) {
    val headerHeight: Dp = if (isCompact) 36.dp else 42.dp
    val dockHeight: Dp = if (isCompact) 56.dp else 66.dp
    val dockWidth: Dp = if (isCompact) 170.dp else 200.dp
    val bannerHeight: Dp = if (isCompact) 28.dp else 34.dp
    val contentPadding: Dp = if (isCompact) 4.dp else 8.dp
    val iconButtonSize: Dp = if (isCompact) 32.dp else 36.dp
    val iconFontSize: TextUnit = if (isCompact) 13.sp else 15.sp
    val titleFontSize: TextUnit = if (isCompact) 20.sp else 22.sp
    val bannerFontSize: TextUnit = if (isCompact) 11.sp else 12.sp
}

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun GameHeader(
    metrics: ScreenMetrics,
    soundEnabled: Boolean,
    onHome: () -> Unit,
    onToggleSound: () -> Unit,
    onRules: () -> Unit,
    onRestart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.headerHeight)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderButton("🏠", "Home", metrics, onHome)
        LudoTitle(fontSize = metrics.titleFontSize)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            HeaderButton(if (soundEnabled) "🔊" else "🔇", if (soundEnabled) "Mute" else "Unmute", metrics, onToggleSound)
            HeaderButton("❓", "Rules", metrics, onRules)
            HeaderButton("🔄", "Restart", metrics, onRestart)
        }
    }
}

@Composable
private fun HeaderButton(icon: String, label: String, metrics: ScreenMetrics, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(metrics.iconButtonSize)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, CardBorderWarm, CircleShape)
            .semantics { contentDescription = label }
    ) {
        Text(icon, fontSize = metrics.iconFontSize)
    }
}

@Composable
private fun PlayerDock(
    state: GameState,
    color: PlayerColor,
    lastRolls: Map<Int, Int>,
    onRoll: () -> Unit,
    modifier: Modifier
) {
    val player = state.players.firstOrNull { it.color == color }
    val isCurrent = player != null && state.currentPlayer?.id == player.id
    CornerPlayerDock(
        player = player,
        isCurrentTurn = isCurrent,
        // Only glow/accept taps when this human can actually roll right now.
        canRoll = isCurrent && player?.isAI == false && state.gamePhase == GamePhase.WAITING_FOR_ROLL &&
            state.isDiceRollingForPlayer == null,
        isRolling = player != null && state.isDiceRollingForPlayer == player.id,
        diceValue = player?.let { lastRolls[it.id] } ?: 1,
        diceSide = DockSides.getValue(color),
        onDiceClick = { if (isCurrent) onRoll() },
        modifier = modifier
    )
}

@Composable
private fun DockRow(metrics: ScreenMetrics, content: @Composable (Modifier) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.dockHeight),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content(Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun DockColumn(metrics: ScreenMetrics, content: @Composable (Modifier) -> Unit) {
    Column(
        modifier = Modifier
            .width(metrics.dockWidth)
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        content(Modifier.fillMaxWidth().height(metrics.dockHeight))
    }
}

@Composable
private fun Board(gameState: GameState, onTokenClick: (Int) -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        LudoBoard(gameState = gameState, onTokenClick = onTokenClick, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun GuidanceBanner(gameState: GameState, metrics: ScreenMetrics) {
    val currentPlayer = gameState.currentPlayer
    val color = PlayerColorUtils.getComposeColor(currentPlayer?.color)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.bannerHeight)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = gameState.moveMessage.ifEmpty { "${currentPlayer?.name ?: "Player"}'s turn" },
            fontSize = metrics.bannerFontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val Rules = listOf(
    "🎲" to "Tap your corner Dice Cup to roll. Each player has their own cup.",
    "🚪" to "Roll a 6 to move a piece out of your home base.",
    "🔄" to "Rolling a 6 grants a bonus roll. Three 6s in a row forfeits the turn.",
    "💥" to "Land on a lone opponent piece to capture it and earn a bonus roll.",
    "🧱" to "Two pieces of one colour on a plain square form a block nobody can land on.",
    "⭐" to "Star squares and coloured starting squares are Safe Zones.",
    "🏠" to "Pieces must land exactly on Home. Reaching Home earns a bonus roll.",
    "🥇" to "Get all 4 pieces Home first to win; others race on for 2nd and 3rd."
)

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
                Text("📜 Game Rules", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((icon, text) in Rules) {
                        Row(verticalAlignment = Alignment.Top) {
                            Text(icon, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                            Text(text, fontSize = 13.sp, color = Color(0xFF616161), lineHeight = 18.sp)
                        }
                    }
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
