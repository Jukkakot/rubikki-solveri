package fi.jukkakot.rubikkisolveri.cube.follow

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

/** The nine front-face colours of [cube], row by row as seen. */
fun front(cube: Cube): List<CubeColor> = (1..9).map { cube.colorAt(Face.F, it) }

/**
 * Colour reading that adapts to the light: references start at the default palette and move
 * towards readings of stickers whose colour is known.
 */
class LiveCalibration {
    private val refs: MutableMap<CubeColor, Lab> =
        ColorClassifier.DEFAULT_PALETTE.mapValues { it.value.toLab() }.toMutableMap()

    fun read(rgb: Rgb): CubeColor {
        val lab = rgb.toLab()
        return refs.minBy { it.value.distance(lab) }.key
    }

    fun read(samples: List<Rgb>): List<CubeColor> = samples.map(::read)

    /** Learns from [samples] whose true colours are [known] (only cells marked true in [use]). */
    fun learn(samples: List<Rgb>, known: List<CubeColor>, use: List<Boolean> = List(samples.size) { true }) {
        for (i in samples.indices) {
            if (!use[i]) continue
            val lab = samples[i].toLab()
            val ref = refs.getValue(known[i])
            refs[known[i]] = Lab(
                ref.l + (lab.l - ref.l) * RATE,
                ref.a + (lab.a - ref.a) * RATE,
                ref.b + (lab.b - ref.b) * RATE,
            )
        }
    }

    private companion object {
        const val RATE = 0.3
    }
}

/** What the camera shows while following a move. */
sealed interface FollowEvent {
    /** Nothing conclusive yet. */
    data object Waiting : FollowEvent

    /** The centre does not read as the front's colour: the cube is not held the right way. */
    data class HoldFront(val centre: CubeColor) : FollowEvent

    /** This move does not change the front face: confirm it with the button. */
    data object NotVisible : FollowEvent

    /** The move was made. */
    data object Done : FollowEvent

    /** A different turn was made; [fix] undoes it. */
    data class WrongMove(val made: Move, val fix: Move) : FollowEvent
}

/**
 * Watches the front face while the user makes [move] on [before]. A front "matches" when at least
 * 8 of 9 cells agree; it must stay so for [stableFrames] frames.
 */
class FollowTracker(private val stableFrames: Int = STABLE_FRAMES) {
    private var key: Pair<Cube, Move>? = null
    private var streakTarget: Any? = null
    private var streak = 0
    val calibration = LiveCalibration()

    /** The colours read from the latest frame. */
    var live: List<CubeColor>? = null
        private set

    fun onFrame(before: Cube, move: Move, samples: List<Rgb>): FollowEvent {
        if (key != (before to move)) {
            key = before to move
            reset()
        }
        val read = calibration.read(samples)
        live = read
        val expectedBefore = front(before)
        val after = before.apply(move)
        val expectedAfter = front(after)
        val scoreBefore = score(read, expectedBefore)
        val scoreAfter = score(read, expectedAfter)
        // A whole-cube turn changes the front centre: matching the front after it means done.
        if (move.isRotation && expectedBefore != expectedAfter && scoreAfter >= MATCH && scoreAfter > scoreBefore) {
            calibration.learn(samples, expectedAfter)
            return if (stable("done")) FollowEvent.Done else FollowEvent.Waiting
        }
        if (read[4] != expectedBefore[4]) {
            reset()
            return FollowEvent.HoldFront(read[4])
        }
        // A frame that is clearly one known front (a move changes at least three cells) teaches the
        // calibration every cell, including the ones it misread.
        if (scoreBefore >= LEARN && scoreBefore > scoreAfter) calibration.learn(samples, expectedBefore)
        if (expectedBefore == expectedAfter) return FollowEvent.NotVisible
        if (scoreAfter >= MATCH && scoreAfter > scoreBefore) {
            calibration.learn(samples, expectedAfter)
            return if (stable("done")) FollowEvent.Done else FollowEvent.Waiting
        }
        if (scoreBefore >= MATCH) {
            streakTarget = null
            streak = 0
            return FollowEvent.Waiting
        }
        val wrong = wrongMove(before, move, read)
        if (wrong != null) {
            return if (stable(wrong)) FollowEvent.WrongMove(wrong, wrong.inverse) else FollowEvent.Waiting
        }
        streakTarget = null
        streak = 0
        return FollowEvent.Waiting
    }

    /** Forget streaks (after an advance or a manual step). */
    fun reset() {
        streakTarget = null
        streak = 0
    }

    private fun stable(target: Any): Boolean {
        if (streakTarget != target) {
            streakTarget = target
            streak = 0
        }
        streak++
        return streak >= stableFrames
    }

    private fun wrongMove(before: Cube, move: Move, read: List<CubeColor>): Move? {
        val expectedBefore = front(before)
        val candidates = FACE_TURNS.filter { it != move }
            .map { it to front(before.apply(it)) }
            .filter { (_, f) -> f != expectedBefore && f != front(before.apply(move)) }
            .map { (m, f) -> m to score(read, f) }
            .filter { it.second >= MATCH }
        val best = candidates.maxByOrNull { it.second } ?: return null
        return if (candidates.count { it.second == best.second } == 1) best.first else null
    }

    private fun score(read: List<CubeColor>, expected: List<CubeColor>) = read.indices.count { read[it] == expected[it] }

    companion object {
        const val STABLE_FRAMES = 3
        const val MATCH = 8
        const val LEARN = 7
        private val FACE_TURNS = listOf(Layer.U, Layer.D, Layer.R, Layer.L, Layer.F, Layer.B)
            .flatMap { layer -> (1..3).map { Move(layer, it) } }
    }
}
