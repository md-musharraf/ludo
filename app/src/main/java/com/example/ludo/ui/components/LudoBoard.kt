package com.example.ludo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
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

private val BoardShape = RoundedCornerShape(10.dp)

/**
 * Classic Ludo board, rendered as two layers:
 * 1. [BoardBackdrop]: the static board in its own graphics layer. It reads no state, so it is
 *    recorded once per size and replayed from the RenderNode afterwards.
 * 2. Goti layer: turn marker, gotis, hops and capture/finish bursts. Animated values are read
 *    only in the draw phase, so animating redraws this layer without recomposition.
 */
@Composable
fun LudoBoard(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayer = gameState.currentPlayer
    val humanTurn = currentPlayer?.isAI == false && !gameState.isGameOver
    val humanChoosing = gameState.gamePhase == GamePhase.WAITING_FOR_MOVE && humanTurn
    val pulse by rememberPulse(active = humanChoosing, from = 0f, to = 1f, durationMs = 520, idle = 0f)
    val breathe by rememberPulse(active = humanTurn, from = 0.55f, to = 1f, durationMs = 900, idle = 0.9f)

    // Intro: the board fades in, then each colour's gotis drop into their sockets in turn.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(INTRO_MS, easing = LinearEasing)) }

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

    // Landing: when a move ends, the goti that just arrived gives a small squash-and-bounce.
    var lastHop by remember { mutableStateOf<HopMove?>(null) }
    var landed by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val landing = remember { Animatable(1f) }
    LaunchedEffect(hop) {
        val finishedHop = lastHop
        lastHop = hop
        if (hop == null && finishedHop != null) {
            landed = finishedHop.playerId to finishedHop.tokenId
            landing.snapTo(0f)
            landing.animateTo(1f, tween(LANDING_MS, easing = LinearEasing))
        }
    }

    val latestSpots by rememberUpdatedState(spots)
    val latestOnTokenClick by rememberUpdatedState(onTokenClick)

    Box(
        modifier
            .graphicsLayer {
                val t = (intro.value / 0.35f).coerceIn(0f, 1f)
                alpha = t
                scaleX = 0.96f + 0.04f * t
                scaleY = 0.96f + 0.04f * t
            }
            .shadow(elevation = 8.dp, shape = BoardShape, clip = false, ambientColor = InkDark, spotColor = InkDark)
            .background(SurfaceWhite, BoardShape)
    ) {
        BoardBackdrop()

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val g = BoardGeometry(Size(size.width.toFloat(), size.height.toFloat()))
                        val col = (tap.x - g.offsetX) / g.cell
                        // Gotis stand above their anchor, so aim at the middle of the pin.
                        val row = (tap.y - g.offsetY) / g.cell
                        latestSpots
                            .filter { it.isValid }
                            .map { it to hypot(it.cx - col, (it.cy - it.radius * PIN_CENTER_OFFSET) - row) }
                            .filter { (_, d) -> d <= TAP_RADIUS_CELLS }
                            .minByOrNull { (_, d) -> d }
                            ?.let { (spot, _) -> latestOnTokenClick(spot.tokenId) }
                    }
                }
        ) {
            val g = BoardGeometry(size)
            currentPlayer?.let { drawTurnMarker(g, it.color, breathe) }

            val introValue = intro.value
            val landingValue = landing.value
            val landedKey = landed
            for (spot in spots) {
                val isLanding = landingValue < 1f && landedKey?.first == spot.playerId && landedKey.second == spot.tokenId
                drawPawn(
                    center = g.point(spot.cx, spot.cy),
                    radius = spot.radius * g.cell,
                    color = spot.color,
                    lift = introDrop(introValue, spot.color) * g.cell +
                        (if (spot.isValid) pulse * g.cell * 0.12f else 0f) +
                        (if (isLanding) landingBounce(landingValue) * g.cell else 0f),
                    selectable = if (spot.isValid) pulse else 0f,
                    squash = if (isLanding) landingSquash(landingValue) else 0f
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
    Canvas(Modifier.fillMaxSize().clip(BoardShape).graphicsLayer()) {
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
private const val INTRO_MS = 1100
private const val LANDING_MS = 360

/** Offset from a goti's anchor to its visual middle (used to aim taps at the pin body). */
private const val PIN_CENTER_OFFSET = (PAWN_TOP - PAWN_BOTTOM) / 2

/**
 * Track gotis stand with their base inside their own square (a small margin above the square's
 * bottom edge); only the head may rise a little into the square above, as a standing piece would.
 */
private const val TRACK_PAWN_RADIUS = 0.52f
private const val BASE_MARGIN = 0.05f

/** Anchor offset below the square's centre that rests a goti of [radius] on the square's floor. */
private fun baseAnchor(radius: Float) = 0.5f - BASE_MARGIN - PAWN_BOTTOM * radius

/** Yard gotis are larger, their base seated low in the socket so they stand up above it. */
private const val YARD_PAWN_RADIUS = 0.6f
private const val YARD_BASE_DROP = 0.25f // Base centre below the socket centre, in cells

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

/** A goti resting on the board. [cx]/[cy]/[radius] are in cell units; (cx, cy) is its anchor. */
private class PawnSpot(
    val playerId: Int,
    val tokenId: Int,
    val color: PlayerColor,
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val isValid: Boolean
)

/** Top-left cell (row, col) of each colour's 6x6 base, by colour ordinal. */
private val BaseOrigins = arrayOf(0 to 0, 0 to 9, 9 to 9, 9 to 0)

/** Yard inset from the base edge, and socket centres within the base (cell units). */
private const val YARD_INSET = 0.85f
private val SocketOffsets = floatArrayOf(1.95f, 4.05f)
private const val SOCKET_RADIUS = 0.6f

/** Socket centre (col, row in cell units) for a colour's token slot 0..3. */
private fun socketCenter(colorOrdinal: Int, tokenId: Int): Pair<Float, Float> {
    val (row, col) = BaseOrigins[colorOrdinal]
    return (col + SocketOffsets[tokenId % 2]) to (row + SocketOffsets[(tokenId / 2) % 2])
}

// Engine yard cells → socket, so hops into and out of a yard land exactly on the socket.
private val SocketByHomeCell: Map<Pair<Int, Int>, Pair<Float, Float>> = buildMap {
    for ((ordinal, cells) in BoardConfig.homePositions) {
        cells.forEachIndexed { id, cell -> put(cell, socketCenter(ordinal, id)) }
    }
}

/** Anchor (col, row) for an engine cell: a socket for yard cells, otherwise the square. */
private fun anchorOf(cell: Pair<Int, Int>): Pair<Float, Float> =
    SocketByHomeCell[cell]?.let { (c, r) -> c to r + YARD_BASE_DROP - 0.3f * YARD_PAWN_RADIUS }
        ?: ((cell.second + 0.5f) to (cell.first + 0.5f + baseAnchor(TRACK_PAWN_RADIUS)))

// Finished gotis line up inside their colour's centre triangle: (col, row, lineIsHorizontal).
private val GoalAnchors = arrayOf(
    Triple(6.45f, 7.6f, false), // Red, left triangle
    Triple(7.5f, 6.55f, true),  // Green, top triangle
    Triple(8.55f, 7.6f, false), // Yellow, right triangle
    Triple(7.5f, 8.65f, true)   // Blue, bottom triangle
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
                    val (cx, cy) = anchorOf(BoardConfig.homeSpot(player.color.ordinal, token.id))
                    spots += PawnSpot(player.id, token.id, player.color, cx, cy, YARD_PAWN_RADIUS, isValid)
                }
                TokenState.ON_BOARD, TokenState.IN_HOME_COLUMN -> {
                    token.boardPosition?.let { byCell.getOrPut(it) { ArrayList(2) }.add(player to token) }
                }
                TokenState.FINISHED -> {
                    val (ax, ay, horizontal) = GoalAnchors[player.color.ordinal]
                    val shift = (finished.indexOf(token) - (finished.size - 1) / 2f) * 0.34f
                    val cx = if (horizontal) ax + shift else ax
                    val cy = if (horizontal) ay else ay + shift
                    spots += PawnSpot(player.id, token.id, player.color, cx, cy, 0.24f, false)
                }
            }
        }
    }

    for ((cell, occupants) in byCell) {
        val (row, col) = cell
        occupants.forEachIndexed { index, (player, token) ->
            val (dx, dy, radius) = stackOffset(occupants.size, index)
            val isValid = humanChoosing && player.id == current?.id && token.id in state.validMoves
            val drop = baseAnchor(radius)
            spots += PawnSpot(player.id, token.id, player.color, col + 0.5f + dx, row + 0.5f + dy + drop, radius, isValid)
        }
    }

    // Paint back-to-front so lower gotis overlap the ones above them.
    spots.sortBy { it.cy }
    return spots
}

