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
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.ScanSides
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.animationScale
import fi.jukkakot.rubikkisolveri.ui.common.colorName
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import org.jetbrains.compose.resources.stringResource

/** How a side's ball shows in the row: still to show, done (dimmed with a tick), or the next one (pulsing). */
enum class SideLook { OPEN, DONE, NEXT }

/** Each side's look for [state] in [ScanSides.ROW] order; every one done once [done] (`scan-side-balls`). */
fun sideLooks(state: VideoScanState, done: Boolean = false): List<Pair<Face, SideLook>> = ScanSides.ROW.map { f ->
    f to when {
        done || state.complete || f in state.doneSides -> SideLook.DONE
        f == state.nextSide -> SideLook.NEXT
        else -> SideLook.OPEN
    }
}

/**
 * The side row (`scan-side-balls`): six balls in the sides' centre colours on a dark pill; done ones
 * dimmed and scaled down with a tick, in place; the next one pulsing (larger and still under reduced
 * motion). Its description says how many are done and which is next.
 */
@Composable
fun SideRow(looks: List<Pair<Face, SideLook>>, modifier: Modifier = Modifier) {
    val doneCount = looks.count { it.second == SideLook.DONE }
    val next = looks.firstOrNull { it.second == SideLook.NEXT }?.first
    val counted = stringResource(Res.string.video_sides, doneCount, looks.size)
    val description = if (next == null) {
        counted
    } else {
        "$counted. ${stringResource(Res.string.video_side_next, stringResource(colorName(ScanSides.color(next))))}"
    }
    val still = animationScale() == 0f
    val pulse = if (still) {
        null
    } else {
        rememberInfiniteTransition(label = "side-pulse").animateFloat(
            1f,
            PULSE_SCALE,
            infiniteRepeatable(tween(PULSE_MILLIS / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "side-pulse-scale",
        )
    }
    Row(
        modifier.background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics { contentDescription = description }
            .testTag(VIDEO_SIDES_TAG),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for ((side, look) in looks) {
            Canvas(
                Modifier.size(BALL).graphicsLayer {
                    val s = when (look) {
                        SideLook.DONE -> DONE_SCALE
                        SideLook.NEXT -> pulse?.value ?: PULSE_SCALE
                        SideLook.OPEN -> 1f
                    }
                    scaleX = s
                    scaleY = s
                },
            ) { drawBall(ScanSides.color(side), look == SideLook.DONE) }
        }
    }
}

/** A ball in [color] with a thin dark outline; dimmed with a white, dark-edged tick over it when [done]. */
private fun DrawScope.drawBall(color: CubeColor, done: Boolean) {
    val k = size.minDimension / 22f
    val alpha = if (done) DONE_ALPHA else 1f
    val r = size.minDimension / 2f - 1f * k
    drawCircle(StickerColors.of(color).copy(alpha = alpha), r)
    drawCircle(StickerColors.PLASTIC.copy(alpha = alpha), r, style = Stroke(1.2f * k))
    if (done) {
        val tick = Path().apply {
            moveTo(6f * k, 11.5f * k)
            lineTo(9.5f * k, 15f * k)
            lineTo(16f * k, 7.5f * k)
        }
        drawPath(tick, StickerColors.PLASTIC.copy(alpha = 0.7f), style = Stroke(4.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(tick, Color.White, style = Stroke(2.5f * k, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A ball's size: six with their gaps leave room for the pulse on a 360 dp screen. */
private val BALL = 22.dp

/** A done side's ball: dimmed and scaled down, in place. */
private const val DONE_ALPHA = 0.25f
private const val DONE_SCALE = 0.8f

/** The next side's pulse: up to this scale and back in [PULSE_MILLIS]. */
private const val PULSE_SCALE = 1.22f
private const val PULSE_MILLIS = 1_100

/** Test tag of the side row. */
const val VIDEO_SIDES_TAG = "video-sides"
