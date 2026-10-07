@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)

package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.text.style.TextAlign
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.ui.common.BackButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.scan.CameraSettings
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEvent
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.cube.scan.ScanSession
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.log.NoScanPictures
import fi.jukkakot.rubikkisolveri.log.ScanPictureStore
import fi.jukkakot.rubikkisolveri.cube.Face
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.atomics.AtomicReference
import fi.jukkakot.rubikkisolveri.ui.elapsedMillis
import fi.jukkakot.rubikkisolveri.ui.common.colorName
import fi.jukkakot.rubikkisolveri.ui.common.faceName

import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

/**
 * The scan of all six faces, or with [only] set just that face (a rescan from the check), whose
 * readings go to [onFace] and whose picture replaces that face's in [LastScan].
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onManual: () -> Unit,
    onResult: (ScanOutcome) -> Unit,
    only: FaceView? = null,
    onFace: (FaceView, List<Rgb>) -> Unit = { _, _ -> },
    pictures: ScanPictureStore = NoScanPictures,
    onSwitch: (() -> Unit)? = null,
) {
    CameraPermissionGate(
        alternative = stringResource(Res.string.scan_manual) to onManual,
        denied = { content -> PermissionScaffold(onBack) { content() } },
    ) {
        val frames = remember {
            MutableSharedFlow<List<Rgb>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }
        var torch by remember { mutableStateOf(false) }
        var torchAvailable by remember { mutableStateOf(false) }
        var lockExposure by remember { mutableStateOf(false) }
        var cameraFailed by remember { mutableStateOf(false) }
        // The latest grid picture and which of its cells look like stickers (worked out on the camera thread).
        val latestPicture = remember { AtomicReference<Pair<IntArray, FrameSampler.GridCheck>?>(null) }
        // The picture of each accepted face as seen, for the check after an unsure scan, and the
        // picture of the capture under review.
        val facePictures = remember { HashMap<Face, IntArray>() }
        val pending = remember { AtomicReference<IntArray?>(null) }
        val scope = rememberCoroutineScope()
        ScanContent(
            frames = frames,
            torch = torch,
            onTorch = { torch = it },
            torchAvailable = torchAvailable,
            onBack = onBack,
            onManual = onManual,
            onResult = { outcome ->
                // Each face's picture turned the way its colours were, from the capture they came from.
                LastScan.pictures = Face.entries.mapNotNull { face ->
                    facePictures[outcome.from[face] ?: face]?.let { face to rotatePicture(it, FrameSampler.PICTURE_SIZE, outcome.rotations[face] ?: 0) }
                }.toMap()
                LastScan.readings = outcome.samples
                onResult(outcome)
            },
            only = only,
            onSwitch = onSwitch,
            onFace = { view, samples ->
                // Still as seen: the check turns it once it knows how the face was held.
                LastScan.pictures = LastScan.pictures + facePictures
                onFace(view, samples)
            },
            cameraFailed = cameraFailed,
            watchStalls = true,
            onLockExposure = { lockExposure = it },
            keepPicture = { view -> pending.exchange(null)?.let { facePictures[view.face] = it } },
            savePicture = { face ->
                latestPicture.load()?.first?.let { picture ->
                    pending.store(picture)
                    pictures.newName(face).also { name ->
                        scope.launch(Dispatchers.Default) {
                            runCatching { pictures.write(name, picture, FrameSampler.PICTURE_SIZE) }
                                .onFailure { AppLog.logger.error(Evt.SCAN_ERROR, it, "picture") }
                        }
                    }
                }
            },
            gridCheck = { latestPicture.load()?.second },
            preview = { modifier ->
                CameraPreview(
                    torch = torch,
                    exposure = CameraSettings(lock = lockExposure),
                    onSamples = { frames.tryEmit(it) },
                    onPicture = { latestPicture.store(it to FrameSampler.check(it)) },
                    onTorchAvailable = { torchAvailable = it },
                    onError = { cameraFailed = true },
                    modifier = modifier,
                )
            },
        )
    }
}

/**
 * The scan without the camera itself: [frames] are the grid readings, [preview] draws the camera.
 * A captured face is accepted by itself after [autoAcceptMillis] (null: only by tapping).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanContent(
    frames: Flow<List<Rgb>>,
    torch: Boolean,
    onTorch: (Boolean) -> Unit,
    onBack: () -> Unit,
    onManual: () -> Unit,
    onResult: (ScanOutcome) -> Unit,
    cameraFailed: Boolean = false,
    holdMillis: Long = ScanSession.HOLD_MILLIS,
    autoAcceptMillis: Int? = AUTO_ACCEPT_MILLIS,
    onLockExposure: (Boolean) -> Unit = {},
    savePicture: (face: String) -> String? = { null },
    keepPicture: (FaceView) -> Unit = {},
    gridCheck: () -> FrameSampler.GridCheck? = { CUBE_IN_VIEW },
    watchStalls: Boolean = false,
    only: FaceView? = null,
    onFace: (FaceView, List<Rgb>) -> Unit = { _, _ -> },
    torchAvailable: Boolean = true,
    onSwitch: (() -> Unit)? = null,
    preview: @Composable (Modifier) -> Unit,
) {
    val session = remember { ScanSession(holdMillis = holdMillis, only = only) }
    var event by remember { mutableStateOf<ScanEvent>(ScanEvent.Waiting) }
    var live by remember { mutableStateOf<List<Rgb>?>(null) }
    var stickers by remember { mutableStateOf<List<Boolean>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var review by remember { mutableStateOf<List<Rgb>?>(null) }
    var recognised by remember { mutableStateOf<FaceView?>(null) }
    // "Face read" stands in the status line from accepting a face until the next one is in the grid.
    var justAccepted by remember { mutableStateOf(false) }
    // A captured face is accepted by itself unless the user touches the review first.
    var autoAccept by remember { mutableStateOf(false) }
    val acceptFill = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current

    fun handle(e: ScanEvent) {
        event = e
        if (e !is ScanEvent.Waiting) justAccepted = false
        live = session.latest
        index = session.index
        if (e is ScanEvent.Captured) {
            review = session.review
            autoAccept = autoAcceptMillis != null
            recognised = e.face
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            val picture = runCatching { savePicture(e.face.face.name) }
                .onFailure { AppLog.logger.error(Evt.SCAN_ERROR, it, "picture") }.getOrNull()
            AppLog.info(
                Evt.SCAN_CAPTURE, null,
                "face" to e.face.face.name,
                "picture" to picture,
                "rgb" to session.review?.joinToString(",") { it.toHex() },
            )
        }
    }

    fun accept() {
        val face = session.reviewFace ?: return
        autoAccept = false
        session.accept()
        keepPicture(face)
        review = null
        justAccepted = true
        index = session.index
        event = ScanEvent.Waiting
        AppLog.info(
            Evt.SCAN_FACE, null,
            "face" to face.face.name,
            "rgb" to session.capturedSamples(face)?.joinToString(",") { it.toHex() },
            "recognised" to recognised?.face?.name,
        )
        if (session.isDone && only != null) {
            session.capturedSamples(only)?.let { onFace(only, it) }
        } else if (session.isDone) {
            val outcome = session.outcome()
            AppLog.info(
                Evt.SCAN_DONE, null,
                "valid" to outcome.validity.isValid,
                "validity" to outcome.validity.toString(),
                "uncertain" to outcome.uncertain.size,
                "cube" to outcome.editor.encode(),
                "rotations" to Face.entries.joinToString("") { "${it.name}${outcome.rotations[it] ?: 0}" },
                "renamed" to outcome.renamed,
            )
            onResult(outcome)
        }
    }

    fun retake() {
        autoAccept = false
        session.retake()
        review = null
        event = ScanEvent.Waiting
    }

    LaunchedEffect(review, autoAccept) {
        if (review == null || !autoAccept) return@LaunchedEffect
        acceptFill.snapTo(0f)
        acceptFill.animateTo(1f, tween(autoAcceptMillis ?: 0, easing = LinearEasing))
        accept()
    }

    LaunchedEffect(session) {
        frames.collect { samples ->
            if (!session.isDone && session.review == null) {
                // No grid picture yet: not a cube yet.
                val check = gridCheck()
                stickers = check?.stickerCells
                handle(session.onFrame(samples, elapsedMillis(), check?.looksLikeCube == true))
            }
        }
    }
    // From the first capture on (the cube held still, the camera settled on it), keep exposure and
    // white balance fixed so every face is read alike. Back to no face done releases it.
    val locked = index > 0 || review != null
    LaunchedEffect(locked) { onLockExposure(locked) }
    if (watchStalls) {
        LaunchedEffect(Unit) {
            var last = 0L
            while (true) {
                withFrameNanos { now ->
                    val gap = (now - last) / 1_000_000
                    if (last > 0 && gap >= UI_STALL_MILLIS) AppLog.info(Evt.SCAN_STALL, null, "where" to "ui", "ms" to gap)
                    last = now
                }
            }
        }
    }

    // The camera fills the screen; everything else is on the picture.
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraFailed) {
            Text(stringResource(Res.string.scan_camera_error), color = Color.White, modifier = Modifier.align(Alignment.Center))
        } else {
            preview(Modifier.fillMaxSize())
        }
        val read = review
        val holding = event as? ScanEvent.Holding
        if (read == null) {
            // The full scan names no face while scanning; a one-face rescan knows its face.
            GridOverlay(live, stickers, holding?.recognised?.takeIf { only != null }?.centreColor(), Modifier.fillMaxSize())
        } else {
            ReviewOverlay(
                read,
                Modifier.fillMaxSize().pointerInput(read) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        autoAccept = false
                    }
                },
            )
        }
        // Back, the face marks, torch and menu (a one-face rescan has no menu).
        ScanOverlayBar(
            onBack = onBack,
            torch = torch,
            onTorch = onTorch,
            torchAvailable = torchAvailable,
            menu = if (only != null) null else { close ->
                if (onSwitch != null) {
                    DropdownMenuItem(text = { Text(stringResource(Res.string.scan_switch_video)) }, onClick = { close(); onSwitch() })
                }
                DropdownMenuItem(text = { Text(stringResource(Res.string.scan_manual_short)) }, onClick = { close(); onManual() })
            },
        ) { Progress(session::isAccepted, { session.capturedSamples(it)?.get(4) }, index, only) }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (read == null) {
                // The request only until the first face is done; a one-face rescan names its face's centre.
                val request = when {
                    only != null -> stringResource(Res.string.scan_one_hint, stringResource(colorName(only.centreColor())))
                    index == 0 -> stringResource(Res.string.scan_any_hint)
                    else -> null
                }
                if (request != null) Text(request, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Text(
                    if (justAccepted) stringResource(Res.string.scan_captured) else statusText(event),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                )
                LinearProgressIndicator(
                    progress = { holding?.progress ?: 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                // The shutter in the middle, redo of the previous face beside it.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Shutter(enabled = live != null, onClick = { handle(session.captureNow()) })
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        // A one-face rescan has no previous face to go back to.
                        if (only == null) {
                            RoundIconButton(
                                onClick = {
                                    session.redo()
                                    index = session.index
                                    event = ScanEvent.Waiting
                                    justAccepted = false
                                },
                                enabled = index > 0,
                            ) { Icon(painterResource(Res.drawable.ic_undo), contentDescription = stringResource(Res.string.scan_redo)) }
                        }
                    }
                }
            } else {
                // The full scan names the faces only at the end; a one-face rescan knows its face.
                if (only != null) {
                    Text(
                        stringResource(Res.string.scan_review_face, stringResource(faceName(only))),
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Text(stringResource(Res.string.scan_review_note), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::retake, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(stringResource(Res.string.scan_retake)) }
                    // "Good, next" fills up until the face is accepted by itself.
                    val fill = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                    val shown = if (autoAccept) acceptFill.value else 0f
                    Button(
                        onClick = ::accept,
                        modifier = Modifier.weight(1f).heightIn(min = 56.dp).clip(CircleShape)
                            .drawWithContent {
                                drawContent()
                                drawRect(fill, size = Size(size.width * shown, size.height))
                            },
                    ) { Text(stringResource(Res.string.scan_accept)) }
                }
            }
        }
    }
}

/** The capture button: a large round shutter in primary with a light ring. */
@Composable
private fun Shutter(enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val description = stringResource(Res.string.scan_capture)
    Box(
        Modifier.padding(horizontal = 8.dp).size(84.dp).clip(CircleShape)
            .background(if (enabled) scheme.primary else scheme.surfaceContainerHigh)
            .border(6.dp, if (enabled) scheme.onSurface else scheme.outlineVariant, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    )
}

@Composable
private fun statusText(event: ScanEvent): String = when (event) {
    is ScanEvent.AlreadyScanned -> stringResource(Res.string.scan_status_turn)
    is ScanEvent.NoCube -> stringResource(Res.string.scan_status_no_cube)
    is ScanEvent.Holding -> stringResource(Res.string.scan_status_hold)
    else -> stringResource(Res.string.scan_status_align)
}

/**
 * The grid as on [FrameSampler]: a centred square, with a dot of what each cell reads and a ring
 * round the centre in the colour of the face it looks like ([centre]), if any.
 */
@Composable
private fun GridOverlay(live: List<Rgb>?, stickers: List<Boolean>?, centre: CubeColor?, modifier: Modifier) {
    Canvas(modifier) {
        val side = FrameSampler.GRID_SIZE * size.minDimension
        val left = (size.width - side) / 2
        val top = (size.height - side) / 2
        val cell = side / 3
        val line = Stroke(width = 3.dp.toPx())
        drawRect(Color.White, Offset(left, top), Size(side, side), style = line)
        for (i in 1..2) {
            drawLine(Color.White, Offset(left + cell * i, top), Offset(left + cell * i, top + side), strokeWidth = 2.dp.toPx())
            drawLine(Color.White, Offset(left, top + cell * i), Offset(left + side, top + cell * i), strokeWidth = 2.dp.toPx())
        }
        // Cells that look like a sticker get a green outline, so the user sees which part is off.
        val inset = 3.dp.toPx()
        stickers?.forEachIndexed { i, sticker ->
            if (sticker) {
                drawRect(
                    STICKER_GREEN, Offset(left + cell * (i % 3) + inset, top + cell * (i / 3) + inset),
                    Size(cell - 2 * inset, cell - 2 * inset), style = Stroke(3.dp.toPx()),
                )
            }
        }
        if (centre != null) drawCircle(StickerColors.of(centre), radius = cell * 0.42f, center = Offset(left + cell * 1.5f, top + cell * 1.5f), style = Stroke(4.dp.toPx()))
        live?.forEachIndexed { i, color ->
            val c = Offset(left + cell * (i % 3 + 0.5f), top + cell * (i / 3 + 0.5f))
            drawCircle(Color.Black, radius = cell * 0.17f, center = c)
            drawCircle(color.toColor(), radius = cell * 0.14f, center = c)
        }
    }
}

/** Fixed, not a theme colour: drawn on the camera image in both themes. */
private val STICKER_GREEN = Color(0xFF4CAF50)

/** What [ScanContent] assumes without a camera: every cell a sticker. */
private val CUBE_IN_VIEW = FrameSampler.GridCheck(List(9) { 100.0 }, List(9) { true })

/** The captured face as the camera saw it: nine large tiles in the grid over a dimmed preview. */
@Composable
private fun ReviewOverlay(samples: List<Rgb>, modifier: Modifier) {
    BoxWithConstraints(modifier.background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
        val cell = min(maxWidth, maxHeight) * FrameSampler.GRID_SIZE / 3
        Column {
            for (row in 0 until 3) {
                Row {
                    for (col in 0 until 3) {
                        Box(Modifier.size(cell).padding(4.dp).clip(RoundedCornerShape(8.dp)).background(samples[row * 3 + col].toColor()))
                    }
                }
            }
        }
    }
}


/**
 * Which faces are done, as six marks on a dark pill: a done face filled with its centre as the camera
 * saw it ([centre]), the face being scanned ringed, the rest empty; no count beside, the marks are
 * it. A one-face rescan ([only]) shows just that face and its name. [done] is read again whenever
 * [count] changes.
 */
@Composable
private fun Progress(done: (FaceView) -> Boolean, centre: (FaceView) -> Rgb?, count: Int, only: FaceView?) {
    Row(
        Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val views = only?.let(::listOf) ?: FaceView.entries
        val doneViews = views.filter { count >= 0 && done(it) }
        views.indices.forEach { i ->
            val view = doneViews.getOrNull(i)
            val current = view == null && i == doneViews.size
            val mark = when {
                view != null -> stringResource(Res.string.scan_captured)
                current -> stringResource(Res.string.scan_pip_current)
                else -> stringResource(Res.string.scan_pip_empty)
            }
            val seen = view?.let { centre(it)?.toColor() ?: StickerColors.of(it.centreColor()) }
            val shape = MaterialTheme.shapes.extraSmall
            Box(
                Modifier.size(22.dp).clip(shape)
                    .then(if (seen != null) Modifier.background(seen) else Modifier)
                    .border(2.5.dp, seen ?: if (current) Color.White else Color.White.copy(alpha = 0.4f), shape)
                    .semantics { contentDescription = mark },
            )
        }
        if (only != null) Text(stringResource(faceName(only)), color = Color.White, style = MaterialTheme.typography.titleSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionScaffold(onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.scan_title)) },
                navigationIcon = { Box(Modifier.padding(horizontal = 8.dp)) { BackButton(onBack) } },
            )
        },
    ) { padding -> Box(Modifier.padding(padding)) { content() } }
}

private fun Rgb.toColor() = Color(r, g, b)

/** A screen frame this late is a stutter worth logging. */
private const val UI_STALL_MILLIS = 150L

/** How long a captured face waits before it is accepted by itself. */
private const val AUTO_ACCEPT_MILLIS = 2_000
