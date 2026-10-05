package fi.jukkakot.rubikkisolveri.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private class Piece(val angle: Float, val speed: Float, val spin: Float, val color: Color)

/**
 * A burst of sticker-coloured confetti from the middle of its bounds, falling and fading out.
 * A new [burst] above 0 starts it again; 0 draws nothing.
 */
@Composable
fun Confetti(burst: Int, modifier: Modifier = Modifier) {
    val time = remember { Animatable(1f) }
    val pieces = remember(burst) {
        val random = Random(burst)
        val colors = CubeColor.entries.map(StickerColors::of)
        List(PIECES) {
            // Mostly upwards, so the pieces rise around the cube before falling.
            val angle = (-PI / 2 + (random.nextFloat() - 0.5f) * PI * 1.4f).toFloat()
            Piece(angle, 0.55f + random.nextFloat() * 0.6f, (random.nextFloat() - 0.5f) * 900f, colors[it % colors.size])
        }
    }
    LaunchedEffect(burst) {
        if (burst == 0) return@LaunchedEffect
        time.snapTo(0f)
        time.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
    }
    Canvas(modifier) {
        val t = time.value
        if (burst == 0 || t >= 1f) return@Canvas
        val reach = size.minDimension * 0.75f
        val piece = Size(size.minDimension * 0.035f, size.minDimension * 0.022f)
        val alpha = if (t < 0.7f) 1f else (1f - t) / 0.3f
        for (p in pieces) {
            val x = center.x + cos(p.angle) * p.speed * reach * t
            val y = center.y + sin(p.angle) * p.speed * reach * t + GRAVITY * reach * t * t
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(p.color.copy(alpha = alpha), Offset(x - piece.width / 2, y - piece.height / 2), piece)
            }
        }
    }
}

private const val PIECES = 42
private const val DURATION_MS = 1600
private const val GRAVITY = 1.1f
