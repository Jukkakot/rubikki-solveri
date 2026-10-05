package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.cube.scan.Orientation
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Pose
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.Stall
import fi.jukkakot.rubikkisolveri.cube.scan.Tilt
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoScanTest {
    private val truth = Cube.fromColorString(VideoFixtures.TRUTH)

    /** Replays a test video's face readings at 10 fps; returns every frame's state. */
    private fun replay(video: String, scan: VideoScan = VideoScan()): List<VideoScanState> = replay(VideoFixtures.load(video), scan)

    private fun replay(frames: List<VideoFixtures.Frame>, scan: VideoScan = VideoScan()): List<VideoScanState> =
        frames.mapIndexed { i, frame -> scan.onFrame(frame.faces, i * 100L) }

    @Test
    fun angledVideoGivesTheTrueCube() {
        val scan = VideoScan()
        val states = replay(VideoFixtures.ANGLED, scan)
        assertTrue(states.any { it.finished }, "finished at some point")
        assertEquals(VideoFixtures.TRUTH, scan.outcome().editor.encode())
        assertTrue(scan.outcome().isConfident)
    }

    @Test
    fun straightVideoGivesTheTrueCube() {
        val scan = VideoScan()
        val states = replay(VideoFixtures.STRAIGHT, scan)
        assertTrue(states.any { it.finished }, "finished at some point")
        assertEquals(VideoFixtures.TRUTH, scan.outcome().editor.encode())
    }

    @Test
    fun recognisedStickersNeverChangeColourOnTheVideos() {
        for (video in listOf(VideoFixtures.ANGLED, VideoFixtures.STRAIGHT)) {
            val states = replay(video)
            for (s in states) for (i in 0 until Stickers.COUNT) {
                val c = s.stickers[i]
                // Once the face's rotation is settled a recognised sticker only ever shows its true colour.
                if (c != null && s.complete) assertEquals(truth[i], c, "$video sticker $i")
            }
        }
    }

    @Test
    fun neverAWrongClearCubeAndClearSoonerThanByConfirmingAll() {
        // Frames to complete when all 54 had to be confirmed one by one (before video-scan-progress).
        val before = mapOf(VideoFixtures.ANGLED to 226, VideoFixtures.STRAIGHT to 143)
        for (video in before.keys) {
            val states = replay(video)
            for ((i, s) in states.withIndex()) if (s.complete) assertEquals(VideoFixtures.TRUTH, s.stickers.joinToString("") { it!!.letter.toString() }, "$video frame $i")
            val clear = states.indexOfFirst { it.complete }
            println("$video frames to clear: $clear (before: ${before[video]})")
            assertTrue(clear in 0 until before.getValue(video), "$video: $clear")
        }
    }

    @Test
    fun eveningVideosNeverGiveAWrongClearCube() {
        for ((video, before) in VideoFixtures.EVENING) {
            val states = replay(video)
            for ((i, s) in states.withIndex()) if (s.complete) assertEquals(VideoFixtures.EVENING_TRUTH, s.stickers.joinToString("") { it!!.letter.toString() }, "$video frame $i")
            val clear = states.indexOfFirst { it.complete }
            println("$video frames to clear: $clear (before: $before)")
            // In dim light red reads as orange on whole faces: no clear cube (the stall asks for light).
            if (before != null) assertTrue(clear in 0..before, "$video: $clear")
        }
    }

    /** Frames until the cube is first complete (null: never) and stickers recognised at the halfway frame, replaying only the faces [keep] lets through. */
    private fun speed(video: String, keep: (FaceReading) -> Boolean): Pair<Int?, Int> {
        val states = replay(VideoFixtures.load(video).map { f -> f.copy(faces = f.faces.filter(keep)) })
        return states.indexOfFirst { it.complete }.takeIf { it >= 0 } to states[states.size / 2].recognised
    }

    @Test
    fun partialFacesDoNotSlowTheScan() {
        for (video in listOf(VideoFixtures.ANGLED, VideoFixtures.STRAIGHT)) {
            val without = speed(video) { it.isFull }
            val with = speed(video) { true }
            println("$video frames to complete / recognised at halfway: full faces only $without, with partial faces $with")
            // A partial face can add a misread vote or two: a couple of frames either way.
            assertTrue(with.first != null && without.first != null && with.first!! <= without.first!! + 3, "$video: $with vs $without")
        }
    }

    /** Readings of [face] of the true cube in its net order, as the default palette would read them. */
    private fun reading(face: Face, turn: Int = 0, wrong: Int? = null, centre: Point = Point(100.0, 100.0), missing: Set<Int> = emptySet()): FaceReading {
        val nine = (0 until 9).map { truth[face.ordinal * 9 + it] }.let { RotationSearch.turned(it, turn) }
        val colors = nine.mapIndexed { n, c ->
            val color = if (n == wrong) CubeColor.entries.first { it != c && it != nine[4] } else c
            if (n in missing) null else ColorClassifier.DEFAULT_PALETTE.getValue(color)
        }
        return FaceReading(colors, centre, Point(30.0, 0.0), Point(0.0, 30.0))
    }

    @Test
    fun oneWrongReadingDoesNotChangeARecognisedSticker() {
        val scan = VideoScan()
        var t = 0L
        repeat(4) { scan.onFrame(listOf(reading(Face.U)), t++ * 100) }
        val before = scan.state.stickers.toList()
        assertEquals(9, before.count { it != null })
        val after = scan.onFrame(listOf(reading(Face.U, wrong = 0)), t * 100)
        assertEquals(before, after.stickers)
        assertTrue(after.contradictions.isEmpty())
    }

    @Test
    fun aFaceTurnedOnScreenVotesForTheSameStickers() {
        val scan = VideoScan()
        repeat(3) { scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        val before = scan.state.stickers.toList()
        repeat(6) { scan.onFrame(listOf(reading(Face.U, turn = 1)), 300L + it * 100) }
        assertEquals(before, scan.state.stickers)
    }

    @Test
    fun cornerViewSettlesTheRotationAndThePose() {
        val scan = VideoScan()
        // F in front, U above it on screen: U's bottom side touches F, F's top side touches U.
        val f = reading(Face.F, centre = Point(100.0, 200.0))
        val u = reading(Face.U, centre = Point(100.0, 110.0))
        var s = VideoScanState.EMPTY
        repeat(4) { s = scan.onFrame(listOf(f, u), it * 100L) }
        assertEquals(Pose(Face.F, Face.U), s.pose)
        for (face in listOf(Face.U, Face.F)) for (n in 0 until 9) assertEquals(truth[face.ordinal * 9 + n], s.stickers[face.ordinal * 9 + n])
    }

    @Test
    fun orientationFollowsTheSettledFrontFaceAndIsNullWithoutOne() {
        val scan = VideoScan()
        assertNull(scan.onFrame(listOf(reading(Face.F)), 0).orientation, "rotation not settled yet")
        val f = reading(Face.F, centre = Point(100.0, 200.0))
        val u = reading(Face.U, centre = Point(100.0, 110.0))
        var s = VideoScanState.EMPTY
        repeat(4) { s = scan.onFrame(listOf(f, u), 100 + it * 100L) }
        // F straight at the camera with U on top: the cube's axes are the camera's.
        val straight = Orientation(listOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0))
        assertTrue(s.orientation!!.angleTo(straight) < 0.05, "${s.orientation}")
        assertNull(scan.onFrame(emptyList(), 600).orientation)
    }

    @Test
    fun oneReadingGivesLeadingColoursAndThreeRecogniseThem() {
        val scan = VideoScan()
        val first = scan.onFrame(listOf(reading(Face.U)), 0)
        assertEquals(1, first.recognised, "only the centre of the face in view")
        for (n in 0 until 9) if (n != 4) assertEquals(truth[Face.U.ordinal * 9 + n], first.leading[Face.U.ordinal * 9 + n])
        assertTrue(first.found.single().recognised.withIndex().none { (n, r) -> r && n != 4 })
        assertEquals(9, first.found.single().names.count { it != null })
        var s = first
        repeat(2) { s = scan.onFrame(listOf(reading(Face.U, turn = 1)), 100 + it * 100L) }
        assertTrue(s.found.single().recognised.all { it })
        assertTrue(s.leading.all { it == null }, "recognised stickers have no leading colour")
        // Named in the reading's own order: the turned reading's first sticker is the face's seventh.
        assertEquals(truth[Face.U.ordinal * 9 + 6], s.found.single().names[0])
    }

    @Test
    fun onlyTheBottomMissingAsksToTiltItIntoView() {
        val stickers = truth.toList().mapIndexed { i, c -> if (i / 9 == Face.D.ordinal) null else c }
        assertEquals(Tilt.UP, VideoScan.hint(Pose(Face.F, Face.U), stickers, emptySet()))
        // Held upside down (D on top), the bottom of the cube is on top of the picture.
        assertEquals(Tilt.DOWN, VideoScan.hint(Pose(Face.F, Face.D), stickers, emptySet()))
        assertNull(VideoScan.hint(Pose(Face.D, Face.F), stickers, emptySet()), "already in view")
        assertNull(VideoScan.hint(Pose(Face.F, Face.U), truth.toList(), emptySet()), "nothing missing")
    }

    /** Places of [face] known in [state], the centre left out (it is known as soon as the face is seen). */
    private fun recognisedOn(state: VideoScanState, face: Face) = (0 until 9).filter { it != 4 && state.stickers[face.ordinal * 9 + it] != null }

    @Test
    fun aPartialFaceVotesForTheStickersItShows() {
        val scan = VideoScan()
        repeat(2) { scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        val s = scan.onFrame(listOf(reading(Face.U, turn = 1, missing = setOf(1))), 200)
        // Missing place 1 of the turned reading is a different place of the face; the other seven around the centre reach three votes.
        assertEquals(7, recognisedOn(s, Face.U).size)
        for (n in recognisedOn(s, Face.U)) assertEquals(truth[Face.U.ordinal * 9 + n], s.stickers[Face.U.ordinal * 9 + n])
    }

    @Test
    fun aPartialFaceWithAWrongLatticeDoesNotVote() {
        val scan = VideoScan()
        repeat(2) { scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        // The stickers of another face around the right centre: a lattice across the cube's edge.
        val r = reading(Face.R, missing = setOf(0))
        val wrong = r.copy(colors = r.colors.toMutableList().also { it[4] = reading(Face.U).colors[4] })
        val s = scan.onFrame(listOf(wrong, wrong), 200)
        assertEquals(emptyList(), recognisedOn(s, Face.U))
    }

    @Test
    fun aPartialFaceWithoutItsCentreOrBeforeAnyFullFaceIsIgnored() {
        val scan = VideoScan()
        repeat(3) { scan.onFrame(listOf(reading(Face.U, missing = setOf(0))), it * 100L) }
        assertEquals(0, scan.state.recognised, "no full face yet")
        repeat(2) { scan.onFrame(listOf(reading(Face.U)), 300 + it * 100L) }
        val s = scan.onFrame(listOf(reading(Face.U, missing = setOf(4))), 500)
        assertEquals(emptyList(), recognisedOn(s, Face.U))
    }

    @Test
    fun aSidewaysCameraFrameIsTurnedUprightForTheFinder() {
        // 4×2 frame, rotation 90: upright it is 2 wide and 4 high; the frame's bottom-left pixel is the top left.
        val bytes = ByteArray(4 * 2 * 4)
        bytes[(1 * 4 + 0) * 4] = 200.toByte()
        val image = FrameSampler.upright(RgbaFrame(4, 2, 16, bytes, 90))
        assertEquals(2 to 4, image.width to image.height)
        assertEquals(200, (image.argb[0] shr 16) and 0xff)
        val small = FrameSampler.upright(RgbaFrame(4, 2, 16, bytes, 0), shortSide = 1)
        assertEquals(2 to 1, small.width to small.height)
    }

    @Test
    fun finishesOnlyAfterHalfASecond() {
        val scan = VideoScan()
        val faces = Face.entries.map { reading(it, centre = Point(100.0, 100.0 + 300 * it.ordinal)) }
        var t = 0L
        // Each face shown until the cube is first clear (the last one needs not be read in full).
        for (face in faces) repeat(10) { if (!scan.state.complete) scan.onFrame(listOf(face), t).also { t += 100 } }
        val first = scan.state
        assertTrue(first.complete)
        assertTrue(!first.finished)
        repeat(5) { scan.onFrame(emptyList(), t); t += 100 }
        assertTrue(scan.state.finished)
        assertEquals(VideoFixtures.TRUTH, scan.outcome().editor.encode())
    }

    /** [face] read as in [reading], all colours scaled by [light] (a dark picture). */
    private fun dark(face: Face, light: Double): FaceReading {
        val r = reading(face)
        return r.copy(colors = r.colors.map { c -> c?.let { Rgb((it.r * light).toInt(), (it.g * light).toInt(), (it.b * light).toInt()) } })
    }

    @Test
    fun darkPictureIsToldAtOnceAndStallsAfterAWhile() {
        val scan = VideoScan()
        val first = scan.onFrame(listOf(dark(Face.U, 0.2)), 0)
        assertTrue(first.dim)
        assertNull(first.stall)
        var s = first
        for (t in 100L..3_000L step 100) s = scan.onFrame(listOf(dark(Face.U, 0.2)), t)
        assertEquals(Stall.DARK, s.stall)
        assertTrue(!scan.onFrame(listOf(reading(Face.U)), 3_100).dim)
    }

    @Test
    fun noCubeForAWhileStalls() {
        val scan = VideoScan()
        scan.onFrame(listOf(reading(Face.U)), 0)
        assertNull(scan.onFrame(emptyList(), 7_000).stall)
        assertEquals(Stall.NO_CUBE, scan.onFrame(emptyList(), 8_000).stall)
        assertNull(scan.onFrame(listOf(reading(Face.U)), 8_100).stall, "cube back in view")
    }

    @Test
    fun theSameFaceForFifteenSecondsStallsAndRestartClearsIt() {
        val scan = VideoScan()
        var s = VideoScanState.EMPTY
        for (t in 0L..14_000L step 200) s = scan.onFrame(listOf(reading(Face.U)), t)
        assertNull(s.stall)
        for (t in 14_200L..15_400L step 200) s = scan.onFrame(listOf(reading(Face.U)), t)
        assertEquals(Stall.STUCK, s.stall)
        scan.reset()
        assertEquals(0, scan.state.recognised)
        val again = scan.onFrame(listOf(reading(Face.U)), 15_600)
        assertNull(again.stall)
        assertEquals(1, again.recognised, "only the centre after one frame")
    }

    @Test
    fun aBorderlineRedOrangeReadingGivesEachAboutHalfAndAClearOneAWholeVote() {
        val refs = ColorClassifier.references()
        val red = refs.getValue(CubeColor.RED)
        val orange = refs.getValue(CubeColor.ORANGE)
        val halfway = Lab((red.l + orange.l) / 2, (red.a + orange.a) / 2, (red.b + orange.b) / 2)
        val split = ColorClassifier.shares(halfway, refs)
        assertEquals(0.5, split[CubeColor.RED.ordinal], 0.05)
        assertEquals(0.5, split[CubeColor.ORANGE.ordinal], 0.05)
        val clear = ColorClassifier.shares(red, refs)
        assertEquals(1.0, clear[CubeColor.RED.ordinal])
        assertEquals(1.0, clear.sum(), 1e-9)
    }

    @Test
    fun washedOutReadingsCountLittle() {
        // Too much light: every sticker's brightest channel at the top of the range.
        fun washed(face: Face) = reading(face).let { r -> r.copy(colors = r.colors.map { c -> c?.let { Rgb(minOf(255, it.r * 2), minOf(255, it.g * 2), minOf(255, it.b * 2)) } }) }
        assertTrue(washed(Face.U).colors.all { VideoScan.weight(it!!) == VideoScan.WASHED_WEIGHT })
        val scan = VideoScan()
        repeat(3) { scan.onFrame(listOf(washed(Face.U)), it * 100L) }
        assertEquals(listOf(4), (0 until 9).filter { scan.state.stickers[Face.U.ordinal * 9 + it] != null }, "three washed-out readings make only the centre known")
    }

    @Test
    fun aSideReadManyTimesButNotConfirmedHasNoTick() {
        val scan = VideoScan()
        repeat(20) { scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        assertEquals(9, recognisedOn(scan.state, Face.U).size + 1, "all nine known from their own votes")
        assertTrue(scan.state.confirmed.isEmpty(), "${scan.state.confirmed}")
        val faces = Face.entries.map { reading(it, centre = Point(100.0, 100.0 + 300 * it.ordinal)) }
        var t = 2_000L
        for (face in faces) repeat(10) { if (!scan.state.complete) scan.onFrame(listOf(face), t).also { t += 100 } }
        assertEquals(Face.entries.toSet(), scan.state.confirmed)
    }
}
