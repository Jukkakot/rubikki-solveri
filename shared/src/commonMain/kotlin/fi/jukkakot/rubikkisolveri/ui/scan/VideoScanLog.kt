package fi.jukkakot.rubikkisolveri.ui.scan

import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.math.roundToInt

/**
 * The video scan's log lines (`scan.video` with a `kind`, `video-scan-progress` design 9): a
 * snapshot every [SNAPSHOT_MILLIS] and a line per turning point (side, clear, stall, restart, leave).
 */
object VideoScanLog {
    const val SNAPSHOT_MILLIS = 2_000L

    /**
     * What the scan knows in [state]: known stickers per side ("U9 R4 …"), the cube as 54 letters
     * with `?` where not known, the smallest margin, brightness, faces per frame and finder time
     * (both averaged since the last snapshot), the stall if any.
     */
    fun snapshot(state: VideoScanState, facesPerFrame: Double, finderMs: Double): Array<Pair<String, Any?>> = arrayOf(
        "kind" to "snapshot",
        "known" to Face.entries.joinToString(" ") { f -> "${f.name}${(0 until 9).count { state.stickers[f.ordinal * 9 + it] != null }}" },
        "cube" to state.stickers.joinToString("") { it?.letter?.toString() ?: "?" },
        "margin" to tenths(state.clearness),
        "light" to state.brightness,
        "faces" to tenths(facesPerFrame),
        "finderMs" to finderMs.roundToInt(),
        "stall" to state.stall?.name?.lowercase(),
    )

    /** The sides whose nine stickers are all known in [state]. */
    fun doneSides(state: VideoScanState): Set<Face> =
        Face.entries.filter { f -> (0 until 9).all { state.stickers[f.ordinal * 9 + it] != null } }.toSet()

    private fun tenths(x: Double): Double = (x * 10).roundToInt() / 10.0
}
