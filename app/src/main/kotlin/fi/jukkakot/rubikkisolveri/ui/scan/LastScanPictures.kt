package fi.jukkakot.rubikkisolveri.ui.scan

import fi.jukkakot.rubikkisolveri.cube.Face

/**
 * The grid pictures of the last finished scan, per face, so the check after an unsure scan can show
 * what the camera saw. In memory only: after the app is restarted there are none.
 */
object LastScanPictures {
    @Volatile
    var byFace: Map<Face, IntArray> = emptyMap()
}
