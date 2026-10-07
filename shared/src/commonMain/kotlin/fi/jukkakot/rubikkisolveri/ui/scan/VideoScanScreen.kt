package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The faces (full and partial) [FaceFinder] found in one camera picture of [width]×[height] pixels, in
 * [finderMs]; [worker] whether the browser's worker found them (null where there is none: the phone).
 */
class FoundFaces(val faces: List<FaceReading>, val width: Int, val height: Int, val finderMs: Long = 0, val worker: Boolean? = null)

/**
 * The video scan: the camera's pictures go through [FaceFinder] off the main thread, the faces found
 * to [VideoScanContent]. In the browser the camera hands over the faces its worker found; only when
 * the worker cannot run do the pictures come here (Dispatchers.Default is then the page's one
 * thread). The finder's time per frame goes into the log's snapshots.
 */
@Composable
fun VideoScanScreen(onBack: () -> Unit, onManual: () -> Unit, onResult: (ScanOutcome) -> Unit, onSwitch: (() -> Unit)? = null) {
    CameraPermissionGate(alternative = stringResource(Res.string.scan_manual) to onManual) {
        val images = remember { MutableSharedFlow<ArgbImage>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }
        val found = remember { MutableSharedFlow<FoundFaces>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }
        var torch by remember { mutableStateOf(false) }
        var torchAvailable by remember { mutableStateOf(false) }
        var exposure by remember { mutableStateOf(CameraSettings.FREE) }
        var maxDarker by remember { mutableStateOf(0) }
        var cameraFailed by remember { mutableStateOf(false) }
        var worker by remember { mutableStateOf<Boolean?>(null) }
        LaunchedEffect(images) {
            withContext(Dispatchers.Default) {
                images.collect { image ->
                    val start = elapsedMillis()
                    val faces = FaceFinder.find(image.argb, image.width, image.height).let { it.faces + it.partial }.map(FaceReading::of)
                    found.emit(FoundFaces(faces, image.width, image.height, elapsedMillis() - start, worker?.let { false }))
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
            onSwitch = onSwitch,
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
                onFaces = { found.tryEmit(it) },
                onWorker = { worker = it },
            )
        }
    }
}

/**
 * The video scan without the camera: [found] are the faces of each picture, [preview] draws the
 * camera, which fills the screen. The progress is painted on the real cube in the picture
 * ([PaintLayer]): a tile per sticker, solid in its colour when known, grey while still needed, a
 * white outline around a side the rest of the cube confirms; the grey parts tell what to show next.
 * On the picture: back, a ring filling with the stickers known, the torch and a menu (one picture at
 * a time, by hand, the colour check with what is known), and one status line at the bottom. When the
 * scan cannot get on, a notice there describes why and offers to start again (the camera keeps
 * running), to fix the colours by hand and the torch; scanning goes on underneath, and a tap on the
 * picture outside it closes it for that reason (until a restart). The camera is set through
 * [onExposure] by [ExposureControl]: metered and focused on the cube once a face is found, made
 * darker (up to [maxDarker] steps) while the stickers wash out, then locked; the torch turned on or
 * off meters again. Every frame is read, also while the camera adjusts. Clear for half a second →
 * [onResult]. [clock] is the time in milliseconds (tests pass their own).
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
    onSwitch: (() -> Unit)? = null,
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

    val share by animateFloatAsState(if (done || state.complete) 1f else state.recognised / Stickers.COUNT.toFloat(), tween(250))
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraFailed) {
            Text(stringResource(Res.string.scan_camera_error), color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            preview(Modifier.fillMaxSize())
        }
        picture?.let { p -> PaintLayer(state, p.width, p.height, Modifier.fillMaxSize()) }
        // A face is in view but nothing read yet: a small sign that the scan is working.
        if (!done && picture?.faces?.isNotEmpty() == true && state.recognised == 0) {
            CircularProgressIndicator(
                Modifier.align(Alignment.Center).size(32.dp).testTag(VIDEO_SPINNER_TAG),
                color = Color.White,
                strokeWidth = 3.dp,
            )
        }
        val stall = state.stall?.takeIf { it !in dismissed }
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
        }
        // Back, torch and menu on the picture; the progress ring between them.
        ScanOverlayBar(
            onBack = onBack,
            torch = torch,
            onTorch = onTorch,
            torchAvailable = torchAvailable,
            menu = { close ->
                ScanMenuItems(
                    close = close,
                    onSwitch = onSwitch,
                    onManual = onManual,
                    onCheck = { finish(scan.outcome()) }.takeIf { state.recognised > 0 },
                )
            },
        ) { ProgressRing(share, state.recognised) }
        if (state.dim && stall == null) DimNotice(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(start = 12.dp, top = 72.dp))
        val bottom = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        if (stall != null) {
            StallNotice(
                stall,
                torch = torch.takeIf { torchAvailable && stall != Stall.NO_CUBE },
                onTorch = onTorch,
                onRestart = ::restart,
                onFix = { finish(scan.outcome()) },
                modifier = bottom,
            )
        } else {
            Text(
                statusText(state),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                modifier = bottom.padding(16.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/** Test tag of the spinner shown while a face is found and no sticker is read yet. */
const val VIDEO_SPINNER_TAG = "video-spinner"

/** The turning points of the scan and a snapshot every two seconds, into the log. */
private class ScanLogger {
    private var lastSnapshot: Long? = null
    private var frames = 0
    private var faces = 0
    private var finderMs = 0L
    private var worker: Boolean? = null

    /** Every picture the finder read. */
    fun picture(f: FoundFaces, at: Long) {
        if (lastSnapshot == null) lastSnapshot = at
        frames++
        faces += f.faces.size
        finderMs += f.finderMs
        worker = f.worker
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
            *VideoScanLog.snapshot(state, faces.toDouble() / n, finderMs.toDouble() / n, fps, torch, darker, worker),
        )
        lastSnapshot = at
        frames = 0
        faces = 0
        finderMs = 0
    }

    private fun event(kind: String, vararg fields: Pair<String, Any?>) = AppLog.info(Evt.SCAN_VIDEO, null, "kind" to kind, *fields)
}

