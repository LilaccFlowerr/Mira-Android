package com.izuuhh.mira.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star

/** The soft Material 3 Expressive shapes used as decoration. */
object Shapes {
    val all: List<RoundedPolygon> by lazy {
        listOf(
            RoundedPolygon(4, rounding = CornerRounding(0.34f)),
            RoundedPolygon.star(8, innerRadius = 0.72f, rounding = CornerRounding(0.2f)),
            RoundedPolygon.star(6, innerRadius = 0.78f, rounding = CornerRounding(0.3f)),
            RoundedPolygon.star(12, innerRadius = 0.86f, rounding = CornerRounding(0.5f)),
            RoundedPolygon(5, rounding = CornerRounding(0.3f)),
            RoundedPolygon(3, rounding = CornerRounding(0.28f)),
        )
    }
}

/** The shapes the loading indicator cycles through, each fitted into the unit square. */
val LoaderShapes: List<RoundedPolygon> by lazy {
    listOf(
        RoundedPolygon.circle(),
        RoundedPolygon.rectangle(1.7f, 1.7f, rounding = CornerRounding(0.35f)),
        RoundedPolygon.rectangle(2f, 1.3f, rounding = CornerRounding(0.65f)),
        RoundedPolygon.rectangle(2.2f, 1.1f, rounding = CornerRounding(0.55f)),
        RoundedPolygon(3, rounding = CornerRounding(0.25f)),
        RoundedPolygon(4, rounding = CornerRounding(0.2f)),
        RoundedPolygon(5, rounding = CornerRounding(0.2f)),
        RoundedPolygon.star(6, innerRadius = 0.72f, rounding = CornerRounding(0.1f)),
        RoundedPolygon.star(8, innerRadius = 0.8f, rounding = CornerRounding(0.1f)),
        RoundedPolygon.star(10, innerRadius = 0.78f, rounding = CornerRounding(0.1f)),
        RoundedPolygon.star(4, innerRadius = 0.74f, rounding = CornerRounding(0.45f)),
        RoundedPolygon.star(6, innerRadius = 0.8f, rounding = CornerRounding(0.5f)),
        RoundedPolygon.star(4, innerRadius = 0.45f, rounding = CornerRounding(0.3f)),
        RoundedPolygon.star(10, innerRadius = 0.82f, rounding = CornerRounding(0.35f)),
        RoundedPolygon.star(8, innerRadius = 0.72f, rounding = CornerRounding(0.5f)),
        RoundedPolygon.star(7, innerRadius = 0.86f, rounding = CornerRounding(0.6f)),
    ).map { it.normalized() }
}

/** Scales a path built in the unit square to [px] pixels. */
fun Path.scaled(px: Float): Path = Path().also { out ->
    out.addPath(this)
    val m = Matrix()
    m.scale(px, px)
    out.transform(m)
}

fun List<Cubic>.toPath(): Path = Path().apply {
    if (isEmpty()) return@apply
    moveTo(first().anchor0X, first().anchor0Y)
    for (c in this@toPath) cubicTo(c.control0X, c.control0Y, c.control1X, c.control1Y, c.anchor1X, c.anchor1Y)
    close()
}

/** Fits a path built in a unit-radius space into a square of [px] pixels. */
fun Path.fitted(px: Float): Path = Path().also { out ->
    out.addPath(this)
    val m = Matrix()
    m.translate(px / 2f, px / 2f)
    m.scale(px / 2f * 0.92f, px / 2f * 0.92f)
    out.transform(m)
}

@Composable
fun ShapeArt(variant: Int, color: Color, size: Dp, modifier: Modifier = Modifier, turn: Float = 0f) {
    val path = remember(variant) { Shapes.all[variant.mod(Shapes.all.size)].cubics.toPath() }
    Canvas(modifier.size(size).rotate(turn)) {
        drawPath(path.fitted(this.size.minDimension), color)
    }
}