/**
 * Offset (cell units) and radius for the [index]-th of [count] gotis sharing a cell. Front-row
 * gotis rest on the square's floor (dy = 0); back-row ones stand a little further up.
 */
private fun stackOffset(count: Int, index: Int): Triple<Float, Float, Float> = when (count) {
    1 -> Triple(0f, 0f, TRACK_PAWN_RADIUS)
    2 -> if (index == 0) Triple(-0.21f, 0f, 0.42f) else Triple(0.21f, 0f, 0.42f)
    3 -> when (index) {
        0 -> Triple(0f, -0.2f, 0.36f)
        1 -> Triple(-0.22f, 0f, 0.36f)
        else -> Triple(0.22f, 0f, 0.36f)
    }
    else -> when (index % 4) {
        0 -> Triple(-0.2f, -0.22f, 0.32f)
        1 -> Triple(0.2f, -0.22f, 0.32f)
        2 -> Triple(-0.2f, 0f, 0.32f)
        else -> Triple(0.2f, 0f, 0.32f)
    }
}

/** How high (cells) a colour's gotis still hang during the intro drop; 0 once landed. */
private fun introDrop(intro: Float, color: PlayerColor): Float {
    if (intro >= 1f) return 0f
    val start = 0.3f + color.ordinal * 0.1f
    val t = ((intro - start) / 0.35f).coerceIn(0f, 1f)
    return (1f - easeOutBack(t)) * 3f
}

