package com.example.ludo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.engine.BoardConfig
import com.example.ludo.model.*
import com.example.ludo.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val BoardShape = RoundedCornerShape(14.dp)

/**
 * Classic Ludo board, rendered as two layers:
 * 1. [BoardBackdrop]: the static board in its own graphics layer. It reads no state, so it is
 *    recorded once per size and replayed from the RenderNode afterwards.
 * 2. Pawn layer: turn marker, pawns, hops and capture/finish bursts. Animated values are read
 *    only in the draw phase, so animating redraws this layer without recomposition.
 */
@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayer = gameState.currentPlayer
    val humanChoosing = gameState.gamePhase == GamePhase.WAITING_FOR_MOVE && currentPlayer?.isAI == false
    val pulse by rememberPulse(active = humanChoosing, from = 0f, to = 1f, durationMs = 520, idle = 0f)

    val spots = remember(gameState) { layoutPawns(gameState) }

    val hop = gameState.hop
    // Keyed on seq so a new hop starts at 0 in the same frame it appears (no one-frame jump).
    val hopProgress = remember(hop?.seq) { Animatable(0f) }
    LaunchedEffect(hop?.seq) {
        if (hop != null) hopProgress.animateTo(1f, tween(hop.durationMs, easing = LinearEasing))
    }

    val effect = gameState.effect
    val effectProgress = remember(effect?.seq) { Animatable(if (effect == null) 1f else 0f) }
    LaunchedEffect(effect?.seq) {
        if (effect != null) effectProgress.animateTo(1f, tween(EFFECT_MS, easing = FastOutSlowInEasing))
    }

    val latestSpots by rememberUpdatedState(spots)
    val latestOnTokenClick by rememberUpdatedState(onTokenClick)

    Box(
        modifier
            .shadow(elevation = 6.dp, shape = BoardShape, ambientColor = InkDark, spotColor = InkDark)
            .clip(BoardShape)
            .background(SurfaceWhite)
            .border(1.dp, HairlineBorder, BoardShape)
    ) {
        BoardBackdrop()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val g = BoardGeometry(Size(size.width.toFloat(), size.height.toFloat()))
                        val col = (tap.x - g.offsetX) / g.cell
                        val row = (tap.y - g.offsetY) / g.cell
                        latestSpots
                            .filter { it.isValid }
                            .map { it to hypot(it.cx - col, it.cy - row) }
                            .filter { (_, d) -> d <= TAP_RADIUS_CELLS }
                            .minByOrNull { (_, d) -> d }
                            ?.let { (spot, _) -> latestOnTokenClick(spot.tokenId) }
                    }
                }
        ) {
            val g = BoardGeometry(size)
            currentPlayer?.let { drawTurnMarker(g, it.color) }

            for (spot in spots) {
                drawPawn(
                    center = g.point(spot.cx, spot.cy),
                    radius = spot.radius * g.cell,
                    color = spot.color,
                    lift = if (spot.isValid) pulse * g.cell * 0.12f else 0f,
                    selectable = if (spot.isValid) 0.4f + 0.6f * pulse else 0f
                )
            }

            if (hop != null) {
                gameState.players.firstOrNull { it.id == hop.playerId }?.color?.let {
                    drawHoppingPawn(g, hop, it, hopProgress.value)
                }
            }

            if (effect != null && effectProgress.value < 1f) drawEffect(g, effect, effectProgress.value)
        }
    }
}

/** Static board art. Parameterless so it never recomposes; its own layer caches the drawing. */
@Composable
private fun BoardBackdrop() {
    Canvas(Modifier.fillMaxSize().graphicsLayer()) {
        val g = BoardGeometry(size)
        for (color in PlayerColor.entries) drawHomeBase(g, color)
        drawTrack(g)
        drawSafeSquares(g)
        drawTrackArrows(g)
        drawCenterHome(g)
    }
}

// region Layout

private const val TAP_RADIUS_CELLS = 1.1f
private const val EFFECT_MS = 700

/** Square board fitted and centred in the canvas; board coordinates are in cell units. */
private class BoardGeometry(size: Size) {
    val boardSize = min(size.width, size.height)
    val cell = boardSize / BoardConfig.BOARD_SIZE
    val offsetX = (size.width - boardSize) / 2f
    val offsetY = (size.height - boardSize) / 2f

    fun x(col: Float) = offsetX + col * cell
    fun y(row: Float) = offsetY + row * cell
    fun point(col: Float, row: Float) = Offset(x(col), y(row))
    fun cellCenter(cell: Pair<Int, Int>) = point(cell.second + 0.5f, cell.first + 0.5f)
}

