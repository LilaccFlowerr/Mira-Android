package com.izuuhh.mira.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.izuuhh.mira.ui.theme.LocalWavy
import com.izuuhh.mira.ui.theme.Mira
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()

/**
 * A clock that ticks every frame while [running], as SystemClock.elapsedRealtime().
 * Read it inside a draw or layout lambda so only drawing is redone, not the whole screen.
 */
@Composable
fun rememberPlaybackClock(running: Boolean): State<Long> {
    val clock = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(running) {
        clock.longValue = SystemClock.elapsedRealtime()
        if (running) while (true) withFrameMillis { clock.longValue = SystemClock.elapsedRealtime() }
    }
    return clock
}

/**
 * Material 3 Expressive style progress: the played part waves while music plays and eases into a
 * flat line when paused. Tap or drag to seek; a thumb appears while dragging.
 */
@Composable
fun WavyProgress(
    position: () -> Long,
    duration: Long,
    playing: Boolean,
    seekable: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 24.dp,
) {
    val m = Mira.colors
    val amplitude by animateFloatAsState(if (playing && LocalWavy.current) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "amplitude")
    var phase by remember { mutableFloatStateOf(0f) }
    var drag by remember { mutableStateOf<Float?>(null) }
    val animating = (playing && LocalWavy.current) || amplitude > 0.01f
    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        var last = 0L
        while (true) withFrameNanos { t ->
            if (last != 0L) phase = (phase + (t - last) / 1e9f * 4.2f) % TWO_PI
            last = t
        }
    }
    val currentDuration by rememberUpdatedState(duration)
    val currentOnSeek by rememberUpdatedState(onSeek)

    Canvas(
        modifier.fillMaxWidth().height(height).pointerInput(seekable) {
            if (!seekable) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                fun fraction(x: Float) = ((x - 2f) / max(1f, size.width - 4f)).coerceIn(0f, 1f)
                drag = fraction(down.position.x)
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.first()
                    if (change.pressed) { drag = fraction(change.position.x); change.consume() }
                } while (event.changes.any { it.pressed })
                drag?.let { if (currentDuration > 0) currentOnSeek((it * currentDuration).toLong()) }
                drag = null
            }
        },
    ) {
        val stroke = 4.dp.toPx()
        val cap = stroke / 2
        val mid = size.height / 2
        val start = cap
        val end = size.width - cap
        val fraction = drag ?: if (duration > 0) (position().toFloat() / duration).coerceIn(0f, 1f) else 0f
        val head = start + (end - start) * fraction
        val thumb = drag != null
        val gap = (if (thumb) 7 else 8).dp.toPx()
        val activeEnd = head - if (thumb) gap else 0f
        val wavelength = 34.dp.toPx()
        val amp = 3.dp.toPx() * amplitude
        val taperLength = 12.dp.toPx()

        if (activeEnd > start + 0.5f) {
            val path = Path()
            var x = start
            while (true) {
                // Taper into the head so the wave meets the thumb or the flat track smoothly.
                val taper = min(1f, (activeEnd - x) / taperLength)
                val y = mid + amp * taper * sin(TWO_PI * x / wavelength - phase)
                if (x == start) path.moveTo(x, y) else path.lineTo(x, y)
                if (x >= activeEnd) break
                x = min(x + 2.dp.toPx(), activeEnd)
            }
            drawPath(path, m.primary, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val restStart = head + gap + if (fraction > 0f) 0f else -gap
        if (restStart < end) {
            drawLine(m.outline, Offset(restStart, mid), Offset(end, mid), stroke, StrokeCap.Round)
            drawCircle(m.primary, 2.dp.toPx(), Offset(end, mid))
        }
        if (thumb) {
            drawRoundRect(m.primary, Offset(head - 2.dp.toPx(), mid - 9.dp.toPx()), Size(4.dp.toPx(), 18.dp.toPx()), CornerRadius(2.dp.toPx()))
        }
    }
}

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
