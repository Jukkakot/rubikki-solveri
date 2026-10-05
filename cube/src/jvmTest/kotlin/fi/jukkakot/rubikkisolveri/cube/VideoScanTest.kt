package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Pose
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
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
    private fun replay(video: String, scan: VideoScan = VideoScan()): List<VideoScanState> =
        VideoFixtures.load(video).mapIndexed { i, frame -> scan.onFrame(frame.faces, i * 100L) }

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

    /** Readings of [face] of the true cube in its net order, as the default palette would read them. */
    private fun reading(face: Face, turn: Int = 0, wrong: Int? = null, centre: Point = Point(100.0, 100.0)): FaceReading {
        val nine = (0 until 9).map { truth[face.ordinal * 9 + it] }.let { RotationSearch.turned(it, turn) }
        val colors = nine.mapIndexed { n, c ->
            val color = if (n == wrong) CubeColor.entries.first { it != c && it != nine[4] } else c
            ColorClassifier.DEFAULT_PALETTE.getValue(color)
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
    fun onlyTheBottomMissingAsksToTiltItIntoView() {
        val stickers = truth.toList().mapIndexed { i, c -> if (i / 9 == Face.D.ordinal) null else c }
        assertEquals(Tilt.UP, VideoScan.hint(Pose(Face.F, Face.U), stickers, emptySet()))
        // Held upside down (D on top), the bottom of the cube is on top of the picture.
        assertEquals(Tilt.DOWN, VideoScan.hint(Pose(Face.F, Face.D), stickers, emptySet()))
        assertNull(VideoScan.hint(Pose(Face.D, Face.F), stickers, emptySet()), "already in view")
        assertNull(VideoScan.hint(Pose(Face.F, Face.U), truth.toList(), emptySet()), "nothing missing")
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
        for (face in faces) repeat(3) { scan.onFrame(listOf(face), t); t += 100 }
        val first = scan.state
        assertTrue(first.complete)
        assertTrue(!first.finished)
        repeat(5) { scan.onFrame(emptyList(), t); t += 100 }
        assertTrue(scan.state.finished)
        assertEquals(VideoFixtures.TRUTH, scan.outcome().editor.encode())
    }
}
