package fi.jukkakot.rubikkisolveri.ui.manual

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.R
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
    title: Int = R.string.manual_title,
) {
    var encoded by rememberSaveable { mutableStateOf(initial.encode()) }
    val editor = CubeEditor.decode(encoded) ?: CubeEditor.empty()
    var faceIndex by rememberSaveable { mutableIntStateOf(0) }
    val view = FaceView.entries[faceIndex]
    var colorIndex by rememberSaveable { mutableIntStateOf(CubeColor.WHITE.ordinal) }
    val selectedColor = CubeColor.entries[colorIndex]
    var validity by remember { mutableStateOf<Validity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val viewState = remember { CubeViewState(viewFor(view)) }
    LaunchedEffect(view) { viewState.animateTo(viewFor(view)) }

    fun update(next: CubeEditor) {
        encoded = next.encode()
        validity = null
    }

    val marked = markedStickers(validity)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.manual_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.manual_clear)) },
                            onClick = {
                                menuOpen = false
                                update(editor.clear())
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.manual_fill_solved)) },
                            onClick = {
                                menuOpen = false
                                update(CubeEditor.of(Cube.solved()))
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniNet(editor, view, marked, onSelect = { faceIndex = it.ordinal })
                Cube3D(
                    colors = editor.colors.map(StickerColors::of),
                    viewState = viewState,
                    marked = marked,
                    onTap = { sticker -> faceIndex = FaceView.of(Face.entries[sticker / 9]).ordinal },
                    description = stringResource(R.string.manual_preview),
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                )
            }
            Text(
                stringResource(R.string.manual_face_title, stringResource(faceName(view)), faceIndex + 1),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(holdHint(view), style = MaterialTheme.typography.bodyLarge)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FaceGrid(
                    editor = editor,
                    view = view,
                    marked = marked,
                    onTap = { index -> update(editor.paint(index, selectedColor)) },
                )
            }
            Palette(editor, selectedColor, onSelect = { colorIndex = it.ordinal })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { view.previous?.let { faceIndex = it.ordinal } }, enabled = view.previous != null, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.manual_previous))
                }
                OutlinedButton(onClick = { view.next?.let { faceIndex = it.ordinal } }, enabled = view.next != null, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.manual_next))
                }
            }
            Button(
                onClick = {
                    val cube = editor.toCube() ?: return@Button
                    val result = CubeCheck.validity(cube)
                    validity = result
                    if (result.isValid) onValid(cube)
                },
                enabled = editor.isComplete,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.manual_check))
            }
            validity?.let { result ->
                Card(
                    colors = if (result.isValid) {
                        CardDefaults.cardColors()
                    } else {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(validityMessage(result), modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FaceGrid(editor: CubeEditor, view: FaceView, marked: Set<Int>, onTap: (Int) -> Unit) {
    val name = stringResource(faceName(view))
    Column(
        Modifier.fillMaxWidth(0.8f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(StickerColors.PLASTIC).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (row in 0 until 3) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 3) {
                    val n = row * 3 + col
                    val index = view.face.ordinal * 9 + n
                    val description = stringResource(R.string.manual_cell, name, n + 1)
                    val state = editor[index]?.let { stringResource(colorName(it)) } ?: stringResource(R.string.color_none)
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
                            if (color == selected) BorderStroke(4.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color.Gray),
                            CircleShape,
                        )
                        .clickable { onSelect(color) }
                        .semantics {
                            contentDescription = name
                            this.selected = color == selected
                        },
                )
                Text(
                    stringResource(R.string.manual_count, count),
                    color = if (count > 9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** All six faces as a small net (top above front, left-front-right-back in a row, bottom below). */
@Composable
private fun MiniNet(editor: CubeEditor, current: FaceView, marked: Set<Int>, onSelect: (FaceView) -> Unit) {
    val cell = 9.dp
    val faceSize = cell * 3 + 4.dp
    @Composable
    fun face(view: FaceView) {
        val name = stringResource(faceName(view))
        Column(
            Modifier.size(faceSize)
                .border(if (view == current) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.dp, Color.Transparent))
                .clickable { onSelect(view) }
                .semantics { contentDescription = name }
                .padding(2.dp),
        ) {
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
    }
    Column {
        Row { Spacer(Modifier.width(faceSize)); face(FaceView.TOP) }
        Row { face(FaceView.LEFT); face(FaceView.FRONT); face(FaceView.RIGHT); face(FaceView.BACK) }
        Row { Spacer(Modifier.width(faceSize)); face(FaceView.BOTTOM) }
    }
}
