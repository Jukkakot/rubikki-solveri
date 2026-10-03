package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

/**
 * The last scan, for the check after an unsure scan: the grid pictures per face, the 54 camera
 * readings, and a face just rescanned on its own (waiting for the check to take it). In memory only:
 * after the app is restarted there is nothing. Snapshot state, so the check redraws when it changes.
 */
object LastScan {
    var pictures: Map<Face, IntArray> by mutableStateOf(emptyMap())

    var readings: List<Rgb>? by mutableStateOf(null)

    var rescanned: Pair<FaceView, List<Rgb>>? by mutableStateOf(null)
}
