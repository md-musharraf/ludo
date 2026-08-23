package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.TokenState
import com.example.ludo.theme.*

enum class DockCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
}

@Composable
fun CornerPlayerDock(
    player: Player?,
    isCurrentTurn: Boolean,
    isRolling: Boolean,
    diceValue: Int,
    corner: DockCorner,
    onDiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (player == null) {
        // Empty slot placeholder
        Box(modifier = modifier.size(100.dp, 60.dp))
        return
    }

    val playerColor = getPlayerComposeColor(player.color)
    val lightColor = getPlayerLightColor(player.color)

    val infiniteTransition = rememberInfiniteTransition(label = "dockPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    val isTop = corner == DockCorner.TOP_LEFT || corner == DockCorner.TOP_RIGHT
    val isLeft = corner == DockCorner.TOP_LEFT || corner == DockCorner.BOTTOM_LEFT

    val tokensHome = player.tokens.count { it.state == TokenState.IN_HOME }
    val tokensFinished = player.tokens.count { it.state == TokenState.FINISHED }
    val tokensOnBoard = player.tokens.count { it.state == TokenState.ON_BOARD || it.state == TokenState.IN_HOME_COLUMN }

    Row(
        modifier = modifier
            .scale(if (isCurrentTurn) pulseScale else 1f)
            .then(
                if (isCurrentTurn) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = playerColor.copy(alpha = haloAlpha),
                        spotColor = playerColor
                    )
                } else {
                    Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                }
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isCurrentTurn) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            lightColor.copy(alpha = 0.35f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF7F7F7)
                        )
                    )
                }
            )
            .then(
                if (isCurrentTurn) {
                    Modifier.border(2.5.dp, playerColor, RoundedCornerShape(16.dp))
                } else {
                    Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
                }
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLeft) {
            // Dice on the left, Avatar & details on the right
            DiceView(
                diceValue = diceValue,
                isRolling = isRolling,
                enabled = isCurrentTurn && !player.isAI,
                playerColor = playerColor,
                onClick = onDiceClick
            )
            PlayerInfoSection(
                player = player,
                playerColor = playerColor,
                isCurrentTurn = isCurrentTurn,
                tokensFinished = tokensFinished,
                tokensOnBoard = tokensOnBoard,
                tokensHome = tokensHome
            )
        } else {
            // Avatar & details on the left, Dice on the right
            PlayerInfoSection(
                player = player,
                playerColor = playerColor,
                isCurrentTurn = isCurrentTurn,
                tokensFinished = tokensFinished,
                tokensOnBoard = tokensOnBoard,
                tokensHome = tokensHome
            )
            DiceView(
                diceValue = diceValue,
                isRolling = isRolling,
                enabled = isCurrentTurn && !player.isAI,
                playerColor = playerColor,
                onClick = onDiceClick
            )
        }
    }
}

@Composable
private fun PlayerInfoSection(
    player: Player,
    playerColor: Color,
    isCurrentTurn: Boolean,
    tokensFinished: Int,
    tokensOnBoard: Int,
    tokensHome: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Avatar + Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(playerColor)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (player.isAI) "🤖" else "👤",
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = player.name,
                fontSize = 12.sp,
                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isCurrentTurn) playerColor else Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Finished & Active token dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            player.tokens.forEach { tok ->
                when (tok.state) {
                    TokenState.FINISHED -> {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(playerColor)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                    TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, playerColor, CircleShape)
                        )
                    }
                    TokenState.IN_HOME -> {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD4D4D4))
                        )
                    }
                }
            }
        }
    }
}

private fun getPlayerComposeColor(color: PlayerColor): Color {
    return when (color) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
}

private fun getPlayerLightColor(color: PlayerColor): Color {
    return when (color) {
        PlayerColor.RED -> LudoRedLight
        PlayerColor.GREEN -> LudoGreenLight
        PlayerColor.YELLOW -> LudoYellowLight
        PlayerColor.BLUE -> LudoBlueLight
    }
}