/** Squash right at touch-down, easing back to upright. */
private fun landingSquash(p: Float): Float = if (p < 0.3f) 0.7f * (1f - p / 0.3f) else 0f

/** One small rebound (cells) after the squash. */
private fun landingBounce(p: Float): Float =
    if (p < 0.3f) 0f else sin((p - 0.3f) / 0.7f * PI.toFloat()) * 0.12f * (1f - p * 0.5f)

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    return 1f + c3 * u * u * u + c1 * u * u
}

// endregion

// region Dynamic layer

private fun DrawScope.drawHoppingPawn(g: BoardGeometry, hop: HopMove, color: PlayerColor, progress: Float) {
    val eased = (1f - cos(progress * PI.toFloat())) / 2f
    val (fromCol, fromRow) = anchorOf(hop.from)
    val (toCol, toRow) = anchorOf(hop.to)

    val ground = g.point(fromCol + (toCol - fromCol) * eased, fromRow + (toRow - fromRow) * eased)
    // Longer journeys (e.g. a captured goti flying home) arc higher.
    val distance = hypot(toCol - fromCol, toRow - fromRow)
    val arc = sin(progress * PI.toFloat())
    val lift = arc * g.cell * (0.5f + 0.14f * distance).coerceAtMost(2f)
    // Crouch at take-off and squash on touch-down; neutral at the top of the arc.
    val squash = 0.6f * (((0.15f - progress) / 0.15f).coerceAtLeast(0f) + ((progress - 0.85f) / 0.15f).coerceAtLeast(0f))

    // Blend sizes so a goti leaving (or flying back to) its yard never snaps in size.
    fun sizeAt(cell: Pair<Int, Int>) = if (SocketByHomeCell.containsKey(cell)) YARD_PAWN_RADIUS else TRACK_PAWN_RADIUS
    val radius = sizeAt(hop.from) + (sizeAt(hop.to) - sizeAt(hop.from)) * eased
    drawPawn(center = ground, radius = g.cell * radius * (1f + arc * 0.1f), color = color, lift = lift, squash = squash)
}

/** Breathing white ring inside the active player's base frame. */
private fun DrawScope.drawTurnMarker(g: BoardGeometry, color: PlayerColor, strength: Float) {
    val (row, col) = BaseOrigins[color.ordinal]
    val inset = g.cell * 0.4f
    drawRoundRect(
        color = Color.White.copy(alpha = 0.95f * strength),
        topLeft = Offset(g.x(col.toFloat()) + inset, g.y(row.toFloat()) + inset),
        size = Size(g.cell * 6 - inset * 2, g.cell * 6 - inset * 2),
        cornerRadius = CornerRadius(g.cell * 0.35f),
        style = Stroke(width = g.cell * 0.1f)
    )
}

