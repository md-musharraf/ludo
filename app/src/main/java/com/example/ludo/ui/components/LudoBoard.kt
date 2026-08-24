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
import kotlin.math.cos
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
    val groundCenter: Offset = Offset.Zero,
    val hopHeight: Float = 0f
)

private data class PawnColorScheme(
    val highlightColor: Color,
    val lightColor: Color,
    val baseColor: Color,
    val darkColor: Color,
    val deepShadow: Color
)

private fun getPawnColors(playerColor: PlayerColor): PawnColorScheme {
    return when (playerColor) {
        PlayerColor.RED -> PawnColorScheme(
            highlightColor = Color(0xFFFF8A80),
            lightColor = Color(0xFFFF5252),
            baseColor = Color(0xFFD32F2F),
            darkColor = Color(0xFFB71C1C),
            deepShadow = Color(0xFF5A0000)
        )
        PlayerColor.GREEN -> PawnColorScheme(
            highlightColor = Color(0xFFB9F6CA),
            lightColor = Color(0xFF4CAF50),
            baseColor = Color(0xFF2E7D32),
            darkColor = Color(0xFF1B5E20),
            deepShadow = Color(0xFF003300)
        )
        PlayerColor.YELLOW -> PawnColorScheme(
            highlightColor = Color(0xFFFFF59D),
            lightColor = Color(0xFFFFD54F),
            baseColor = Color(0xFFFBC02D),
            darkColor = Color(0xFFF57F17),
            deepShadow = Color(0xFF7F4000)
        )
        PlayerColor.BLUE -> PawnColorScheme(
            highlightColor = Color(0xFF80D8FF),
            lightColor = Color(0xFF42A5F5),
            baseColor = Color(0xFF1976D2),
            darkColor = Color(0xFF0D47A1),
            deepShadow = Color(0xFF001F54)
        )
    }
}

