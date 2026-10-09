package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.scan.ScanCorners
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.animationScale
import fi.jukkakot.rubikkisolveri.ui.common.colorName
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import org.jetbrains.compose.resources.stringResource

/** How a corner shows in the row: still to show, read (dimmed with a tick), or the next one (pulsing). */
enum class CornerLook { OPEN, READ, NEXT }

/** Each corner's look for [state] in [ScanCorners.ROW] order; every one read once [done] (`scan-next-view` design 1). */
fun cornerLooks(state: VideoScanState, done: Boolean = false): List<Pair<Corner, CornerLook>> = ScanCorners.ROW.map { c ->
    c to when {
        done || state.complete || c in state.readCorners -> CornerLook.READ
        c == state.nextCorner -> CornerLook.NEXT
        else -> CornerLook.OPEN
    }
}

/**
 * The corner row (`scan-next-view` design 2): the cube's eight corners as small isometric pictures in
 * their centres' colours on a dark pill; read ones dimmed and scaled down with a tick, in place; the
 * next one pulsing (larger and still under reduced motion). Its description says how many are read
 * and which is next.
 */
@Composable
fun CornerRow(looks: List<Pair<Corner, CornerLook>>, modifier: Modifier = Modifier) {
    val read = looks.count { it.second == CornerLook.READ }
    val next = looks.firstOrNull { it.second == CornerLook.NEXT }?.first
    val counted = stringResource(Res.string.video_corners, read, looks.size)
    val description = if (next == null) {
        counted
    } else {
        val names = ScanCorners.colors(next).map { stringResource(colorName(it)) }.joinToString("–")
        "$counted. ${stringResource(Res.string.video_corner_next, names)}"
    }
    val still = animationScale() == 0f
    val pulse = if (still) {
        null
    } else {
        rememberInfiniteTransition(label = "corner-pulse").animateFloat(
            1f,
            PULSE_SCALE,
            infiniteRepeatable(tween(PULSE_MILLIS / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "corner-pulse-scale",
        )
    }
    Row(
        modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { contentDescription = description }
            .testTag(VIDEO_CORNERS_TAG),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((corner, look) in looks) {
            val colors = ScanCorners.colors(corner)
            Canvas(
                Modifier.size(CHIP).graphicsLayer {
                    val s = when (look) {
                        CornerLook.READ -> READ_SCALE
                        CornerLook.NEXT -> pulse?.value ?: PULSE_SCALE
                        CornerLook.OPEN -> 1f
                    }
                    scaleX = s
                    scaleY = s
                },
            ) { drawCorner(colors, look == CornerLook.READ) }
        }
    }
}

/** An isometric corner in a 30-unit box: the top rhombus, the left and the right one; a tick over it when [read]. */
private fun DrawScope.drawCorner(colors: List<CubeColor>, read: Boolean) {
    val k = size.minDimension / 30f
    fun p(x: Float, y: Float) = Offset(x * k, y * k)
    val alpha = if (read) READ_ALPHA else 1f
    // ScanCorners.colors: top, right, left.
    val rhombi = listOf(
        colors[0] to listOf(p(15f, 2f), p(27f, 9f), p(15f, 16f), p(3f, 9f)),
        colors[2] to listOf(p(3f, 9f), p(15f, 16f), p(15f, 29f), p(3f, 22f)),
        colors[1] to listOf(p(27f, 9f), p(15f, 16f), p(15f, 29f), p(27f, 22f)),
    )
    for ((color, points) in rhombi) {
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (q in points.drop(1)) lineTo(q.x, q.y)
            close()
        }
        drawPath(path, StickerColors.of(color).copy(alpha = alpha))
        drawPath(path, StickerColors.PLASTIC.copy(alpha = alpha), style = Stroke(1.2f * k, join = StrokeJoin.Round))
    }
    if (read) {
        val tick = Path().apply {
            moveTo(8f * k, 16f * k)
            lineTo(13f * k, 21f * k)
            lineTo(22f * k, 10f * k)
        }
        drawPath(tick, StickerColors.PLASTIC.copy(alpha = 0.6f), style = Stroke(5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(tick, Color.White, style = Stroke(3f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A corner picture's size: eight with their gaps fit a 360 dp screen. */
private val CHIP = 28.dp

/** A read corner: dimmed and scaled down, in place (the mockup "Kulmarivi", E1). */
private const val READ_ALPHA = 0.25f
private const val READ_SCALE = 0.8f

/** The next corner's pulse: up to this scale and back in [PULSE_MILLIS]. */
private const val PULSE_SCALE = 1.22f
private const val PULSE_MILLIS = 1_100

/** Test tag of the corner row. */
const val VIDEO_CORNERS_TAG = "video-corners"