/** Expanding ring plus radiating sparks; gold sparks mark a goti reaching home. */
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
private val sharedStarPath = Path()
private val sharedTrianglePath = Path()

/** Bold solid base, white yard, and four solid coloured sockets. */
private fun DrawScope.drawHomeBase(g: BoardGeometry, player: PlayerColor) {
    val color = PlayerColorUtils.getComposeColor(player)
    val dark = PlayerColorUtils.getPawnColorScheme(player).darkColor
    val (row, col) = BaseOrigins[player.ordinal]
    val x = g.x(col.toFloat())
    val y = g.y(row.toFloat())
    val homeSize = g.cell * 6

    drawRect(color = color, topLeft = Offset(x, y), size = Size(homeSize, homeSize))

    val inset = g.cell * YARD_INSET
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(x + inset, y + inset),
        size = Size(homeSize - inset * 2, homeSize - inset * 2),
        cornerRadius = CornerRadius(g.cell * 0.12f)
    )

    for (id in 0 until 4) {
        val (sc, sr) = socketCenter(player.ordinal, id)
        val center = g.point(sc, sr)
        drawCircle(color, g.cell * SOCKET_RADIUS, center)
        drawCircle(dark.copy(alpha = 0.35f), g.cell * SOCKET_RADIUS, center, style = Stroke(g.cell * 0.05f))
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
    val line = Stroke(width = 1.dp.toPx())
    for (cell in cells) {
        val topLeft = g.point(cell.second.toFloat(), cell.first.toFloat())
        colored[cell]?.let { drawRect(it, topLeft, cellSize) }
        drawRect(color = Color(0xFFC9CDD2), topLeft = topLeft, size = cellSize, style = line)
    }
}

// Thin arrows on the square before each home column: (col, row, angle) by colour ordinal.
private val ArrowSpecs = arrayOf(Triple(0, 7, 0f), Triple(7, 0, 90f), Triple(14, 7, 180f), Triple(7, 14, 270f))

private fun DrawScope.drawTrackArrows(g: BoardGeometry) {
    for (color in PlayerColor.entries) {
        val (col, row, angle) = ArrowSpecs[color.ordinal]
        drawLineArrow(g.point(col + 0.5f, row + 0.5f), g.cell * 0.56f, PlayerColorUtils.getComposeColor(color), angle, g.cell * 0.07f)
    }
}

private fun DrawScope.drawLineArrow(center: Offset, length: Float, color: Color, angleDeg: Float, width: Float) {
    val rad = Math.toRadians(angleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()
    fun rotated(px: Float, py: Float) = Offset(center.x + px * cosA - py * sinA, center.y + px * sinA + py * cosA)

    val half = length / 2
    val tip = rotated(half, 0f)
    drawLine(color, rotated(-half, 0f), tip, width, StrokeCap.Round)
    drawLine(color, tip, rotated(half - length * 0.35f, -length * 0.3f), width, StrokeCap.Round)
    drawLine(color, tip, rotated(half - length * 0.35f, length * 0.3f), width, StrokeCap.Round)
}

/** Outlined stars on the neutral safe squares. */
private fun DrawScope.drawSafeSquares(g: BoardGeometry) {
    for (index in BoardConfig.starSpotIndices) {
        drawStar(g.cellCenter(BoardConfig.mainTrack[index]), g.cell * 0.36f, Color.White, Color(0xFF9AA1A9))
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, fillColor: Color, strokeColor: Color) {
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
    drawPath(sharedStarPath, color = strokeColor, style = Stroke(width = 1.4.dp.toPx()))
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
    for (color in PlayerColor.entries) {
        val c = TriangleCorners[color.ordinal]
        sharedTrianglePath.reset()
        sharedTrianglePath.moveTo(center.x + c[0] * g.cell, center.y + c[1] * g.cell)
        sharedTrianglePath.lineTo(center.x + c[2] * g.cell, center.y + c[3] * g.cell)
        sharedTrianglePath.lineTo(center.x, center.y)
        sharedTrianglePath.close()
        drawPath(sharedTrianglePath, color = PlayerColorUtils.getComposeColor(color), style = Fill)
    }
}

// endregion
