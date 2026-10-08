package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.ExposureControl
import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.cube.scan.Orientation
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Pose
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
import fi.jukkakot.rubikkisolveri.cube.scan.Stall
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The video scan's behaviour, run for both scanners ([RulesVideoScanTest] runs it with [ScanEngine.RULES]). */
open class VideoScanTest {
    protected open val engine: ScanEngine = ScanEngine.LOOK

    private val truth = Cube.fromColorString(VideoFixtures.TRUTH)

    /** Replays a test video's face readings at 10 fps; returns every frame's state. */
    private fun replay(video: String, scan: VideoScan = VideoScan(engine = engine)): List<VideoScanState> = replay(VideoFixtures.load(video), scan)

    private fun replay(frames: List<VideoFixtures.Frame>, scan: VideoScan = VideoScan(engine = engine)): List<VideoScanState> =
        frames.mapIndexed { i, frame -> scan.onFrame(frame.faces, i * 100L) }

    @Test
    fun angledVideoGivesTheTrueCube() {
        val scan = VideoScan(engine = engine)
        val states = replay(VideoFixtures.ANGLED, scan)
        assertTrue(states.any { it.finished }, "finished at some point")
        assertEquals(VideoFixtures.TRUTH, scan.outcome().editor.encode())
        assertTrue(scan.outcome().isConfident)
    }

