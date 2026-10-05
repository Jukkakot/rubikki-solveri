package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.ArgbImage
import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.cube.scan.Tilt
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.common.BackButton
import fi.jukkakot.rubikkisolveri.ui.common.FitColumn
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconToggle
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.holdFor
import fi.jukkakot.rubikkisolveri.ui.elapsedMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The faces [FaceFinder] found in one camera picture of [width]×[height] pixels (for drawing them). */
class FoundFaces(val faces: List<FaceReading>, val width: Int, val height: Int)

/**
 * The video scan: the camera's pictures go through [FaceFinder] off the main thread (in the browser
 * Dispatchers.Default is the page's one thread; its time per frame is logged), the faces found to
 * [VideoScanContent].
 */
@Composable
fun VideoScanScreen(onBack: () -> Unit, onManual: () -> Unit, onResult: (ScanOutcome) -> Unit) {
    CameraPermissionGate(alternative = stringResource(Res.string.scan_manual) to onManual) {
        val images = remember { MutableSharedFlow<ArgbImage>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }
        val found = remember { MutableSharedFlow<FoundFaces>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }
        var torch by remember { mutableStateOf(false) }
        var torchAvailable by remember { mutableStateOf(false) }
        var lockExposure by remember { mutableStateOf(false) }
        var cameraFailed by remember { mutableStateOf(false) }
        LaunchedEffect(images) {
            withContext(Dispatchers.Default) {
                var count = 0
                var millis = 0L
                images.collect { image ->
                    val start = elapsedMillis()
                    val faces = FaceFinder.find(image.argb, image.width, image.height).faces.map(FaceReading::of)
                    millis += elapsedMillis() - start
                    if (++count % TIMING_FRAMES == 0) {
                        AppLog.info(Evt.SCAN_VIDEO, null, "finderMs" to millis / TIMING_FRAMES, "size" to "${image.width}x${image.height}")
                        millis = 0
                    }
                    found.emit(FoundFaces(faces, image.width, image.height))
                }
            }
        }
        VideoScanContent(
            found = found,
            torch = torch,
            onTorch = { torch = it },
            torchAvailable = torchAvailable,
            onBack = onBack,
            onManual = onManual,
            onResult = onResult,
            cameraFailed = cameraFailed,
            onLockExposure = { lockExposure = it },
        ) { modifier ->
            CameraPreview(
                torch = torch,
                onSamples = {},
                onError = { cameraFailed = true },
                modifier = modifier,
                lockExposure = lockExposure,
                onTorchAvailable = { torchAvailable = it },
                onImage = { images.tryEmit(it) },
            )
        }
    }
}