/** A pawn resting on the board. [cx]/[cy]/[radius] are in cell units (cell centre = index + 0.5). */
private class PawnSpot(
    val playerId: Int,
    val tokenId: Int,
    val color: PlayerColor,
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val isValid: Boolean
)

// Finished pawns line up inside their colour's centre triangle: (col, row, lineIsHorizontal).
private val GoalAnchors = arrayOf(
    Triple(6.45f, 7.5f, false), // Red, left triangle
    Triple(7.5f, 6.45f, true),  // Green, top triangle
    Triple(8.55f, 7.5f, false), // Yellow, right triangle
    Triple(7.5f, 8.55f, true)   // Blue, bottom triangle
)

private fun layoutPawns(state: GameState): List<PawnSpot> {
    val current = state.currentPlayer
    val humanChoosing = state.gamePhase == GamePhase.WAITING_FOR_MOVE && current?.isAI == false
    val hop = state.hop
    val spots = ArrayList<PawnSpot>(16)
    val byCell = LinkedHashMap<Pair<Int, Int>, MutableList<Pair<Player, Token>>>()

    for (player in state.players) {
        val finished = player.tokens.filter { it.state == TokenState.FINISHED }
        for (token in player.tokens) {
            if (hop != null && hop.playerId == player.id && hop.tokenId == token.id) continue
            val isValid = humanChoosing && player.id == current?.id && token.id in state.validMoves
            when (token.state) {
                TokenState.IN_HOME -> {
                    val (row, col) = BoardConfig.homeSpot(player.color.ordinal, token.id)
                    spots += PawnSpot(player.id, token.id, player.color, col + 0.5f, row + 0.5f, 0.44f, isValid)
                }
                TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> {
                    token.boardPosition?.let { byCell.getOrPut(it) { ArrayList(2) }.add(player to token) }
                }
                TokenState.FINISHED -> {
                    val (ax, ay, horizontal) = GoalAnchors[player.color.ordinal]
                    val shift = (finished.indexOf(token) - (finished.size - 1) / 2f) * 0.34f
                    val cx = if (horizontal) ax + shift else ax
                    val cy = if (horizontal) ay else ay + shift
                    spots += PawnSpot(player.id, token.id, player.color, cx, cy, 0.2f, false)
                }
            }
        }
    }

    for ((cell, occupants) in byCell) {
        val (row, col) = cell
        occupants.forEachIndexed { index, (player, token) ->
            val (dx, dy, radius) = stackOffset(occupants.size, index)
            val isValid = humanChoosing && player.id == current?.id && token.id in state.validMoves
            spots += PawnSpot(player.id, token.id, player.color, col + 0.5f + dx, row + 0.5f + dy, radius, isValid)
        }
    }

    // Paint back-to-front so lower pawns overlap the ones above them.
    spots.sortBy { it.cy }
    return spots
}

/** Offset (cell units) and radius for the [index]-th of [count] pawns sharing a cell. */
private fun stackOffset(count: Int, index: Int): Triple<Float, Float, Float> = when (count) {
    1 -> Triple(0f, 0f, 0.44f)
    2 -> if (index == 0) Triple(-0.16f, -0.12f, 0.32f) else Triple(0.16f, 0.12f, 0.32f)
    3 -> when (index) {
        0 -> Triple(0f, -0.16f, 0.28f)
        1 -> Triple(-0.18f, 0.14f, 0.28f)
        else -> Triple(0.18f, 0.14f, 0.28f)
    }
    else -> when (index % 4) {
        0 -> Triple(-0.18f, -0.18f, 0.24f)
        1 -> Triple(0.18f, -0.18f, 0.24f)
        2 -> Triple(-0.18f, 0.18f, 0.24f)
        else -> Triple(0.18f, 0.18f, 0.24f)
    }
}

// endregion

// region Dynamic layer

private fun DrawScope.drawHoppingPawn(g: BoardGeometry, hop: HopMove, color: PlayerColor, progress: Float) {
    val eased = (1f - cos(progress * PI.toFloat())) / 2f
    val fromCol = hop.from.second + 0.5f
    val fromRow = hop.from.first + 0.5f
    val toCol = hop.to.second + 0.5f
    val toRow = hop.to.first + 0.5f

    val ground = g.point(fromCol + (toCol - fromCol) * eased, fromRow + (toRow - fromRow) * eased)
    // Longer journeys (e.g. a captured pawn flying home) arc higher.
    val distance = hypot(toCol - fromCol, toRow - fromRow)
    val arc = sin(progress * PI.toFloat())
    val lift = arc * g.cell * (0.55f + 0.12f * distance).coerceAtMost(2f)

    drawPawn(center = ground, radius = g.cell * 0.44f * (1f + arc * 0.12f), color = color, lift = lift)
}

