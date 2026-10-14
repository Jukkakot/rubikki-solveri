package fi.jukkakot.rubikkisolveri.ui.guide

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.V3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Skin and outline of the drawn hands: one warm tone that reads on light and dark backgrounds. */
private val SKIN = Color(0xFFF1C09A)
private val SKIN_SHADE = Color(0xFFE0A47C)
private val NAIL = Color(0xFFFBE2CF)
private val OUTLINE = Color(0xFF7A4A2E)

/** The cube takes this share of the picture's width; the hands use the rest. */
private const val CUBE_SHARE = 0.9f

/**
 * [cube] in the guide's holding view, held by two drawn hands: the palms on the left and right
 * sides (the left one behind the cube), the thumbs on the front face. The hands are drawn in the
 * cube's own 3D space, so they sit right on it whatever the picture's size.
 */
@Composable
fun HoldingCube(cube: Cube, modifier: Modifier = Modifier, description: String? = null) {
    BoxWithConstraints(modifier.clipToBounds().semantics { if (description != null) contentDescription = description }) {
        val inset = maxWidth * ((1f - CUBE_SHARE) / 2f)
        val view = CubeScene.DEFAULT_VIEW
        // Behind the cube: the left palm.
        Canvas(Modifier.fillMaxSize()) { inCube(inset.toPx()) { w, h -> hand(left = true, front = false, w, h) } }
        Box(Modifier.fillMaxSize().padding(horizontal = inset)) {
            Cube3D(
                colors = remember(cube) { cube.toList().map(StickerColors::of) },
                viewState = remember { CubeViewState(view) },
                draggable = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // In front: the right palm and both thumbs.
        Canvas(Modifier.fillMaxSize()) {
            inCube(inset.toPx()) { w, h ->
                hand(left = false, front = false, w, h)
                hand(left = true, front = true, w, h)
                hand(left = false, front = true, w, h)
            }
        }
    }
}

/** Draws with the cube's projection: [block] gets the cube box's size, the canvas moved to it. */
private fun DrawScope.inCube(inset: Float, block: DrawScope.(Float, Float) -> Unit) {
    val w = size.width - 2 * inset
    if (w <= 0f) return
    drawContext.transform.translate(inset, 0f)
    block(w, size.height)
    drawContext.transform.translate(-inset, 0f)
}

/**
 * One hand: with [front] false the palm and forearm on the side face (x = ±1), else the thumb on the
 * front face (z = +1). Cube coordinates: x right, y up, z towards the user, the cube from −1.5 to 1.5.
 */
private fun DrawScope.hand(left: Boolean, front: Boolean, w: Float, h: Float) {
    val s = if (left) -1f else 1f
    fun at(p: V3): Pair<Float, Float> = CubeScene.projectPoint(p, CubeScene.DEFAULT_VIEW, w, h)
    val stroke = Stroke(width = w * 0.012f, join = StrokeJoin.Round)
    if (!front) {
        // The palm lies against the side face, a little off it.
        val x = s * 1.62f
        fun side(u: Float, v: Float) = V3(x, v, u)
        val palm = roundedRect(-1.25f, -1.75f, 1.35f, 0.75f, 0.55f).map { (u, v) -> side(u, v) }
        shape(palm.map(::at), SKIN, stroke)
        // Fingers reach to the back: their tips show past the back edge.
        for (i in 0 until 4) {
            val v = 0.55f - i * 0.5f
            val finger = capsule(-1.0f, v, -1.95f, v - 0.05f, 0.21f).map { (u, vv) -> side(u, vv) }
            shape(finger.map(::at), SKIN, stroke)
        }
    } else {
        // Below the cube's front corner the heel of the hand and the wrist, going down and out; from the
        // heel the thumb lies on the front face towards the middle, a little below centre.
        fun plane(z: Float) = { u: Float, v: Float -> V3(s * u, v, z) }
        val low = plane(1.1f)
        val forearm = listOf(low(1.1f, -1.9f), low(2.15f, -1.9f), low(2.7f, -5f), low(1.3f, -5f))
        shape(forearm.map(::at), SKIN_SHADE, stroke)
        shape(roundedRect(0.95f, -2.45f, 2.2f, -1.25f, 0.5f).map { (u, v) -> at(low(u, v)) }, SKIN, stroke)
        val face = plane(1.58f)
        val thumb = capsule(1.55f, -1.6f, 0.6f, -0.45f, 0.28f).map { (u, v) -> face(u, v) }
        shape(thumb.map(::at), SKIN, stroke)
        val nail = capsule(0.9f, -0.62f, 0.66f, -0.47f, 0.14f).map { (u, v) -> face(u, v) }
        shape(nail.map(::at), NAIL, Stroke(width = w * 0.006f))
    }
}

private fun DrawScope.shape(points: List<Pair<Float, Float>>, fill: Color, stroke: Stroke) {
    val path = Path().apply {
        moveTo(points[0].first, points[0].second)
        for (p in points.drop(1)) lineTo(p.first, p.second)
        close()
    }
    drawPath(path, fill)
    drawPath(path, OUTLINE, style = stroke)
}

/** Outline of a rounded rectangle in a plane's (u, v) coordinates. */
private fun roundedRect(u0: Float, v0: Float, u1: Float, v1: Float, r: Float): List<Pair<Float, Float>> = buildList {
    val corners = listOf(Triple(u1 - r, v1 - r, 0.0), Triple(u0 + r, v1 - r, PI / 2), Triple(u0 + r, v0 + r, PI), Triple(u1 - r, v0 + r, 3 * PI / 2))
    for ((cu, cv, a0) in corners) {
        for (k in 0..6) {
            val a = a0 + k * (PI / 2) / 6
            add(cu + r * cos(a).toFloat() to cv + r * sin(a).toFloat())
        }
    }
}

/** Outline of a capsule from (ua, va) to (ub, vb) with radius [r], in a plane's (u, v) coordinates. */
private fun capsule(ua: Float, va: Float, ub: Float, vb: Float, r: Float): List<Pair<Float, Float>> = buildList {
    val dir = kotlin.math.atan2((vb - va).toDouble(), (ub - ua).toDouble())
    for (k in 0..10) {
        val a = dir + PI / 2 + k * PI / 10
        add(ua + r * cos(a).toFloat() to va + r * sin(a).toFloat())
    }
    for (k in 0..10) {
        val a = dir - PI / 2 + k * PI / 10
        add(ub + r * cos(a).toFloat() to vb + r * sin(a).toFloat())
    }
}
