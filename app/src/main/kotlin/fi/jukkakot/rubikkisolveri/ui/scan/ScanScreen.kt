package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEvent
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.cube.scan.ScanSession
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
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
        var cameraFailed by remember { mutableStateOf(false) }
        ScanContent(
            frames = frames,
            torch = torch,
            onTorch = { torch = it },
            onBack = onBack,
            onManual = onManual,
            onResult = onResult,
            cameraFailed = cameraFailed,
            preview = { modifier ->
                CameraPreview(
                    torch = torch,
                    onSamples = { frames.tryEmit(it) },
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
    preview: @Composable (Modifier) -> Unit,
) {
    val session = remember { ScanSession(holdMillis = holdMillis) }
    var event by remember { mutableStateOf<ScanEvent>(ScanEvent.Waiting) }
    var live by remember { mutableStateOf<List<CubeColor>?>(null) }
    var index by remember { mutableIntStateOf(0) }
    var review by remember { mutableStateOf<List<CubeColor>?>(null) }
    var lastCaptured by remember { mutableStateOf<FaceView?>(null) }
    val haptics = LocalHapticFeedback.current

    fun handle(e: ScanEvent) {
        event = e
        live = session.live
        index = session.index
        if (e is ScanEvent.Captured) {
            review = session.review?.second?.map(ColorClassifier::live)
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    fun accept() {
        val face = session.review?.first ?: return
        session.accept()
        review = null
        lastCaptured = face
        index = session.index
        event = ScanEvent.Waiting
        AppLog.info(
            Evt.SCAN_FACE, null,
            "face" to face.face.name,
            "rgb" to session.capturedSamples(face)?.joinToString(",") { it.toHex() },
            "live" to session.live?.joinToString("") { it.letter.toString() },
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
            if (!session.isDone && session.review == null) handle(session.onFrame(samples, System.nanoTime() / 1_000_000))
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
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.scan_face_title, stringResource(faceName(view)), index + 1),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(holdHint(view), style = MaterialTheme.typography.bodyLarge)
            Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
                if (cameraFailed) {
                    Text(stringResource(R.string.scan_camera_error), color = Color.White, modifier = Modifier.align(Alignment.Center))
                } else {
                    preview(Modifier.fillMaxSize())
                }
                val read = review
                if (read == null) {
                    GridOverlay(live, view.centreColor(), Modifier.fillMaxSize())
                } else {
                    ReviewOverlay(read, Modifier.fillMaxSize())
                }
            }
            val holding = (event as? ScanEvent.Holding)?.progress ?: 0f
            LinearProgressIndicator(progress = { if (review != null) 1f else holding }, modifier = Modifier.fillMaxWidth())
            Text(
                if (review != null) stringResource(R.string.scan_review) else statusText(event, live),
                style = MaterialTheme.typography.titleMedium,
            )
            lastCaptured?.let {
                Text(stringResource(R.string.scan_captured, stringResource(faceName(it))), style = MaterialTheme.typography.bodyMedium)
            }
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
}

@Composable
private fun statusText(event: ScanEvent, live: List<CubeColor>?): String = when (event) {
    is ScanEvent.WrongFace -> stringResource(R.string.scan_status_wrong, stringResource(colorName(event.expected)))
    else -> if (live == null) stringResource(R.string.scan_status_align) else stringResource(R.string.scan_status_hold)
}

/** The grid as on [FrameSampler]: a centred square, with a dot of the live colour in each cell. */
@Composable
private fun GridOverlay(live: List<CubeColor>?, expectedCentre: CubeColor, modifier: Modifier) {
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
            drawCircle(StickerColors.of(color), radius = cell * 0.14f, center = c)
        }
    }
}

/** The captured face as read: nine large tiles in the grid over a dimmed preview. */
@Composable
private fun ReviewOverlay(colors: List<CubeColor>, modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Color.Black.copy(alpha = 0.6f))
        val side = FrameSampler.GRID_SIZE * size.minDimension
        val left = (size.width - side) / 2
        val top = (size.height - side) / 2
        val cell = side / 3
        val gap = 4.dp.toPx()
        colors.forEachIndexed { i, color ->
            drawRoundRect(
                StickerColors.of(color),
                topLeft = Offset(left + cell * (i % 3) + gap, top + cell * (i / 3) + gap),
                size = Size(cell - 2 * gap, cell - 2 * gap),
                cornerRadius = CornerRadius(8.dp.toPx()),
            )
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
