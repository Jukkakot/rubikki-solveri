package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.follow.FollowEvent
import fi.jukkakot.rubikkisolveri.cube.follow.FollowTracker
import fi.jukkakot.rubikkisolveri.cube.follow.FrontArrow
import fi.jukkakot.rubikkisolveri.cube.follow.LiveCalibration
import fi.jukkakot.rubikkisolveri.cube.follow.front
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrontArrowTest {
    private fun arrow(text: String) = FrontArrow.of(Notation.parseMove(text)!!)

    @Test
    fun rightTurn() {
        val a = arrow("R")!!
        assertEquals(FrontArrow.Kind.STRAIGHT, a.kind)
        assertTrue(a.x0 > 0.7f && a.x1 > 0.7f, "right column")
        assertTrue(a.y1 < a.y0, "upwards")
        assertTrue(arrow("R'")!!.let { it.y1 > it.y0 })
    }

    @Test
    fun topTurn() {
        val a = arrow("U")!!
        assertTrue(a.y0 < 0.3f && a.y1 < 0.3f, "top row")
        assertTrue(a.x1 < a.x0, "leftwards")
    }

    @Test
    fun arrowsAgreeWithTheModel() {
        // The arrow moves the front stickers it lies on the way the model moves them.
        val solved = Cube.solved()
        for (text in listOf("U", "U'", "D", "D'", "R", "R'", "L", "L'", "M", "E'")) {
            val move = Notation.parseMove(text)!!
            val a = FrontArrow.of(move)!!
            val dx = a.x1 - a.x0
            val dy = a.y1 - a.y0
            // Front sticker under the arrow's start, and where its colour went.
            val col = (a.x0 * 3).toInt().coerceIn(0, 2)
            val row = (a.y0 * 3).toInt().coerceIn(0, 2)
            val start = Stickers.index(Face.F, row * 3 + col + 1)
            val target = Stickers.all[move.permutation[start]]
            // A sticker leaving the front to the left/right/up/down lands on L/R/U/D.
            val expectedFace = when {
                dx < 0 -> Face.L
                dx > 0 -> Face.R
                dy < 0 -> Face.U
                else -> Face.D
            }
            assertEquals(expectedFace, target.face, text)
            assertTrue(solved.apply(move) != solved)
        }
        assertEquals(FrontArrow.Kind.CLOCKWISE, arrow("F")!!.kind)
        assertEquals(FrontArrow.Kind.COUNTER_CLOCKWISE, arrow("F'")!!.kind)
        assertTrue(arrow("F2")!!.double)
        assertNull(arrow("B"))
        assertNull(arrow("S"))
        assertNull(arrow("y"))
    }
}

class FollowTrackerTest {
    private val palette = ColorClassifier.DEFAULT_PALETTE
    private val before = Cube.solved().apply("R U F' D2 L")

    private fun seen(cube: Cube): List<Rgb> = front(cube).map { palette.getValue(it) }

    @Test
    fun moveDone() {
        val tracker = FollowTracker(stableFrames = 3)
        val move = Notation.parseMove("R")!!
        assertEquals(FollowEvent.Waiting, tracker.onFrame(before, move, seen(before)))
        val after = before.apply(move)
        assertEquals(FollowEvent.Waiting, tracker.onFrame(before, move, seen(after)))
        assertEquals(FollowEvent.Waiting, tracker.onFrame(before, move, seen(after)))
        assertEquals(FollowEvent.Done, tracker.onFrame(before, move, seen(after)))
    }

    @Test
    fun oneMisreadCellStillMatches() {
        val tracker = FollowTracker(stableFrames = 1)
        val move = Notation.parseMove("U")!!
        val after = before.apply(move)
        val samples = seen(after).toMutableList()
        val wrongColor = CubeColor.entries.first { it != front(after)[0] }
        samples[0] = palette.getValue(wrongColor)
        assertEquals(FollowEvent.Done, tracker.onFrame(before, move, samples))
    }

    @Test
    fun wrongDirection() {
        val tracker = FollowTracker(stableFrames = 2)
        val move = Notation.parseMove("R")!!
        val wrong = before.apply("R'")
        tracker.onFrame(before, move, seen(wrong))
        val event = tracker.onFrame(before, move, seen(wrong))
        assertEquals(FollowEvent.WrongMove(Notation.parseMove("R'")!!, Notation.parseMove("R")!!), event)
    }

    @Test
    fun backTurn() {
        val tracker = FollowTracker()
        assertEquals(FollowEvent.NotVisible, tracker.onFrame(before, Notation.parseMove("B")!!, seen(before)))
    }

    @Test
    fun trackerLearnsAMisreadColour() {
        // A light that reads only white wrongly (as the default yellow): the front's whites misread.
        fun light(c: CubeColor): Rgb = if (c == CubeColor.WHITE) Rgb(225, 205, 70) else palette.getValue(c)
        val start = Cube.solved().apply("R U R' U'")
        val face = front(start)
        val whites = face.count { it == CubeColor.WHITE }
        assertTrue(whites in 1..2, "test cube must have one or two white cells in front, has $whites")
        val tracker = FollowTracker(stableFrames = 1)
        val move = Notation.parseMove("F")!!
        repeat(8) { tracker.onFrame(start, move, face.map(::light)) }
        assertEquals(face, tracker.live)
    }

    @Test
    fun wrongHold() {
        val tracker = FollowTracker()
        val event = tracker.onFrame(before, Notation.parseMove("R")!!, seen(before.apply("y")))
        assertTrue(event is FollowEvent.HoldFront, "$event")
    }

    @Test
    fun warmLight() {
        // Warm light: white reads like the default yellow.
        fun warm(c: CubeColor): Rgb {
            val p = palette.getValue(c)
            return Rgb(p.r, (p.g * 0.93).toInt(), (p.b * 0.25).toInt())
        }
        val calibration = LiveCalibration()
        assertNotEquals(CubeColor.WHITE, calibration.read(warm(CubeColor.WHITE)), "the test light must fool the default palette")
        val face = front(before)
        repeat(6) { calibration.learn(face.map(::warm), face) }
        assertEquals(CubeColor.WHITE, calibration.read(warm(CubeColor.WHITE)))
        assertEquals(face, calibration.read(face.map(::warm)))
    }
}