    @Test
    fun straightVideoGivesTheTrueCube() {
        val scan = VideoScan(engine = engine)
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
    fun eveningVideosClearWithTheTrueCubeOnly() {
        for ((video, before) in VideoFixtures.EVENING) {
            val states = replay(video)
            for ((i, s) in states.withIndex()) if (s.complete) assertEquals(VideoFixtures.EVENING_TRUTH, s.stickers.joinToString("") { it!!.letter.toString() }, "$video frame $i")
            val clear = states.indexOfFirst { it.complete }
            println("$video frames to clear: $clear (before: $before)")
            // The two dim ones never cleared before soft votes (`video-scan-light`); now all four do.
            assertTrue(clear >= 0, "$video never clears")
            if (before != null) assertTrue(clear <= before, "$video: $clear")
        }
    }

    /**
     * Every sticker known when the scan stops (the first finished frame, else the last) is the true
     * one ([truth], URFDLB), and every complete frame up to then is the true cube. After the finish
     * the app has moved on: what later frames read does not count.
     */
    private fun assertKnownTrue(video: String, all: List<VideoScanState>, truth: String, allowWrong: Int = 0) {
        val states = all.indexOfFirst { it.finished }.let { if (it >= 0) all.take(it + 1) else all }
        val last = states.last()
        val wrong = (0 until Stickers.COUNT).filter { i -> last.stickers[i]?.let { it.letter != truth[i] } == true }
        assertTrue(wrong.size <= allowWrong, "$video stickers known wrong: $wrong")
        for ((n, s) in states.withIndex()) if (s.complete) assertEquals(truth, s.stickers.joinToString("") { it!!.letter.toString() }, "$video frame $n")
    }

    /** The faces whose known stickers fit the true face in no turn ([truth], URFDLB): shown wrong, whatever the rotation. */
    private fun wrongFaces(s: VideoScanState, truth: String): List<Face> = Face.entries.filter { f ->
        val known = (0 until 9).map { s.stickers[f.ordinal * 9 + it] }
        (0 until 4).none { k -> RotationSearch.turned(known, k).withIndex().all { (n, c) -> c == null || c.letter == truth[f.ordinal * 9 + n] } }
    }

    @Test
    fun videosOf20261007ReadTheTrueCube() {
        val camera = replay(VideoFixtures.CAMERA_1007)
        // Its last second: two red stickers of the white face in glare read pale pink, known white (scan-centre-naming, known limitation).
        assertKnownTrue(VideoFixtures.CAMERA_1007, camera, VideoFixtures.TRUTH_1007, allowWrong = 2)
        println("${VideoFixtures.CAMERA_1007}: ${camera.last().recognised} known at the end")
        val tour = replay(VideoFixtures.TOUR_1007)
        assertKnownTrue(VideoFixtures.TOUR_1007, tour, VideoFixtures.TRUTH_1007)
        val finish = tour.indexOfFirst { it.finished }
        println("${VideoFixtures.TOUR_1007} frames to finish: $finish (before scan-centre-naming: 280)")
        // The yellow stickers on the blue face, seen in dimmer light than the washed-out yellow centre, read yellow.
        assertTrue(finish in 0..170, "finishes by frame 170: $finish")
        assertEquals(VideoFixtures.TRUTH_1007, tour[finish].stickers.joinToString("") { it!!.letter.toString() })
    }

    @Test
    fun blueFaceFirstIsNeverShownAsWhite() {
        // scan-centre-naming: the blue face alone at the start, its centre pale (named white by the palette), as in the web test of 14:55.
        val states = replay(VideoFixtures.blueFirst(30))
        for (n in 0 until 30) assertEquals(emptyList(), wrongFaces(states[n], VideoFixtures.TRUTH_1007), "frame $n")
        val finish = states.indexOfFirst { it.finished }
        println("blue face first: frames to finish $finish (before scan-centre-naming: 310)")
        assertTrue(finish in 0..220, "finishes: $finish")
        assertEquals(VideoFixtures.TRUTH_1007, states[finish].stickers.joinToString("") { it!!.letter.toString() })
    }

    @Test
    fun orangeFaceFirstNeverShowsTheRedSideWrong() {
        // The user's camera video 20261007_152753: blue on top, the orange face named red until the red face is seen at frame 78.
        val states = replay(VideoFixtures.BLUE_FIRST_1007)
        val finish = states.indexOfFirst { it.finished }
        println("${VideoFixtures.BLUE_FIRST_1007} frames to finish: $finish (before scan-centre-naming: never)")
        for (n in 0..(if (finish >= 0) finish else states.lastIndex)) assertEquals(emptyList(), wrongFaces(states[n], VideoFixtures.TRUTH_1007), "frame $n")
        assertTrue(finish >= 0, "finishes")
        assertEquals(VideoFixtures.TRUTH_1007, states[finish].stickers.joinToString("") { it!!.letter.toString() })
    }

    @Test
    fun centresNamedTogetherEachColourOnce() {
        val p = ColorClassifier.DEFAULT_PALETTE
        val three = ColorClassifier.centreNamingCosts(listOf(p.getValue(CubeColor.RED), p.getValue(CubeColor.WHITE), p.getValue(CubeColor.GREEN))).first().second
        assertEquals(listOf(CubeColor.RED, CubeColor.WHITE, CubeColor.GREEN), three)
        // A pale blue that the palette alone names white, next to the white face: blue and white.
        val pale = VideoFixtures.paleBlue(p.getValue(CubeColor.BLUE))
        assertEquals(CubeColor.WHITE, ColorClassifier.rankedCentre(pale).first())
        val five = listOf(CubeColor.WHITE, CubeColor.RED, CubeColor.GREEN, CubeColor.ORANGE).map { p.getValue(it) } + pale
        val named = ColorClassifier.centreNamingCosts(five).first().second
        assertEquals(listOf(CubeColor.WHITE, CubeColor.RED, CubeColor.GREEN, CubeColor.ORANGE, CubeColor.BLUE), named)
        assertEquals(setOf(CubeColor.YELLOW), CubeColor.entries.toSet() - named.toSet(), "the sixth follows")
        val six = CubeColor.entries.reversed()
        assertEquals(six, ColorClassifier.centreNamingCosts(six.map { p.getValue(it) }).first().second)
        assertEquals(720, ColorClassifier.centreNamingCosts(six.map { p.getValue(it) }).size)
    }

    /** [reading] of [face] with its centre made pale until the palette names it white. */
    private fun paleCentre(face: Face, centre: Point = Point(100.0, 100.0)) =
        reading(face, centre = centre).let { r -> r.copy(colors = r.colors.toMutableList().also { it[4] = VideoFixtures.paleBlue(it[4]!!) }) }

    @Test
    fun aPaleBlueFaceAloneShowsNothingUntilTheWhiteFaceIsSeen() {
        val scan = VideoScan(engine = engine)
        var s = VideoScanState.EMPTY
        repeat(6) { s = scan.onFrame(listOf(paleCentre(Face.B)), it * 100L) }
        assertEquals(0, s.recognised, "doubtful between white and blue: nothing known, not even the centre")
        repeat(6) { s = scan.onFrame(listOf(reading(Face.U)), 600 + it * 100L) }
        assertEquals(CubeColor.WHITE, s.stickers[Face.U.ordinal * 9 + 4])
        assertEquals(CubeColor.BLUE, s.stickers[Face.B.ordinal * 9 + 4], "two piles, named blue and white")
        assertTrue(recognisedOn(s, Face.B).isNotEmpty(), "the blue face's stickers become known")
        for (i in 0 until Stickers.COUNT) s.stickers[i]?.let { assertEquals(truth[i], it, "sticker $i") }
    }

    @Test
    fun yellowStickersInDimLightBesideAWashedOutYellowCentreReadYellow() {
        // TOUR 2026-10-07: the yellow centre washed out to near white (a1d0ac), yellow stickers elsewhere dim olive (7c8933).
        val washed = reading(Face.D).let { r -> r.copy(colors = r.colors.toMutableList().also { it[4] = Rgb.fromHex("a1d0ac") }) }
        val olive = Rgb.fromHex("7c8933")
        val u = reading(Face.U).let { r -> r.copy(colors = r.colors.map { c -> if (c == ColorClassifier.DEFAULT_PALETTE.getValue(CubeColor.YELLOW)) olive else c }) }
        val green = reading(Face.F).let { r -> r.copy(colors = r.colors.map { c -> if (c == ColorClassifier.DEFAULT_PALETTE.getValue(CubeColor.GREEN)) Rgb.fromHex("1e6a35") else c }) }
        val scan = VideoScan(engine = engine)
        var t = 0L
        for (face in listOf(washed, green, u)) repeat(4) { scan.onFrame(listOf(face), t); t += 100 }
        val yellowOnU = (0 until 9).filter { truth[Face.U.ordinal * 9 + it] == CubeColor.YELLOW }
        assertTrue(yellowOnU.isNotEmpty())
        if (engine == ScanEngine.RULES) {
            // Known limit of the rules scanner (scan-rules design, "Known limits"): the washed-out yellow face
            // shown alone first looks whiter than the white face itself, and nothing else tells them apart
            // until a view of neighbours; it is taken for the white face meanwhile. It never finishes so.
            assertTrue(!scan.state.complete)
            return
        }
        for (n in yellowOnU) assertEquals(CubeColor.YELLOW, scan.state.stickers[Face.U.ordinal * 9 + n] ?: scan.state.leading[Face.U.ordinal * 9 + n], "U $n")
    }

    @Test
    fun aDarkBlueCentreNamedWhiteDoesNotSpoilTheWhiteFace() {
        // scan-centre-clash, 2026-10-07: the cube still on the table for ~10 s, white on top, blue on the right in shadow.
        val frames = VideoFixtures.load(VideoFixtures.WEB_1007).filter { it.faces.size == 3 }
        val pale = frames.map { f ->
            f.copy(faces = f.faces.map { r -> if (ColorClassifier.rankedCentre(r.colors[4]!!).first() == CubeColor.BLUE) r.copy(colors = r.colors.toMutableList().also { it[4] = VideoFixtures.paleBlue(it[4]!!) }) else r })
        }
        assertTrue(pale.flatMap { it.faces }.count { ColorClassifier.rankedCentre(it.colors[4]!!).first() == CubeColor.WHITE } >= 2 * pale.size, "two faces named white per picture")
        fun still(list: List<VideoFixtures.Frame>) = replay(List(100) { list[it % list.size] }).last()
        fun white(s: VideoScanState) = (0 until 9).map { s.stickers[Face.U.ordinal * 9 + it] ?: s.leading[Face.U.ordinal * 9 + it] }
        val asSeen = still(frames)
        val s = still(pale)
        // The white face reads as it does with the blue centre named blue (one corner in shadow reads blue in this fixture either way).
        assertEquals(white(asSeen), white(s))
        val trueWhite = VideoFixtures.TRUTH_1007.substring(Face.U.ordinal * 9, Face.U.ordinal * 9 + 9)
        assertTrue((0 until 4).maxOf { k -> RotationSearch.turned(white(s), k).withIndex().count { (n, c) -> c?.letter == trueWhite[n] } } >= 8)
        assertTrue(recognisedOn(s, Face.B).isNotEmpty(), "the blue face gets its readings")
    }

    @Test
    fun twoFacesOfOnePictureNamingTheSameCentreAreToldApart() {
        // B's centre read paler than U's own white: U keeps white, B takes its next-best colour (blue).
        val u = reading(Face.U, centre = Point(100.0, 110.0))
        val b = reading(Face.B, centre = Point(190.0, 200.0)).let { r -> r.copy(colors = r.colors.toMutableList().also { it[4] = VideoFixtures.paleBlue(it[4]!!) }) }
        assertEquals(CubeColor.WHITE, ColorClassifier.rankedCentre(b.colors[4]!!).first())
        val scan = VideoScan(engine = engine)
        var s = VideoScanState.EMPTY
        repeat(4) { s = scan.onFrame(listOf(u, b), it * 100L) }
        for (n in 0 until 9) s.stickers[Face.U.ordinal * 9 + n]?.let { assertEquals(truth[Face.U.ordinal * 9 + n], it, "U $n") }
        assertEquals(CubeColor.BLUE, s.stickers[Face.B.ordinal * 9 + 4])
        assertEquals(aloneCount(Face.U, (0 until 9) - 4), recognisedOn(s, Face.U).size)
    }

    @Test
    fun aFaceKnownWrongIsPutRightByLaterClearViews() {
        val scan = VideoScan(engine = engine)
        // Sixty readings with three stickers wrong (too many to agree with the right ones), then forty right ones.
        val right = reading(Face.U)
        val names = (0 until 9).map { truth[Face.U.ordinal * 9 + it] }
        val bad = right.copy(colors = right.colors.mapIndexed { n, c -> if (n in setOf(0, 1, 2)) ColorClassifier.DEFAULT_PALETTE.getValue(CubeColor.entries.first { it != names[n] && it != names[4] && it != CubeColor.WHITE }) else c })
        repeat(60) { scan.onFrame(listOf(bad), it * 100L) }
        assertTrue((0..2).all { scan.state.stickers[Face.U.ordinal * 9 + it] != null && scan.state.stickers[Face.U.ordinal * 9 + it] != names[it] }, "known wrong first")
        repeat(40) { scan.onFrame(listOf(right), 6_000 + it * 100L) }
        for (n in 0 until 9) assertEquals(aloneShows(Face.U, n), scan.state.stickers[Face.U.ordinal * 9 + n], "sticker $n")
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
        val scan = VideoScan(engine = engine)
        var t = 0L
        repeat(4) { scan.onFrame(listOf(reading(Face.U)), t++ * 100) }
        val before = scan.state.stickers.toList()
        assertEquals(aloneCount(Face.U, 0 until 9), before.count { it != null })
        val after = scan.onFrame(listOf(reading(Face.U, wrong = 0)), t * 100)
        assertEquals(before, after.stickers)
        assertTrue(after.contradictions.isEmpty())
    }

    @Test
    fun aFaceTurnedOnScreenVotesForTheSameStickers() {
        val scan = VideoScan(engine = engine)
        repeat(3) { scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        val before = scan.state.stickers.toList()
        repeat(6) { scan.onFrame(listOf(reading(Face.U, turn = 1)), 300L + it * 100) }
        assertEquals(before, scan.state.stickers)
    }

    @Test
    fun cornerViewSettlesTheRotationAndThePose() {
        val scan = VideoScan(engine = engine)
        // F in front, U above it on screen: U's bottom side touches F, F's top side touches U.
        val f = reading(Face.F, centre = Point(100.0, 200.0))
        val u = reading(Face.U, centre = Point(100.0, 110.0))
        var s = VideoScanState.EMPTY
        repeat(4) { s = scan.onFrame(listOf(f, u), it * 100L) }
        assertEquals(Pose(Face.F, Face.U), s.pose)
        for (face in listOf(Face.U, Face.F)) for (n in 0 until 9) assertEquals(aloneShows(face, n), s.stickers[face.ordinal * 9 + n])
    }

    @Test
    fun orientationFollowsTheSettledFrontFaceAndIsNullWithoutOne() {
        val scan = VideoScan(engine = engine)
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
        if (engine == ScanEngine.RULES) return threeReadingsRecogniseAFace()
        val scan = VideoScan(engine = engine)
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

    /** The rules scanner: a face followed for [FaceTracks.MIN_READINGS] readings shows its stickers, named in the reading's own order. */
    private fun threeReadingsRecogniseAFace() {
        val scan = VideoScan(engine = engine)
        var s = VideoScanState.EMPTY
        repeat(FaceTracks.MIN_READINGS - 1) { s = scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        assertEquals(0, s.recognised, "not yet")
        val first = (0 until 6).map { scan.onFrame(listOf(reading(Face.U)), 1_000L + it * 100) }.indexOfFirst { it.recognised > 0 }
        println("rules scanner: a face alone shows after ${FaceTracks.MIN_READINGS - 1 + first + 1} readings")
        assertTrue(first in 0..2, "shows within a few readings: $first")
        s = scan.state
        assertEquals((0 until 9).map { aloneShows(Face.U, it) != null }, s.found.single().recognised)
        assertEquals(9, s.found.single().names.count { it != null })
        for (n in 0 until 9) assertEquals(aloneShows(Face.U, n), s.stickers[Face.U.ordinal * 9 + n])
    }

    /** A corner view of F (U above it) four times from [start]: F and U settle and a projection is built. */
    private fun settleCorner(scan: VideoScan, start: Long): VideoScanState {
        var s = VideoScanState.EMPTY
        repeat(4) { s = scan.onFrame(listOf(reading(Face.F, centre = Point(100.0, 200.0)), reading(Face.U, centre = Point(100.0, 110.0))), start + it * 100L) }
        return s
    }

    @Test
    fun theProjectionIsHeldOverAFramelessGapAndAges() {
        val scan = VideoScan(engine = engine)
        val built = settleCorner(scan, 0)
        val projection = assertNotNull(built.projection)
        assertEquals(0, built.projectionAge)
        val gap = scan.onFrame(emptyList(), 500)
        assertEquals(projection, gap.projection, "held unchanged without faces")
        assertEquals(200, gap.projectionAge)
        assertNull(scan.onFrame(emptyList(), 300 + VideoScan.HOLD_MILLIS + 1).projection, "dropped after a while")
    }

    @Test
    fun aHeldProjectionMovesOntoTheFaceFound() {
        val scan = VideoScan(engine = engine)
        val projection = assertNotNull(settleCorner(scan, 0).projection)
        // L seen alone, its rotation not settled: no orientation, the held projection follows L.
        val l = reading(Face.L, centre = Point(40.0, 300.0))
        val s = scan.onFrame(listOf(l), 400)
        assertNull(s.orientation)
        val moved = assertNotNull(s.projection)
        val lCentre = moved.points[Face.L.ordinal * 9 + 4]
        assertTrue((lCentre - l.centre).length < 1e-6, "L's centre lies on the face found: $lCentre")
        assertEquals(projection.facing, moved.facing)
        assertEquals(0, s.projectionAge)
    }

    @Test
    fun resetClearsTheHeldProjection() {
        val scan = VideoScan(engine = engine)
        settleCorner(scan, 0)
        scan.reset()
        assertNull(scan.onFrame(emptyList(), 500).projection)
    }

    /** Places of [face] known in [state], the centre left out (it is known as soon as the face is seen). */
    private fun recognisedOn(state: VideoScanState, face: Face) = (0 until 9).filter { it != 4 && state.stickers[face.ordinal * 9 + it] != null }

    /**
     * [face]'s sticker [n] as a face seen without the red and orange centres shows it: the rules scanner
     * holds red and orange until both those centres are known (scan-rules-phone).
     */
    private fun aloneShows(face: Face, n: Int): CubeColor? =
        truth[face.ordinal * 9 + n].takeUnless { engine == ScanEngine.RULES && n != 4 && it in FaceTracks.WARM }

    /** How many of [face]'s places [places] such a face shows. */
    private fun aloneCount(face: Face, places: Iterable<Int>) = places.count { aloneShows(face, it) != null }

    @Test
    fun aPartialFaceVotesForTheStickersItShows() {
        val scan = VideoScan(engine = engine)
        repeat(2) { scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        // The rules scanner follows a face by where it is: a face turned a quarter between two pictures on
        // the same lattice is another face to it, and a partial reading only continues one.
        val turn = if (engine == ScanEngine.LOOK) 1 else 0
        val s = scan.onFrame(listOf(reading(Face.U, turn = turn, missing = setOf(1))), 200)
        // Missing place 1 of the reading is one place of the face; the other seven around the centre reach three votes.
        assertEquals(aloneCount(Face.U, listOf(0, 2, 3, 5, 6, 7, 8)), recognisedOn(s, Face.U).size)
        for (n in recognisedOn(s, Face.U)) assertEquals(truth[Face.U.ordinal * 9 + n], s.stickers[Face.U.ordinal * 9 + n])
    }

    @Test
    fun aPartialFaceWithAWrongLatticeDoesNotVote() {
        val scan = VideoScan(engine = engine)
        repeat(2) { scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        // The stickers of another face around the right centre: a lattice across the cube's edge.
        val r = reading(Face.R, missing = setOf(0))
        val wrong = r.copy(colors = r.colors.toMutableList().also { it[4] = reading(Face.U).colors[4] })
        val s = scan.onFrame(listOf(wrong, wrong), 200)
        assertEquals(emptyList(), recognisedOn(s, Face.U))
    }

    @Test
    fun aPartialFaceWithoutItsCentreOrBeforeAnyFullFaceIsIgnored() {
        val scan = VideoScan(engine = engine)
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
        val scan = VideoScan(engine = engine)
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
        val scan = VideoScan(engine = engine)
        val first = scan.onFrame(listOf(dark(Face.U, 0.2)), 0)
        assertTrue(first.dim)
        assertNull(first.stall)
        var s = first
        for (t in 100L..7_900L step 100) s = scan.onFrame(listOf(dark(Face.U, 0.2)), t)
        assertNull(s.stall, "not before eight seconds")
        s = scan.onFrame(listOf(dark(Face.U, 0.2)), 8_000)
        assertEquals(Stall.DARK, s.stall)
        assertTrue(!scan.onFrame(listOf(reading(Face.U)), 8_100).dim)
    }

    @Test
    fun noCubeForAWhileStalls() {
        val scan = VideoScan(engine = engine)
        scan.onFrame(listOf(reading(Face.U)), 0)
        assertNull(scan.onFrame(emptyList(), 12_000).stall)
        assertEquals(Stall.NO_CUBE, scan.onFrame(emptyList(), 13_000).stall)
        assertNull(scan.onFrame(listOf(reading(Face.U)), 13_100).stall, "cube back in view")
    }

    @Test
    fun theSameFaceForTwentySecondsStallsAndRestartClearsIt() {
        val scan = VideoScan(engine = engine)
        var s = VideoScanState.EMPTY
        for (t in 0L..19_000L step 200) s = scan.onFrame(listOf(reading(Face.U)), t)
        assertNull(s.stall)
        for (t in 19_200L..20_400L step 200) s = scan.onFrame(listOf(reading(Face.U)), t)
        assertEquals(Stall.STUCK, s.stall)
        scan.reset()
        assertEquals(0, scan.state.recognised)
        val again = scan.onFrame(listOf(reading(Face.U)), 20_600)
        assertNull(again.stall)
        // The rules scanner shows a face once it has been followed for a few readings (FaceTracks.MIN_READINGS).
        assertEquals(if (engine == ScanEngine.LOOK) 1 else 0, again.recognised, "only the centre after one frame")
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
        val scan = VideoScan(engine = engine)
        repeat(3) { scan.onFrame(listOf(washed(Face.U)), it * 100L) }
        assertEquals(listOf(4), (0 until 9).filter { scan.state.stickers[Face.U.ordinal * 9 + it] != null }, "three washed-out readings make only the centre known")
    }

    @Test
    fun aSideReadManyTimesButNotConfirmedHasNoTick() {
        val scan = VideoScan(engine = engine)
        repeat(20) { scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        assertEquals(aloneCount(Face.U, 0 until 9), recognisedOn(scan.state, Face.U).size + 1, "all known from their own votes")
        assertTrue(scan.state.confirmed.isEmpty(), "${scan.state.confirmed}")
        val faces = Face.entries.map { reading(it, centre = Point(100.0, 100.0 + 300 * it.ordinal)) }
        var t = 2_000L
        for (face in faces) repeat(10) { if (!scan.state.complete) scan.onFrame(listOf(face), t).also { t += 100 } }
        assertEquals(Face.entries.toSet(), scan.state.confirmed)
    }

    @Test
    fun aFaceHeldLongStillShowsItsReading() {
        val scan = VideoScan(engine = engine)
        var s = VideoScanState.EMPTY
        // More frames than a face keeps readings: the newest is never the one dropped.
        repeat(VideoScan.MAX_READINGS + 20) { s = scan.onFrame(listOf(reading(Face.U)), it * 100L) }
        assertEquals(9, s.found.single().names.count { it != null })
    }

    @Test
    fun aColourLeadingOnSharesAloneDoesNotBreakTheScan() {
        // Sticker 0 (first in reading order) read between red and orange (named red) and between orange and yellow (named
        // yellow): orange leads on the shares, though no reading named it (crash in the browser, 2026-10-05).
        val refs = ColorClassifier.references()
        fun mix(a: CubeColor, b: CubeColor, t: Double): Rgb {
            val x = ColorClassifier.DEFAULT_PALETTE.getValue(a)
            val y = ColorClassifier.DEFAULT_PALETTE.getValue(b)
            return Rgb((x.r + (y.r - x.r) * t).toInt(), (x.g + (y.g - x.g) * t).toInt(), (x.b + (y.b - x.b) * t).toInt())
        }
        fun split(a: CubeColor) = (1..99).map { mix(a, CubeColor.ORANGE, it / 100.0) }
            .first { ColorClassifier.shares(it.toLab(), refs)[CubeColor.ORANGE.ordinal] in 0.35..0.5 }
        val nearRed = split(CubeColor.RED)
        val nearYellow = split(CubeColor.YELLOW)
        assertEquals(CubeColor.RED, ColorClassifier.live(nearRed))
        assertEquals(CubeColor.YELLOW, ColorClassifier.live(nearYellow))
        val scan = VideoScan(engine = engine)
        val u = reading(Face.U)
        fun show(t: Long, vararg set: Pair<Int, Rgb>) = scan.onFrame(listOf(u.copy(colors = u.colors.toMutableList().also { c -> for ((n, rgb) in set) c[n] = rgb })), t)
        val orange = ColorClassifier.DEFAULT_PALETTE.getValue(CubeColor.ORANGE)
        val wrong = ColorClassifier.DEFAULT_PALETTE.getValue(CubeColor.entries.first { it != u.colors.let { c -> ColorClassifier.live(c[1]!!) } && it != CubeColor.ORANGE })
        // First read clearly orange (it sticks), with two other stickers misread: once the split readings
        // outnumber them they no longer belong (the anchor moves), and no reading names orange any more.
        repeat(4) { show(it * 100L, 0 to orange, 1 to wrong, 2 to wrong) }
        repeat(VideoScan.MAX_READINGS + 10) { k -> show(400L + k * 100, 0 to if (k % 2 == 0) nearRed else nearYellow) }
        scan.outcome()
    }

    /** [reading] with cells [size] across. */
    private fun sized(face: Face, centre: Point, size: Double) = reading(face, centre = centre).copy(u = Point(size, 0.0), v = Point(0.0, size))

    @Test
    fun stickersAreKnownBeforeTheCameraLocksWhenTheCubeTurnsFromTheStart() {
        // 2026-10-07: two faces in view, the cube turned in the hand so the larger face swaps, darker=0.
        val scan = VideoScan(engine = engine)
        val exposure = ExposureControl()
        var knownBeforeLock = 0
        var lockedAt: Long? = null
        for (t in 0L..2_000L step 33) {
            val big = (t / 33) % 2 == 0L
            val faces = listOf(sized(Face.F, Point(100.0, 200.0), if (big) 34.0 else 26.0), sized(Face.U, Point(100.0, 110.0), if (big) 26.0 else 34.0))
            exposure.onFrame(faces, 200, 300, t)
            val state = scan.onFrame(faces, t)
            if (exposure.phase == ExposureControl.Phase.LOCKED) lockedAt = lockedAt ?: t else knownBeforeLock = state.recognised
        }
        assertTrue(knownBeforeLock > 0, "stickers known before the lock")
        assertTrue(lockedAt!! <= ExposureControl.METER_LIMIT_MILLIS + 33, "locked at $lockedAt")
        assertEquals(0, exposure.settings.darker)
    }

    @Test
    fun readingGoesOnWhileTheCameraMetersAgainAfterATorchChange() {
        val scan = VideoScan(engine = engine)
        val exposure = ExposureControl()
        var t = 0L
        while (exposure.phase != ExposureControl.Phase.LOCKED) {
            listOf(reading(Face.U)).let { exposure.onFrame(it, 200, 200, t); scan.onFrame(it, t) }
            t += 100
        }
        val before = scan.state.recognised
        exposure.onTorch(true, t)
        repeat(4) {
            t += 100
            listOf(reading(Face.F)).let { exposure.onFrame(it, 200, 200, t); scan.onFrame(it, t) }
        }
        assertEquals(ExposureControl.Phase.METERING, exposure.phase)
        assertTrue(scan.state.recognised > before, "${scan.state.recognised} after $before")
    }

    /** The user's striped pattern cube of the 2026-10-07 18:19 web test (white and yellow solid, stripes round the sides). */
    private val striped = Cube.fromColorString("WWWWWWWWWBRGBRGBRGOGROGROGRYYYYYYYYYGOBGOBGOBRBORBORBO")

    /** A red centre faded towards orange until the palette names it orange. */
    private val orangeRed: Rgb = ColorClassifier.DEFAULT_PALETTE.let { p ->
        val r = p.getValue(CubeColor.RED)
        val o = p.getValue(CubeColor.ORANGE)
        (1..100).map { t -> Rgb(r.r + (o.r - r.r) * t / 100, r.g + (o.g - r.g) * t / 100, r.b + (o.b - r.b) * t / 100) }
            .first { ColorClassifier.rankedCentre(it).first() == CubeColor.ORANGE }
    }

    /** [face] of [cube] in its net order as the default palette reads it, the red centre looking orange. */
    private fun stripedReading(cube: Cube, face: Face, centre: Point): FaceReading {
        val colors = (0 until 9).map { n ->
            val c = cube[face.ordinal * 9 + n]
            if (n == 4 && c == CubeColor.RED) orangeRed else ColorClassifier.DEFAULT_PALETTE.getValue(c)
        }
        return FaceReading(colors, centre, Point(30.0, 0.0), Point(0.0, 30.0))
    }

    @Test
    fun aRedCentreLookingOrangeIsNamedRedOnceTheOtherFiveAreSure() {
        // scan-steady-progress, web test 2026-10-07 18:19: the red face stayed unnamed for 40 s.
        val scan = VideoScan(engine = engine)
        var t = 0L
        // Each view somewhere else in the picture: turning the cube to the next side turns the top face's
        // lattice too, so a camera would not follow the top face from one view to the next as one face.
        fun corner(top: Face, bottom: Face, times: Int = 4) = repeat(times) {
            val x = 100.0 + 200 * (top.ordinal * 6 + bottom.ordinal)
            scan.onFrame(listOf(stripedReading(striped, bottom, Point(x, 200.0)), stripedReading(striped, top, Point(x, 110.0))), t)
            t += 100
        }
        for (side in listOf(Face.F, Face.L, Face.B)) corner(Face.U, side)
        corner(Face.F, Face.D)
        val known = ArrayList<Int>()
        repeat(12) {
            corner(Face.U, Face.R, times = 1)
            known += scan.state.recognised
        }
        assertEquals(known.sorted(), known, "progress never goes back: $known")
        val s = scan.state
        assertEquals(CubeColor.RED, s.stickers[Face.R.ordinal * 9 + 4], "the red centre is named red")
        assertEquals(8, recognisedOn(s, Face.R).size, "the red face's stickers are known")
        assertTrue(s.complete, "the scan finishes")
        for (i in 0 until Stickers.COUNT) assertEquals(striped[i], s.stickers[i], "sticker $i")
    }
}
