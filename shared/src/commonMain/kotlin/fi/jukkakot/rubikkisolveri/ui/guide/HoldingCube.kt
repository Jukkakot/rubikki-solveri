package fi.jukkakot.rubikkisolveri.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.ui.animationScale
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.Quat
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.V3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The ring: a dark outer stroke and a white inner one, so it reads on any sticker colour. */
private val RING_OUTER = Color(0xFF14151A)
private val RING_INNER = Color(0xFFFFFFFF)

/** The tilted pose the cube turns from, and how long the turn and the rings' fade take. */
private val TILT: Quat = Quat.axisAngle(V3(0.6f, 1f, 0.35f).normalized(), (150 * PI / 180).toFloat())
private const val TURN_MS = 1_200
private const val REVEAL_MS = 300

/**
 * [cube] turning into the guide's holding view, then pulsing rings around the front and top centre pieces:
 * the pieces that name the sides. The rings lie in the faces' planes (ellipses in perspective). With
 * animations off the cube is in the hold at once and nothing pulses.
 */
@Composable
fun HoldingCube(cube: Cube, modifier: Modifier = Modifier, description: String? = null) {
    val still = animationScale() == 0f
    val viewState = remember { CubeViewState(if (still) CubeScene.DEFAULT_VIEW else CubeScene.DEFAULT_VIEW * TILT) }
    val reveal = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reveal.value < 1f) {
            viewState.animateTo(CubeScene.DEFAULT_VIEW, TURN_MS)
            reveal.animateTo(1f, tween(REVEAL_MS))
        }
    }
    val pulse = if (still) {
        1f
    } else {
        rememberInfiniteTransition().animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        ).value
    }
    Box(modifier.semantics { if (description != null) contentDescription = description }) {
        Cube3D(
            colors = remember(cube) { cube.toList().map(StickerColors::of) },
            viewState = viewState,
            draggable = false,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(Modifier.fillMaxSize()) {
            val a = reveal.value * pulse
            if (a > 0f) {
                centreRing(V3(0f, 0f, 1f), V3(1f, 0f, 0f), V3(0f, 1f, 0f), a)
                centreRing(V3(0f, 1f, 0f), V3(1f, 0f, 0f), V3(0f, 0f, -1f), a)
            }
        }
    }
}

/** A ring on the face with [normal] around its centre piece, in the face's plane. */
private fun DrawScope.centreRing(normal: V3, u: V3, v: V3, alpha: Float) {
    val centre = normal * 1.53f
    val points = (0 until 40).map { k ->
        val t = 2 * PI * k / 40
        val p = centre + u * (0.62f * cos(t).toFloat()) + v * (0.62f * sin(t).toFloat())
        CubeScene.projectPoint(p, CubeScene.DEFAULT_VIEW, size.width, size.height).let { (x, y) -> Offset(x, y) }
    }
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (p in points.drop(1)) lineTo(p.x, p.y)
        close()
    }
    val w = minOf(size.width, size.height)
    drawPath(path, RING_OUTER.copy(alpha = alpha), style = Stroke(width = w * 0.026f, join = StrokeJoin.Round))
    drawPath(path, RING_INNER.copy(alpha = alpha), style = Stroke(width = w * 0.013f, join = StrokeJoin.Round))
}
