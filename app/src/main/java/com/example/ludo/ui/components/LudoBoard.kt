package com.example.ludo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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

private data class TokenRenderInfo(
    val playerId: Int,
    val tokenId: Int,
    val playerColor: PlayerColor,
    val center: Offset,
    val radius: Float,
    val isValid: Boolean,
    val isCurrentPlayer: Boolean
)

@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation for valid moves
    val infiniteTransition = rememberInfiniteTransition(label = "boardAnimations")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Store rendered tokens for tap detection
    var renderedTokens by remember { mutableStateOf<List<TokenRenderInfo>>(emptyList()) }

    Canvas(
        modifier = modifier.pointerInput(gameState) {
            detectTapGestures { tapOffset ->
                val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex) ?: return@detectTapGestures
                
                // Check tokens in reverse order (topmost drawn first)
                for (info in renderedTokens.reversed()) {
                    if (info.playerId == currentPlayer.id && info.isValid) {
                        val dx = tapOffset.x - info.center.x
                        val dy = tapOffset.y - info.center.y
                        val hitDistance = info.radius * 1.5f
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

        // 1. Board Outer Container & Shadow
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

        // 2. Four Home Bases (6x6 cells each)
        drawHomeBase(offsetX, offsetY, cellSize, 0, 0, LudoRed, LudoRedLight, "RED", currentPlayer?.color == PlayerColor.RED, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 0, 9, LudoGreen, LudoGreenLight, "GREEN", currentPlayer?.color == PlayerColor.GREEN, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 9, 9, LudoYellow, LudoYellowLight, "YELLOW", currentPlayer?.color == PlayerColor.YELLOW, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 9, 0, LudoBlue, LudoBlueLight, "BLUE", currentPlayer?.color == PlayerColor.BLUE, pulseAlpha)

        // 3. Track Cells (52 squares)
        drawTrackCells(offsetX, offsetY, cellSize)

        // 4. Starting Squares with Colored Fill & Entry Arrows
        drawStartingSquares(offsetX, offsetY, cellSize)

        // 5. Safe Zones (Stars)
        drawSafeZones(offsetX, offsetY, cellSize)

        // 6. Home Columns (Colored path into center)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.redHomeColumn, LudoRed, LudoRedLight)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.greenHomeColumn, LudoGreen, LudoGreenLight)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.yellowHomeColumn, LudoYellow, LudoYellowLight)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.blueHomeColumn, LudoBlue, LudoBlueLight)

        // 7. Center Home Triangles
        drawCenterHome(offsetX, offsetY, cellSize)

        // 8. Outer Border
        drawRoundRect(
            color = BoardBorder,
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.4f),
            style = Stroke(width = 3.5f)
        )

        // 9. Collect & Render Home Base Tokens
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

        // 10. Collect & Render Board Tokens with Multi-Token Stacking Offset
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

        // 11. Draw all tokens (with active pulsing on movable ones)
        for (info in tokenList) {
            drawLudoToken(
                center = info.center,
                radius = info.radius,
                color = getPlayerColor(info.playerColor),
                tokenId = info.tokenId,
                isValid = info.isValid,
                pulseAlpha = pulseAlpha,
                pulseScale = pulseScale
            )
        }

        renderedTokens = tokenList
    }
}

private fun DrawScope.drawHomeBase(
    offsetX: Float, offsetY: Float, cellSize: Float,
    startRow: Int, startCol: Int, color: Color, lightColor: Color,
    label: String, isCurrentPlayer: Boolean, pulseAlpha: Float
) {
    val x = offsetX + startCol * cellSize
    val y = offsetY + startRow * cellSize
    val homeSize = cellSize * 6

    // Base background
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(homeSize, homeSize),
        cornerRadius = CornerRadius(cellSize * 0.4f)
    )

    // Current player base highlight
    if (isCurrentPlayer) {
        drawRoundRect(
            color = Color.White.copy(alpha = pulseAlpha * 0.6f),
            topLeft = Offset(x - 2f, y - 2f),
            size = Size(homeSize + 4f, homeSize + 4f),
            cornerRadius = CornerRadius(cellSize * 0.4f),
            style = Stroke(width = 3f)
        )
    }

    // Inner White Container
    val innerMargin = cellSize * 0.85f
    val innerSize = homeSize - innerMargin * 2
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(cellSize * 0.35f)
    )

    // Inner subtle border
    drawRoundRect(
        color = color.copy(alpha = 0.3f),
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(cellSize * 0.35f),
        style = Stroke(width = 1.5f)
    )

    // 4 Token Resting Spots
    val spotRadius = cellSize * 0.45f
    val spotCenters = listOf(
        Offset(x + homeSize * 0.32f, y + homeSize * 0.32f),
        Offset(x + homeSize * 0.68f, y + homeSize * 0.32f),
        Offset(x + homeSize * 0.32f, y + homeSize * 0.68f),
        Offset(x + homeSize * 0.68f, y + homeSize * 0.68f),
    )

    spotCenters.forEach { center ->
        // Spot Shadow
        drawCircle(color = Color(0x15000000), radius = spotRadius, center = Offset(center.x + 1f, center.y + 1.5f))
        // Spot Fill
        drawCircle(color = lightColor.copy(alpha = 0.5f), radius = spotRadius, center = center)
        // Spot Border
        drawCircle(color = color, radius = spotRadius, center = center, style = Stroke(width = 2.5f))
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
        Pair(BoardConfig.RED_START_INDEX, LudoRed),
        Pair(BoardConfig.GREEN_START_INDEX, LudoGreen),
        Pair(BoardConfig.YELLOW_START_INDEX, LudoYellow),
        Pair(BoardConfig.BLUE_START_INDEX, LudoBlue)
    )
    for ((index, color) in starts) {
        val (row, col) = BoardConfig.mainTrack[index]
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawHomeColumn(
    offsetX: Float, offsetY: Float, cellSize: Float,
    positions: List<Pair<Int, Int>>, color: Color, lightColor: Color
) {
    positions.forEachIndexed { idx, (row, col) ->
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        // Gradient or solid fill
        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun DrawScope.drawSafeZones(offsetX: Float, offsetY: Float, cellSize: Float) {
    for (index in BoardConfig.safeSpotsIndices) {
        // Skip start squares which already have full color fill
        if (index == BoardConfig.RED_START_INDEX ||
            index == BoardConfig.GREEN_START_INDEX ||
            index == BoardConfig.YELLOW_START_INDEX ||
            index == BoardConfig.BLUE_START_INDEX) {
            // Draw a white star on start squares
            val (row, col) = BoardConfig.mainTrack[index]
            val cx = offsetX + col * cellSize + cellSize / 2
            val cy = offsetY + row * cellSize + cellSize / 2
            drawStar(cx, cy, cellSize * 0.32f, Color.White, Color.White)
        } else {
            val (row, col) = BoardConfig.mainTrack[index]
            val cx = offsetX + col * cellSize + cellSize / 2
            val cy = offsetY + row * cellSize + cellSize / 2

            // Star on gray track square
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
        // Red Triangle (Left)
        Triple(
            listOf(Offset(centerX - triangleSpan, centerY - triangleSpan),
                   Offset(centerX, centerY),
                   Offset(centerX - triangleSpan, centerY + triangleSpan)),
            LudoRed,
            LudoRedLight
        ),
        // Green Triangle (Top)
        Triple(
            listOf(Offset(centerX - triangleSpan, centerY - triangleSpan),
                   Offset(centerX + triangleSpan, centerY - triangleSpan),
                   Offset(centerX, centerY)),
            LudoGreen,
            LudoGreenLight
        ),
        // Yellow Triangle (Right)
        Triple(
            listOf(Offset(centerX + triangleSpan, centerY - triangleSpan),
                   Offset(centerX + triangleSpan, centerY + triangleSpan),
                   Offset(centerX, centerY)),
            LudoYellow,
            LudoYellowLight
        ),
        // Blue Triangle (Bottom)
        Triple(
            listOf(Offset(centerX - triangleSpan, centerY + triangleSpan),
                   Offset(centerX + triangleSpan, centerY + triangleSpan),
                   Offset(centerX, centerY)),
            LudoBlue,
            LudoBlueLight
        )
    )

    for ((points, color, _) in triangleDefs) {
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            lineTo(points[2].x, points[2].y)
            close()
        }
        drawPath(path, color = color, style = Fill)
        drawPath(path, color = Color.White, style = Stroke(width = 2.5f))
    }

    // Center Gold Crown / Circle
    drawCircle(
        color = Color(0xFFFFD700),
        radius = cellSize * 0.45f,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = Color.White,
        radius = cellSize * 0.45f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2f)
    )
    drawStar(centerX, centerY, cellSize * 0.25f, Color.White, Color(0xFFF57F17))
}

private fun DrawScope.drawLudoToken(
    center: Offset,
    radius: Float,
    color: Color,
    tokenId: Int,
    isValid: Boolean,
    pulseAlpha: Float,
    pulseScale: Float
) {
    val (cx, cy) = center.x to center.y

    // 1. Animated Ripple / Glow on valid moveable tokens
    if (isValid) {
        drawCircle(
            color = Color.White.copy(alpha = pulseAlpha * 0.7f),
            radius = radius * pulseScale * 1.35f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = color.copy(alpha = pulseAlpha * 0.5f),
            radius = radius * pulseScale * 1.15f,
            center = Offset(cx, cy)
        )
    }

    // 2. Token Shadow
    drawCircle(
        color = Color(0x44000000),
        radius = radius * 1.05f,
        center = Offset(cx + 2.5f, cy + 3.5f)
    )

    // 3. Metallic Outer Rim
    drawCircle(
        color = Color.White,
        radius = radius,
        center = Offset(cx, cy)
    )

    // 4. Token Body (Vibrant Color)
    drawCircle(
        color = color,
        radius = radius * 0.88f,
        center = Offset(cx, cy)
    )

    // 5. Glossy Top-Left Sheen Highlight
    val sheenRadius = radius * 0.4f
    drawCircle(
        color = Color.White.copy(alpha = 0.55f),
        radius = sheenRadius,
        center = Offset(cx - radius * 0.28f, cy - radius * 0.28f)
    )

    // 6. Inner Gold Ring
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = radius * 0.45f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f)
    )

    // 7. Center Dot
    drawCircle(
        color = Color.White,
        radius = radius * 0.2f,
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
