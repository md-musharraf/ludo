package com.example.ludo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ludo.core.settings.AppSettings
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
    var soundEnabled by remember { mutableStateOf(AppSettings.soundEnabled) }

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
            .background(AppBackground)
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
                .padding(horizontal = metrics.contentPadding, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(
                metrics = metrics,
                soundEnabled = soundEnabled,
                onHome = requestLeave,
                onToggleSound = {
                    soundEnabled = !soundEnabled
                    AppSettings.soundEnabled = soundEnabled
                },
                onRules = { showRulesDialog = true },
                onRestart = { showRestartDialog = true }
            )

            // Play area: docks, board and status travel together as one centred group.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(metrics.groupSpacing, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
    val headerHeight: Dp = if (isCompact) 44.dp else 52.dp
    val dockHeight: Dp = if (isCompact) 58.dp else 66.dp
    val dockWidth: Dp = if (isCompact) 190.dp else 230.dp
    val bannerHeight: Dp = if (isCompact) 34.dp else 40.dp
    val contentPadding: Dp = if (isCompact) 8.dp else 12.dp
    val iconButtonSize: Dp = if (isCompact) 36.dp else 40.dp
    val titleFontSize: TextUnit = if (isCompact) 22.sp else 26.sp
    val bannerFontSize: TextUnit = if (isCompact) 12.sp else 13.sp
    val groupSpacing: Dp = if (isCompact) 8.dp else 14.dp
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
            .height(metrics.headerHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircleButton(Icons.Rounded.Home, "Home", onHome, size = metrics.iconButtonSize)
        LudoTitle(fontSize = metrics.titleFontSize)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconCircleButton(
                if (soundEnabled) LudoIcons.VolumeOn else LudoIcons.VolumeOff,
                if (soundEnabled) "Mute sound" else "Turn sound on",
                onToggleSound,
                size = metrics.iconButtonSize
            )
            IconCircleButton(Icons.Rounded.Info, "How to play", onRules, size = metrics.iconButtonSize)
            IconCircleButton(Icons.Rounded.Refresh, "Restart match", onRestart, size = metrics.iconButtonSize)
        }
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
        diceValue = (if (isCurrent) state.diceResult?.value else null) ?: player?.let { lastRolls[it.id] } ?: 1,
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
            .padding(2.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        LudoBoard(gameState = gameState, onTokenClick = onTokenClick, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun GuidanceBanner(gameState: GameState, metrics: ScreenMetrics) {
    val currentPlayer = gameState.currentPlayer
    val color = PlayerColorUtils.getComposeColor(currentPlayer?.color)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(metrics.bannerHeight)
            .shadow(1.dp, CircleShape, ambientColor = InkDark, spotColor = InkDark)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .border(1.dp, HairlineBorder, CircleShape)
            .padding(horizontal = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = gameState.moveMessage.ifEmpty { "${currentPlayer?.name ?: "Player"}'s turn" },
            fontSize = metrics.bannerFontSize,
            fontWeight = FontWeight.SemiBold,
            color = InkDark,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

