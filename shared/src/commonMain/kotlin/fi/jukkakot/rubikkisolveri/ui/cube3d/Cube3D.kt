package fi.jukkakot.rubikkisolveri.ui.cube3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Move
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI

/** The curved direction arrow of [move], with a dark outline and a head at its end. */
private fun DrawScope.drawArrow(move: Move, view: Quat, path: Path, mirror: Boolean) {
    val points = CubeScene.arrow(move, view).map { CubeScene.projectPoint(it, view, size.width, size.height, mirror) }
    // Sized by the cube on screen.
    val cubeSize = CubeScene.fit(size.width, size.height, mirror).scale
    val width = cubeSize * 0.025f
    val headLength = cubeSize * 0.057f
    val (ex, ey) = points.last()
    val (px, py) = points[points.size - 3]
    val dx = ex - px
    val dy = ey - py
    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1e-3f)
    val ux = dx / len
    val uy = dy / len
    // The shaft stops where the head begins.
    val shaftEnd = points.indexOfLast { (x, y) -> (x - ex) * ux + (y - ey) * uy < -headLength * 0.6f }.coerceAtLeast(1)
    path.reset()
    path.moveTo(points[0].first, points[0].second)
    for (i in 1..shaftEnd) path.lineTo(points[i].first, points[i].second)
    val round = androidx.compose.ui.graphics.StrokeCap.Round
    drawPath(path, StickerColors.ARROW_OUTLINE, style = Stroke(width * 1.6f, cap = round))
    drawPath(path, StickerColors.ARROW, style = Stroke(width, cap = round))
    val head = Path().apply {
        moveTo(ex + ux * headLength * 0.25f, ey + uy * headLength * 0.25f)
        lineTo(ex - ux * headLength + -uy * headLength * 0.55f, ey - uy * headLength + ux * headLength * 0.55f)
        lineTo(ex - ux * headLength - -uy * headLength * 0.55f, ey - uy * headLength - ux * headLength * 0.55f)
        close()
    }
    drawPath(head, StickerColors.ARROW_OUTLINE, style = Stroke(width * 0.6f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    drawPath(head, StickerColors.ARROW)
}

/** Real-cube sticker colours; not themed, they must match the cube in the user's hand. */
object StickerColors {
    val UNKNOWN = Color(0xFF8A8A8A)
    val PLASTIC = Color(0xFF141414)
    val MARK = Color(0xFFFF1744)
    val ARROW = Color(0xFFFFB300)
    val ARROW_OUTLINE = Color(0xFF1A1A1A)

    /** The mirror's glass: a light blue-grey in both themes, so it reads as glass. */
    val GLASS = Color(0xFFBFCDD8)
    private val DIM = Color(0xFF808080)

    /** A sticker outside the highlighted layer: mostly grey, a hint of its colour left. */
    fun dim(color: Color): Color = lerp(color, DIM, 0.6f)

    fun of(color: CubeColor?): Color = when (color) {
        CubeColor.WHITE -> Color(0xFFF4F4F4)
        CubeColor.YELLOW -> Color(0xFFFFD500)
        CubeColor.GREEN -> Color(0xFF009E60)
        CubeColor.BLUE -> Color(0xFF0051BA)
        CubeColor.RED -> Color(0xFFC41E3A)
        CubeColor.ORANGE -> Color(0xFFFF5800)
        null -> UNKNOWN
    }
}

/** Which way the 3D cube is seen; dragging changes it, screens can animate it. */
@Stable
class CubeViewState(initial: Quat = CubeScene.DEFAULT_VIEW) {
    var rotation: Quat by mutableStateOf(initial)

    /** Turns the view by a drag of ([dx], [dy]) pixels on a view [width] wide. */
    fun drag(dx: Float, dy: Float, width: Float) {
        val k = (PI / width).toFloat()
        val yaw = Quat.axisAngle(V3(0f, 1f, 0f), dx * k)
        val pitch = Quat.axisAngle(V3(1f, 0f, 0f), dy * k)
        rotation = (pitch * yaw * rotation).normalized()
    }

    /** Whether the view is within 2° of [target]. */
    fun isAt(target: Quat): Boolean {
        val r = rotation
        val dot = kotlin.math.abs(r.w * target.w + r.x * target.x + r.y * target.y + r.z * target.z).coerceAtMost(1f)
        return 2 * kotlin.math.acos(dot) < (2 * PI / 180).toFloat()
    }

    suspend fun animateTo(target: Quat, millis: Int = 450) {
        val from = rotation
        val t = Animatable(0f)
        t.animateTo(1f, tween(millis)) { rotation = from.slerp(target, value) }
    }
}

/**
 * The 3D cube. [colors] are the 54 sticker colours (URFDLB order) of the state before [move];
 * [progress] 0..1 turns the move's layers. [marked] stickers get a strong outline. [onTap] gets the
 * tapped sticker's index. A cube that is not [draggable] leaves drags to its parent (e.g. a pager).
 * [mirror] adds a framed mirror behind the cube, fixed on screen, showing the cube's reflection.
 */
@Composable
fun Cube3D(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    move: Move? = null,
    progress: Float = 0f,
    viewState: CubeViewState = remember { CubeViewState() },
    marked: Set<Int> = emptySet(),
    onTap: ((Int) -> Unit)? = null,
    description: String? = null,
    highlight: Move? = null,
    arrow: Move? = null,
    draggable: Boolean = true,
    mirror: Boolean = false,
) {
    var size by remember { mutableStateOf(Size.Zero) }
    val path = remember { Path() }
    val glassPath = remember { Path() }
    // The frame contrasts with the light glass: secondary on a light background, its dark container on a dark one.
    val scheme = MaterialTheme.colorScheme
    val frameColor = if (scheme.background.luminance() < 0.5f) scheme.secondaryContainer else scheme.secondary
    Canvas(
        modifier
            .semantics { if (description != null) contentDescription = description }
            .pointerInput(viewState, draggable) {
                if (draggable) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        viewState.drag(drag.x, drag.y, this.size.width.toFloat())
                    }
                }
            }
            .pointerInput(onTap, viewState) {
                if (onTap != null) {
                    detectTapGestures { offset ->
                        val projected = CubeScene.project(
                            CubeScene.quads(null, 0f), viewState.rotation,
                            this.size.width.toFloat(), this.size.height.toFloat(), mirror = mirror,
                        )
                        CubeScene.hitTest(projected, offset.x, offset.y)?.let(onTap)
                    }
                }
            },
    ) {
        size = this.size
        val quads = CubeScene.quads(move, progress)
        val view = viewState.rotation
        val showArrow = arrow != null
        if (mirror) {
            // The mirror is always behind the cube: frame, glass and the reflection clipped to the glass first.
            val (fx, fy) = CubeScene.projectMirror(frame = true, size.width, size.height)
            quadPath(glassPath, fx, fy)
            drawPath(glassPath, frameColor)
            drawPath(glassPath, frameColor, style = Stroke(size.minDimension * 0.02f, join = StrokeJoin.Round))
            val (gx, gy) = CubeScene.projectMirror(frame = false, size.width, size.height)
            quadPath(glassPath, gx, gy)
            drawPath(glassPath, StickerColors.GLASS)
            clipPath(glassPath) {
                val image = CubeScene.project(quads, view, size.width, size.height, highlight, reflected = true)
                // No arrow in the mirror: the main cube's arrow is placed where it can be seen.
                drawQuads(image, colors, marked, path)
            }
        }
        drawQuads(CubeScene.project(quads, view, size.width, size.height, highlight, mirror), colors, marked, path)
        if (showArrow) drawArrow(arrow!!, view, path, mirror)
    }
}

private fun quadPath(path: Path, xs: FloatArray, ys: FloatArray) {
    path.reset()
    path.moveTo(xs[0], ys[0])
    for (i in 1 until 4) path.lineTo(xs[i], ys[i])
    path.close()
}

private fun DrawScope.drawQuads(projected: List<ProjectedQuad>, colors: List<Color>, marked: Set<Int>, path: Path) {
    val outline = Stroke(width = size.minDimension * 0.012f)
    for (q in projected) {
        quadPath(path, q.xs, q.ys)
        val base = when {
            q.sticker < 0 -> StickerColors.PLASTIC
            q.dimmed -> StickerColors.dim(colors[q.sticker])
            else -> colors[q.sticker]
        }
        drawPath(path, lerp(Color.Black, base, q.light))
        if (q.sticker >= 0 && q.sticker in marked) drawPath(path, StickerColors.MARK, style = outline)
    }
}
