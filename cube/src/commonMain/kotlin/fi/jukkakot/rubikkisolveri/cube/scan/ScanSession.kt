package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Validity

/** What the latest frame meant for the scan. */
sealed interface ScanEvent {
    /** Nothing to read yet (or a face is under review). */
    data object Waiting : ScanEvent

    /**
     * A face is steady; [progress] (0…1) of the hold time has passed. [recognised] is the face not
     * yet scanned whose centre colour the centre reads closest to.
     */
    data class Holding(val progress: Float, val recognised: FaceView? = null) : ScanEvent

    /** The camera sees a face already accepted (in any rotation): the user should turn the cube. */
    data object AlreadyScanned : ScanEvent

    /** The grid does not look like cube stickers (a desk, a hand): nothing is captured automatically. */
    data object NoCube : ScanEvent

    /** A face recognised as [face] was captured and awaits [ScanSession.accept] or [ScanSession.retake]. */
    data class Captured(val face: FaceView) : ScanEvent
}

/**
 * The finished scan: the colours, which stickers to double-check, whether it can be solved, and the
 * 54 raw readings (URFDLB, net order) the colours were worked out from. [rotations] is how many
 * quarter turns clockwise each face's capture was turned to net order, and [from] which face's
 * capture ended up on each face (see [RotationResult]); the pictures turn the same way.
 */
data class ScanOutcome(
    val editor: CubeEditor,
    val uncertain: Set<Int>,
    val validity: Validity,
    val samples: List<Rgb> = emptyList(),
    val rotations: Map<Face, Int> = emptyMap(),
    val from: Map<Face, Face> = emptyMap(),
) {
    /** Valid and no doubtful sticker: go straight to the solution. */
    val isConfident: Boolean get() = validity.isValid && uncertain.isEmpty()

    /** Stickers to mark for the user. */
    val marked: Set<Int> get() = uncertain + validity.markedStickers
}

/**
 * The six-face scan, frame by frame, in any order and with each face turned any way. The centre
 * tells which face is in view (among the faces not yet scanned); the user confirms or changes it in
 * the review. A face is captured when every cell has stayed steady for [holdMillis] and at least
 * [minFrames] frames (the captured samples are the per-cell median of those frames) and is under
 * review until [accept] or [retake]. The colours and how each face was turned are only worked out
 * at the end, from all 54 readings together ([RotationSearch]).
 *
 * With [only] set, the session scans just that face (a rescan from the check): accepting it ends
 * the session and [redo] does nothing.
 */
