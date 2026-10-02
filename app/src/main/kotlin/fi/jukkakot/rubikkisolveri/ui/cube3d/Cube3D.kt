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
import kotlin.math.PI

/** The curved direction arrow of [move], with a dark outline and a head at its end. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(move: Move, view: Quat, path: Path) {
    val points = CubeScene.arrow(move, view).map { CubeScene.projectPoint(it, view, size.width, size.height) }
    val width = size.minDimension * 0.035f
    val headLength = size.minDimension * 0.08f
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

    suspend fun animateTo(target: Quat, millis: Int = 450) {
        val from = rotation
        val t = Animatable(0f)
        t.animateTo(1f, tween(millis)) { rotation = from.slerp(target, value) }
    }
}

/**
 * The 3D cube. [colors] are the 54 sticker colours (URFDLB order) of the state before [move];
 * [progress] 0..1 turns the move's layers. [marked] stickers get a strong outline. [onTap] gets the
 * tapped sticker's index.
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
) {
    var size by remember { mutableStateOf(Size.Zero) }
    val path = remember { Path() }
    Canvas(
        modifier
            .semantics { if (description != null) contentDescription = description }
            .pointerInput(viewState) {
                detectDragGestures { change, drag ->
                    change.consume()
                    viewState.drag(drag.x, drag.y, this.size.width.toFloat())
                }
            }
            .pointerInput(onTap, viewState) {
                if (onTap != null) {
                    detectTapGestures { offset ->
                        val projected = CubeScene.project(
                            CubeScene.quads(null, 0f), viewState.rotation,
                            this.size.width.toFloat(), this.size.height.toFloat(),
                        )
                        CubeScene.hitTest(projected, offset.x, offset.y)?.let(onTap)
                    }
                }
            },
    ) {
        size = this.size
        val projected = CubeScene.project(CubeScene.quads(move, progress), viewState.rotation, size.width, size.height, highlight)
        val outline = Stroke(width = size.minDimension * 0.012f)
        for (q in projected) {
            path.reset()
            path.moveTo(q.xs[0], q.ys[0])
            for (i in 1 until 4) path.lineTo(q.xs[i], q.ys[i])
            path.close()
            val base = when {
                q.sticker < 0 -> StickerColors.PLASTIC
                q.dimmed -> StickerColors.dim(colors[q.sticker])
                else -> colors[q.sticker]
            }
            drawPath(path, lerp(Color.Black, base, q.light))
            if (q.sticker >= 0 && q.sticker in marked) drawPath(path, StickerColors.MARK, style = outline)
        }
        if (arrow != null && move == null) drawArrow(arrow, viewState.rotation, path)
    }
}