/**
 * Authentic 3D Classic Ludo Board with Realistic Turned-Wood / Gloss Plastic Gotis (Pawns).
 * Features:
 * - 3D Chess/Halma Pawn Anatomy: Stepped pedestal base, tapered waist stem, toroidal collar, and glossy spherical crown head
 * - Multi-stop radial and linear gradients for photorealistic 3D lighting
 * - Diffused optical ground shadow anchored to board surface
 * - 4 vibrant home bases with recessed socket bowls
 * - Crisp safe stars, center triangles, and directional arrows
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
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val renderedTokens = remember { mutableListOf<TokenRenderInfo>() }

    Canvas(
        modifier = modifier.pointerInput(gameState) {
            detectTapGestures { tapOffset ->
                val currentPlayer = gameState.players.getOrNull(gameState.currentPlayerIndex) ?: return@detectTapGestures

                val tokensCopy = synchronized(renderedTokens) { renderedTokens.toList() }
                for (info in tokensCopy.reversed()) {
                    if (info.playerId == currentPlayer.id && info.isValid) {
                        val dx = tapOffset.x - info.center.x
                        val dy = tapOffset.y - info.center.y
                        val hitDistance = info.radius * 2.4f
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

        // 1. Clean Crisp Board Canvas Background
        drawRoundRect(
            color = Color(0x28000000),
            topLeft = Offset(offsetX + 3f, offsetY + 5f),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f)
        )

        // 2. Four Classic Home Bases
        drawHomeBase(
            offsetX, offsetY, cellSize, 0, 0, LudoRed, LudoRedLight,
            currentPlayer?.color == PlayerColor.RED, pulseAlpha
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 0, 9, LudoGreen, LudoGreenLight,
            currentPlayer?.color == PlayerColor.GREEN, pulseAlpha
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 9, 9, LudoYellow, LudoYellowLight,
            currentPlayer?.color == PlayerColor.YELLOW, pulseAlpha
        )
        drawHomeBase(
            offsetX, offsetY, cellSize, 9, 0, LudoBlue, LudoBlueLight,
            currentPlayer?.color == PlayerColor.BLUE, pulseAlpha
        )

        // 3. Track Cells (52 Squares)
        drawTrackCells(offsetX, offsetY, cellSize)

        // 4. Starting Squares
        drawStartingSquares(offsetX, offsetY, cellSize)

        // 5. Safe Star Zones (4 Outlined Stars)
        drawSafeZones(offsetX, offsetY, cellSize)

        // 6. Home Columns
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.redHomeColumn, LudoRed)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.greenHomeColumn, LudoGreen)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.yellowHomeColumn, LudoYellow)
        drawHomeColumn(offsetX, offsetY, cellSize, BoardConfig.blueHomeColumn, LudoBlue)

        // 7. Directional Arrows on Track
        drawTrackArrows(offsetX, offsetY, cellSize)

        // 8. Center Home Triangles
        drawCenterHome(offsetX, offsetY, cellSize)

        // 9. Outer Board Border Frame
        drawRoundRect(
            color = Color(0xFF212121),
            topLeft = Offset(offsetX, offsetY),
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(cellSize * 0.45f),
            style = Stroke(width = 2.5f)
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
                        val radius = cellSize * 0.44f

                        tokenList.add(
                            TokenRenderInfo(
                                playerId = player.id,
                                tokenId = token.id,
                                playerColor = player.color,
                                center = Offset(cx, cy),
                                radius = radius,
                                isValid = isValid,
                                isCurrentPlayer = isCurr,
                                groundCenter = Offset(cx, cy)
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
                    1 -> Triple(0f, 0f, cellSize * 0.44f)
                    2 -> {
                        val d = cellSize * 0.16f
                        if (index == 0) Triple(-d, -d, cellSize * 0.32f)
                        else Triple(d, d, cellSize * 0.32f)
                    }
                    3 -> {
                        val d = cellSize * 0.16f
                        when (index) {
                            0 -> Triple(0f, -d, cellSize * 0.28f)
                            1 -> Triple(-d, d * 0.9f, cellSize * 0.28f)
                            else -> Triple(d, d * 0.9f, cellSize * 0.28f)
                        }
                    }
                    else -> {
                        val d = cellSize * 0.18f
                        when (index % 4) {
                            0 -> Triple(-d, -d, cellSize * 0.24f)
                            1 -> Triple(d, -d, cellSize * 0.24f)
                            2 -> Triple(-d, d, cellSize * 0.24f)
                            else -> Triple(d, d, cellSize * 0.24f)
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
                        isCurrentPlayer = isCurr,
                        groundCenter = Offset(cx, cy)
                    )
                )
            }
        }

        // 12. Silky Smooth Sinusoidal Physics Hop Animation
        if (gameState.animatingPlayerId != null && gameState.animatingTokenId != null &&
            gameState.animatingFromPos != null && gameState.animatingToPos != null) {

            val from = gameState.animatingFromPos
            val to = gameState.animatingToPos
            val rawProgress = gameState.animatingHopProgress

            // Smooth ease-in-out cosine interpolation for horizontal track trajectory
            val easedProgress = (1f - cos(rawProgress * Math.PI.toFloat())) / 2f

            val fromX = offsetX + from.second * cellSize + cellSize / 2
            val fromY = offsetY + from.first * cellSize + cellSize / 2
            val toX = offsetX + to.second * cellSize + cellSize / 2
            val toY = offsetY + to.first * cellSize + cellSize / 2

            val curGroundX = fromX + (toX - fromX) * easedProgress
            val curGroundY = fromY + (toY - fromY) * easedProgress

            // Natural parabolic gravity arc for elevation
            val hopHeight = sin(rawProgress * Math.PI.toFloat()) * cellSize * 0.95f
            val elevatedY = curGroundY - hopHeight
            val elevatedScale = 1f + sin(rawProgress * Math.PI.toFloat()) * 0.20f
            val tokenRadius = cellSize * 0.44f * elevatedScale

            val player = gameState.players.firstOrNull { it.id == gameState.animatingPlayerId }
            if (player != null) {
                tokenList.add(
                    TokenRenderInfo(
                        playerId = player.id,
                        tokenId = gameState.animatingTokenId,
                        playerColor = player.color,
                        center = Offset(curGroundX, elevatedY),
                        radius = tokenRadius,
                        isValid = false,
                        isCurrentPlayer = true,
                        isAnimating = true,
                        groundCenter = Offset(curGroundX, curGroundY),
                        hopHeight = hopHeight
                    )
                )
            }
        }

        // 13. Draw All 3D Classic Pawns (Gotis)
        for (info in tokenList.sortedBy { if (it.isAnimating) 1 else 0 }) {
            draw3DClassicPawn(
                center = info.center,
                radius = info.radius,
                playerColor = info.playerColor,
                isValid = info.isValid,
                pulseAlpha = pulseAlpha,
                pulseScale = pulseScale,
                groundCenter = info.groundCenter,
                hopHeight = info.hopHeight,
                isAnimating = info.isAnimating
            )
        }

        synchronized(renderedTokens) {
            renderedTokens.clear()
            renderedTokens.addAll(tokenList)
        }
    }
}

/**
 * Renders an authentic, realistic 3D Classic Ludo Pawn (Goti).
 * True Halma/Chess Pawn structure matching reference:
 * 1. Stepped Pedestal Base Disc
 * 2. Tapered Waist Stem with Directional Lighting
 * 3. Toroidal Collar Ring
 * 4. Spherical Crown Head with 3D Radial Depth & Specular Shine
 * 5. Diffused Optical Ground Shadow
 */
