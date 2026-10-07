package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Tracker
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TrackerTest {
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun name(rgb: fi.jukkakot.rubikkisolveri.cube.scan.Rgb) = ColorClassifier.live(rgb)

    /**
     * [face] of [cube] lying at [angle] radians on screen at [at], read as the finder would: the
     * lattice labelled so that its rows run as nearly rightwards as possible.
     */
    private fun turnedOnScreen(face: Face, angle: Double, at: Point, missing: Set<Int> = emptySet()): FaceReading {
        val row = Point(cos(angle), sin(angle)) * 30.0
        val col = Point(-sin(angle), cos(angle)) * 30.0
        fun physical(n: Int) = at + row * (n % 3 - 1.0) + col * (n / 3 - 1.0)
        val (u, v) = listOf(row to col, col to row * -1.0, row * -1.0 to col * -1.0, col * -1.0 to row).maxBy { it.first.x }
        val colors = (0 until 9).map { j ->
            val p = at + u * (j % 3 - 1.0) + v * (j / 3 - 1.0)
            val n = (0 until 9).minBy { (physical(it) - p).length }
            if (j in missing) null else ColorClassifier.DEFAULT_PALETTE.getValue(cube[face.ordinal * 9 + n])
        }
        return FaceReading(colors, at, u, v)
    }

    @Test
    fun aFaceMovingAndTurningSlowlyStaysOneTrack() {
        val tracker = Tracker()
        val truth = (0 until 9).map { cube[Face.F.ordinal * 9 + it] }
        for (k in 0 until 40) {
            // Ten degrees and five pixels a picture: more than a whole turn over the run.
            val r = tracker.onFrame(listOf(turnedOnScreen(Face.F, k * PI / 18, Point(100.0 + 5 * k, 200.0))), k * 100L, ::name).single()
            val (track, reading) = assertNotNull(r)
            assertEquals(0, track.id, "picture $k")
            for (m in 0 until 9) assertEquals(truth[m], name(reading.face.colors[reading.at(m)]!!), "picture $k sticker $m")
        }
        assertEquals(1, tracker.tracks.size)
    }

    @Test
    fun aJumpToAnotherFaceStartsANewTrack() {
        val tracker = Tracker()
        val a = tracker.onFrame(listOf(turnedOnScreen(Face.F, 0.0, Point(100.0, 100.0))), 0, ::name).single()!!.first
        // Another face at the same place (the cube turned quickly): its stickers disagree.
        val b = tracker.onFrame(listOf(turnedOnScreen(Face.R, 0.0, Point(100.0, 100.0))), 100, ::name).single()!!.first
        assertNotEquals(a, b)
        // The same face far away: a new track too.
        val c = tracker.onFrame(listOf(turnedOnScreen(Face.R, 0.0, Point(300.0, 100.0))), 200, ::name).single()!!.first
        assertNotEquals(b, c)
        // And after a gap.
        val d = tracker.onFrame(listOf(turnedOnScreen(Face.R, 0.0, Point(300.0, 100.0))), 200 + Tracker.GAP_MILLIS + 1, ::name).single()!!.first
        assertNotEquals(c, d)
    }

    @Test
    fun aPartialReadingContinuesATrackOnly() {
        val tracker = Tracker()
        assertNull(tracker.onFrame(listOf(turnedOnScreen(Face.U, 0.0, Point(100.0, 100.0), missing = setOf(0))), 0, ::name).single())
        val a = tracker.onFrame(listOf(turnedOnScreen(Face.U, 0.0, Point(100.0, 100.0))), 100, ::name).single()!!.first
        val (b, reading) = tracker.onFrame(listOf(turnedOnScreen(Face.U, 0.2, Point(104.0, 100.0), missing = setOf(2))), 200, ::name).single()!!
        assertEquals(a, b)
        assertEquals(2, a.size)
        assertEquals(0, reading.turn)
    }

    @Test
    fun aSecondLatticeOnTheSameFaceIsLeftOut() {
        val tracker = Tracker()
        val big = turnedOnScreen(Face.U, 0.0, Point(100.0, 100.0))
        val small = big.copy(centre = Point(110.0, 100.0), u = Point(25.0, 0.0), v = Point(0.0, 25.0))
        val r = tracker.onFrame(listOf(small, big), 0, ::name)
        assertNull(r[0])
        assertNotNull(r[1])
        assertEquals(1, tracker.tracks.size)
    }

    @Test
    fun twoFacesSideBySideAreTwoTracks() {
        val tracker = Tracker()
        repeat(3) { k ->
            val r = tracker.onFrame(listOf(turnedOnScreen(Face.F, 0.0, Point(100.0, 200.0)), turnedOnScreen(Face.U, 0.0, Point(100.0, 110.0))), k * 100L, ::name)
            assertEquals(listOf(0, 1), r.map { it!!.first.id })
        }
        assertEquals(2, tracker.tracks.size)
        assert(abs(tracker.tracks[0].centre.y - 200.0) < 1e-9)
    }
}