/** The one status line: show the cube, turn the cube (two faces could still be told apart either way), show the grey parts, or ready. */
@Composable
private fun statusText(state: VideoScanState): String = stringResource(videoStatus(state))

/** The status line's text for [state]. */
fun videoStatus(state: VideoScanState): StringResource = when {
    state.complete -> Res.string.video_status_done
    state.found.isEmpty() -> Res.string.video_status_find
    state.undecided -> Res.string.video_status_turn
    else -> Res.string.video_status_grey
}

/**
 * The paint on the real cube in the latest picture of [width]×[height] pixels (the picture fills the
 * box, `scan-paint-calm`): a grey veil over every sticker still needed, a small dot in its read
 * colour on every known one (`scan-steady-progress`), a white outline and a tick on a confirmed side,
 * a dim outline round each other face found ([ScanPaint]). Veils and dots glide towards their places
 * at the display's rate ([Glide]); everything fades while the
 * cube moves quickly ([MotionFade]) and the projection's part when it grows old ([paintAlpha]).
 */
@Composable
private fun PaintLayer(state: VideoScanState, width: Int, height: Int, modifier: Modifier) {
    val paint = remember(state) { ScanPaint.of(state) }
    val glide = remember { Glide() }
    val motion = remember { MotionFade() }
    val still = remember(state) {
        val projection = state.projection
        val largest = state.found.maxByOrNull { it.reading.area }?.reading
        val centre = projection?.let { pr -> Point(pr.points.sumOf { it.x } / pr.points.size, pr.points.sumOf { it.y } / pr.points.size) } ?: largest?.centre
        val side = projection?.let { 3 * it.step } ?: largest?.let { 3 * it.u.length } ?: 0.0
        motion.step(centre, side, elapsedMillis())
    }
    val shown by animateFloatAsState(if (still) 1f else 0f, tween(MOTION_FADE_MILLIS), label = "motion")
    var drawn by remember { mutableStateOf<Map<Int, Point>>(emptyMap()) }
    // Glides for a few frames after each new picture, then rests until the next one.
    LaunchedEffect(paint) {
        val targets = paint.tiles.associate { it.key to it.centre } + paint.dots.associate { it.key to it.centre }
        val snaps = paint.tiles.associate { it.key to SNAP_STEPS * maxOf(it.u.length, it.v.length) } +
            paint.dots.associate { it.key to SNAP_STEPS * maxOf(it.u.length, it.v.length) }
        var last: Long? = null
        while (true) {
            val t = withFrameNanos { it }
            val dt = last?.let { (t - it) / 1_000_000f } ?: FRAME_MILLIS
            last = t
            drawn = glide.step(targets, dt) { snaps[it] ?: 0.0 }
            if (drawn.all { (key, at) -> (targets.getValue(key) - at).length < REST_PIXELS }) break
        }
    }
    val alpha = paintAlpha(state.projectionAge) * shown
    Canvas(modifier) {
        val sx = size.width / width
        val sy = size.height / height
        fun at(p: Point) = Offset((p.x * sx).toFloat(), (p.y * sy).toFloat())
        fun outline(corners: List<Point>) = Path().apply {
            corners.forEachIndexed { i, p -> at(p).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
            close()
        }
        for (tile in paint.tiles) {
            val centre = drawn[tile.key] ?: tile.centre
            // Projection veils fade with the projection's age; a face found in this frame is current.
            val a = (if (tile.key < Stickers.COUNT) alpha else shown)
            if (a <= 0f) continue
            val h = TILE_SHARE / 2
            val corners = listOf(-h to -h, h to -h, h to h, -h to h).map { (du, dv) -> centre + tile.u * du.toDouble() + tile.v * dv.toDouble() }
            drawPath(outline(corners), NEEDED.copy(alpha = VEIL_ALPHA * a))
        }
        for (dot in paint.dots) {
            val a = (if (dot.key < Stickers.COUNT) alpha else shown)
            if (a <= 0f) continue
            val c = at(drawn[dot.key] ?: dot.centre)
            val r = (DOT_SHARE / 2 * minOf(dot.u.length * sx, dot.v.length * sy)).toFloat()
            // A dark rim so a white or yellow dot shows on a bright sticker.
            drawCircle(StickerColors.PLASTIC.copy(alpha = 0.7f * a), r + 1.5.dp.toPx(), c)
            drawCircle(StickerColors.of(dot.color).copy(alpha = a), r, c)
        }
        for (corners in paint.found) {
            drawPath(outline(corners), Color.White.copy(alpha = 0.35f * shown), style = Stroke(1.dp.toPx(), join = StrokeJoin.Round))
        }
        for (corners in paint.outlines) {
            val path = outline(corners)
            drawPath(path, StickerColors.PLASTIC.copy(alpha = 0.6f * alpha), style = Stroke(6.dp.toPx(), join = StrokeJoin.Round))
            drawPath(path, Color.White.copy(alpha = alpha), style = Stroke(3.dp.toPx(), join = StrokeJoin.Round))
        }
        for (tick in paint.ticks) {
            // A check mark on the side's centre sticker.
            val path = Path().apply {
                listOf(-0.3 to 0.0, -0.08 to 0.24, 0.32 to -0.22).forEachIndexed { i, (du, dv) ->
                    val c = at(tick.centre + tick.u * du + tick.v * dv)
                    if (i == 0) moveTo(c.x, c.y) else lineTo(c.x, c.y)
                }
            }
            drawPath(path, StickerColors.PLASTIC.copy(alpha = 0.6f * alpha), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path, Color.White.copy(alpha = alpha), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** How long the marks take to fade out or in as the cube starts or stops moving. */
private const val MOTION_FADE_MILLIS = 150

/** A known sticker's dot across, as a share of its sticker step: small, so the real sticker shows round it. */
private const val DOT_SHARE = 0.35

/** How strongly a needed sticker's grey veil covers it. */
private const val VEIL_ALPHA = 0.55f

/** A small ring that fills with the share of stickers known (no number on screen). */
@Composable
private fun ProgressRing(share: Float, known: Int) {
    val description = stringResource(Res.string.video_progress, known)
    Canvas(Modifier.size(40.dp).semantics { contentDescription = description }) {
        val stroke = 4.dp.toPx()
        drawCircle(Color.Black.copy(alpha = 0.55f))
        val inset = stroke / 2 + 4.dp.toPx()
        val box = Size(size.width - 2 * inset, size.height - 2 * inset)
        drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, Offset(inset, inset), box, style = Stroke(stroke))
        drawArc(Color.White, -90f, 360f * share, false, Offset(inset, inset), box, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

/** The ⋮ menu's items: one picture at a time, by hand, and the colour check with what is known ([onCheck] null until a sticker is). */
@Composable
private fun ScanMenuItems(close: () -> Unit, onSwitch: (() -> Unit)?, onManual: () -> Unit, onCheck: (() -> Unit)?) {
    if (onSwitch != null) {
        DropdownMenuItem(text = { Text(stringResource(Res.string.scan_switch_photo)) }, onClick = { close(); onSwitch() })
    }
    DropdownMenuItem(text = { Text(stringResource(Res.string.scan_manual_short)) }, onClick = { close(); onManual() })
    DropdownMenuItem(
        text = { Text(stringResource(Res.string.video_check)) },
        enabled = onCheck != null,
        onClick = { close(); onCheck?.invoke() },
    )
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

/** Fill of a sticker still needed (fixed, drawn on the camera picture in both themes). */
private val NEEDED = Color(0xFF8A8A8A)

/** A tile's side as a share of its sticker step: smaller than the sticker, so the real one shows around it. */
private const val TILE_SHARE = 0.62f

/** A tile further than this many sticker steps from where it should be jumps instead of gliding. */
private const val SNAP_STEPS = 1.5

/** Tiles this close (picture pixels) to their places have arrived: the glide rests. */
private const val REST_PIXELS = 0.5

/** The first glide step's length when there is no previous frame yet. */
private const val FRAME_MILLIS = 16f

/** Shortest gap between two vibrations for new stickers. */
private const val BUZZ_MILLIS = 300L
