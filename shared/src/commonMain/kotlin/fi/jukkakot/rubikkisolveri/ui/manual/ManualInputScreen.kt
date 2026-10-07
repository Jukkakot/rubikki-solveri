package fi.jukkakot.rubikkisolveri.ui.manual

import org.jetbrains.compose.resources.StringResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.FilterQuality
import fi.jukkakot.rubikkisolveri.ui.argbToImageBitmap
import androidx.compose.ui.unit.min
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import fi.jukkakot.rubikkisolveri.ui.common.BigButton
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanCheck
import fi.jukkakot.rubikkisolveri.cube.scan.Verdict
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Validity
import fi.jukkakot.rubikkisolveri.ui.common.colorName
import fi.jukkakot.rubikkisolveri.ui.common.faceName
import fi.jukkakot.rubikkisolveri.ui.common.holdHint
import fi.jukkakot.rubikkisolveri.ui.common.markedStickers
import fi.jukkakot.rubikkisolveri.ui.common.validityMessage
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.viewFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputScreen(
    onBack: () -> Unit,
    onValid: (Cube) -> Unit,
    initial: CubeEditor = CubeEditor.empty(),
    initialMarked: Set<Int> = emptySet(),
    title: StringResource = Res.string.manual_title,
    note: StringResource? = null,
    pictures: Map<Face, IntArray> = emptyMap(),
    onScanAgain: (() -> Unit)? = null,
    check: ScanCheck? = null,
    onScanFace: ((FaceView) -> Unit)? = null,
    rescanned: Pair<FaceView, List<Rgb>>? = null,
    onRescanUsed: () -> Unit = {},
    onReadings: (List<Rgb>) -> Unit = {},
    onRescanTurned: (Face, Int) -> Unit = { _, _ -> },
    confident: Boolean = false,
) {
    var encoded by rememberSaveable { mutableStateOf(initial.encode()) }
    // The face-by-face check of a scan (null for plain manual input); its readings are not saved
    // across rotation, the caller hands them in again with [check].
    var checkText by rememberSaveable { mutableStateOf(check?.encode()) }
    var readings by remember { mutableStateOf(check?.readings) }
    val scanCheck = checkText?.let { ScanCheck.decode(it, readings) }
    val editor = scanCheck?.editor ?: CubeEditor.decode(encoded) ?: CubeEditor.empty()
    // Marks handed over by a scan; they stay until the user changes the cube.
    var handedMarks by rememberSaveable { mutableStateOf(initialMarked.joinToString(",")) }
    var faceIndex by rememberSaveable {
        mutableIntStateOf(
            check?.nextUnchecked()?.ordinal
                ?: initialMarked.minOrNull()?.let { FaceView.of(Face.entries[it / 9]).ordinal } ?: 0,
        )
    }
    // The faces named by the last "cannot be right" verdict (ordinals), shown until the next verdict.
    var verdictFaces by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val view = FaceView.entries[faceIndex]
    var colorIndex by rememberSaveable { mutableIntStateOf(CubeColor.WHITE.ordinal) }
    val selectedColor = CubeColor.entries[colorIndex]
    var validity by remember { mutableStateOf<Validity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    // The first paint, "looks right" or "scan again" on the check: its instruction has done its job.
    var acted by rememberSaveable { mutableStateOf(false) }
    val viewState = remember { CubeViewState(viewFor(view)) }
    LaunchedEffect(view) { viewState.animateTo(viewFor(view)) }

    fun update(next: CubeEditor) {
        encoded = next.encode()
        validity = null
        handedMarks = ""
    }

    fun setCheck(next: ScanCheck) {
        checkText = next.encode()
        readings = next.readings
    }

    fun paint(index: Int) {
        acted = true
        if (scanCheck != null) setCheck(scanCheck.paint(index, selectedColor)) else update(editor.paint(index, selectedColor))
    }

    // Every face checked: a solvable cube opens its solution, otherwise the faces to look at again.
    fun judge(next: ScanCheck) {
        scope.launch {
            withFrameNanos { } // the spinner first: in the browser the check runs on the one thread
            val result = withContext(Dispatchers.Default) { next.verdict() }
            when (result) {
                is Verdict.Solvable -> {
                    AppLog.info(Evt.SCAN_CHECK, null, "verdict" to "solvable", "cube" to next.editor.encode())
                    setCheck(next)
                    verdictFaces = ""
                    onValid(result.cube)
                }
                is Verdict.Impossible -> {
                    AppLog.info(
                        Evt.SCAN_CHECK, null,
                        "verdict" to "impossible",
                        "validity" to result.validity.toString(),
                        "faces" to result.faces.joinToString(",") { it.face.name },
                        "marked" to result.marked.sorted().joinToString(","),
                        "cube" to next.editor.encode(),
                    )
                    val after = next.apply(result)
                    setCheck(after)
                    verdictFaces = result.faces.joinToString(",") { it.ordinal.toString() }
                    after.nextUnchecked()?.let { faceIndex = it.ordinal }
                }
            }
        }
    }

    // "Looks right": the next unchecked face, or, after the last one, the verdict.
    fun lookRight() {
        val current = scanCheck ?: return
        acted = true
        val next = current.lookRight(view)
        if (next.unchecked.isNotEmpty()) {
            setCheck(next)
            faceIndex = next.nextUnchecked(view)!!.ordinal
            return
        }
        judge(next)
    }

    // A scan that cannot be right without any doubtful sticker starts with every face checked: say
    // at once which faces to look at.
    LaunchedEffect(Unit) {
        if (!confident && check != null && check.unchecked.isEmpty() && verdictFaces.isEmpty()) judge(check)
    }

    // A face rescanned on its own comes back: it replaces that face in the check.
    LaunchedEffect(rescanned) {
        val (face, samples) = rescanned ?: return@LaunchedEffect
        val current = scanCheck
        if (current?.readings != null) {
            val (next, turns) = current.replaceFace(face, samples)
            onRescanTurned(face.face, turns)
            setCheck(next)
            next.readings?.let(onReadings)
            faceIndex = face.ordinal
            AppLog.info(Evt.SCAN_CHECK, null, "rescan" to face.face.name, "rotation" to turns, "colors" to next.editor.encode())
        }
        onRescanUsed()
    }

    val marked = scanCheck?.marks ?: (markedStickers(validity) + handedMarks.split(',').mapNotNull { it.toIntOrNull() })
    val verdictNames = verdictFaces.split(',').mapNotNull { it.toIntOrNull() }.map { stringResource(faceName(FaceView.entries[it])) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.manual_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        onScanAgain?.let { scanAgain ->
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.check_scan_whole)) },
                                onClick = {
                                    menuOpen = false
                                    scanAgain()
                                },
                            )
                        }
                        // Clearing or a solved cube make no sense while checking a scan.
                        if (scanCheck == null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.manual_clear)) },
                                onClick = {
                                    menuOpen = false
                                    update(editor.clear())
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.manual_fill_solved)) },
                                onClick = {
                                    menuOpen = false
                                    update(CubeEditor.of(Cube.solved()))
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            // Palette and actions always in view: the screen never scrolls.
            Surface(tonalElevation = 3.dp) {
                Column(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    validity?.let { result ->
                        Text(
                            validityMessage(result),
                            color = if (result.isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (verdictNames.isNotEmpty()) {
                        Text(
                            stringResource(Res.string.check_impossible, joinNames(verdictNames)),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Palette(editor, selectedColor, onSelect = { colorIndex = it.ordinal })
                    if (scanCheck != null && confident && onScanAgain != null) {
                        // A sure scan, opened again from its solution: scan again or back to the solution.
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = onScanAgain, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                                Text(stringResource(Res.string.check_scan_whole), maxLines = 2)
                            }
                            Button(onClick = ::lookRight, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                                Text(stringResource(Res.string.check_looks_right), maxLines = 2)
                            }
                        }
                    } else if (scanCheck != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onScanFace != null && scanCheck.readings != null) {
                                OutlinedButton(onClick = { acted = true; onScanFace(view) }, modifier = Modifier.weight(1f)) {
                                    Text(stringResource(Res.string.check_rescan_face), maxLines = 1)
                                }
                            }
                            Button(onClick = ::lookRight, modifier = Modifier.weight(1f)) {
                                Text(stringResource(Res.string.check_looks_right), maxLines = 1)
                            }
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        // ‹ › through the faces and ✓ as the main action; their names stay for screen readers.
                        RoundIconButton(onClick = { view.previous?.let { faceIndex = it.ordinal } }, enabled = view.previous != null, size = 56.dp) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(Res.string.manual_previous))
                        }
                        RoundIconButton(onClick = { view.next?.let { faceIndex = it.ordinal } }, enabled = view.next != null, size = 56.dp) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(Res.string.manual_next))
                        }
                        BigButton(
                            onClick = {
                                val cube = editor.toCube() ?: return@BigButton
                                val result = CubeCheck.validity(cube)
                                validity = result
                                if (result.isValid) onValid(cube)
                            },
                            enabled = editor.isComplete,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(Res.string.manual_check), modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // One line until the first action on the check; the map's ticks show the faces left.
            if (note != null && !acted) {
                Text(stringResource(note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary, maxLines = 1)
            }
            Row(
                Modifier.fillMaxWidth().height(110.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniNet(editor, view, marked, checked = scanCheck?.checked.orEmpty(), onSelect = { faceIndex = it.ordinal })
                Cube3D(
                    colors = editor.colors.map(StickerColors::of),
                    viewState = viewState,
                    marked = marked,
                    onTap = { sticker -> faceIndex = FaceView.of(Face.entries[sticker / 9]).ordinal },
                    description = stringResource(Res.string.manual_preview),
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
            Text(
                stringResource(Res.string.manual_face_title, stringResource(faceName(view)), faceIndex + 1),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(holdHint(view), style = MaterialTheme.typography.bodySmall)
            // The face fills what is left; when checking a scan, the camera's picture sits beside it.
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val picture = pictures[view.face]
                if (picture == null) {
                    FaceGrid(editor, view, marked, onTap = ::paint, Modifier.size(min(maxWidth, maxHeight)))
                } else {
                    val side = min((maxWidth - 12.dp) / 2, maxHeight - 24.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(Res.string.check_picture), style = MaterialTheme.typography.labelLarge)
                            CameraPicture(picture, Modifier.size(side))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(Res.string.check_colors), style = MaterialTheme.typography.labelLarge)
                            FaceGrid(editor, view, marked, onTap = ::paint, Modifier.size(side))
                        }
                    }
                }
            }
        }
    }
}

/** A grid picture from the scan ([FrameSampler.PICTURE_SIZE] square ARGB), drawn sharp. */
@Composable
private fun CameraPicture(argb: IntArray, modifier: Modifier) {
    val size = FrameSampler.PICTURE_SIZE
    val bitmap = remember(argb) { argbToImageBitmap(argb, size, size) }
    Image(
        bitmap = bitmap,
        contentDescription = stringResource(Res.string.check_picture),
        filterQuality = FilterQuality.None,
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
    )
}

@Composable
private fun FaceGrid(editor: CubeEditor, view: FaceView, marked: Set<Int>, onTap: (Int) -> Unit, modifier: Modifier) {
    val name = stringResource(faceName(view))
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(StickerColors.PLASTIC).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (row in 0 until 3) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 3) {
                    val n = row * 3 + col
                    val index = view.face.ordinal * 9 + n
                    val description = stringResource(Res.string.manual_cell, name, n + 1)
                    val state = editor[index]?.let { stringResource(colorName(it)) } ?: stringResource(Res.string.color_none)
                    Box(
                        Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(8.dp))
                            .background(StickerColors.of(editor[index]))
                            .then(if (index in marked) Modifier.border(BorderStroke(4.dp, StickerColors.MARK), RoundedCornerShape(8.dp)) else Modifier)
                            .clickable { onTap(index) }
                            .semantics {
                                contentDescription = description
                                stateDescription = state
                            },
                    )
                }
            }
        }
    }
}