class ScanSession(
    private val scheme: ColorScheme = ColorScheme.STANDARD,
    private val holdMillis: Long = HOLD_MILLIS,
    private val minFrames: Int = MIN_FRAMES,
    val only: FaceView? = null,
) {
    /** Accepted faces as seen, in the order they were accepted. */
    private val captured = LinkedHashMap<FaceView, List<Rgb>>()
    private val streak = ArrayList<List<Rgb>>()
    private var streakLabs: List<Lab>? = null
    private var streakStart = 0L
    private var refs = ColorClassifier.references()

    /** Number of faces done. */
    val index: Int get() = captured.size

    /** The faces still to scan. */
    val remaining: List<FaceView> get() = (only?.let(::listOf) ?: FaceView.entries).filter { it !in captured }

    val isDone: Boolean get() = remaining.isEmpty()

    fun isAccepted(view: FaceView): Boolean = view in captured

    /** The captured samples waiting for confirmation. */
    var review: List<Rgb>? = null
        private set

    /** Which face the samples under review are taken as; the user may [choose] another. */
    var reviewFace: FaceView? = null
        private set

    /** Live colours of the latest frame, for the dots on the grid. */
    var live: List<CubeColor>? = null
        private set

    /** Latest raw samples, for the capture button. */
    var latest: List<Rgb>? = null
        private set

    /** Confirmed readings so far per face, as seen (null = not yet). */
    fun capturedSamples(view: FaceView): List<Rgb>? = captured[view]

    /**
     * Handles one frame of grid readings taken at [nowMillis]. [looksLikeCube] is whether the frame
     * shows cube stickers at all (see [FrameSampler.looksLikeCube]).
     */
    fun onFrame(samples: List<Rgb>, nowMillis: Long, looksLikeCube: Boolean = true): ScanEvent {
        require(samples.size == 9)
        if (review != null || isDone) return ScanEvent.Waiting
        latest = samples
        live = samples.map { ColorClassifier.live(it, refs) }
        val labs = samples.map { it.toLab() }
        if (captured.values.any { seen -> alreadySeen(labs, seen.map { it.toLab() }) }) {
            resetStreak()
            return ScanEvent.AlreadyScanned
        }
        if (!looksLikeCube) {
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
            return ScanEvent.Holding(progress.coerceIn(0f, 1f), recognise(samples[CENTRE]))
        }
        return capture(medianOf(streak))
    }

    /** Captures the latest frame at once (the capture button). */
    fun captureNow(): ScanEvent = if (review != null || isDone) ScanEvent.Waiting else latest?.let { capture(it) } ?: ScanEvent.Waiting

    /** The face under review is taken as [view] (one not yet scanned). */
    fun choose(view: FaceView) {
        if (review != null && view in remaining) reviewFace = view
    }

    /** Keeps the face under review as [reviewFace]; any face not yet scanned is asked for next. */
    fun accept() {
        val samples = review ?: return
        captured[reviewFace ?: return] = samples
        clearReview()
        updateReferences()
        resetStreak()
    }

    /** Drops the face under review; nothing is stored for it. */
    fun retake() {
        clearReview()
        resetStreak()
    }

    /** Takes back the face accepted last, so it can be scanned again. */
    fun redo() {
        clearReview()
        if (only == null) captured.keys.lastOrNull()?.let { captured.remove(it) }
        updateReferences()
        resetStreak()
    }

    fun outcome(): ScanOutcome {
        check(isDone && only == null) { "Scan not finished" }
        val seen = Face.entries.flatMap { captured.getValue(FaceView.of(it)) }
        val classification = ColorClassifier.classify(seen, scheme)
        val found = RotationSearch.search(classification.colors, scheme, seen)
        val uncertainSeen = classification.uncertain()
        val uncertain = (0 until seen.size).filter { i ->
            found.source[i] in uncertainSeen || (i % 9 != CENTRE && Face.entries[i / 9] in found.ambiguous)
        }.toSet()
        return ScanOutcome(
            editor = CubeEditor(found.colors, scheme),
            uncertain = uncertain,
            validity = found.validity,
            samples = found.source.map { seen[it] },
            rotations = found.rotations,
            from = found.from,
        )
    }

    private fun capture(samples: List<Rgb>): ScanEvent {
        val face = recognise(samples[CENTRE]) ?: return ScanEvent.Waiting
        review = samples
        reviewFace = face
        resetStreak()
        return ScanEvent.Captured(face)
    }

    /** The face not yet scanned whose centre colour [centre] reads closest to. */
    private fun recognise(centre: Rgb): FaceView? {
        val left = remaining
        val color = ColorClassifier.ranked(centre, refs).firstOrNull { c -> left.any { it.centreColor(scheme) == c } }
        return left.firstOrNull { it.centreColor(scheme) == color }
    }

    private fun clearReview() {
        review = null
        reviewFace = null
    }

    /** The cube's own centres seen so far become the references of the live reading. */
    private fun updateReferences() {
        val known = captured.entries.associate { (view, samples) -> view.centreColor(scheme) to listOf(samples[CENTRE].toLab()) }
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

        /** Every cell of [a] is within [STEADY_DISTANCE] of the same cell of [b]. */
        private fun looksAlike(a: List<Lab>, b: List<Lab>): Boolean = a.indices.all { a[it].distance(b[it]) < STEADY_DISTANCE }

        /** [a] looks like [b] turned any way. */
        private fun alreadySeen(a: List<Lab>, b: List<Lab>): Boolean = (0 until 4).any { looksAlike(a, RotationSearch.turned(b, it)) }
    }
}
