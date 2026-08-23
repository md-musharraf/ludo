package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.model.TokenState
import com.example.ludo.theme.*

@Composable
fun PlayerPanel(
    players: List<Player>,
    currentPlayerIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        players.forEachIndexed { index, player ->
            PlayerCard(
                player = player,
                isCurrent = index == currentPlayerIndex,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlayerCard(
    player: Player,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "playerCardPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val color = getPlayerComposeColor(player.color)
    val lightColor = getPlayerLightColor(player.color)

    val tokensHome = player.tokens.count { it.state == TokenState.IN_HOME }
    val tokensFinished = player.tokens.count { it.state == TokenState.FINISHED }
    val tokensOnBoard = player.tokens.count { it.state == TokenState.ON_BOARD || it.state == TokenState.IN_HOME_COLUMN }

    Box(
        modifier = modifier
            .scale(if (isCurrent) pulseScale else 1f)
            .then(
                if (isCurrent) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = color.copy(alpha = glowAlpha),
                        spotColor = color
                    )
                } else {
                    Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
                }
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isCurrent) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            lightColor.copy(alpha = 0.25f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF9F9F9)
                        )
                    )
                }
            )
            .then(
                if (isCurrent) {
                    Modifier.border(2.5.dp, color, RoundedCornerShape(14.dp))
                } else {
                    Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                }
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Row: Avatar icon + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Color Dot Avatar
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (player.isAI) "🤖" else "👤",
                        fontSize = 7.sp
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = player.name,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) color else Color(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 4 Token Dots:
            // Green filled = Finished
            // Colored ring = Active on Board
            // Gray = In Base
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
                                    .background(color)
                                    .border(1.dp, Color.White, CircleShape)
                            )
                        }
                        TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(2.dp, color, CircleShape)
                            )
                        }
                        TokenState.IN_HOME -> {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD0D0D0))
                            )
                        }
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
