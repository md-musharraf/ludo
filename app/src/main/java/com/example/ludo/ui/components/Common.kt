package com.example.ludo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ludo.theme.*

val CardShape = RoundedCornerShape(20.dp)
val ControlShape = RoundedCornerShape(14.dp)

/**
 * A value that ping-pongs between [from] and [to] only while [active]; otherwise it rests at
 * [idle] and no frames are scheduled, so idle widgets cost nothing.
 *
 * Read the returned state inside draw/graphicsLayer lambdas to keep updates out of composition.
 */
@Composable
fun rememberPulse(
    active: Boolean,
    from: Float,
    to: Float,
    durationMs: Int,
    idle: Float = from,
    easing: Easing = FastOutSlowInEasing,
    repeatMode: RepeatMode = RepeatMode.Reverse
): State<Float> {
    val anim = remember { Animatable(idle) }
    LaunchedEffect(active, from, to, durationMs) {
        if (active) {
            anim.snapTo(from)
            anim.animateTo(to, infiniteRepeatable(tween(durationMs, easing = easing), repeatMode))
        } else {
            anim.snapTo(idle)
        }
    }
    return anim.asState()
}

private val TitleColors = listOf('L' to LudoRed, 'U' to LudoGreen, 'D' to LudoYellow, 'O' to LudoBlue)

/** The four-colour "LUDO" wordmark. */
@Composable
fun LudoTitle(fontSize: TextUnit, modifier: Modifier = Modifier) {
    Row(modifier) {
        for ((letter, color) in TitleColors) {
            Text(letter.toString(), color = color, fontSize = fontSize, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

/** App mark: a miniature board, four coloured bases around a white centre. */
@Composable
fun LogoMark(size: Dp, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(size)
            .shadow(6.dp, RoundedCornerShape(size * 0.24f), ambientColor = InkDark, spotColor = InkDark)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(SurfaceWhite)
    ) {
        val s = this.size.minDimension
        val gap = s * 0.06f
        val q = (s - gap * 3) / 2
        val colors = listOf(LudoRed, LudoGreen, LudoBlue, LudoYellow)
        for (i in 0 until 4) {
            val x = gap + (i % 2) * (q + gap)
            val y = gap + (i / 2) * (q + gap)
            drawRoundRect(colors[i], Offset(x, y), Size(q, q), CornerRadius(q * 0.28f))
            drawCircle(Color.White, q * 0.2f, Offset(x + q / 2, y + q / 2))
        }
        drawCircle(Color.White, s * 0.15f, Offset(s / 2, s / 2))
        drawCircle(InkDark, s * 0.06f, Offset(s / 2, s / 2))
    }
}

/** White rounded card with a hairline border and a soft shadow. */
@Composable
fun LudoCard(
    modifier: Modifier = Modifier,
    elevation: Dp = 3.dp,
    borderColor: Color = HairlineBorder,
    borderWidth: Dp = 1.dp,
    shape: RoundedCornerShape = CardShape,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = InkDark, spotColor = InkDark)
            .clip(shape)
            .background(SurfaceWhite)
            .border(borderWidth, borderColor, shape),
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = InkMuted
    )
}

/** Round white icon button used in top bars. */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = InkDark, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** Pill-shaped single-choice control. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = InkDark
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(ControlShape)
            .background(SubtleFill)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (option in options) {
            val isSelected = option == selected
            val bg by animateColorAsState(if (isSelected) SurfaceWhite else Color.Transparent, label = "segBg")
            val fg by animateColorAsState(if (isSelected) accent else InkMuted, label = "segFg")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(if (isSelected) Modifier.shadow(1.dp, RoundedCornerShape(10.dp)) else Modifier)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) }
            ) {
                Text(
                    text = label(option),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = fg
                )
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = LudoGreen,
    height: Dp = 56.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite, contentColor = InkDark)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Centred dialog card shared by rules, settings and confirmations. */
@Composable
fun LudoDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = { if (dismissible) onDismiss() }) {
        LudoCard(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(), elevation = 12.dp, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = InkDark)
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmColor: Color = LudoRed
) {
    LudoDialog(title = title, onDismiss = onDismiss) {
        Text(message, fontSize = 15.sp, color = InkMuted, lineHeight = 21.sp)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton("Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) {
                Text(confirmLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

private val Rules = listOf(
    "Tap your dice to roll. Every player has their own dice.",
    "Roll a 6 to bring a piece out of its base.",
    "A 6 gives a bonus roll. Three 6s in a row ends the turn.",
    "Land on a lone opponent piece to send it home and roll again.",
    "Two pieces of one colour on a plain square form a block nobody can land on.",
    "Star squares and coloured start squares are safe.",
    "Pieces must reach Home with an exact roll, which also earns a bonus roll.",
    "Bring all four pieces Home first to win. Others play on for 2nd and 3rd."
)
private val RuleColors = listOf(LudoRed, LudoGreen, LudoYellow, LudoBlue)

@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    LudoDialog(title = "How to Play", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Rules.forEachIndexed { i, rule ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(RuleColors[i % 4])
                    ) {
                        Text("${i + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(rule, fontSize = 14.sp, color = InkDark, lineHeight = 19.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Got it", onClick = onDismiss, height = 50.dp)
    }
}

@Composable
fun SettingsDialog(soundEnabled: Boolean, onSoundChange: (Boolean) -> Unit, onDismiss: () -> Unit) {
    LudoDialog(title = "Settings", onDismiss = onDismiss) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(ControlShape)
                .background(SubtleFill)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                if (soundEnabled) LudoIcons.VolumeOn else LudoIcons.VolumeOff,
                contentDescription = null,
                tint = InkDark,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sound effects", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = InkDark)
                Text("Dice, moves and captures", fontSize = 12.sp, color = InkMuted)
            }
            Switch(
                checked = soundEnabled,
                onCheckedChange = onSoundChange,
                colors = SwitchDefaults.colors(checkedTrackColor = LudoGreen, checkedThumbColor = Color.White)
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Done", onClick = onDismiss, height = 50.dp, color = InkDark)
    }
}
