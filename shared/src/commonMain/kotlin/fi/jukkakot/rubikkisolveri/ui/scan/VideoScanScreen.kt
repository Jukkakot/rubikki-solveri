package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.ArgbImage
import fi.jukkakot.rubikkisolveri.cube.scan.CameraSettings
import fi.jukkakot.rubikkisolveri.cube.scan.ExposureControl
import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.cube.scan.Stall
import fi.jukkakot.rubikkisolveri.cube.scan.Tilt
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.common.BackButton
import fi.jukkakot.rubikkisolveri.ui.common.FitColumn
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconToggle
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.elapsedMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** The faces (full and partial) [FaceFinder] found in one camera picture of [width]×[height] pixels, in [finderMs]. */
class FoundFaces(val faces: List<FaceReading>, val width: Int, val height: Int, val finderMs: Long = 0)

/**
 * The video scan: the camera's pictures go through [FaceFinder] off the main thread (in the browser
 * Dispatchers.Default is the page's one thread; its time per frame goes into the log's snapshots),
 * the faces found to [VideoScanContent].
 */
@Composable
fun VideoScanScreen(onBack: () -> Unit, onManual: () -> Unit, onResult: (ScanOutcome) -> Unit) {
    CameraPermissionGate(alternative = stringResource(Res.string.scan_manual) to onManual) {
        val images = remember { MutableSharedFlow<ArgbImage>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }
        val found = remember { MutableSharedFlow<FoundFaces>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }
        var torch by remember { mutableStateOf(false) }
        var torchAvailable by remember { mutableStateOf(false) }
        var exposure by remember { mutableStateOf(CameraSettings.FREE) }
        var maxDarker by remember { mutableStateOf(0) }
        var cameraFailed by remember { mutableStateOf(false) }
        LaunchedEffect(images) {
            withContext(Dispatchers.Default) {
                images.collect { image ->
                    val start = elapsedMillis()
                    val faces = FaceFinder.find(image.argb, image.width, image.height).let { it.faces + it.partial }.map(FaceReading::of)
                    found.emit(FoundFaces(faces, image.width, image.height, elapsedMillis() - start))
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
            maxDarker = maxDarker,
            onExposure = { exposure = it },
        ) { modifier ->
            CameraPreview(
                torch = torch,
                onSamples = {},
                onError = { cameraFailed = true },
                modifier = modifier,
                exposure = exposure,
                onTorchAvailable = { torchAvailable = it },
                onImage = { images.tryEmit(it) },
                onMaxDarker = { maxDarker = it },
            )
        }
    }
}

/**
 * The video scan without the camera: [found] are the faces of each picture, [preview] draws the
 * camera. The progress is drawn on the real cube in the picture ([CubeMarks]): a solid dot per known
 * sticker, an empty ring per sticker still needed, a tick on each side done; a large arrow beside the
 * cube shows which way to turn it, and a row of the six side colours under the picture tells which
 * sides are done. When the scan cannot get on, a notice at the bottom of the picture describes why
 * and offers to start again (the camera keeps running), to fix the colours by hand and the torch;
 * scanning goes on underneath, and a tap on the picture outside it closes it for that reason (until
 * a restart). The camera is set through [onExposure] by [ExposureControl]: metered and focused on the
 * cube once a face is found, made darker (up to [maxDarker] steps) while the stickers wash out, then
 * locked; the torch turned on or off meters again. Frames are not read while the camera adjusts.
 * Clear for half a second → [onResult]; "fix colours" hands over what is known. [clock] is the time in
 * milliseconds (tests pass their own).
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
    maxDarker: Int = ExposureControl.MAX_DARKER,
    onExposure: (CameraSettings) -> Unit = {},
    clock: () -> Long = ::elapsedMillis,
    preview: @Composable (Modifier) -> Unit,
) {
    val scan = remember { VideoScan() }
    var state by remember { mutableStateOf(VideoScanState.EMPTY) }
    var picture by remember { mutableStateOf<FoundFaces?>(null) }
    var done by remember { mutableStateOf(false) }
    var dismissed by remember { mutableStateOf(emptySet<Stall>()) }
    val exposure = remember { ExposureControl() }
    exposure.maxDarker = maxDarker
    var lastTorch by remember { mutableStateOf(torch) }
    val log = remember { ScanLogger() }
    val haptics = LocalHapticFeedback.current

    fun finish(outcome: ScanOutcome) {
        if (done) return
        done = true
        AppLog.info(
            Evt.SCAN_DONE, null,
            "way" to "video",
            "valid" to outcome.validity.isValid,
            "uncertain" to outcome.uncertain.size,
            "inferred" to outcome.inferred.size,
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
            val before = exposure.settings
            val frame = exposure.onFrame(f.faces, f.width, f.height, now)
            if (exposure.settings != before) onExposure(exposure.settings)
            frame.lockedWashed?.let { log.lock(exposure.settings.darker, it) }
            log.picture(f, now)
            if (!frame.read) {
                picture = f
                return@collect
            }
            val stateBefore = state
            state = scan.onFrame(f.faces, now)
            picture = f
            log.onFrame(stateBefore, state, now, torch, exposure.settings.darker)
            if (state.newStickers > 0 && now - lastBuzz >= BUZZ_MILLIS) {
                lastBuzz = now
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            if (state.finished) finish(scan.outcome())
        }
    }
    // The torch changes the light: the camera meters again before its frames are read and locked.
    LaunchedEffect(torch) {
        if (torch == lastTorch) return@LaunchedEffect
        lastTorch = torch
        exposure.onTorch(torch, clock())
        onExposure(exposure.settings)
    }
    DisposableEffect(Unit) { onDispose { if (!done) log.leave(state, clock(), torch, exposure.settings.darker) } }

    fun restart() {
        log.restart(state)
        scan.reset()
        state = scan.state
        dismissed = emptySet()
    }

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
                picture?.let { p ->
                    CubeMarks(state, p.width, p.height, Modifier.fillMaxSize())
                    TurnArrow(state, p.width, p.height, Modifier.fillMaxSize())
                }
                val stall = state.stall?.takeIf { it !in dismissed }
                if (state.dim && stall == null) DimNotice(Modifier.align(Alignment.TopStart).padding(10.dp))
                if (stall != null) {
                    // A tap on the picture outside the notice closes it; that reason does not come back.
                    val close = stringResource(Res.string.video_notice_close)
                    Box(
                        Modifier.fillMaxSize()
                            .semantics { contentDescription = close }
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                log.dismiss(stall)
                                dismissed = dismissed + stall
                            },
                    )
                    StallNotice(
                        stall,
                        torch = torch.takeIf { torchAvailable && stall != Stall.NO_CUBE },
                        onTorch = onTorch,
                        onRestart = ::restart,
                        onFix = { finish(scan.outcome()) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(statusText(state), style = MaterialTheme.typography.titleSmall)
                DoneSides(VideoScanLog.doneSides(state))
            }
        }
    }
}

/** The turning points of the scan and a snapshot every two seconds, into the log. */
private class ScanLogger {
    private var lastSnapshot: Long? = null
    private var frames = 0
    private var faces = 0
    private var finderMs = 0L

    /** Every picture the finder read, whether the scan took it or not (the camera was adjusting). */
    fun picture(f: FoundFaces, at: Long) {
        if (lastSnapshot == null) lastSnapshot = at
        frames++
        faces += f.faces.size
        finderMs += f.finderMs
    }

    fun onFrame(before: VideoScanState, now: VideoScanState, at: Long, torch: Boolean, darker: Int) {
        for (side in VideoScanLog.doneSides(now) - VideoScanLog.doneSides(before)) event("side", "side" to side.name)
        if (now.complete && !before.complete) event("clear")
        val stall = now.stall
        if (stall != null && stall != before.stall) event("stall", "reason" to stall.name.lowercase())
        val last = lastSnapshot ?: at
        if (at - last >= VideoScanLog.SNAPSHOT_MILLIS) snapshot(now, at, torch, darker)
    }

    fun lock(darker: Int, washed: Double) = event("lock", "darker" to darker, "washed" to (washed * 100).roundToInt())

    fun restart(state: VideoScanState) = event("restart", "reason" to state.stall?.name?.lowercase())

    fun dismiss(stall: Stall) = event("dismiss", "reason" to stall.name.lowercase())

    fun leave(state: VideoScanState, at: Long, torch: Boolean, darker: Int) {
        event("leave")
        snapshot(state, at, torch, darker)
    }

    private fun snapshot(state: VideoScanState, at: Long, torch: Boolean, darker: Int) {
        val n = frames.coerceAtLeast(1)
        val seconds = (at - (lastSnapshot ?: at)) / 1000.0
        val fps = if (seconds > 0) frames / seconds else 0.0
        AppLog.info(
            Evt.SCAN_VIDEO, null,
            *VideoScanLog.snapshot(state, faces.toDouble() / n, finderMs.toDouble() / n, fps, torch, darker),
        )
        lastSnapshot = at
        frames = 0
        faces = 0
        finderMs = 0
    }

    private fun event(kind: String, vararg fields: Pair<String, Any?>) = AppLog.info(Evt.SCAN_VIDEO, null, "kind" to kind, *fields)
}

@Composable
private fun statusText(state: VideoScanState): String {
    val hint = state.hint
    return stringResource(
        when {
            state.complete -> Res.string.video_status_done
            hint != null -> when (hint) {
                Tilt.UP -> Res.string.video_hint_up
                Tilt.DOWN -> Res.string.video_hint_down
                Tilt.LEFT -> Res.string.video_hint_left
                Tilt.RIGHT -> Res.string.video_hint_right
            }
            state.found.isEmpty() -> Res.string.video_status_find
            else -> Res.string.video_status_keep
        },
    )
}

/**
 * The progress on the real cube in the latest picture of [width]×[height] pixels (the picture fills
 * the box). With the cube's pose known, every sticker of the sides facing the camera is marked where
 * it lies: a solid dot in its colour when known, an empty ring when still needed. The faces found in
 * the picture win over the projection there: each sticker is marked in the colour it was read as,
 * solid once known. A side done gets a tick at its centre.
 */
@Composable
private fun CubeMarks(state: VideoScanState, width: Int, height: Int, modifier: Modifier) {
    val done = VideoScanLog.doneSides(state)
    Canvas(modifier) {
        val sx = size.width / width
        val sy = size.height / height
        fun at(p: Point) = Offset((p.x * sx).toFloat(), (p.y * sy).toFloat())
        val foundSides = HashSet<Int>()
        val ticks = ArrayList<Pair<Offset, Float>>()
        for (face in state.found) {
            val reading = face.reading
            val step = minOf(reading.u.length * sx, reading.v.length * sy).toFloat()
            for (n in 0 until 9) {
                val name = face.names[n] ?: continue
                mark(at(reading.centre + reading.u * (n % 3 - 1.0) + reading.v * (n / 3 - 1.0)), step, StickerColors.of(name), face.recognised[n])
            }
            // The side this face is, once its centre is named: the centre's colour tells it.
            val side = face.names[4]?.let { c -> Face.entries.firstOrNull { ColorScheme.STANDARD[it] == c } }
            if (side != null) {
                foundSides += side.ordinal
                if (side in done) ticks += at(reading.centre) to step
            }
        }
        state.projection?.let { projection ->
            for (side in projection.facing) {
                if (side.ordinal in foundSides) continue
                // A side seen at an angle is narrower: its marks follow its own sticker spacing.
                val p = { n: Int -> at(projection.points[side.ordinal * 9 + n]) }
                val step = minOf((p(1) - p(0)).getDistance(), (p(3) - p(0)).getDistance())
                for (n in 0 until 9) {
                    val i = side.ordinal * 9 + n
                    val color = state.stickers[i]
                    mark(at(projection.points[i]), step, color?.let(StickerColors::of) ?: NEEDED, color != null)
                }
                if (side in done) ticks += at(projection.points[side.ordinal * 9 + 4]) to step
            }
        }
        for ((centre, step) in ticks) tick(centre, step * TICK_SHARE)
    }
}

/** A sticker's mark: a solid dot in [color] when [solid], else an empty ring; smaller than the sticker. */
private fun DrawScope.mark(centre: Offset, step: Float, color: Color, solid: Boolean) {
    val radius = step * DOT_SHARE / 2
    if (solid) {
        drawCircle(color, radius, centre)
        drawCircle(StickerColors.PLASTIC, radius, centre, style = Stroke(maxOf(1f, radius * 0.18f)))
    } else {
        drawCircle(StickerColors.PLASTIC.copy(alpha = 0.6f), radius, centre, style = Stroke(maxOf(2f, radius * 0.5f)))
        drawCircle(color, radius, centre, style = Stroke(maxOf(1.5f, radius * 0.28f)))
    }
}

/** A white tick with a dark outline, [size] wide, centred on [centre]. */
private fun DrawScope.tick(centre: Offset, size: Float) {
    val path = Path().apply {
        moveTo(centre.x - size * 0.45f, centre.y)
        lineTo(centre.x - size * 0.12f, centre.y + size * 0.32f)
        lineTo(centre.x + size * 0.45f, centre.y - size * 0.35f)
    }
    drawPath(path, StickerColors.PLASTIC, style = Stroke(size * 0.3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, Color.White, style = Stroke(size * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/**
 * The turning hint as a large arrow beside the real cube, on the side the cube should move towards
 * and pointing that way; it fades out while no face is found.
 */
@Composable
private fun TurnArrow(state: VideoScanState, width: Int, height: Int, modifier: Modifier) {
    var last by remember { mutableStateOf<Pair<Tilt, List<Point>>?>(null) }
    val hint = state.hint
    val outline = cubeOutline(state)
    if (hint != null && outline.isNotEmpty()) last = hint to outline
    val alpha by animateFloatAsState(if (hint != null && outline.isNotEmpty()) 1f else 0f)
    val (tilt, points) = last ?: return
    if (alpha == 0f) return
    Canvas(modifier) {
        val sx = size.width / width
        val sy = size.height / height
        val (dx, dy) = when (tilt) {
            Tilt.UP -> 0f to -1f
            Tilt.DOWN -> 0f to 1f
            Tilt.LEFT -> -1f to 0f
            Tilt.RIGHT -> 1f to 0f
        }
        val shown = points.map { Offset((it.x * sx).toFloat(), (it.y * sy).toFloat()) }
        // The cube's middle across the arrow, and its edge in the arrow's direction.
        val across = if (dx != 0f) shown.map { it.y } else shown.map { it.x }
        val middle = (across.min() + across.max()) / 2
        val edge = shown.maxOf { it.x * dx + it.y * dy }
        val length = size.minDimension * 0.22f
        val margin = 8.dp.toPx()
        val along = edge + 12.dp.toPx() + length / 2
        val mid = if (dx != 0f) {
            Offset(
                (along * dx).coerceIn(margin + length / 2, size.width - margin - length / 2),
                middle.coerceIn(margin + length * 0.3f, size.height - margin - length * 0.3f),
            )
        } else {
            Offset(
                middle.coerceIn(margin + length * 0.3f, size.width - margin - length * 0.3f),
                (along * dy).coerceIn(margin + length / 2, size.height - margin - length / 2),
            )
        }
        arrow(mid, dx, dy, length, alpha)
    }
}

/** The points the real cube covers in the picture: its projected sides facing the camera and the faces found. */
private fun cubeOutline(state: VideoScanState): List<Point> {
    val points = ArrayList<Point>()
    state.projection?.let { p -> p.facing.forEach { f -> (0 until 9).mapTo(points) { p.points[f.ordinal * 9 + it] } } }
    state.found.forEach { points += it.reading.outline }
    return points
}

/** A straight arrow of [length] centred on [mid], pointing along ([dx], [dy]). */
private fun DrawScope.arrow(mid: Offset, dx: Float, dy: Float, length: Float, alpha: Float) {
    val start = Offset(mid.x - dx * length / 2, mid.y - dy * length / 2)
    val end = Offset(mid.x + dx * length / 2, mid.y + dy * length / 2)
    val head = length * 0.38f
    val width = length * 0.14f
    val shaftEnd = Offset(end.x - dx * head * 0.8f, end.y - dy * head * 0.8f)
    val outline = StickerColors.ARROW_OUTLINE.copy(alpha = alpha)
    val fill = StickerColors.ARROW.copy(alpha = alpha)
    drawLine(outline, start, shaftEnd, width * 1.6f, cap = StrokeCap.Round)
    drawLine(fill, start, shaftEnd, width, cap = StrokeCap.Round)
    val tip = Path().apply {
        moveTo(end.x + dx * head * 0.15f, end.y + dy * head * 0.15f)
        lineTo(end.x - dx * head - dy * head * 0.6f, end.y - dy * head + dx * head * 0.6f)
        lineTo(end.x - dx * head + dy * head * 0.6f, end.y - dy * head - dx * head * 0.6f)
        close()
    }
    drawPath(tip, outline, style = Stroke(width * 0.5f, join = StrokeJoin.Round))
    drawPath(tip, fill)
}

/** A small notice on the picture that the light is dim, before the scan stalls. */
@Composable
private fun DimNotice(modifier: Modifier) {
    Row(
        modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(Res.drawable.ic_torch), contentDescription = null, tint = StickerColors.ARROW, modifier = Modifier.size(18.dp))
        Text(stringResource(Res.string.video_dim), color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Why the scan cannot get on, described (icon and a few words), at the bottom of the picture without
 * covering the cube: "start over", "fix colours" and, where [torch] is given (the device has one and
 * the reason is the light or no progress), the torch.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StallNotice(stall: Stall, torch: Boolean?, onTorch: (Boolean) -> Unit, onRestart: () -> Unit, onFix: () -> Unit, modifier: Modifier) {
    val (icon, text) = when (stall) {
        Stall.DARK -> Res.drawable.ic_torch to Res.string.video_notice_dark
        Stall.NO_CUBE -> Res.drawable.ic_cube to Res.string.video_notice_no_cube
        Stall.STUCK -> Res.drawable.ic_reset_view to Res.string.video_notice_stuck
    }
    Column(
        modifier.fillMaxWidth().padding(10.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f), MaterialTheme.shapes.large)
            // Taps on the notice itself do not reach the picture behind it (which closes it).
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(stringResource(text), style = MaterialTheme.typography.titleSmall)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = onRestart, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.video_restart)) }
            OutlinedButton(onClick = onFix, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.video_check)) }
            if (torch != null) {
                FilterChip(
                    selected = torch,
                    onClick = { onTorch(!torch) },
                    label = { Text(stringResource(Res.string.scan_torch)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_torch), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}

/** The six side colours in a row, a tick on each side the rest of the cube confirms. */
@Composable
private fun DoneSides(done: Set<Face>) {
    val description = stringResource(Res.string.video_sides_done, done.size)
    Row(Modifier.semantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (side in Face.entries) {
            val isDone = side in done
            Canvas(Modifier.size(28.dp)) {
                val radius = size.minDimension / 2
                drawCircle(StickerColors.of(ColorScheme.STANDARD[side]).copy(alpha = if (isDone) 1f else 0.35f), radius)
                drawCircle(StickerColors.PLASTIC, radius, style = Stroke(2.dp.toPx()))
                if (isDone) tick(center, radius * 1.3f)
            }
        }
    }
}

/** Ring colour of a sticker still needed (fixed, drawn on the camera picture in both themes). */
private val NEEDED = Color.White

/** A sticker's mark on the camera picture, as a share of its step. */
private const val DOT_SHARE = 0.5f

/** A done side's tick, as a share of a sticker step. */
private const val TICK_SHARE = 0.9f

/** Shortest gap between two vibrations for new stickers. */
private const val BUZZ_MILLIS = 300L
