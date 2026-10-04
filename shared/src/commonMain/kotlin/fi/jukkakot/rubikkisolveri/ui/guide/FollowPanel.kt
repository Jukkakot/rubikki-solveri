package fi.jukkakot.rubikkisolveri.ui.guide

import kotlin.math.PI
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.follow.FollowEvent
import fi.jukkakot.rubikkisolveri.cube.follow.FollowTracker
import fi.jukkakot.rubikkisolveri.cube.follow.FrontArrow
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.ui.common.moveDescription
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import kotlinx.coroutines.flow.Flow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Camera mode of the stepper: the camera with the grid, the current move drawn on the front face,
 * a small 3D guide in the corner, and automatic advance when the front shows the move was made.
 */
@Composable
fun FollowPanel(state: StepperState, frames: Flow<List<Rgb>>, preview: @Composable (Modifier) -> Unit) {
    val tracker = remember { FollowTracker() }
    var event by remember { mutableStateOf<FollowEvent>(FollowEvent.Waiting) }
    var live by remember { mutableStateOf<List<CubeColor>?>(null) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state) {
        frames.collect { samples ->
            val move = state.current ?: return@collect
            val e = tracker.onFrame(state.cubeAt(state.index), move, samples)
            live = tracker.live
            if (e != event && e !is FollowEvent.Waiting) AppLog.info(Evt.FOLLOW_EVENT, null, "move" to move.toString(), "event" to e.toString())
            event = e
            if (e is FollowEvent.Done) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                state.done()
                tracker.reset()
                event = FollowEvent.Waiting
            }
        }
    }
    val move = state.current
    Column {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(MaterialTheme.shapes.extraLarge).background(Color.Black)) {
            preview(Modifier.fillMaxSize())
            FollowOverlay(live, move?.let(FrontArrow::of), Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.BottomEnd).fillMaxWidth(0.3f).padding(6.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x99000000))) {
                GuideCube(state)
            }
        }
        val message = when (val e = event) {
            is FollowEvent.HoldFront -> stringResource(Res.string.follow_hold)
            FollowEvent.NotVisible -> stringResource(Res.string.follow_not_visible)
            is FollowEvent.WrongMove -> stringResource(Res.string.follow_wrong, moveDescription(e.fix))
            else -> stringResource(Res.string.follow_waiting)
        }
        Text(
            message,
            style = MaterialTheme.typography.titleMedium,
            color = if (event is FollowEvent.WrongMove) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** The grid (as in the scan), live dots, and the move's arrow on the front face. */
@Composable
private fun FollowOverlay(live: List<CubeColor>?, arrow: FrontArrow?, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val double = stringResource(Res.string.follow_double)
    Canvas(modifier) {
        val side = FrameSampler.GRID_SIZE * size.minDimension
        val left = (size.width - side) / 2
        val top = (size.height - side) / 2
        val cell = side / 3
        drawRect(Color.White, Offset(left, top), Size(side, side), style = Stroke(3.dp.toPx()))
        for (i in 1..2) {
            drawLine(Color.White, Offset(left + cell * i, top), Offset(left + cell * i, top + side), strokeWidth = 2.dp.toPx())
            drawLine(Color.White, Offset(left, top + cell * i), Offset(left + side, top + cell * i), strokeWidth = 2.dp.toPx())
        }
        live?.forEachIndexed { i, color ->
            val c = Offset(left + cell * (i % 3 + 0.5f), top + cell * (i / 3 + 0.5f))
            drawCircle(Color.Black, radius = cell * 0.11f, center = c)
            drawCircle(StickerColors.of(color), radius = cell * 0.08f, center = c)
        }
        if (arrow != null) {
            val end = drawFrontArrow(arrow, left, top, side)
            if (arrow.double) {
                val label = measurer.measure(double, TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = StickerColors.ARROW))
                drawText(label, topLeft = Offset(end.x - label.size.width / 2f, end.y - label.size.height * 1.4f))
            }
        }
    }
}

/** Draws [arrow] in the grid square; returns where its head is. */
private fun DrawScope.drawFrontArrow(arrow: FrontArrow, left: Float, top: Float, side: Float): Offset {
    val width = side * 0.06f
    val points: List<Offset> = when (arrow.kind) {
        FrontArrow.Kind.STRAIGHT -> listOf(
            Offset(left + arrow.x0 * side, top + arrow.y0 * side),
            Offset(left + arrow.x1 * side, top + arrow.y1 * side),
        )
        else -> {
            // Round arrow about the centre; on screen (y down) a growing angle turns clockwise.
            val sign = if (arrow.kind == FrontArrow.Kind.CLOCKWISE) 1 else -1
            val start = if (sign > 0) 200.0 else -20.0
            (0..30).map { k ->
                val a = (start + sign * 250.0 * k / 30) * PI / 180
                Offset(left + side / 2 + (side * 0.33f * cos(a)).toFloat(), top + side / 2 + (side * 0.33f * sin(a)).toFloat())
            }
        }
    }
    val end = points.last()
    val before = points[points.size - 2]
    val dir = (end - before).let { it / it.getDistance().coerceAtLeast(1e-3f) }
    val head = side * 0.12f
    val shaftEnd = end - dir * (head * 0.7f)
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (p in points.drop(1).dropLast(1)) lineTo(p.x, p.y)
        lineTo(shaftEnd.x, shaftEnd.y)
    }
    drawPath(path, StickerColors.ARROW_OUTLINE, style = Stroke(width * 1.6f, cap = StrokeCap.Round))
    drawPath(path, StickerColors.ARROW, style = Stroke(width, cap = StrokeCap.Round))
    val normal = Offset(-dir.y, dir.x)
    val tri = Path().apply {
        moveTo(end.x + dir.x * head * 0.2f, end.y + dir.y * head * 0.2f)
        val base = end - dir * head
        lineTo(base.x + normal.x * head * 0.55f, base.y + normal.y * head * 0.55f)
        lineTo(base.x - normal.x * head * 0.55f, base.y - normal.y * head * 0.55f)
        close()
    }
    drawPath(tri, StickerColors.ARROW_OUTLINE, style = Stroke(width * 0.5f))
    drawPath(tri, StickerColors.ARROW)
    return end
}
