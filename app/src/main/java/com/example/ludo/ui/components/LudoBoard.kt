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
    val shadowOffset: Offset = Offset(2.5f, 4f),
    val shadowRadius: Float = 1.0f
)

/**
 * Classic Authentic Ludo Board with 3D Tactile Gotis (Pawns).
 * Clean, modern, symmetrical 15x15 board with smooth physics-based token hops.
 */
@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "boardAnimations")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
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
                        val hitDistance = info.radius * 2.2f
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

        // 1. Board Drop Shadow & Wooden Base Frame
        drawRoundRect(
            color = Color(0x38000000),
            topLeft = Offset(offsetX + 4f, offsetY + 6f),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f)
        )
        drawRoundRect(
            color = Color(0xFFFDFBF7),
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f)
        )

        // 2. Four Classic Home Bases
        drawHomeBase(offsetX, offsetY, cellSize, 0, 0, LudoRed, LudoRedLight, currentPlayer?.color == PlayerColor.RED, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 0, 9, LudoGreen, LudoGreenLight, currentPlayer?.color == PlayerColor.GREEN, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 9, 9, LudoYellow, LudoYellowLight, currentPlayer?.color == PlayerColor.YELLOW, pulseAlpha)
        drawHomeBase(offsetX, offsetY, cellSize, 9, 0, LudoBlue, LudoBlueLight, currentPlayer?.color == PlayerColor.BLUE, pulseAlpha)

        // 3. Track Cells (52 Squares)
        drawTrackCells(offsetX, offsetY, cellSize)

        // 4. Starting Squares
        drawStartingSquares(offsetX, offsetY, cellSize)

        // 5. Safe Star Zones
        drawSafeZones(offsetX, offsetY, cellSize)

        // 6. Home Columns
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.redHomeColumn, LudoRed)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.greenHomeColumn, LudoGreen)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.yellowHomeColumn, LudoYellow)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.blueHomeColumn, LudoBlue)

        // 7. Center Home Triangles & Golden Trophy Star
        drawCenterHome(offsetX, offsetY, cellSize)

        // 8. Outer Mahogany Wood Border Frame
        drawRoundRect(
            color = Color(0xFF4E342E),
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f),
            style = Stroke(width = 4f)
        )

        // 9. Collect Home Base Tokens
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
                        val radius = cellSize * 0.40f

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

        // 10. Collect Board Tokens with Stacking
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
                    1 -> Triple(0f, 0f, cellSize * 0.40f)
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

        // 11. Smooth Parabolic Hop Animation
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
            val hopHeight = sin(progress * Math.PI).toFloat() * cellSize * 0.85f
            val elevatedY = curY - hopHeight
            val elevatedScale = 1f + sin(progress * Math.PI).toFloat() * 0.32f
            val tokenRadius = cellSize * 0.40f * elevatedScale

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
                        shadowOffset = Offset(2.5f, hopHeight + 4f),
                        shadowRadius = 1f + hopHeight / 8f
                    )
                )
            }
        }

        // 12. Draw All 3D Gotis (Pawns)
        for (info in tokenList.sortedBy { if (it.isAnimating) 1 else 0 }) {
            draw3DGoti(
                center = info.center,
                radius = info.radius,
                color = getPlayerColor(info.playerColor),
                isValid = info.isValid,
                pulseAlpha = pulseAlpha,
                pulseScale = pulseScale,
                shadowOffset = info.shadowOffset,
                shadowRadius = info.shadowRadius,
                isAnimating = info.isAnimating
            )
        }

        renderedTokens = tokenList
    }
}

/**
 * Renders a tactile, classic & modern 3D Ludo Goti (Pawn).
 * Anatomical layers:
 * 1. Ground contact shadow
 * 2. Flared pedestal base with bevel rim
 * 3. Tapered waist/collar
 * 4. Domed spherical crown head
 * 5. High-gloss specular reflections
 * 6. Active glowing pulse halo when valid to move
 */