/** Top-left cell (row, col) of each colour's 6x6 base, by colour ordinal. */
private val BaseOrigins = arrayOf(0 to 0, 0 to 9, 9 to 9, 9 to 0)

/** White ring inside the active player's base frame. */
private fun DrawScope.drawTurnMarker(g: BoardGeometry, color: PlayerColor) {
    val (row, col) = BaseOrigins[color.ordinal]
    val inset = g.cell * 0.36f
    val stroke = g.cell * 0.1f
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(g.x(col.toFloat()) + inset, g.y(row.toFloat()) + inset),
        size = Size(g.cell * 6 - inset * 2, g.cell * 6 - inset * 2),
        cornerRadius = CornerRadius(g.cell * 0.55f),
        style = Stroke(width = stroke)
    )
}

/** Expanding ring plus radiating sparks; gold sparks mark a piece reaching home. */
private fun DrawScope.drawEffect(g: BoardGeometry, effect: BoardEffect, p: Float) {
    val color = PlayerColorUtils.getComposeColor(effect.color)
    val isFinish = effect.kind == BoardEffect.Kind.FINISH
    val center = if (isFinish) {
        val (ax, ay, _) = GoalAnchors[effect.color.ordinal]
        g.point(ax, ay)
    } else {
        g.cellCenter(effect.cell)
    }
    val fade = 1f - p
    val reach = g.cell * (if (isFinish) 1.7f else 1.3f) * p

    if (!isFinish) drawCircle(Color.White.copy(alpha = 0.7f * fade), g.cell * 0.55f * (1f - 0.4f * p), center)
    drawCircle(color.copy(alpha = 0.85f * fade), reach, center, style = Stroke(g.cell * 0.14f * fade + 1f))

    val sparkColor = if (isFinish) Color(0xFFF7C948) else color
    for (i in 0 until 8) {
        val angle = i * (PI.toFloat() / 4f) + if (isFinish) p * 0.6f else 0f
        drawCircle(
            color = sparkColor.copy(alpha = fade),
            radius = g.cell * 0.08f * (0.4f + fade),
            center = Offset(center.x + cos(angle) * reach * 1.1f, center.y + sin(angle) * reach * 1.1f)
        )
    }
}

// endregion

// region Static board art

// Main-thread-only scratch paths for the static layer.
private val sharedArrowPath = Path()
private val sharedStarPath = Path()
private val sharedTrianglePath = Path()

private fun DrawScope.drawHomeBase(g: BoardGeometry, player: PlayerColor) {
    val color = PlayerColorUtils.getComposeColor(player)
    val light = PlayerColorUtils.getLightColor(player)
    val (row, col) = BaseOrigins[player.ordinal]
    val x = g.x(col.toFloat())
    val y = g.y(row.toFloat())
    val homeSize = g.cell * 6

    drawRect(color = color, topLeft = Offset(x, y), size = Size(homeSize, homeSize))

    // White yard with a hairline edge.
    val inset = g.cell * 0.75f
    val yardSize = homeSize - inset * 2
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x + inset, y + inset),
        size = Size(yardSize, yardSize),
        cornerRadius = CornerRadius(g.cell * 0.45f)
    )

    // Four ringed spots where pieces wait.
    val spotRadius = g.cell * 0.6f
    for (spot in BoardConfig.homePositions.getValue(player.ordinal)) {
        val center = g.cellCenter(spot)
        drawCircle(light, spotRadius, center)
        drawCircle(color, spotRadius, center, style = Stroke(g.cell * 0.07f))
    }
}

