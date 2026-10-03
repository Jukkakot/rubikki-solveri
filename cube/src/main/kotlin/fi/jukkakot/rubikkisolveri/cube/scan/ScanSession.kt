package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Stickers
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
    private val corrections = arrayOfNulls<Map<Int, CubeColor>>(FaceView.entries.size)
    private val streak = ArrayList<List<Rgb>>()
    private var streakColors: List<CubeColor>? = null
    private var streakStart = 0L
    private var refs = ColorClassifier.references()
    private var calibrated: Set<CubeColor> = emptySet()
    private var reviewRead: List<CubeColor> = emptyList()
    private val reviewFixed = HashMap<Int, CubeColor>()

    /** The colours shown for the face under review: as read, with the user's corrections. */
    var reviewColors: List<CubeColor>? = null
        private set

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
        val colors = samples.map { ColorClassifier.live(it, refs) }
        live = colors
        val expected = view.centreColor(scheme)
        if (!centreMatches(samples[CENTRE], expected)) {
            resetStreak()
            return ScanEvent.WrongFace(expected, colors[CENTRE])
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

    /**
     * Changes the colour of [cell] (0–8, not the centre) of the face under review to the next
     * closest colour; the user's choice is kept for the result and teaches the live reading.
     */
    fun cycle(cell: Int) {
        val (_, samples) = review ?: return
        val shown = reviewColors ?: return
        if (cell == CENTRE) return
        val ranked = ColorClassifier.ranked(samples[cell], refs)
        val next = ranked[(ranked.indexOf(shown[cell]) + 1) % ranked.size]
        if (next == reviewRead[cell]) reviewFixed.remove(cell) else reviewFixed[cell] = next
        reviewColors = shown.toMutableList().also { it[cell] = next }
    }

    /** The cells of the face under review the user has corrected, with their colours. */
    val reviewCorrections: Map<Int, CubeColor> get() = reviewFixed.toMap()

    /** Keeps the face under review (with its corrections) and moves on to the next one. */
    fun accept() {
        val (view, samples) = review ?: return
        captured[view.ordinal] = samples
        corrections[view.ordinal] = reviewFixed.toMap()
        index++
        clearReview()
        updateReferences()
        resetStreak()
    }

    /** Drops the face under review; the same face is asked for again. */
    fun retake() {
        clearReview()
        resetStreak()
    }

    /** Goes back to scan the previous confirmed face again. */
    fun redo() {
        clearReview()
        if (index > 0) {
            index--
            captured[index] = null
            corrections[index] = null
            updateReferences()
        }
        resetStreak()
    }

    fun outcome(): ScanOutcome {
        check(isDone) { "Scan not finished" }
        val samples = FaceView.entries.sortedBy { it.face.ordinal }.flatMap { captured[it.ordinal]!! }
        val fixed = FaceView.entries.flatMap { view ->
            corrections[view.ordinal].orEmpty().map { (cell, color) -> Stickers.index(view.face, cell + 1) to color }
        }.toMap()
        val classification = ColorClassifier.classify(samples, scheme, fixed)
        val editor = CubeEditor(classification.colors, scheme)
        val validity = CubeCheck.validity(editor.toCube()!!, scheme)
        return ScanOutcome(editor, classification.uncertain(), validity)
    }

    private fun capture(samples: List<Rgb>): ScanEvent {
        val view = current ?: return ScanEvent.Waiting
        review = view to samples
        reviewRead = samples.map { ColorClassifier.live(it, refs) }.toMutableList().also { it[CENTRE] = view.centreColor(scheme) }
        reviewColors = reviewRead
        reviewFixed.clear()
        resetStreak()
        return ScanEvent.Captured(view)
    }

    /**
     * The centre is [expected] when it reads closest to it, or second closest after a colour this
     * cube has not shown yet: before its own red is known, a cube's red may read as the default
     * orange (and its white as yellow).
     */
    private fun centreMatches(centre: Rgb, expected: CubeColor): Boolean {
        val ranked = ColorClassifier.ranked(centre, refs)
        return ranked[0] == expected || (ranked[1] == expected && ranked[0] !in calibrated)
    }

    private fun clearReview() {
        review = null
        reviewColors = null
        reviewFixed.clear()
    }

    /** The cube's own centres and the user's corrections so far become the live references. */
    private fun updateReferences() {
        val known = HashMap<CubeColor, MutableList<Lab>>()
        for (view in FaceView.entries) {
            val samples = captured[view.ordinal] ?: continue
            known.getOrPut(view.centreColor(scheme)) { ArrayList() } += samples[CENTRE].toLab()
            corrections[view.ordinal]?.forEach { (cell, color) -> known.getOrPut(color) { ArrayList() } += samples[cell].toLab() }
        }
        refs = ColorClassifier.references(known)
        calibrated = known.keys.toSet()
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
        private const val CENTRE = 4
    }
}
