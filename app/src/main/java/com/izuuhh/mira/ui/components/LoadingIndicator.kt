package com.izuuhh.mira.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.izuuhh.mira.ui.theme.Mira

/** A shape that keeps morphing into the next one while it turns: Mira's loading animation. */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier, size: Dp = 56.dp, color: Color = Mira.colors.primary) {
    val order = remember { listOf(3, 1, 4, 0, 2, 5) }
    val morphs = remember { order.indices.map { Morph(Shapes.all[order[it]], Shapes.all[order[(it + 1) % order.size]]) } }
    val transition = rememberInfiniteTransition(label = "loading")
    val t by transition.animateFloat(0f, order.size.toFloat(), infiniteRepeatable(tween(order.size * 650, easing = LinearEasing)), label = "morph")
    val turn by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "turn")
    val index = t.toInt().coerceIn(0, order.size - 1)
    val eased = FastOutSlowInEasing.transform(t - index)
    Canvas(modifier.size(size).rotate(turn)) {
        drawPath(morphs[index].asCubics(eased).toPath().fitted(this.size.minDimension), color)
    }
}