private fun DrawScope.drawTrack(g: BoardGeometry) {
    val cellSize = Size(g.cell, g.cell)
    val colored = HashMap<Pair<Int, Int>, Color>()
    for (color in PlayerColor.entries) {
        val c = PlayerColorUtils.getComposeColor(color)
        colored[BoardConfig.mainTrack[BoardConfig.startIndices[color.ordinal]]] = c
        for (cell in BoardConfig.homeColumns[color.ordinal].dropLast(1)) colored[cell] = c // Last cell sits in the centre
    }
    val cells = BoardConfig.mainTrack + BoardConfig.homeColumns.flatMap { it.dropLast(1) }
    for (cell in cells) {
        val topLeft = g.point(cell.second.toFloat(), cell.first.toFloat())
        val fill = colored[cell]
        if (fill != null) drawRect(fill, topLeft, cellSize)
        drawRect(
            color = if (fill != null) Color.White.copy(alpha = 0.35f) else BoardLine,
            topLeft = topLeft,
            size = cellSize,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// Entry arrows pointing into each home column: (col, row, angle) by colour ordinal.
private val ArrowSpecs = arrayOf(Triple(0, 7, 0f), Triple(7, 0, 90f), Triple(14, 7, 180f), Triple(7, 14, 270f))

private fun DrawScope.drawTrackArrows(g: BoardGeometry) {
    for (color in PlayerColor.entries) {
        val (col, row, angle) = ArrowSpecs[color.ordinal]
        drawDirectionArrow(g.point(col + 0.5f, row + 0.5f), g.cell * 0.46f, PlayerColorUtils.getComposeColor(color), angle)
    }
}

private fun DrawScope.drawDirectionArrow(center: Offset, size: Float, color: Color, angleDeg: Float) {
    val half = size / 2
    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    fun rotated(px: Float, py: Float) = Offset(center.x + px * cosA - py * sinA, center.y + px * sinA + py * cosA)

    val tip = rotated(half, 0f)
    val top = rotated(-half, -half * 0.7f)
    val mid = rotated(-half * 0.35f, 0f)
    val bottom = rotated(-half, half * 0.7f)

    sharedArrowPath.reset()
    sharedArrowPath.moveTo(tip.x, tip.y)
    sharedArrowPath.lineTo(top.x, top.y)
    sharedArrowPath.lineTo(mid.x, mid.y)
    sharedArrowPath.lineTo(bottom.x, bottom.y)
    sharedArrowPath.close()
    drawPath(sharedArrowPath, color = color, style = Fill)
}

/** Grey outlined stars on the neutral safe squares, white stars on the coloured start squares. */
private fun DrawScope.drawSafeSquares(g: BoardGeometry) {
    for (index in BoardConfig.starSpotIndices) {
        drawStar(g.cellCenter(BoardConfig.mainTrack[index]), g.cell * 0.34f, SubtleFill, InkFaint)
    }
    for (start in BoardConfig.startIndices) {
        drawStar(g.cellCenter(BoardConfig.mainTrack[start]), g.cell * 0.3f, Color.White.copy(alpha = 0.9f), null)
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, fillColor: Color, strokeColor: Color?) {
    val points = 5
    sharedStarPath.reset()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.45f
        val angle = Math.toRadians((i * 360.0 / (points * 2)) - 90.0)
        val x = center.x + r * cos(angle).toFloat()
        val y = center.y + r * sin(angle).toFloat()
        if (i == 0) sharedStarPath.moveTo(x, y) else sharedStarPath.lineTo(x, y)
    }
    sharedStarPath.close()
    drawPath(sharedStarPath, color = fillColor, style = Fill)
    if (strokeColor != null) drawPath(sharedStarPath, color = strokeColor, style = Stroke(width = 1.2.dp.toPx()))
}

// Outer corners (col, row offsets from centre, in cells) of each colour's centre triangle.
private val TriangleCorners = arrayOf(
    floatArrayOf(-1.5f, -1.5f, -1.5f, 1.5f), // Red, left
    floatArrayOf(-1.5f, -1.5f, 1.5f, -1.5f), // Green, top
    floatArrayOf(1.5f, -1.5f, 1.5f, 1.5f),   // Yellow, right
    floatArrayOf(-1.5f, 1.5f, 1.5f, 1.5f)    // Blue, bottom
)

private fun DrawScope.drawCenterHome(g: BoardGeometry) {
    val center = g.point(7.5f, 7.5f)
    val seam = Stroke(width = 1.5.dp.toPx())
    for (color in PlayerColor.entries) {
        val c = TriangleCorners[color.ordinal]
        sharedTrianglePath.reset()
        sharedTrianglePath.moveTo(center.x + c[0] * g.cell, center.y + c[1] * g.cell)
        sharedTrianglePath.lineTo(center.x + c[2] * g.cell, center.y + c[3] * g.cell)
        sharedTrianglePath.lineTo(center.x, center.y)
        sharedTrianglePath.close()
        drawPath(sharedTrianglePath, color = PlayerColorUtils.getComposeColor(color), style = Fill)
        drawPath(sharedTrianglePath, color = Color.White.copy(alpha = 0.6f), style = seam)
    }
}

// endregion
