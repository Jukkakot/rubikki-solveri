package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Validity

/** What the latest frame meant for the scan. */
sealed interface ScanEvent {
    /** Nothing in the grid that looks like the asked face yet (or a face is under review). */
    data object Waiting : ScanEvent

    /** The asked face is steady; [progress] (0…1) of the hold time has passed. */
    data class Holding(val progress: Float) : ScanEvent

    /** The centre is [seen] but the face asked for has centre [expected]. */
    data class WrongFace(val expected: CubeColor, val seen: CubeColor) : ScanEvent

    /** [face] was captured and awaits [ScanSession.accept] or [ScanSession.retake]. */
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
 * The six-face scan, frame by frame. A face is captured when its readings have agreed and shown the
 * right centre for [holdMillis] and at least [minFrames] frames; the captured samples are the
 * per-cell median of those frames. A captured face is under review until [accept] or [retake].
 */
class ScanSession(
    private val scheme: ColorScheme = ColorScheme.STANDARD,
    private val holdMillis: Long = HOLD_MILLIS,
    private val minFrames: Int = MIN_FRAMES,
) {
    private val captured = arrayOfNulls<List<Rgb>>(FaceView.entries.size)
    private val streak = ArrayList<List<Rgb>>()
    private var streakColors: List<CubeColor>? = null
    private var streakStart = 0L

    var index: Int = 0
        private set

    val current: FaceView? get() = FaceView.entries.getOrNull(index)

    val isDone: Boolean get() = index >= FaceView.entries.size

    /** The captured face waiting for confirmation, and its samples. */
    var review: Pair<FaceView, List<Rgb>>? = null
        private set

    /** Live colours of the latest frame, for the dots on the grid. */
    var live: List<CubeColor>? = null
        private set

    /** Latest raw samples, for the capture button. */
    var latest: List<Rgb>? = null
        private set

    /** Confirmed readings so far per face (null = not yet). */
    fun capturedSamples(view: FaceView): List<Rgb>? = captured[view.ordinal]

    /** Handles one frame of grid readings taken at [nowMillis]. */
    fun onFrame(samples: List<Rgb>, nowMillis: Long): ScanEvent {
        require(samples.size == 9)
        if (review != null) return ScanEvent.Waiting
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
            streakStart = nowMillis
        }
        streak += samples
        val held = nowMillis - streakStart
        if (streak.size < minFrames || held < holdMillis) {
            val progress = if (holdMillis <= 0) streak.size.toFloat() / minFrames else held.toFloat() / holdMillis
            return ScanEvent.Holding(progress.coerceIn(0f, 1f))
        }
        return capture(medianOf(streak))
    }

    /** Captures the latest frame at once (the capture button). */
    fun captureNow(): ScanEvent = if (review != null) ScanEvent.Waiting else latest?.let { capture(it) } ?: ScanEvent.Waiting

    /** Keeps the face under review and moves on to the next one. */
    fun accept() {
        val (view, samples) = review ?: return
        captured[view.ordinal] = samples
        index++
        review = null
        resetStreak()
    }

    /** Drops the face under review; the same face is asked for again. */
    fun retake() {
        review = null
        resetStreak()
    }

    /** Goes back to scan the previous confirmed face again. */
    fun redo() {
        review = null
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
        review = view to samples
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
        /** How long a face must stay steady before it is captured. */
        const val HOLD_MILLIS = 1_500L
        const val MIN_FRAMES = 3
    }
}
