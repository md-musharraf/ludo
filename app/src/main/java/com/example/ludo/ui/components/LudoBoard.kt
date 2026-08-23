package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.ludo.engine.BoardConfig
import com.example.ludo.model.*
import com.example.ludo.theme.*
import kotlin.math.min
import kotlin.math.sin

private data class TokenRenderInfo(
    val playerId: Int,
    val tokenId: Int,
    val playerColor: PlayerColor,
    val center: Offset,
    val radius: Float,
    val isValid: Boolean,
    val isCurrentPlayer: Boolean,
    val isAnimating: Boolean = false,
    val shadowOffset: Offset = Offset(2.5f, 3.5f),
    val shadowRadius: Float = 1.05f
)

@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "boardAnimations")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val portalPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portalPulse"
    )

    var renderedTokens by remember { mutableStateOf<List<TokenRenderInfo>>(emptyList()) }

    Canvas(
        modifier = modifier.pointerInput(gameState) {
            detectTapGestures { tapOffset ->
                val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex) ?: return@detectTapGestures

                for (info in renderedTokens.reversed()) {
                    if (info.playerId == currentPlayer.id && info.isValid) {
                        val dx = tapOffset.x - info.center.x
                        val dy = tapOffset.y - info.center.y
                        val hitDistance = info.radius * 1.6f
                        if (dx * dx + dy * dy <= hitDistance * hitDistance) {
                            onTokenClick(info.tokenId)
                            return@detectTapGestures
                        }
                    }
                }
            }
        }
    ) {
        val boardSize = min(size.width, size.height)
        val cellSize = boardSize / 15f
        val offsetX = (size.width - boardSize) / 2f
        val offsetY = (size.height - boardSize) / 2f

        val tokenList = mutableListOf<TokenRenderInfo>()
        val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex)

        // 1. Board Background with warm classic styling
        drawRoundRect(
            color = Color(0x33000000),
            topLeft = Offset(offsetX + 4f, offsetY + 6f),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.4f)
        )
        drawRoundRect(
            color = BoardBackground,
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.4f)
        )

        // 2. Four Home Bases
        drawHomeBase(offsetX, offsetY, cellSize, 0, 0, LudoRed, LudoRedLight, currentPlayer?.color == PlayerColor.RED, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 0, 9, LudoGreen, LudoGreenLight, currentPlayer?.color == PlayerColor.GREEN, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 9, 9, LudoYellow, LudoYellowLight, currentPlayer?.color == PlayerColor.YELLOW, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 9, 0, LudoBlue, LudoBlueLight, currentPlayer?.color == PlayerColor.BLUE, pulseAlpha)

        // 3. Track Cells
        drawTrackCells(offsetX, offsetY, cellSize)

        // 4. Starting Squares
        drawStartingSquares(offsetX, offsetY, cellSize)

        // 5. Safe Zones
        drawSafeZones(offsetX, offsetY, cellSize)

        // 6. Symmetrical Snakes & Ladders on the Board
        drawSnakesAndLaddersPortals(offsetX, offsetY, cellSize, portalPulse)

        // 7. Home Columns
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.redHomeColumn, LudoRed, LudoRedLight)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.greenHomeColumn, LudoGreen, LudoGreenLight)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.yellowHomeColumn, LudoYellow, LudoYellowLight)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.blueHomeColumn, LudoBlue, LudoBlueLight)

        // 8. Center Home Triangles
        drawCenterHome(offsetX, offsetY, cellSize)

        // 9. Outer Wood Border Frame
        drawRoundRect(
            color = BoardBorder,
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.4f),
            style = Stroke(width = 3.5f)
        )

        // 10. Collect Home Base Tokens
        for (player in gameState.players) {
            val colorOrdinal = player.color.ordinal
            for (token in player.tokens) {
                if (token.state == TokenState.IN_HOME) {
                    val homePos = BoardConfig.homePositions[colorOrdinal]?.getOrNull(token.id)
                    if (homePos != null) {
                        val cx = offsetX + homePos.second * cellSize + cellSize / 2
                        val cy = offsetY + homePos.first * cellSize + cellSize / 2
                        val isCurr = player.id == currentPlayer?.id
                        val isValid = isCurr && gameState.validMoves.contains(token.id) && gameState.gamePhase == GamePhase.WAITING_FOR_MOVE
                        val radius = cellSize * 0.38f

                        tokenList.add(
                            TokenRenderInfo(
                                playerId = player.id,
                                tokenId = token.id,
                                playerColor = player.color,
                                center = Offset(cx, cy),
                                radius = radius,
                                isValid = isValid,
                                isCurrentPlayer = isCurr
                            )
                        )
                    }
                }
            }
        }

        // 11. Collect Board Tokens with Stacking
        val boardTokensByCell = mutableMapOf<Pair<Int, Int>, MutableList<Pair<Player, Token>>>()
        for (player in gameState.players) {
            for (token in player.tokens) {
                if (token.state == TokenState.ON_BOARD || token.state == TokenState.IN_HOME_COLUMN) {
                    token.boardPosition?.let { pos ->
                        boardTokensByCell.getOrPut(pos) { mutableListOf() }.add(Pair(player, token))
                    }
                }
            }
        }

        for ((cellPos, tokensInCell) in boardTokensByCell) {
            val (row, col) = cellPos
            val cellCenterX = offsetX + col * cellSize + cellSize / 2
            val cellCenterY = offsetY + row * cellSize + cellSize / 2
            val count = tokensInCell.size

            tokensInCell.forEachIndexed { index, (player, token) ->
                if (gameState.animatingTokenId == token.id && gameState.animatingPlayerId == player.id && gameState.animatingFromPos != null) {
                    return@forEachIndexed
                }

                val (offsetXShift, offsetYShift, tokenRadius) = when (count) {
                    1 -> Triple(0f, 0f, cellSize * 0.38f)
                    2 -> {
                        val d = cellSize * 0.16f
                        if (index == 0) Triple(-d, -d, cellSize * 0.28f)
                        else Triple(d, d, cellSize * 0.28f)
                    }
                    3 -> {
                        val d = cellSize * 0.16f
                        when (index) {
                            0 -> Triple(0f, -d, cellSize * 0.24f)
                            1 -> Triple(-d, d * 0.9f, cellSize * 0.24f)
                            else -> Triple(d, d * 0.9f, cellSize * 0.24f)
                        }
                    }
                    else -> {
                        val d = cellSize * 0.18f
                        when (index % 4) {
                            0 -> Triple(-d, -d, cellSize * 0.22f)
                            1 -> Triple(d, -d, cellSize * 0.22f)
                            2 -> Triple(-d, d, cellSize * 0.22f)
                            else -> Triple(d, d, cellSize * 0.22f)
                        }
                    }
                }

                val cx = cellCenterX + offsetXShift
                val cy = cellCenterY + offsetYShift
                val isCurr = player.id == currentPlayer?.id
                val isValid = isCurr && gameState.validMoves.contains(token.id) && gameState.gamePhase == GamePhase.WAITING_FOR_MOVE

                tokenList.add(
                    TokenRenderInfo(
                        playerId = player.id,
                        tokenId = token.id,
                        playerColor = player.color,
                        center = Offset(cx, cy),
                        radius = tokenRadius,
                        isValid = isValid,
                        isCurrentPlayer = isCurr
                    )
                )
            }
        }

        // 12. Parabolic Hop Animation
        if (gameState.animatingPlayerId != null && gameState.animatingTokenId != null &&
            gameState.animatingFromPos != null && gameState.animatingToPos != null) {

            val from = gameState.animatingFromPos
            val to = gameState.animatingToPos
            val progress = gameState.animatingHopProgress

            val fromX = offsetX + from.second * cellSize + cellSize / 2
            val fromY = offsetY + from.first * cellSize + cellSize / 2
            val toX = offsetX + to.second * cellSize + cellSize / 2
            val toY = offsetY + to.first * cellSize + cellSize / 2

            val curX = fromX + (toX - fromX) * progress
            val curY = fromY + (toY - fromY) * progress
            val hopHeight = sin(progress * Math.PI).toFloat() * cellSize * 0.7f
            val elevatedY = curY - hopHeight
            val elevatedScale = 1f + sin(progress * Math.PI).toFloat() * 0.3f
            val tokenRadius = cellSize * 0.38f * elevatedScale

            val player = gameState.players.firstOrNull { it.id == gameState.animatingPlayerId }
            if (player != null) {
                tokenList.add(
                    TokenRenderInfo(
                        playerId = player.id,
                        tokenId = gameState.animatingTokenId,
                        playerColor = player.color,
                        center = Offset(curX, elevatedY),
                        radius = tokenRadius,
                        isValid = false,
                        isCurrentPlayer = true,
                        isAnimating = true,
                        shadowOffset = Offset(2f, hopHeight + 3.5f),
                        shadowRadius = 1f + hopHeight / 8f
                    )
                )
            }
        }

        // 13. Draw Tokens
        for (info in tokenList.sortedBy { if (it.isAnimating) 1 else 0 }) {
            drawLudoToken(
                center = info.center,
                radius = info.radius,
                color = getPlayerColor(info.playerColor),
                tokenId = info.tokenId,
                isValid = info.isValid,
                pulseAlpha = pulseAlpha,
                pulseScale = pulseScale,
                shadowOffset = info.shadowOffset,
                shadowRadius = info.shadowRadius
            )
        }

        renderedTokens = tokenList
    }
}

