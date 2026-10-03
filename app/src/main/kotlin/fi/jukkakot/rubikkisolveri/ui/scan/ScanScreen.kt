package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEvent
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.cube.scan.ScanSession
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.log.ScanPictures
import fi.jukkakot.rubikkisolveri.cube.Face
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import fi.jukkakot.rubikkisolveri.ui.common.colorName
import fi.jukkakot.rubikkisolveri.ui.common.faceName
import fi.jukkakot.rubikkisolveri.ui.common.holdHint
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow

@Composable
fun ScanScreen(onBack: () -> Unit, onManual: () -> Unit, onResult: (ScanOutcome) -> Unit) {
    CameraPermissionGate(
        alternative = stringResource(R.string.scan_manual) to onManual,
        denied = { content -> PermissionScaffold(onBack) { content() } },
    ) {
        val frames = remember {
            MutableSharedFlow<List<Rgb>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        }
        var torch by remember { mutableStateOf(false) }
        var lockExposure by remember { mutableStateOf(false) }
        var cameraFailed by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val pictures = remember { ScanPictures.of(context) }
        // The latest grid picture and whether it shows cube stickers (worked out on the camera thread).
        val latestPicture = remember { AtomicReference<Pair<IntArray, Boolean>?>(null) }
        // The picture of each face as last captured, for the check after an unsure scan.
        val facePictures = remember { HashMap<Face, IntArray>() }
        val scope = rememberCoroutineScope()
        ScanContent(
            frames = frames,
            torch = torch,
            onTorch = { torch = it },
            onBack = onBack,
            onManual = onManual,
            onResult = { outcome ->
                LastScanPictures.byFace = facePictures.toMap()
                onResult(outcome)
            },
            cameraFailed = cameraFailed,
            watchStalls = true,
            onLockExposure = { lockExposure = it },
            savePicture = { face ->
                latestPicture.get()?.first?.let { picture ->
                    facePictures[Face.valueOf(face)] = picture
                    pictures.newName(face).also { name ->
                        scope.launch(Dispatchers.IO) {
                            runCatching { pictures.write(name, picture, FrameSampler.PICTURE_SIZE) }
                                .onFailure { AppLog.logger.error(Evt.SCAN_ERROR, it, "picture") }
                        }
                    }
                }
            },
            looksLikeCube = { latestPicture.get()?.second ?: true },
            preview = { modifier ->
                CameraPreview(
                    torch = torch,
                    lockExposure = lockExposure,
                    onSamples = { frames.tryEmit(it) },
                    onPicture = { latestPicture.set(it to FrameSampler.looksLikeCube(it)) },
                    onError = { cameraFailed = true },
                    modifier = modifier,
                )
            },
        )
    }
}

