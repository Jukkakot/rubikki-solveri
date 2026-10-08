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
     * (both averaged since the last snapshot), pictures read per second, the torch, how many steps
     * darker the camera is set, whether the browser's worker found the faces (null on the phone), the
     * stall if any.
     */
    fun snapshot(
        state: VideoScanState,
        facesPerFrame: Double,
        finderMs: Double,
        fps: Double = 0.0,
        torch: Boolean = false,
        darker: Int = 0,
        worker: Boolean? = null,
        centres: String = "",
        scanMs: Double = 0.0,
        paintMs: Double = 0.0,
        showMs: Double = 0.0,
    ): Array<Pair<String, Any?>> = arrayOf(
        "kind" to "snapshot",
        "known" to Face.entries.joinToString(" ") { f -> "${f.name}${(0 until 9).count { state.stickers[f.ordinal * 9 + it] != null }}" },
        "cube" to state.stickers.joinToString("") { it?.letter?.toString() ?: "?" },
        // At most `VideoScan.MARGIN_LOOK` (2.5): the scan works margins out only as far as it decides with them (`scan-speed-up-4`).
        "margin" to tenths(state.clearness),
        "light" to state.brightness,
        "faces" to tenths(facesPerFrame),
        "finderMs" to finderMs.roundToInt(),
        // Per picture beside the finder's: the scan's own work, and working out and drawing the paint (not the GPU's part), `scan-speed-up`.
        "scanMs" to tenths(scanMs),
        "paintMs" to tenths(paintMs),
        // The browser's copy of each picture read, shown in the camera's place (`scan-feedback`).
        "showMs" to tenths(showMs),
        "fps" to tenths(fps),
        "torch" to torch,
        "darker" to darker,
        "worker" to worker,
        "stall" to state.stall?.name?.lowercase(),
        // The centres as the camera reads them (rules scanner), e.g. how red its orange looks.
        "centres" to centres.ifEmpty { null },
    )

    /** The sides done in [state]: the rest of the cube confirms all nine stickers ([VideoScanState.confirmed]). */
    fun doneSides(state: VideoScanState): Set<Face> = state.confirmed

    private fun tenths(x: Double): Double = (x * 10).roundToInt() / 10.0
}