private fun DrawScope.drawSnakesAndLaddersPortals(
    offsetX: Float, offsetY: Float, cellSize: Float, portalPulse: Float
) {
    // 1. Draw Ladders: Classic green ladders with rungs
    for (ladder in BoardConfig.ladders) {
        val startX = offsetX + ladder.fromPos.second * cellSize + cellSize / 2
        val startY = offsetY + ladder.fromPos.first * cellSize + cellSize / 2
        val endX = offsetX + ladder.toPos.second * cellSize + cellSize / 2
        val endY = offsetY + ladder.toPos.first * cellSize + cellSize / 2

        drawLine(
            color = LudoGreen.copy(alpha = 0.75f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.5f
        )

        drawCircle(
            color = LudoGreen.copy(alpha = 0.2f),
            radius = cellSize * 0.38f,
            center = Offset(startX, startY)
        )
        drawCircle(
            color = LudoGreen,
            radius = cellSize * 0.24f,
            center = Offset(startX, startY),
            style = Stroke(width = 1.5f)
        )

        drawCircle(
            color = LudoGreen.copy(alpha = 0.2f),
            radius = cellSize * 0.38f,
            center = Offset(endX, endY)
        )
    }

    // 2. Draw Snakes: Classic orange/red snake curves
    for (snake in BoardConfig.snakes) {
        val headX = offsetX + snake.fromPos.second * cellSize + cellSize / 2
        val headY = offsetY + snake.fromPos.first * cellSize + cellSize / 2
        val tailX = offsetX + snake.toPos.second * cellSize + cellSize / 2
        val tailY = offsetY + snake.toPos.first * cellSize + cellSize / 2

        val midX = (headX + tailX) / 2 + (headY - tailY) * 0.2f
        val midY = (headY + tailY) / 2 + (tailX - headX) * 0.2f

        val snakePath = Path().apply {
            moveTo(headX, headY)
            quadraticTo(midX, midY, tailX, tailY)
        }

        drawPath(
            snakePath,
            color = LudoRed.copy(alpha = 0.75f),
            style = Stroke(width = 3.5f)
        )

        drawCircle(
            color = LudoRed.copy(alpha = 0.2f),
            radius = cellSize * 0.38f,
            center = Offset(headX, headY)
        )
        drawCircle(
            color = LudoRed,
            radius = cellSize * 0.24f,
            center = Offset(headX, headY),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawHomeBase(
    offsetX: Float, offsetY: Float, cellSize: Float,
    startRow: Int, startCol: Int, color: Color, lightColor: Color,
    isCurrentPlayer: Boolean, pulseAlpha: Float
) {
    val x = offsetX + startCol * cellSize
    val y = offsetY + startRow * cellSize
    val homeSize = cellSize * 6

    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(homeSize, homeSize),
        cornerRadius = CornerRadius(cellSize * 0.4f)
    )

    if (isCurrentPlayer) {
        drawRoundRect(
            color = color.copy(alpha = pulseAlpha * 0.6f),
            topLeft = Offset(x - 2.5f, y - 2.5f),
            size = Size(homeSize + 5f, homeSize + 5f),
            cornerRadius = CornerRadius(cellSize * 0.4f),
            style = Stroke(width = 3.5f)
        )
    }

    val innerMargin = cellSize * 0.85f
    val innerSize = homeSize - innerMargin * 2
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(cellSize * 0.35f)
    )

    val spotRadius = cellSize * 0.45f
    val spotCenters = listOf(
        Offset(x + homeSize * 0.32f, y + homeSize * 0.32f),
        Offset(x + homeSize * 0.68f, y + homeSize * 0.32f),
        Offset(x + homeSize * 0.32f, y + homeSize * 0.68f),
        Offset(x + homeSize * 0.68f, y + homeSize * 0.68f),
    )

    spotCenters.forEach { center ->
        drawCircle(color = color, radius = spotRadius, center = center)
        drawCircle(color = Color.White, radius = spotRadius * 0.6f, center = center)
    }
}

private fun DrawScope.drawTrackCells(offsetX: Float, offsetY: Float, cellSize: Float) {
    for (pos in BoardConfig.mainTrack) {
        val (row, col) = pos
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        drawRect(
            color = TrackWhite,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawStartingSquares(offsetX: Float, offsetY: Float, cellSize: Float) {
    val starts = listOf(
        Triple(BoardConfig.RED_START_INDEX, LudoRed, "↓"),
        Triple(BoardConfig.GREEN_START_INDEX, LudoGreen, "→"),
        Triple(BoardConfig.YELLOW_START_INDEX, LudoYellow, "↑"),
        Triple(BoardConfig.BLUE_START_INDEX, LudoBlue, "←")
    )
    for ((index, color, _) in starts) {
        val (row, col) = BoardConfig.mainTrack[index]
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawHomeColumn(
    offsetX: Float, offsetY: Float, cellSize: Float,
    positions: List<Pair<Int, Int>>, color: Color, lightColor: Color
) {
    positions.forEachIndexed { _, (row, col) ->
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1f)
        )
    }
}

private fun DrawScope.drawSafeZones(offsetX: Float, offsetY: Float, cellSize: Float) {
    for (index in BoardConfig.safeSpotsIndices) {
        val (row, col) = BoardConfig.mainTrack[index]
        val cx = offsetX + col * cellSize + cellSize / 2
        val cy = offsetY + row * cellSize + cellSize / 2

        if (index == BoardConfig.RED_START_INDEX ||
            index == BoardConfig.GREEN_START_INDEX ||
            index == BoardConfig.YELLOW_START_INDEX ||
            index == BoardConfig.BLUE_START_INDEX) {
            drawStar(cx, cy, cellSize * 0.32f, Color.White, Color.White)
        } else {
            drawStar(cx, cy, cellSize * 0.32f, SafeZoneStar, Color(0xFFF57F17))
        }
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, fillColor: Color, strokeColor: Color) {
    val points = 5
    val path = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.42f
        val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
        val x = cx + r * Math.cos(angle).toFloat()
        val y = cy + r * Math.sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = strokeColor, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawCenterHome(offsetX: Float, offsetY: Float, cellSize: Float) {
    val centerX = offsetX + 7.5f * cellSize
    val centerY = offsetY + 7.5f * cellSize
    val triangleSpan = cellSize * 1.5f

    val triangleDefs = listOf(
        Pair(
            listOf(Offset(centerX - triangleSpan, centerY - triangleSpan),
                   Offset(centerX, centerY),
                   Offset(centerX - triangleSpan, centerY + triangleSpan)),
            LudoRed
        ),
        Pair(
            listOf(Offset(centerX - triangleSpan, centerY - triangleSpan),
                   Offset(centerX + triangleSpan, centerY - triangleSpan),
                   Offset(centerX, centerY)),
            LudoGreen
        ),
        Pair(
            listOf(Offset(centerX + triangleSpan, centerY - triangleSpan),
                   Offset(centerX + triangleSpan, centerY + triangleSpan),
                   Offset(centerX, centerY)),
            LudoYellow
        ),
        Pair(
            listOf(Offset(centerX - triangleSpan, centerY + triangleSpan),
                   Offset(centerX + triangleSpan, centerY + triangleSpan),
                   Offset(centerX, centerY)),
            LudoBlue
        )
    )

    for ((points, color) in triangleDefs) {
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            lineTo(points[2].x, points[2].y)
            close()
        }
        drawPath(path, color = color, style = Fill)
    }

    drawCircle(
        color = Color.White,
        radius = cellSize * 0.42f,
        center = Offset(centerX, centerY)
    )
    drawStar(centerX, centerY, cellSize * 0.26f, SafeZoneStar, Color(0xFFF57F17))
}

private fun DrawScope.drawLudoToken(
    center: Offset,
    radius: Float,
    color: Color,
    tokenId: Int,
    isValid: Boolean,
    pulseAlpha: Float,
    pulseScale: Float,
    shadowOffset: Offset = Offset(2.5f, 3.5f),
    shadowRadius: Float = 1.05f
) {
    val (cx, cy) = center.x to center.y

    if (isValid) {
        drawCircle(
            color = color.copy(alpha = pulseAlpha * 0.5f),
            radius = radius * pulseScale * 1.4f,
            center = Offset(cx, cy)
        )
    }

    drawCircle(
        color = Color(0x33000000),
        radius = radius * shadowRadius,
        center = Offset(cx + shadowOffset.x, cy + shadowOffset.y)
    )

    drawCircle(
        color = Color.White,
        radius = radius * 1.05f,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = color,
        radius = radius * 0.85f,
        center = Offset(cx, cy)
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.5f),
        radius = radius * 0.35f,
        center = Offset(cx - radius * 0.25f, cy - radius * 0.25f)
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.7f),
        radius = radius * 0.4f,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f)
    )

    drawCircle(
        color = Color.White,
        radius = radius * 0.15f,
        center = Offset(cx, cy)
    )
}

private fun getPlayerColor(playerColor: PlayerColor): Color {
    return when (playerColor) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
}