/** The scan without the camera itself: [frames] are the grid readings, [preview] draws the camera. */
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
    onLockExposure: (Boolean) -> Unit = {},
    savePicture: (face: String) -> String? = { null },
    looksLikeCube: () -> Boolean = { true },
    watchStalls: Boolean = false,
    preview: @Composable (Modifier) -> Unit,
) {
    val session = remember { ScanSession(holdMillis = holdMillis) }
    var event by remember { mutableStateOf<ScanEvent>(ScanEvent.Waiting) }
    var live by remember { mutableStateOf<List<Rgb>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var review by remember { mutableStateOf<List<Rgb>?>(null) }
    var reviewHint by remember { mutableStateOf<CubeColor?>(null) }
    var lastCaptured by remember { mutableStateOf<FaceView?>(null) }
    val haptics = LocalHapticFeedback.current

    fun handle(e: ScanEvent) {
        event = e
        live = session.latest
        index = session.index
        if (e is ScanEvent.Captured) {
            review = session.review?.second
            reviewHint = session.reviewCentreLooksLike
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            val picture = runCatching { savePicture(e.face.face.name) }
                .onFailure { AppLog.logger.error(Evt.SCAN_ERROR, it, "picture") }.getOrNull()
            AppLog.info(
                Evt.SCAN_CAPTURE, null,
                "face" to e.face.face.name,
                "picture" to picture,
                "rgb" to session.review?.second?.joinToString(",") { it.toHex() },
            )
        }
    }

    fun accept() {
        val face = session.review?.first ?: return
        val hint = session.reviewCentreLooksLike
        session.accept()
        review = null
        lastCaptured = face
        index = session.index
        event = ScanEvent.Waiting
        AppLog.info(
            Evt.SCAN_FACE, null,
            "face" to face.face.name,
            "rgb" to session.capturedSamples(face)?.joinToString(",") { it.toHex() },
            "centreLooksLike" to hint?.letter,
        )
        if (session.isDone) {
            val outcome = session.outcome()
            AppLog.info(
                Evt.SCAN_DONE, null,
                "valid" to outcome.validity.isValid,
                "validity" to outcome.validity.toString(),
                "uncertain" to outcome.uncertain.size,
                "cube" to outcome.editor.encode(),
            )
            onResult(outcome)
        }
    }

    fun retake() {
        session.retake()
        review = null
        event = ScanEvent.Waiting
    }

    LaunchedEffect(session) {
        frames.collect { samples ->
            if (!session.isDone && session.review == null) handle(session.onFrame(samples, System.nanoTime() / 1_000_000, looksLikeCube()))
        }
    }
    // From the first capture on (the cube held still, the camera settled on it), keep exposure and
    // white balance fixed so every face is read alike. Scanning the front again releases it.
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

    val view = FaceView.entries.getOrNull(index) ?: FaceView.BOTTOM
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconToggleButton(checked = torch, onCheckedChange = onTorch) {
                        Icon(painterResource(R.drawable.ic_torch), contentDescription = stringResource(R.string.scan_torch))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Progress(index)
                    if (review != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = ::retake, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.scan_retake)) }
                            Button(onClick = ::accept, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.scan_accept)) }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    session.redo()
                                    index = session.index
                                    event = ScanEvent.Waiting
                                    lastCaptured = null
                                },
                                enabled = index > 0,
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.scan_redo)) }
                            Button(onClick = { handle(session.captureNow()) }, enabled = live != null, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.scan_capture))
                            }
                        }
                    }
                    TextButton(onClick = onManual) { Text(stringResource(R.string.scan_manual)) }
                }
            }
        },
    ) { padding ->
        // One screen, no scrolling: the camera takes what the texts above and the actions below leave.
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.scan_face_title, stringResource(faceName(view)), index + 1),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(holdHint(view), style = MaterialTheme.typography.bodyMedium)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.aspectRatio(3f / 4f, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(16.dp)).background(Color.Black),
                ) {
                    if (cameraFailed) {
                        Text(stringResource(R.string.scan_camera_error), color = Color.White, modifier = Modifier.align(Alignment.Center))
                    } else {
                        preview(Modifier.fillMaxSize())
                    }
                    val read = review
                    if (read == null) {
                        GridOverlay(live, view.centreColor(), Modifier.fillMaxSize())
                        val holding = (event as? ScanEvent.Holding)?.progress ?: 0f
                        OnCamera(Modifier.align(Alignment.BottomCenter)) {
                            Text(statusText(event), color = Color.White, style = MaterialTheme.typography.titleSmall)
                            LinearProgressIndicator(progress = { holding }, modifier = Modifier.fillMaxWidth())
                        }
                        lastCaptured?.let {
                            Text(
                                stringResource(R.string.scan_captured, stringResource(faceName(it))),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        ReviewOverlay(read, Modifier.fillMaxSize())
                        OnCamera(Modifier.align(Alignment.BottomCenter), scrim = false) {
                            Text(stringResource(R.string.scan_review), color = Color.White, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.scan_review_note), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            reviewHint?.let {
                                Text(
                                    stringResource(R.string.scan_review_centre, stringResource(colorName(it))),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Text on the camera view: white on a dark band, readable whatever the camera shows. */
@Composable
private fun OnCamera(modifier: Modifier, scrim: Boolean = true, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .then(if (scrim) Modifier.background(Color.Black.copy(alpha = 0.6f)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun statusText(event: ScanEvent): String = when (event) {
    is ScanEvent.PreviousFace -> stringResource(R.string.scan_status_turn)
    is ScanEvent.NoCube -> stringResource(R.string.scan_status_no_cube)
    is ScanEvent.Holding -> event.centreLooksLike
        ?.let { stringResource(R.string.scan_status_centre, stringResource(colorName(it))) }
        ?: stringResource(R.string.scan_status_hold)
    else -> stringResource(R.string.scan_status_align)
}

/** The grid as on [FrameSampler]: a centred square, with a dot of what each cell reads. */
@Composable
private fun GridOverlay(live: List<Rgb>?, expectedCentre: CubeColor, modifier: Modifier) {
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
        // The centre cell shows which colour belongs there.
        drawCircle(StickerColors.of(expectedCentre), radius = cell * 0.42f, center = Offset(left + cell * 1.5f, top + cell * 1.5f), style = Stroke(4.dp.toPx()))
        live?.forEachIndexed { i, color ->
            val c = Offset(left + cell * (i % 3 + 0.5f), top + cell * (i / 3 + 0.5f))
            drawCircle(Color.Black, radius = cell * 0.17f, center = c)
            drawCircle(color.toColor(), radius = cell * 0.14f, center = c)
        }
    }
}

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


@Composable
private fun Progress(done: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.scan_progress, done.coerceAtMost(6)), style = MaterialTheme.typography.labelLarge)
        for (view in FaceView.entries) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(StickerColors.of(view.centreColor()))
                    .border(1.dp, Color.Gray, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (view.ordinal < done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionScaffold(onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding -> Box(Modifier.padding(padding)) { content() } }
}

private fun Rgb.toColor() = Color(r, g, b)

/** A screen frame this late is a stutter worth logging. */
private const val UI_STALL_MILLIS = 150L
