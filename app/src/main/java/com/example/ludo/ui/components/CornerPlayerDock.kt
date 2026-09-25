package com.example.ludo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.Player
import com.example.ludo.model.PlayerColor
import com.example.ludo.theme.*

private val DockShape = RoundedCornerShape(16.dp)

/** Which side of the dock the dice sits on, so dice hug the outer screen edges. */
enum class DiceSide { START, END }

@Composable
fun CornerPlayerDock(
    player: Player?,
    isCurrentTurn: Boolean,
    canRoll: Boolean,
    isRolling: Boolean,
    diceValue: Int,
    diceSide: DiceSide,
    onDiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (player == null) {
        Box(modifier = modifier)
        return
    }

    val playerColor = PlayerColorUtils.getComposeColor(player.color)
    val border by animateColorAsState(if (isCurrentTurn) playerColor else HairlineBorder, label = "dockBorder")
    val borderWidth by animateDpAsState(if (isCurrentTurn) 2.dp else 1.dp, label = "dockBorderWidth")
    val elevation by animateDpAsState(if (isCurrentTurn) 6.dp else 1.dp, label = "dockElevation")

    BoxWithConstraints(modifier) {
        // The avatar only fits once the dock is roomy (landscape, tablets); names come first.
        val showAvatar = maxWidth >= 210.dp
        Row(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation, DockShape, ambientColor = playerColor, spotColor = if (isCurrentTurn) playerColor else InkDark)
                .clip(DockShape)
                .background(SurfaceWhite)
                .border(borderWidth, border, DockShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val dice = @Composable {
                DiceView(
                    diceValue = diceValue,
                    isRolling = isRolling,
                    enabled = canRoll,
                    onClick = onDiceClick,
                    playerColor = playerColor
                )
            }
            if (diceSide == DiceSide.START) dice()
            PlayerInfo(player, playerColor, isCurrentTurn, showAvatar, alignEnd = diceSide == DiceSide.END, modifier = Modifier.weight(1f))
            if (diceSide == DiceSide.END) dice()
        }
    }
}

@Composable
private fun PlayerInfo(
    player: Player,
    playerColor: Color,
    isCurrentTurn: Boolean,
    showAvatar: Boolean,
    alignEnd: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        if (showAvatar && !alignEnd) {
            PawnAvatar(player.color)
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start, modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = player.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentTurn) playerColor else InkDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (player.hasFinished) {
                Text(rankLabel(player.rank), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = playerColor)
            } else {
                Text(if (player.isAI) "Computer" else "Human", fontSize = 10.sp, color = InkMuted, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                TokenIndicatorRow(tokens = player.tokens, playerColor = playerColor, dotSize = 7.dp, spacing = 3.dp)
            }
        }
        if (showAvatar && alignEnd) {
            Spacer(Modifier.width(8.dp))
            PawnAvatar(player.color)
        }
    }
}

/** Player avatar: their pawn on a soft tint of their colour. */
@Composable
fun PawnAvatar(color: PlayerColor, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 30.dp) {
    Canvas(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(PlayerColorUtils.getLightColor(color))
    ) {
        val r = this.size.minDimension * 0.3f
        drawPawn(center = Offset(this.size.width / 2, this.size.height / 2 + pawnVisualCenterOffset(r)), radius = r, color = color)
    }
}

fun rankLabel(rank: Int): String = when (rank) {
    1 -> "1st place"
    2 -> "2nd place"
    3 -> "3rd place"
    else -> "${rank}th place"
}