@Composable
private fun Palette(editor: CubeEditor, selected: CubeColor, onSelect: (CubeColor) -> Unit) {
    val counts = editor.counts()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (color in CubeColor.entries) {
            val count = counts.getValue(color)
            val name = stringResource(colorName(color))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(StickerColors.of(color))
                        .border(
                            if (color == selected) BorderStroke(4.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            CircleShape,
                        )
                        .clickable { onSelect(color) }
                        .semantics {
                            contentDescription = name
                            this.selected = color == selected
                        },
                )
                Text(
                    stringResource(Res.string.manual_count, count),
                    color = if (count > 9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** "Etupuoli ja Yläpuoli", "Etupuoli, Vasen puoli ja Yläpuoli". */
@Composable
private fun joinNames(names: List<String>): String {
    if (names.size <= 1) return names.joinToString()
    return names.dropLast(1).joinToString(", ") + " " + stringResource(Res.string.check_and) + " " + names.last()
}

/**
 * All six faces as a small net (top above front, left-front-right-back in a row, bottom below);
 * [checked] faces (in the check of a scan) carry a check mark.
 */
@Composable
private fun MiniNet(editor: CubeEditor, current: FaceView, marked: Set<Int>, checked: Set<FaceView> = emptySet(), onSelect: (FaceView) -> Unit) {
    val cell = 9.dp
    val faceSize = cell * 3 + 4.dp
    @Composable
    fun face(view: FaceView) {
        val name = if (view in checked) {
            stringResource(Res.string.check_face_checked, stringResource(faceName(view)))
        } else {
            stringResource(faceName(view))
        }
        Box(
            Modifier.size(faceSize)
                .border(if (view == current) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent))
                .clickable { onSelect(view) }
                .semantics { contentDescription = name },
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.padding(2.dp)) {
                for (row in 0 until 3) {
                    Row {
                        for (col in 0 until 3) {
                            val index = view.face.ordinal * 9 + row * 3 + col
                            Box(
                                Modifier.size(cell).padding(0.5.dp)
                                    .background(if (index in marked) StickerColors.MARK else StickerColors.of(editor[index])),
                            )
                        }
                    }
                }
            }
            if (view in checked) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(2.dp),
                )
            }
        }
    }
    Column {
        Row { Spacer(Modifier.width(faceSize)); face(FaceView.TOP) }
        Row { face(FaceView.LEFT); face(FaceView.FRONT); face(FaceView.RIGHT); face(FaceView.BACK) }
        Row { Spacer(Modifier.width(faceSize)); face(FaceView.BOTTOM) }
    }
}

/** How long the check of a sure scan waits before it opens the solution by itself. */
