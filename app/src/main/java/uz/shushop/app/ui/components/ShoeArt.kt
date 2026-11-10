package uz.shushop.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import uz.shushop.app.util.ShoeColors
import uz.shushop.app.util.ShoeGeometry
import uz.shushop.app.util.ShoeLayer
import uz.shushop.app.util.ShoeSpec

/**
 * A side-view sneaker drawn with Canvas - the same drawing the website makes as SVG - used as the shoe picture
 * until the shop owner adds a real photo. Changing [colorHex] repaints it, so the picture follows the colour dot.
 */
@Composable
fun ShoeArt(style: String, colorHex: String?, seed: Long, modifier: Modifier = Modifier) {
    val prepared = remember(style, colorHex, seed) {
        val accent = ShoeColors.toHex(ShoeColors.accentFor(seed))
        Prepared.from(ShoeGeometry.spec(style, colorHex, accent))
    }
    Canvas(modifier) {
        drawShoe(prepared, minOf(size.width, size.height))
    }
}

// ---- the layers with their paths already parsed (parsing happens once, not on every frame) -------------------------

private sealed interface Layer {
    class Fill(val path: Path, val color: Color) : Layer
    class Shade(val path: Path) : Layer
    class Line(val path: Path, val color: Color, val width: Float, val dashed: Boolean) : Layer
    class Boxes(val clip: Path, val boxes: List<ShoeLayer.Box>) : Layer
}

private class Prepared(val offsetY: Float, val upperLift: Float, val upper: List<Layer>, val sole: List<Layer>) {
    companion object {
        fun from(spec: ShoeSpec) = Prepared(spec.offsetY, spec.upperLift, spec.upper.map(::layer), spec.sole.map(::layer))

        private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

        private fun layer(l: ShoeLayer): Layer = when (l) {
            is ShoeLayer.Fill -> Layer.Fill(parse(l.path), Color(l.argb))
            is ShoeLayer.Shade -> Layer.Shade(parse(l.path))
            is ShoeLayer.Line -> Layer.Line(parse(l.path), Color(l.argb), l.width, l.dashed)
            is ShoeLayer.ClippedBoxes -> Layer.Boxes(parse(l.clipPath), l.boxes)
        }
    }
}

private val ShadowCenter = Color.Black.copy(alpha = 0.30f)
private val ShadowEdge = Color.Black.copy(alpha = 0f)

/** The drawing is made in a 600 x 600 space; this scales it to [side] pixels and applies the same transforms as the SVG. */
private fun DrawScope.drawShoe(shoe: Prepared, side: Float) {
    val k = side / 600f
    // centre the square picture inside the canvas
    val dx = (size.width - side) / 2f
    val dy = (size.height - side) / 2f
    withTransform({
        translate(dx, dy)
        scale(k, k, pivot = Offset.Zero)
        translate(0f, shoe.offsetY)
        translate(300f, 300f)
        scale(1.14f, 1.14f, pivot = Offset.Zero)
        translate(-300f, -300f)
    }) {
        // soft shadow on the ground: a round gradient squashed into an ellipse (rx 236, ry 20)
        withTransform({
            translate(305f, 428f)
            scale(1f, 20f / 236f, pivot = Offset.Zero)
        }) {
            drawCircle(
                brush = Brush.radialGradient(listOf(ShadowCenter, ShadowEdge), center = Offset.Zero, radius = 236f),
                radius = 236f,
                center = Offset.Zero,
            )
        }
        // the shoe itself is turned a little, like a product photo
        withTransform({
            translate(0f, -6f)
            rotate(-3f, pivot = Offset(300f, 330f))
        }) {
            withTransform({ translate(0f, shoe.upperLift) }) {
                shoe.upper.forEach { drawLayer(it) }
            }
            shoe.sole.forEach { drawLayer(it) }
        }
    }
}

private val DashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))

private fun DrawScope.drawLayer(layer: Layer) {
    when (layer) {
        is Layer.Fill -> drawPath(layer.path, layer.color)
        is Layer.Shade -> {
            val b = layer.path.getBounds()
            drawPath(
                layer.path,
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.22f),
                    0.55f to Color.White.copy(alpha = 0f),
                    1f to Color.Black.copy(alpha = 0.16f),
                    startY = b.top,
                    endY = b.bottom,
                ),
            )
        }
        is Layer.Line -> drawPath(
            layer.path,
            layer.color,
            style = Stroke(
                width = layer.width,
                cap = if (layer.dashed) StrokeCap.Butt else StrokeCap.Round,
                pathEffect = if (layer.dashed) DashEffect else null,
            ),
        )
        is Layer.Boxes -> clipPath(layer.clip) {
            layer.boxes.forEach { box ->
                drawRoundRect(
                    color = Color(box.argb),
                    topLeft = Offset(box.x, box.y),
                    size = Size(box.w, box.h),
                    cornerRadius = CornerRadius(box.radius, box.radius),
                )
            }
        }
    }
}
