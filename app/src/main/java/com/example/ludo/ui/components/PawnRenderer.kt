package com.example.ludo.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.ludo.core.util.PlayerColorUtils
import com.example.ludo.model.PlayerColor

// Reused across draws (main thread only) to avoid per-frame Path allocations.
private val bodyPath = Path()

/**
 * Classic Ludo goti: a rounded base, tapered body, collar and round head, lit from the
 * top-left with one glossy highlight. A dark outline keeps it legible at small sizes and on
 * cells of its own colour.
 *
 * @param center cell point the pawn stands on; the drawing is vertically centred on it.
 * @param radius overall scale (roughly half the pawn's width).
 * @param lift how far (px) the pawn is raised off the board, e.g. mid-hop; the shadow stays put.
 * @param selectable 0..1 strength of the "you can move me" ring drawn around the base.
 */
internal fun DrawScope.drawPawn(
    center: Offset,
    radius: Float,
    color: PlayerColor,
    lift: Float = 0f,
    selectable: Float = 0f
) {
    val c = PlayerColorUtils.getPawnColorScheme(color)
    val r = radius
    val cx = center.x
    val groundY = center.y + r * 0.62f // Base bottom rests here
    val by = center.y + r * 0.15f - lift // Body origin: centres the silhouette on the cell
    val outline = r * 0.09f

    // 1. Ground shadow: shrinks and fades as the pawn rises.
    val liftAmount = (lift / (r * 2.2f)).coerceIn(0f, 1f)
    val shadowW = r * 1.7f * (1f - 0.35f * liftAmount)
    val shadowH = r * 0.5f * (1f - 0.35f * liftAmount)
    drawOval(
        color = Color.Black.copy(alpha = 0.22f * (1f - 0.5f * liftAmount)),
        topLeft = Offset(cx - shadowW / 2, groundY - shadowH / 2 + r * 0.06f),
        size = Size(shadowW, shadowH)
    )

    // 2. Selection ring around the base.
    if (selectable > 0f) {
        val ringW = r * (2.1f + 0.35f * selectable)
        val ringH = ringW * 0.38f
        val ringTopLeft = Offset(cx - ringW / 2, groundY - ringH / 2)
        drawOval(Color.White.copy(alpha = 0.55f + 0.4f * selectable), ringTopLeft, Size(ringW, ringH), style = Stroke(r * 0.2f))
        drawOval(c.baseColor, ringTopLeft, Size(ringW, ringH), style = Stroke(r * 0.09f))
    }

    val baseRectW = r * 1.56f
    val baseRectH = r * 0.5f
    val baseTop = by + r * 0.26f
    val headCenter = Offset(cx, by - r * 0.62f)
    val headRadius = r * 0.46f

    // 3. Outline: every part drawn slightly larger in the dark shade, then filled over.
    drawOval(c.deepShadow, Offset(cx - baseRectW / 2 - outline, baseTop - outline), Size(baseRectW + outline * 2, baseRectH + outline * 2))
    buildBody(cx, by, r, inflate = outline)
    drawPath(bodyPath, c.deepShadow)
    drawCircle(c.deepShadow, headRadius + outline, headCenter)

    // 4. Base: a flat disc with a darker rim.
    drawOval(
        brush = Brush.verticalGradient(listOf(c.baseColor, c.darkColor), startY = baseTop, endY = baseTop + baseRectH),
        topLeft = Offset(cx - baseRectW / 2, baseTop),
        size = Size(baseRectW, baseRectH)
    )

    // 5. Body, lit from the left.
    buildBody(cx, by, r, inflate = 0f)
    drawPath(
        bodyPath,
        Brush.horizontalGradient(
            listOf(c.lightColor, c.baseColor, c.darkColor),
            startX = cx - r * 0.62f,
            endX = cx + r * 0.62f
        )
    )

    // 6. Collar under the head.
    val collarW = r * 0.82f
    val collarH = r * 0.24f
    drawOval(c.darkColor, Offset(cx - collarW / 2, by - r * 0.36f), Size(collarW, collarH))

    // 7. Glossy head.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(c.highlightColor, c.lightColor, c.baseColor, c.darkColor),
            center = Offset(headCenter.x - headRadius * 0.35f, headCenter.y - headRadius * 0.4f),
            radius = headRadius * 1.5f
        ),
        radius = headRadius,
        center = headCenter
    )
    drawOval(
        color = Color.White.copy(alpha = 0.8f),
        topLeft = Offset(headCenter.x - headRadius * 0.55f, headCenter.y - headRadius * 0.62f),
        size = Size(headRadius * 0.5f, headRadius * 0.34f)
    )
}

/** Tapered body from the base up to the neck, optionally grown by [inflate] for the outline. */
private fun buildBody(cx: Float, by: Float, r: Float, inflate: Float) {
    val bottomHalf = r * 0.62f + inflate
    val neckHalf = r * 0.22f + inflate
    val bottomY = by + r * 0.46f + inflate
    val neckY = by - r * 0.42f - inflate
    bodyPath.reset()
    bodyPath.moveTo(cx - bottomHalf, bottomY)
    bodyPath.cubicTo(cx - bottomHalf * 0.9f, by + r * 0.05f, cx - neckHalf * 1.3f, by - r * 0.12f, cx - neckHalf, neckY)
    bodyPath.lineTo(cx + neckHalf, neckY)
    bodyPath.cubicTo(cx + neckHalf * 1.3f, by - r * 0.12f, cx + bottomHalf * 0.9f, by + r * 0.05f, cx + bottomHalf, bottomY)
    bodyPath.close()
}
