package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Validity
import kotlin.math.hypot

/** What the latest frame meant for the scan. */
sealed interface ScanEvent {
    /** Nothing to read yet (or a face is under review). */
    data object Waiting : ScanEvent

    /**
     * A face is steady; [progress] (0…1) of the hold time has passed. [centreLooksLike] is set when
     * the centre reads as another colour than the asked face's: a hint, not a stop.
     */
    data class Holding(val progress: Float, val centreLooksLike: CubeColor? = null) : ScanEvent

    /** The camera still sees the face accepted last: the user should turn the cube. */
    data object PreviousFace : ScanEvent

    /** The grid does not look like cube stickers (a desk, a hand): nothing is captured automatically. */
    data object NoCube : ScanEvent

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
 * The six-face scan, frame by frame. The session guides the order of the faces but the user knows
 * best which face is in view: a centre that reads as another colour is only a hint. A face is
 * captured when every cell has stayed steady for [holdMillis] and at least [minFrames] frames (the
 * captured samples are the per-cell median of those frames) and is under review until [accept] or
 * [retake]. The colours are only decided at the end, from all 54 readings together.
 */
class ScanSession(
    private val scheme: ColorScheme = ColorScheme.STANDARD,
    private val holdMillis: Long = HOLD_MILLIS,
    private val minFrames: Int = MIN_FRAMES,
) {
    private val captured = arrayOfNulls<List<Rgb>>(FaceView.entries.size)
    private val streak = ArrayList<List<Rgb>>()
    private var streakLabs: List<Lab>? = null
    private var streakStart = 0L
    private var refs = ColorClassifier.references()

    var index: Int = 0
        private set

    val current: FaceView? get() = FaceView.entries.getOrNull(index)

    val isDone: Boolean get() = index >= FaceView.entries.size

    /** The captured face waiting for confirmation, and its samples. */
    var review: Pair<FaceView, List<Rgb>>? = null
        private set

    /** Set when the centre of the face under review read as another colour than its face's. */
    var reviewCentreLooksLike: CubeColor? = null
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
        live = samples.map { ColorClassifier.live(it, refs) }
        val labs = samples.map { it.toLab() }
        val previous = FaceView.entries.getOrNull(index - 1)?.let { captured[it.ordinal] }
        if (previous != null && looksAlike(labs, previous.map { it.toLab() })) {
            resetStreak()
            return ScanEvent.PreviousFace
        }
        if (!looksLikeCube(labs)) {
            resetStreak()
            return ScanEvent.NoCube
        }
        val steady = streakLabs?.let { looksAlike(labs, it) } == true
        if (!steady) {
            resetStreak()
            streakLabs = labs
            streakStart = nowMillis
        }
        streak += samples
        val held = nowMillis - streakStart
        if (streak.size < minFrames || held < holdMillis) {
            val progress = if (holdMillis <= 0) streak.size.toFloat() / minFrames else held.toFloat() / holdMillis
            return ScanEvent.Holding(progress.coerceIn(0f, 1f), centreHint(samples[CENTRE], view))
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
            updateReferences()
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
        reviewCentreLooksLike = centreHint(samples[CENTRE], view)
        resetStreak()
        return ScanEvent.Captured(view)
    }

    /** The colour the centre reads as, when that is not the asked face's colour. */
    private fun centreHint(centre: Rgb, view: FaceView): CubeColor? =
        ColorClassifier.live(centre, refs).takeIf { it != view.centreColor(scheme) }

    private fun clearReview() {
        review = null
        reviewCentreLooksLike = null
    }

    /** The cube's own centres seen so far become the references of the live reading. */
    private fun updateReferences() {
        val known = FaceView.entries.mapNotNull { view ->
            captured[view.ordinal]?.let { view.centreColor(scheme) to listOf(it[CENTRE].toLab()) }
        }.toMap()
        refs = ColorClassifier.references(known)
    }

    private fun resetStreak() {
        streak.clear()
        streakLabs = null
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

        /** Largest colour difference per cell that still counts as the same view. */
        const val STEADY_DISTANCE = 12.0
        private const val CENTRE = 4

        /** A sticker is clearly coloured (blue is dark but saturated) or bright (white). */
        const val MIN_CHROMA = 20.0
        const val MIN_WHITE_LIGHTNESS = 55.0

        /** All cells but at most one (glare, a shadow) read as stickers. */
        fun looksLikeCube(labs: List<Lab>): Boolean =
            labs.count { hypot(it.a, it.b) < MIN_CHROMA && it.l < MIN_WHITE_LIGHTNESS } <= 1

        /** Every cell of [a] is within [STEADY_DISTANCE] of the same cell of [b]. */
        private fun looksAlike(a: List<Lab>, b: List<Lab>): Boolean = a.indices.all { a[it].distance(b[it]) < STEADY_DISTANCE }
    }
}
