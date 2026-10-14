package fi.jukkakot.rubikkisolveri.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Skin and outline of the drawn hands: one warm tone that reads on light and dark backgrounds. */
private val SKIN = Color(0xFFF0C9A6)
private val OUTLINE = Color(0xFF14151A)
private val RING_INNER = Color(0xFFFFFFFF)

/** The cube's box takes this share of the picture's width and leaves this share of its height below for the arms. */
private const val CUBE_SHARE = 0.86f
private const val ARMS_BELOW = 0.2f

/** The tilted pose the cube turns from, and how long the turn and the hands' fade take. */
private val TILT: Quat = Quat.axisAngle(V3(0.6f, 1f, 0.35f).normalized(), (150 * PI / 180).toFloat())
private const val TURN_MS = 1_200
private const val REVEAL_MS = 300

/**
 * [cube] turning into the guide's holding view, then held by two drawn hands as the user sees their own:
 * forearms from the bottom of the picture, palms at the cube's sides (behind it), thumbs on the front.
 * Pulsing rings mark the front and top centre pieces, the ones that name the sides. The hands and rings
 * are drawn in the cube's own 3D projection, so they sit right on it whatever the picture's size.
 * With animations off the cube is in the hold at once and nothing pulses.
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
    BoxWithConstraints(modifier.clipToBounds().semantics { if (description != null) contentDescription = description }) {
        val inset = maxWidth * ((1f - CUBE_SHARE) / 2f)
        val below = maxHeight * ARMS_BELOW
        val a = reveal.value
        // Behind the cube: the arms and palms.
        Canvas(Modifier.fillMaxSize()) { inCube(inset.toPx(), below.toPx()) { w, h -> if (a > 0f) arms(w, h, a) } }
        Box(Modifier.fillMaxSize().padding(start = inset, end = inset, bottom = below)) {
            Cube3D(
                colors = remember(cube) { cube.toList().map(StickerColors::of) },
                viewState = viewState,
                draggable = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // In front: the thumbs and the centre rings.
        Canvas(Modifier.fillMaxSize()) {
            inCube(inset.toPx(), below.toPx()) { w, h ->
                if (a > 0f) {
                    thumbs(w, h, a)
                    centreRing(w, h, V3(0f, 0f, 1f), V3(1f, 0f, 0f), V3(0f, 1f, 0f), a * pulse)
                    centreRing(w, h, V3(0f, 1f, 0f), V3(1f, 0f, 0f), V3(0f, 0f, -1f), a * pulse)
                }
            }
        }
    }
}

/** Draws in the cube box's coordinates: [block] gets the box's size, the canvas moved to it. */
private fun DrawScope.inCube(inset: Float, below: Float, block: DrawScope.(Float, Float) -> Unit) {
    val w = size.width - 2 * inset
    val h = size.height - below
    if (w <= 0f || h <= 0f) return
    drawContext.transform.translate(inset, 0f)
    block(w, h)
    drawContext.transform.translate(-inset, 0f)
}

private fun DrawScope.at(p: V3, w: Float, h: Float): Offset =
    CubeScene.projectPoint(p, CubeScene.DEFAULT_VIEW, w, h).let { (x, y) -> Offset(x, y) }

/** The forearms from below the picture's bottom corners up to the palms at the cube's sides. */
private fun DrawScope.arms(w: Float, h: Float, alpha: Float) {
    val stroke = Stroke(width = w * 0.012f, join = StrokeJoin.Round)
    for (s in listOf(-1f, 1f)) {
        val palm = at(V3(s * 1.75f, -0.7f, 0.2f), w, h)
        val elbow = Offset(if (s < 0) -w * 0.12f else w * 1.12f, size.height + w * 0.15f)
        // The forearm fades out towards the picture's bottom edge instead of being cut there.
        val fade = { c: Color -> Brush.verticalGradient(0f to c, 0.75f to c, 1f to c.copy(alpha = 0f), startY = 0f, endY = size.height) }
        val arm = path(capsule(elbow, palm, w * 0.1f))
        drawPath(arm, fade(SKIN), alpha = alpha)
        drawPath(arm, fade(OUTLINE), alpha = alpha, style = stroke)
        val hand = Offset(palm.x + s * w * 0.02f, palm.y)
        shape(capsule(hand, Offset(hand.x, hand.y - w * 0.04f), w * 0.12f), SKIN, stroke, alpha)
    }
}

/** The thumbs on the front face, from the lower side edge in towards the centre. */
private fun DrawScope.thumbs(w: Float, h: Float, alpha: Float) {
    val stroke = Stroke(width = w * 0.012f, join = StrokeJoin.Round)
    for (s in listOf(-1f, 1f)) {
        val outline = capsuleUV(1.75f, -1.35f, 0.75f, -0.65f, 0.26f).map { (u, v) -> at(V3(s * u, v, 1.55f), w, h) }
        shape(outline, SKIN, stroke, alpha)
    }
}

/** A ring on the face with [normal] around its centre piece, in the face's plane (an ellipse in perspective). */
private fun DrawScope.centreRing(w: Float, h: Float, normal: V3, u: V3, v: V3, alpha: Float) {
    val centre = normal * 1.53f
    val ring = (0 until 40).map { k ->
        val t = 2 * PI * k / 40
        at(centre + u * (0.62f * cos(t).toFloat()) + v * (0.62f * sin(t).toFloat()), w, h)
    }
    val path = path(ring)
    drawPath(path, OUTLINE.copy(alpha = alpha), style = Stroke(width = w * 0.03f, join = StrokeJoin.Round))
    drawPath(path, RING_INNER.copy(alpha = alpha), style = Stroke(width = w * 0.015f, join = StrokeJoin.Round))
}

private fun path(points: List<Offset>) = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (p in points.drop(1)) lineTo(p.x, p.y)
    close()
}

private fun DrawScope.shape(points: List<Offset>, fill: Color, stroke: Stroke, alpha: Float) {
    val p = path(points)
    drawPath(p, fill.copy(alpha = alpha))
    drawPath(p, OUTLINE.copy(alpha = alpha), style = stroke)
}

/** Outline of a capsule from [a] to [b] with radius [r] on screen. */
private fun capsule(a: Offset, b: Offset, r: Float): List<Offset> =
    capsuleUV(a.x, a.y, b.x, b.y, r).map { (x, y) -> Offset(x, y) }

/** Outline of a capsule from (ua, va) to (ub, vb) with radius [r], in a plane's (u, v) coordinates. */
private fun capsuleUV(ua: Float, va: Float, ub: Float, vb: Float, r: Float): List<Pair<Float, Float>> = buildList {
    val dir = atan2((vb - va).toDouble(), (ub - ua).toDouble())
    for (k in 0..10) {
        val t = dir + PI / 2 + k * PI / 10
        add(ua + r * cos(t).toFloat() to va + r * sin(t).toFloat())
    }
    for (k in 0..10) {
        val t = dir - PI / 2 + k * PI / 10
        add(ub + r * cos(t).toFloat() to vb + r * sin(t).toFloat())
    }
}