/**
 * The video scan without the camera: [found] are the faces of each picture, [preview] draws the
 * camera. The camera picture is large, the faces found outlined on it; the progress cube sits in its
 * top corner with the turning hint's arrow on it, the hint's line below the picture. Exposure is
 * locked once the first face is found. All recognised and possible for half a second → [onResult];
 * "check now" hands over what is known. [clock] is the time in milliseconds (tests pass their own).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScanContent(
    found: Flow<FoundFaces>,
    torch: Boolean,
    onTorch: (Boolean) -> Unit,
    onBack: () -> Unit,
    onManual: () -> Unit,
    onResult: (ScanOutcome) -> Unit,
    cameraFailed: Boolean = false,
    torchAvailable: Boolean = true,
    onLockExposure: (Boolean) -> Unit = {},
    clock: () -> Long = ::elapsedMillis,
    preview: @Composable (Modifier) -> Unit,
) {
    val scan = remember { VideoScan() }
    var state by remember { mutableStateOf(VideoScanState.EMPTY) }
    var picture by remember { mutableStateOf<FoundFaces?>(null) }
    var seenFace by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    fun finish(outcome: ScanOutcome) {
        if (done) return
        done = true
        AppLog.info(
            Evt.SCAN_DONE, null,
            "way" to "video",
            "valid" to outcome.validity.isValid,
            "uncertain" to outcome.uncertain.size,
            "cube" to outcome.editor.encode(),
            "rotations" to Face.entries.joinToString("") { "${it.name}${outcome.rotations[it] ?: 0}" },
        )
        onResult(outcome)
    }

    LaunchedEffect(scan) {
        var lastBuzz = 0L
        found.collect { f ->
            if (done) return@collect
            val now = clock()
            state = scan.onFrame(f.faces, now)
            picture = f
            if (f.faces.isNotEmpty()) seenFace = true
            if (state.newStickers > 0 && now - lastBuzz >= BUZZ_MILLIS) {
                lastBuzz = now
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            if (state.finished) finish(scan.outcome())
        }
    }
    LaunchedEffect(seenFace) { onLockExposure(seenFace) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.video_title)) },
                navigationIcon = { Box(Modifier.padding(horizontal = 8.dp)) { BackButton(onBack) } },
                actions = {
                    if (torchAvailable) {
                        RoundIconToggle(checked = torch, onCheckedChange = onTorch, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Icon(painterResource(Res.drawable.ic_torch), contentDescription = stringResource(Res.string.scan_torch))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onManual, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(stringResource(Res.string.scan_manual_short)) }
                Button(
                    onClick = { finish(scan.outcome()) },
                    enabled = state.recognised > 0,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                ) { Text(stringResource(Res.string.video_check)) }
            }
        },
    ) { padding ->
        FitColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Box(Modifier.fitSlot().fillMaxWidth().clip(MaterialTheme.shapes.extraLarge).background(Color.Black)) {
                if (cameraFailed) {
                    Text(stringResource(Res.string.scan_camera_error), color = Color.White, modifier = Modifier.align(Alignment.Center))
                } else {
                    preview(Modifier.fillMaxSize())
                }
                picture?.let { FaceOutlines(it, Modifier.fillMaxSize()) }
                ProgressCube(state, Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(statusText(state), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(Res.string.video_count, state.recognised),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun statusText(state: VideoScanState): String {
    val hint = state.hint
    return when {
        state.complete -> stringResource(Res.string.video_status_done)
        state.contradictions.isNotEmpty() -> stringResource(Res.string.video_status_marked)
        hint != null -> stringResource(
            when (hint) {
                Tilt.UP -> Res.string.video_hint_up
                Tilt.DOWN -> Res.string.video_hint_down
                Tilt.LEFT -> Res.string.video_hint_left
                Tilt.RIGHT -> Res.string.video_hint_right
            },
        )
        state.found.isEmpty() -> stringResource(Res.string.video_status_find)
        else -> stringResource(Res.string.video_status_keep)
    }
}

/** The faces found in the latest picture, outlined; the picture fills the box (both are the visible part). */
@Composable
private fun FaceOutlines(found: FoundFaces, modifier: Modifier) {
    Canvas(modifier) {
        val sx = size.width / found.width
        val sy = size.height / found.height
        val path = Path()
        for (face in found.faces) {
            val corners = face.outline.map { Offset((it.x * sx).toFloat(), (it.y * sy).toFloat()) }
            path.reset()
            path.moveTo(corners[0].x, corners[0].y)
            for (c in corners.drop(1)) path.lineTo(c.x, c.y)
            path.close()
            drawPath(path, Color.Black.copy(alpha = 0.5f), style = Stroke(6.dp.toPx(), join = StrokeJoin.Round))
            drawPath(path, OUTLINE, style = Stroke(3.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}

/**
 * The cube as recognised so far, small on a dark rounded backing: grey until a sticker is
 * recognised, contradictions marked, turned to the real cube's pose once that is known, and the
 * turning hint's arrow on it.
 */
@Composable
private fun ProgressCube(state: VideoScanState, modifier: Modifier) {
    val view = remember { CubeViewState() }
    LaunchedEffect(state.pose) { state.pose?.let { view.animateTo(CubeScene.viewFor(holdFor(it))) } }
    Box(modifier.size(PROGRESS_SIZE.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(6.dp)) {
        Cube3D(
            colors = state.stickers.map { StickerColors.of(it) },
            modifier = Modifier.fillMaxSize(),
            viewState = view,
            marked = state.contradictions,
            draggable = false,
            description = stringResource(Res.string.video_progress, state.recognised),
        )
        state.hint?.let { TiltArrow(it, Modifier.fillMaxSize()) }
    }
}

/** A straight arrow across the progress cube, the way the real cube's front should move. */
@Composable
private fun TiltArrow(tilt: Tilt, modifier: Modifier) {
    Canvas(modifier) {
        val (dx, dy) = when (tilt) {
            Tilt.UP -> 0f to -1f
            Tilt.DOWN -> 0f to 1f
            Tilt.LEFT -> -1f to 0f
            Tilt.RIGHT -> 1f to 0f
        }
        val half = size.minDimension * 0.34f
        val c = center
        val start = Offset(c.x - dx * half, c.y - dy * half)
        val end = Offset(c.x + dx * half, c.y + dy * half)
        val head = size.minDimension * 0.16f
        val width = size.minDimension * 0.06f
        val shaftEnd = Offset(end.x - dx * head * 0.8f, end.y - dy * head * 0.8f)
        drawLine(StickerColors.ARROW_OUTLINE, start, shaftEnd, width * 1.7f, cap = StrokeCap.Round)
        drawLine(StickerColors.ARROW, start, shaftEnd, width, cap = StrokeCap.Round)
        val tip = Path().apply {
            moveTo(end.x + dx * head * 0.2f, end.y + dy * head * 0.2f)
            lineTo(end.x - dx * head - dy * head * 0.6f, end.y - dy * head + dx * head * 0.6f)
            lineTo(end.x - dx * head + dy * head * 0.6f, end.y - dy * head - dx * head * 0.6f)
            close()
        }
        drawPath(tip, StickerColors.ARROW_OUTLINE, style = Stroke(width * 0.6f, join = StrokeJoin.Round))
        drawPath(tip, StickerColors.ARROW)
    }
}

/** Fixed, not a theme colour: drawn on the camera image in both themes. */
private val OUTLINE = Color(0xFF4DD0E1)

private const val PROGRESS_SIZE = 116

/** Frames between two timing lines in the log. */
private const val TIMING_FRAMES = 50

/** Shortest gap between two vibrations for new stickers. */
private const val BUZZ_MILLIS = 300L