private fun DrawScope.draw3DClassicPawn(
    center: Offset,
    radius: Float,
    playerColor: PlayerColor,
    isValid: Boolean,
    pulseAlpha: Float,
    pulseScale: Float,
    groundCenter: Offset,
    hopHeight: Float,
    isAnimating: Boolean
) {
    val (cx, cy) = center.x to center.y
    val colors = getPawnColors(playerColor)

    // 1. Realistic Optical Ground Shadow (Diffuses smoothly when hopping)
    val baseShadowWidth = radius * 1.9f
    val baseShadowHeight = radius * 0.75f
    val groundY = groundCenter.y + radius * 0.32f

    if (isAnimating) {
        val shadowProgress = (hopHeight / (radius * 2.5f)).coerceIn(0f, 1f)
        val shadowScale = 1f + shadowProgress * 0.65f
        val shadowAlpha = (0.35f * (1f - shadowProgress * 0.55f)).coerceIn(0.06f, 0.35f)
        val shadowW = baseShadowWidth * shadowScale
        val shadowH = baseShadowHeight * shadowScale

        // Outer soft ambient diffusion
        drawOval(
            color = Color.Black.copy(alpha = shadowAlpha * 0.40f),
            topLeft = Offset(groundCenter.x - shadowW * 0.65f + 1f, groundY - shadowH * 0.65f + 2f),
            size = Size(shadowW * 1.3f, shadowH * 1.3f)
        )
        // Inner contact shadow core
        drawOval(
            color = Color.Black.copy(alpha = shadowAlpha),
            topLeft = Offset(groundCenter.x - shadowW / 2f + 1f, groundY - shadowH / 2f + 2f),
            size = Size(shadowW, shadowH)
        )
    } else {
        // Resting Ground Shadow
        drawOval(
            color = Color.Black.copy(alpha = 0.15f),
            topLeft = Offset(groundCenter.x - baseShadowWidth * 0.60f + 1.5f, groundY - baseShadowHeight * 0.60f + 2.5f),
            size = Size(baseShadowWidth * 1.2f, baseShadowHeight * 1.2f)
        )
        drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(groundCenter.x - baseShadowWidth / 2f + 1f, groundY - baseShadowHeight / 2f + 2f),
            size = Size(baseShadowWidth, baseShadowHeight)
        )
    }

    // 2. Glowing Halo when Valid to Move
    if (isValid) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.lightColor.copy(alpha = pulseAlpha * 0.65f), Color.Transparent),
                center = Offset(cx, cy - radius * 0.25f),
                radius = radius * pulseScale * 1.75f
            ),
            radius = radius * pulseScale * 1.75f,
            center = Offset(cx, cy - radius * 0.25f)
        )
        drawCircle(
            color = Color.White.copy(alpha = pulseAlpha * 0.95f),
            radius = radius * pulseScale * 1.28f,
            center = Offset(cx, cy - radius * 0.25f),
            style = Stroke(width = 2.4f)
        )
    }

    // 3. Pawn Pedestal Base Disc
    val baseY = cy + radius * 0.28f
    val baseWidth = radius * 1.75f
    val baseHeight = radius * 0.70f

    // Base Lower Shadow & Dark Rim
    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(colors.darkColor, colors.deepShadow),
            startY = baseY - baseHeight / 2,
            endY = baseY + baseHeight / 2 + 3f
        ),
        topLeft = Offset(cx - baseWidth / 2, baseY - baseHeight / 2 + 2f),
        size = Size(baseWidth, baseHeight + 2f)
    )

    // Base Beveled Upper Disc
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(colors.lightColor, colors.baseColor, colors.darkColor),
            center = Offset(cx - baseWidth * 0.2f, baseY - baseHeight * 0.2f),
            radius = baseWidth * 0.75f
        ),
        topLeft = Offset(cx - baseWidth / 2, baseY - baseHeight / 2),
        size = Size(baseWidth, baseHeight)
    )

    // Base Top Edge Specular Glint
    drawOval(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(cx - baseWidth * 0.42f, baseY - baseHeight * 0.45f),
        size = Size(baseWidth * 0.84f, baseHeight * 0.35f),
        style = Stroke(width = 1.4f)
    )

    // 4. Pawn Tapered Waist / Stem
    val neckY = cy - radius * 0.28f
    val stemTopWidth = radius * 0.65f
    val stemBottomWidth = radius * 1.25f

    val stemPath = Path().apply {
        moveTo(cx - stemBottomWidth / 2, baseY - baseHeight * 0.25f)
        cubicTo(
            cx - stemBottomWidth * 0.35f, cy,
            cx - stemTopWidth * 0.65f, neckY + radius * 0.15f,
            cx - stemTopWidth / 2, neckY
        )
        lineTo(cx + stemTopWidth / 2, neckY)
        cubicTo(
            cx + stemTopWidth * 0.65f, neckY + radius * 0.15f,
            cx + stemBottomWidth * 0.35f, cy,
            cx + stemBottomWidth / 2, baseY - baseHeight * 0.25f
        )
        close()
    }

    drawPath(
        path = stemPath,
        brush = Brush.horizontalGradient(
            colors = listOf(colors.highlightColor, colors.lightColor, colors.baseColor, colors.darkColor, colors.deepShadow),
            startX = cx - stemBottomWidth / 2,
            endX = cx + stemBottomWidth / 2
        )
    )

    // 5. Collar Ring (Toroidal Bead below Crown)
    val collarWidth = radius * 0.95f
    val collarHeight = radius * 0.40f
    val collarY = neckY + radius * 0.04f

    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(colors.darkColor, colors.deepShadow),
            startY = collarY,
            endY = collarY + collarHeight
        ),
        topLeft = Offset(cx - collarWidth / 2, collarY - collarHeight / 2 + 1.5f),
        size = Size(collarWidth, collarHeight)
    )
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(colors.highlightColor, colors.lightColor, colors.baseColor, colors.darkColor),
            center = Offset(cx - collarWidth * 0.22f, collarY - collarHeight * 0.22f),
            radius = collarWidth * 0.65f
        ),
        topLeft = Offset(cx - collarWidth / 2, collarY - collarHeight / 2),
        size = Size(collarWidth, collarHeight)
    )

    // 6. Spherical Crown Head (Sphere with 3D Gloss)
    val headRadius = radius * 0.66f
    val headCenter = Offset(cx, cy - radius * 0.58f)

    // Head Contact Drop Shadow on Collar
    drawCircle(
        color = Color(0x35000000),
        radius = headRadius * 1.05f,
        center = Offset(headCenter.x + 0.8f, headCenter.y + 2f)
    )

    // 3D Sphere Body with Spherical Radial Gradient
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(colors.highlightColor, colors.lightColor, colors.baseColor, colors.darkColor, colors.deepShadow),
            center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.35f),
            radius = headRadius * 1.25f
        ),
        radius = headRadius,
        center = headCenter
    )

    // High-Gloss Specular Highlight (Primary bright crescent glint)
    drawOval(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(headCenter.x - headRadius * 0.58f, headCenter.y - headRadius * 0.65f),
        size = Size(headRadius * 0.55f, headRadius * 0.40f)
    )

    // Secondary Micro Specular Dot
    drawCircle(
        color = Color.White,
        radius = headRadius * 0.14f,
        center = Offset(headCenter.x - headRadius * 0.42f, headCenter.y - headRadius * 0.46f)
    )

    // Subtle Rim Light on Lower-Right Edge (Reflected Ambient Light)
    drawArc(
        color = colors.lightColor.copy(alpha = 0.45f),
        startAngle = 30f,
        sweepAngle = 100f,
        useCenter = false,
        topLeft = Offset(headCenter.x - headRadius + 1f, headCenter.y - headRadius + 1f),
        size = Size(headRadius * 2 - 2f, headRadius * 2 - 2f),
        style = Stroke(width = 1.8f)
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

    // 4 Sunken Goti Nest Bowls
    val spotRadius = cellSize * 0.46f
    val spotCenters = listOf(
        Offset(x + 1.5f * cellSize, y + 1.5f * cellSize),
        Offset(x + 4.5f * cellSize, y + 1.5f * cellSize),
        Offset(x + 1.5f * cellSize, y + 4.5f * cellSize),
        Offset(x + 4.5f * cellSize, y + 4.5f * cellSize),
    )

    spotCenters.forEach { center ->
        // Outer socket shadow
        drawCircle(color = Color(0x20000000), radius = spotRadius * 1.08f, center = Offset(center.x + 0.8f, center.y + 1f))
        // Colored socket rim
        drawCircle(color = color.copy(alpha = 0.30f), radius = spotRadius, center = center)
        drawCircle(color = color, radius = spotRadius, center = center, style = Stroke(width = 3.0f))
        // Inner nest bed
        drawCircle(color = lightColor.copy(alpha = 0.30f), radius = spotRadius * 0.72f, center = center)
        drawCircle(color = color.copy(alpha = 0.50f), radius = spotRadius * 0.72f, center = center, style = Stroke(width = 1.2f))
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
            color = Color(0xFFCFD8DC),
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
            color = Color(0xFFB0BEC5),
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

private fun DrawScope.drawTrackArrows(offsetX: Float, offsetY: Float, cellSize: Float) {
    val arrows = listOf(
        Triple(Pair(7, 0), LudoRed, 0f),       // Red arrow pointing Right →
        Triple(Pair(0, 7), LudoGreen, 90f),    // Green arrow pointing Down ↓
        Triple(Pair(7, 14), LudoYellow, 180f), // Yellow arrow pointing Left ←
        Triple(Pair(14, 7), LudoBlue, 270f)    // Blue arrow pointing Up ↑
    )

    arrows.forEach { (pos, color, angleDeg) ->
        val (row, col) = pos
        val cx = offsetX + col * cellSize + cellSize / 2
        val cy = offsetY + row * cellSize + cellSize / 2

        drawDirectionArrow(cx, cy, cellSize * 0.45f, color, angleDeg)
    }
}

private fun DrawScope.drawDirectionArrow(cx: Float, cy: Float, size: Float, color: Color, angleDeg: Float) {
    val path = Path()
    val half = size / 2

    // Arrow pointing Right (0 deg)
    val tipX = half
    val tipY = 0f
    val backX = -half
    val topY = -half * 0.7f
    val botY = half * 0.7f

    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    fun rotatePoint(px: Float, py: Float): Offset {
        val rx = px * cosA - py * sinA
        val ry = px * sinA + py * cosA
        return Offset(cx + rx, cy + ry)
    }

    val pTip = rotatePoint(tipX, tipY)
    val pTop = rotatePoint(backX, topY)
    val pBot = rotatePoint(backX, botY)
    val pMid = rotatePoint(backX * 0.4f, 0f)

    path.moveTo(pTip.x, pTip.y)
    path.lineTo(pTop.x, pTop.y)
    path.lineTo(pMid.x, pMid.y)
    path.lineTo(pBot.x, pBot.y)
    path.close()

    drawPath(path, color = color, style = Fill)
}

private fun DrawScope.drawSafeZones(offsetX: Float, offsetY: Float, cellSize: Float) {
    // 4 Classic Safe Spot Stars (Outlined Star with White Interior matching reference)
    val starIndices = listOf(8, 21, 34, 47)
    for (index in starIndices) {
        val (row, col) = BoardConfig.mainTrack[index]
        val cx = offsetX + col * cellSize + cellSize / 2
        val cy = offsetY + row * cellSize + cellSize / 2

        drawStar(cx, cy, cellSize * 0.36f, Color.White, Color(0xFF424242))
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, fillColor: Color, strokeColor: Color) {
    val points = 5
    val path = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.42f
        val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
        val x = cx + r * cos(angle).toFloat()
        val y = cy + r * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = strokeColor, style = Stroke(width = 1.8f))
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
}

private fun getPlayerColor(playerColor: PlayerColor): Color {
    return when (playerColor) {
        PlayerColor.RED -> LudoRed
        PlayerColor.GREEN -> LudoGreen
        PlayerColor.YELLOW -> LudoYellow
        PlayerColor.BLUE -> LudoBlue
    }
}


