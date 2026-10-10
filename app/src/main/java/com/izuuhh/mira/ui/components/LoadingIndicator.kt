package com.izuuhh.mira.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.izuuhh.mira.ui.theme.Mira
import kotlinx.coroutines.delay

/**
 * Material 3 Expressive loading indicator: a round container with one shape that snaps into the next
 * one every half second. It waits a moment before showing, so quick loads do not flash it.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    color: Color = Mira.colors.primary,
    containerColor: Color = Mira.colors.container,
) {
    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(150), label = "loader-alpha")
    var from by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150)
        shown = true
        // Each shape holds for a moment, then snaps into the next with a quick, springy curve.
        while (true) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(300, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)))
            from = (from + 1) % LoaderShapes.size
            delay(200)
        }
    }
    val morph = remember(from) { Morph(LoaderShapes[from], LoaderShapes[(from + 1) % LoaderShapes.size]) }
    Canvas(modifier.size(size).alpha(alpha)) {
        drawCircle(containerColor, radius = this.size.minDimension / 2)
        val half = this.size.minDimension / 2
        val path = morph.asCubics(progress.value.coerceIn(0f, 1f)).toPath().scaled(half)
        // Centre the half-size shape in the circle.
        translate(rememberleft = half / 2, top = half / 2) {
            drawPath(path, color)
        }
    }
}
