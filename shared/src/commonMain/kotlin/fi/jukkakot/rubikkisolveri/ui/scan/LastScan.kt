package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch

/**
 * The last scan, for the check after an unsure scan: the grid pictures per face, the 54 camera
 * readings, and a face just rescanned on its own (waiting for the check to take it). In memory only:
 * after the app is restarted there is nothing. Snapshot state, so the check redraws when it changes.
 */
object LastScan {
    var pictures: Map<Face, IntArray> by mutableStateOf(emptyMap())

    var readings: List<Rgb>? by mutableStateOf(null)

    var rescanned: Pair<FaceView, List<Rgb>>? by mutableStateOf(null)

    /** Turns [face]'s picture [quarterTurns] clockwise (a rescanned face once the check knows how it was held). */
    fun turnPicture(face: Face, quarterTurns: Int, size: Int = FrameSampler.PICTURE_SIZE) {
        val picture = pictures[face] ?: return
        pictures = pictures + (face to rotatePicture(picture, size, quarterTurns))
    }
}

/** A square ARGB [picture] of [size]² pixels turned [quarterTurns] clockwise, as [RotationSearch.turned] turns readings. */
fun rotatePicture(picture: IntArray, size: Int, quarterTurns: Int): IntArray {
    var out = picture
    repeat(quarterTurns.mod(4)) {
        val src = out
        out = IntArray(size * size) { i -> src[(size - 1 - i % size) * size + i / size] }
    }
    return out
}