private fun DrawScope.draw3DGoti(
    center: Offset,
    radius: Float,
    color: Color,
    isValid: Boolean,
    pulseAlpha: Float,
    pulseScale: Float,
    shadowOffset: Offset,
    shadowRadius: Float,
    isAnimating: Boolean
) {
    val (cx, cy) = center.x to center.y

    // 1. Selectable Active Glowing Pulsing Halo
    if (isValid) {
        drawCircle(
            color = color.copy(alpha = pulseAlpha * 0.55f),
            radius = radius * pulseScale * 1.55f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White.copy(alpha = pulseAlpha * 0.95f),
            radius = radius * pulseScale * 1.25f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.4f)
        )
    }

    // 2. Soft Ground Contact Shadow (Expands during air hop)
    val shadowWidth = radius * 1.9f * shadowRadius
    val shadowHeight = radius * 0.75f * shadowRadius
    drawOval(
        color = Color(0x3D000000),
        topLeft = Offset(cx - shadowWidth / 2 + shadowOffset.x, cy + radius * 0.35f + shadowOffset.y),
        size = Size(shadowWidth, shadowHeight)
    )

    // 3. Flared Pedestal Base Disc
    val baseCenter = Offset(cx, cy + radius * 0.22f)
    val baseRadius = radius * 0.95f

    // Outer Dark Bevel Rim
    drawCircle(
        color = Color(0x33000000),
        radius = baseRadius,
        center = Offset(baseCenter.x + 0.8f, baseCenter.y + 1.2f)
    )
    // White Base Ring Accent
    drawCircle(
        color = Color.White,
        radius = baseRadius,
        center = baseCenter
    )
    // Base Disc Body
    drawCircle(
        color = color,
        radius = baseRadius * 0.88f,
        center = baseCenter
    )
    // Base Top Edge Specular Rim
    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = baseRadius * 0.88f,
        center = baseCenter,
        style = Stroke(width = 1.5f)
    )

    // 4. Domed Spherical Crown Head (Offset upward for 3D standing perspective)
    val headCenter = Offset(cx, cy - radius * 0.18f)
    val headRadius = radius * 0.64f

    // Head Shadow Drop onto Base
    drawCircle(
        color = Color(0x30000000),
        radius = headRadius * 1.08f,
        center = Offset(headCenter.x + 0.8f, headCenter.y + 1.4f)
    )

    // White Head Border
    drawCircle(
        color = Color.White,
        radius = headRadius * 1.06f,
        center = headCenter
    )

    // 3D Saturated Jewel Head Body
    drawCircle(
        color = color,
        radius = headRadius,
        center = headCenter
    )

    // Inner Metallic Crown Ring
    drawCircle(
        color = Color.White.copy(alpha = 0.65f),
        radius = headRadius * 0.48f,
        center = headCenter,
        style = Stroke(width = 1.6f)
    )

    // Center Jewel Accent Dot
    drawCircle(
        color = Color.White,
        radius = headRadius * 0.18f,
        center = headCenter
    )

    // Top-Left Primary Specular Gloss Highlight
    val glossCenter = Offset(headCenter.x - headRadius * 0.30f, headCenter.y - headRadius * 0.30f)
    drawCircle(
        color = Color.White.copy(alpha = 0.80f),
        radius = headRadius * 0.32f,
        center = glossCenter
    )
    drawCircle(
        color = Color.White,
        radius = headRadius * 0.15f,
        center = Offset(glossCenter.x - headRadius * 0.05f, glossCenter.y - headRadius * 0.05f)
    )
}

private fun DrawScope.drawHomeBase(
    offsetX: Float, offsetY: Float, cellSize: Float,
    startRow: Int, startCol: Int, color: Color, lightColor: Color,
    isCurrentPlayer: Boolean, pulseAlpha: Float
) {
    val x = offsetX + startCol * cellSize
    val y = offsetY + startRow * cellSize
    val homeSize = cellSize * 6

    // Outer Colored Rounded Frame
    drawRoundRect(
        color = color,
        topLeft = Offset(x, y),
        size = Size(homeSize, homeSize),
        cornerRadius = CornerRadius(cellSize * 0.45f)
    )

    // Active Player Turn Halo
    if (isCurrentPlayer) {
        drawRoundRect(
            color = color.copy(alpha = pulseAlpha * 0.6f),
            topLeft = Offset(x - 3f, y - 3f),
            size = Size(homeSize + 6f, homeSize + 6f),
            cornerRadius = CornerRadius(cellSize * 0.45f),
            style = Stroke(width = 3.5f)
        )
    }

    // Inner White Platform
    val innerMargin = cellSize * 0.85f
    val innerSize = homeSize - innerMargin * 2
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x + innerMargin, y + innerMargin),
        size = Size(innerSize, innerSize),
        cornerRadius = CornerRadius(cellSize * 0.38f)
    )

    // 4 Sunken Goti Nests
    val spotRadius = cellSize * 0.46f
    val spotCenters = listOf(
        Offset(x + homeSize * 0.32f, y + homeSize * 0.32f),
        Offset(x + homeSize * 0.68f, y + homeSize * 0.32f),
        Offset(x + homeSize * 0.32f, y + homeSize * 0.68f),
        Offset(x + homeSize * 0.68f, y + homeSize * 0.68f),
    )

    spotCenters.forEach { center ->
        // Outer socket shadow
        drawCircle(color = Color(0x18000000), radius = spotRadius * 1.08f, center = Offset(center.x + 0.8f, center.y + 1f))
        // Colored socket rim
        drawCircle(color = color.copy(alpha = 0.25f), radius = spotRadius, center = center)
        drawCircle(color = color, radius = spotRadius, center = center, style = Stroke(width = 2.5f))
        // White inner nest bed
        drawCircle(color = Color.White, radius = spotRadius * 0.65f, center = center)
    }
}

private fun DrawScope.drawTrackCells(offsetX: Float, offsetY: Float, cellSize: Float) {
    for (pos in BoardConfig.mainTrack) {
        val (row, col) = pos
        val x = offsetX + col * cellSize
        val y = offsetY + row * cellSize

        drawRect(
            color = Color.White,
            topLeft = Offset(x, y),
            size = Size(cellSize, cellSize)
        )
        drawRect(
            color = Color(0xFFE2D9CC),
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
            style = Stroke(width = 1.2f)
        )
    }
}

private fun DrawScope.drawHomeColumn(
    offsetX: Float, offsetY: Float, cellSize: Float,
    positions: List<Pair<Int, Int>>, color: Color
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
            color = Color.White.copy(alpha = 0.5f),
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
            drawStar(cx, cy, cellSize * 0.32f, SafeZoneStar, Color(0xFFE65100))
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
    drawPath(path, color = strokeColor, style = Stroke(width = 1.6f))
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

    // Center Home Golden Star Medallion
    drawCircle(
        color = Color(0x33000000),
        radius = cellSize * 0.44f,
        center = Offset(centerX + 0.8f, centerY + 1f)
    )
    drawCircle(
        color = Color.White,
        radius = cellSize * 0.44f,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = Color(0xFFFFF8E1),
        radius = cellSize * 0.38f,
        center = Offset(centerX, centerY)
    )
    drawStar(centerX, centerY, cellSize * 0.26f, SafeZoneStar, Color(0xFFE65100))
}

private fun getPlayerColor(playerColor: PlayerColor): Color {
    return when (playerColor) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
}

