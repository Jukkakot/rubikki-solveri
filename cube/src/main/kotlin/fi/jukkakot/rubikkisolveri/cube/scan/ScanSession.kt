package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Validity

/** What the latest frame meant for the scan. */
sealed interface ScanEvent {
    /** Nothing in the grid that looks like the asked face yet, or still settling. */
    data object Waiting : ScanEvent

    /** The centre is [seen] but the face asked for has centre [expected]. */
    data class WrongFace(val expected: CubeColor, val seen: CubeColor) : ScanEvent

    /** [face] was captured; the session moved on (or is done). */
    data class Captured(val face: FaceView) : ScanEvent
}

/** The finished scan: the colours, which stickers to double-check, and whether it can be solved. */
data class ScanOutcome(val editor: CubeEditor, val uncertain: Set<Int>, val validity: Validity) {
    /** Valid and no doubtful sticker: go straight to the solution. */
    val isConfident: Boolean get() = validity.isValid && uncertain.isEmpty()

    /** Stickers to mark for the user. */
    val marked: Set<Int> get() = uncertain + validity.markedStickers
}

/**
 * The six-face scan, frame by frame. A face is captured when [stableFrames] readings in a row agree
 * and show the right centre; the stored samples are the per-cell median of those frames.
 */
class ScanSession(
    private val scheme: ColorScheme = ColorScheme.STANDARD,
    private val stableFrames: Int = STABLE_FRAMES,
) {
    private val captured = arrayOfNulls<List<Rgb>>(FaceView.entries.size)
    private val streak = ArrayList<List<Rgb>>()
    private var streakColors: List<CubeColor>? = null

    var index: Int = 0
        private set

    val current: FaceView? get() = FaceView.entries.getOrNull(index)

    val isDone: Boolean get() = index >= FaceView.entries.size

    /** Live colours of the latest frame, for the dots on the grid. */
    var live: List<CubeColor>? = null
        private set

    /** Latest raw samples, for the capture button. */
    var latest: List<Rgb>? = null
        private set

    /** Captured readings so far per face (null = not yet). */
    fun capturedSamples(view: FaceView): List<Rgb>? = captured[view.ordinal]

    fun onFrame(samples: List<Rgb>): ScanEvent {
        require(samples.size == 9)
        val view = current ?: return ScanEvent.Waiting
        latest = samples
        val colors = samples.map(ColorClassifier::live)
        live = colors
        val expected = view.centreColor(scheme)
        if (colors[4] != expected) {
            resetStreak()
            return ScanEvent.WrongFace(expected, colors[4])
        }
        if (colors != streakColors) {
            resetStreak()
            streakColors = colors
        }
        streak += samples
        if (streak.size < stableFrames) return ScanEvent.Waiting
        return capture(medianOf(streak))
    }

    /** Captures the latest frame at once (the capture button). */
    fun captureNow(): ScanEvent = latest?.let { capture(it) } ?: ScanEvent.Waiting

    /** Goes back to scan the previous face again. */
    fun redo() {
        if (index > 0) {
            index--
            captured[index] = null
        }
        resetStreak()
    }

    fun outcome(): ScanOutcome {
        check(isDone) { "Scan not finished" }
        val samples = FaceView.entries.sortedBy { it.face.ordinal }.flatMap { captured[it.ordinal]!! }
        val classification = ColorClassifier.classify(samples, scheme)
        val editor = CubeEditor(classification.colors, scheme)
        val validity = CubeCheck.validity(editor.toCube()!!, scheme)
        return ScanOutcome(editor, classification.uncertain(), validity)
    }

    private fun capture(samples: List<Rgb>): ScanEvent {
        val view = current ?: return ScanEvent.Waiting
        captured[view.ordinal] = samples
        index++
        resetStreak()
        return ScanEvent.Captured(view)
    }

    private fun resetStreak() {
        streak.clear()
        streakColors = null
    }

    private fun medianOf(frames: List<List<Rgb>>): List<Rgb> = (0 until 9).map { cell ->
        Rgb(
            FrameSampler.median(frames.map { it[cell].r }),
            FrameSampler.median(frames.map { it[cell].g }),
            FrameSampler.median(frames.map { it[cell].b }),
        )
    }

    companion object {
        const val STABLE_FRAMES = 6
    }
}
