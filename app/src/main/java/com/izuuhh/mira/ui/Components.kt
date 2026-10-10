package com.izuuhh.mira.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.izuuhh.mira.ui.theme.Mira
import kotlin.math.sin

@Composable
fun Cover(url: String?, size: Dp, modifier: Modifier = Modifier, radius: Dp = 12.dp, description: String? = null) {
    val m = Mira.colors
    Box(modifier.size(size).clip(RoundedCornerShape(radius)).background(m.container)) {
        if (url != null) AsyncImage(url, description, Modifier.size(size))
    }
}

/** Round icon button; [filled] gives the primary pill used for play/pause. */
@Composable
fun RoundButton(
    icon: ImageVector, description: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, filled: Boolean = false, selected: Boolean = false, enabled: Boolean = true, size: Dp = 48.dp,
) {
    val m = Mira.colors
    IconButton(
        onClick = onClick, enabled = enabled, modifier = modifier.size(size),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = when { filled -> m.primary; selected -> m.container; else -> androidx.compose.ui.graphics.Color.Transparent },
            contentColor = when { filled -> m.onPrimary; selected -> m.primary; else -> m.text },
            disabledContentColor = m.muted.copy(alpha = 0.5f),
            disabledContainerColor = if (filled) m.hover else androidx.compose.ui.graphics.Color.Transparent,
        ),
    ) { Icon(icon, description) }
}

/** Progress line that is wavy for the played part while music plays; tap or drag to seek. */
@Composable
fun WavyProgress(
    position: Long, duration: Long, playing: Boolean, seekable: Boolean,
    onSeek: (Long) -> Unit, modifier: Modifier = Modifier, height: Dp = 24.dp,
) {
    val m = Mira.colors
    var dragging by remember { mutableStateOf<Float?>(null) }
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "phase",
    )
    val fraction = dragging ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Canvas(
        modifier.fillMaxWidth().height(height)
            .pointerInput(seekable, duration) {
                if (!seekable || duration <= 0) return@pointerInput
                detectTapGestures { onSeek((it.x / size.width * duration).toLong().coerceIn(0, duration)) }
            }
            .pointerInput(seekable, duration) {
                if (!seekable || duration <= 0) return@pointerInput
                detectDragGestures(
                    onDragStart = { dragging = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragging?.let { f -> onSeek((f * duration).toLong()) }; dragging = null },
                    onDragCancel = { dragging = null },
                ) { change, _ -> dragging = (change.position.x / size.width).coerceIn(0f, 1f) }
            },
    ) {
        val stroke = 4.dp.toPx()
        val midY = size.height / 2
        val endX = size.width * fraction
        val amp = if (playing) 3.dp.toPx() else 0f
        val wavelength = 24.dp.toPx()
        // Rest of the track: flat line.
        drawLine(m.outline, Offset(endX, midY), Offset(size.width, midY), stroke, StrokeCap.Round)
        // Played part: wave while playing, straight when paused.
        val path = Path().apply {
            moveTo(0f, midY)
            var x = 0f
            while (x < endX) { x = minOf(x + 2f, endX); lineTo(x, midY + amp * sin(x / wavelength * 2f * Math.PI.toFloat() - phase)) }
        }
        drawPath(path, m.primary, style = Stroke(stroke, cap = StrokeCap.Round))
        drawCircle(m.primary, 7.dp.toPx(), Offset(endX, midY))
    }
}

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

@Composable
fun Dot(modifier: Modifier = Modifier) = Box(modifier.size(4.dp).clip(CircleShape).background(Mira.colors.muted))
