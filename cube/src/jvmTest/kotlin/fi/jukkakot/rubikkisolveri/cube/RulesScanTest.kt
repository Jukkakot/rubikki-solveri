package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The rules scanner's own scenarios (`scan-rules`), beside [RulesVideoScanTest] which runs the shared ones. */
class RulesScanTest {
    private val cube = Cube.solved().apply("R U F' D2 L B")
    private val p = ColorClassifier.DEFAULT_PALETTE

    @Test
    fun aScanFinishedWithoutTheOrangeFaceKnowsItsRedAndOrangeStickers() {
        // Phone test 2026-10-08 07:43: the orange face never shown, the cube finished from the rules, yet red and
        // orange were still held as if not known, and the finished cube lacked them.
        val scan = VideoScan(engine = ScanEngine.RULES)
        val views = listOf(Edge.UF, Edge.UR, Edge.UB, Edge.DF, Edge.DR, Edge.DB, Edge.FR, Edge.BR).mapIndexed { k, e ->
            SyntheticViews.edge(cube, e, roll = 0.3, at = Point(200.0 + 300 * k, 300.0))
        } + listOf(Corner.URF, Corner.UBR, Corner.DFR, Corner.DRB).mapIndexed { k, c -> SyntheticViews.corner(cube, c, roll = 0.5, at = Point(200.0 + 300 * k, 900.0)) }
        var t = 0L
        var finished: fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState? = null
        for (round in 0 until 3) for (view in views) repeat(6) {
            val s = scan.onFrame(view, t)
            t += 100
            if (s.finished && finished == null) finished = s
        }
        val s = finished
        assertTrue(s != null, "finishes")
        assertTrue((0 until Stickers.COUNT).all { s.stickers[it] == cube[it] }, "every sticker known and right: ${s.stickers.joinToString("") { it?.letter?.toString() ?: "." }}")
    }

    @Test
    fun whiteAndPaleYellowSideBySideAreNeverWhiteAndYellowTogether() {
        // The front face's centre pale yellow (as a white centre can look in warm light): by its look white or yellow.
        val y = p.getValue(CubeColor.YELLOW)
        val pale = Rgb((y.r + 255) / 2, (y.g + 255) / 2, (y.b + 255) / 2)
        val scan = VideoScan(engine = ScanEngine.RULES)
        repeat(20) { k ->
            val s = scan.onFrame(SyntheticViews.edge(cube, Edge.UF, roll = 0.2, centre = mapOf(Face.F to pale)), k * 100L)
            val named = s.found.mapNotNull { it.names[4] }.toSet()
            assertTrue(!named.containsAll(setOf(CubeColor.WHITE, CubeColor.YELLOW)), "frame $k: $named")
            assertNotEquals(CubeColor.YELLOW, s.stickers[Face.D.ordinal * 9 + 4], "frame $k: no face beside the white one is the yellow one")
        }
    }

    @Test
    fun orangeThatTheCameraShowsRedIsNeverShownRedAndTheScanFinishes() {
        // The phone test of 2026-10-08: the camera's orange (stickers and centre) well on the red side of the palette.
        val o = p.getValue(CubeColor.ORANGE)
        val r = p.getValue(CubeColor.RED)
        val cameraOrange = Rgb((o.r + (r.r - o.r) * 0.7).toInt(), (o.g + (r.g - o.g) * 0.7).toInt(), (o.b + (r.b - o.b) * 0.7).toInt())
        assertTrue(ColorClassifier.rankedCentre(cameraOrange).first() == CubeColor.RED)
        val scan = scanStriped({ c -> if (c == CubeColor.ORANGE) cameraOrange else p.getValue(c) }, listOf(Edge.UF, Edge.UL, Edge.UB, Edge.UR, Edge.DF, Edge.DL))
        assertTrue(scan.centreLog.contains("L" + cameraOrange.toHex()), "the log carries the camera's orange: ${scan.centreLog}")
    }

    @Test
    fun theCamerasOwnColoursOfTheSecondPhoneTestFinishRight() {
        // The centres as the phone's camera read them (scan log 2026-10-08 07:43, the stuck scan): the orange
        // centre b53116 and the red 810412 were both taken for the red face. Orange seen before red, as there.
        val camera = mapOf(
            CubeColor.WHITE to "c0d8ef", CubeColor.YELLOW to "7e8d1c", CubeColor.GREEN to "0e7f38",
            CubeColor.BLUE to "005597", CubeColor.RED to "810412", CubeColor.ORANGE to "b53116",
        ).mapValues { Rgb.fromHex(it.value) }
        scanStriped(camera::getValue, listOf(Edge.UL, Edge.UF, Edge.UR, Edge.UB, Edge.DF, Edge.DR))
    }

    /**
     * The striped cube scanned in [colors] as the camera shows them: [edges] two faces at a time, then the
     * corners round the top, each view elsewhere in the picture (turning the cube turns the top face's
     * lattice too), three rounds. No orange sticker is ever shown red, and it finishes with the true cube.
     */
    private fun scanStriped(colors: (CubeColor) -> Rgb, edges: List<Edge>): VideoScan {
        val striped = Cube.fromColorString(VideoFixtures.STRIPED_TRUTH)
        val scan = VideoScan(engine = ScanEngine.RULES)
        var t = 0L
        var finished = false
        val corners = listOf(Corner.URF, Corner.UFL, Corner.ULB, Corner.UBR)
        val views = edges.mapIndexed { k, e -> SyntheticViews.edge(striped, e, roll = 0.3, at = Point(200.0 + 300 * k, 300.0), colors = colors) } +
            corners.mapIndexed { k, c -> SyntheticViews.corner(striped, c, roll = 0.5, at = Point(200.0 + 300 * k, 900.0), colors = colors) }
        for (round in 0 until 3) for (view in views) repeat(6) {
            val s = scan.onFrame(view, t)
            t += 100
            for (i in 0 until Stickers.COUNT) {
                val c = s.stickers[i] ?: continue
                assertTrue(!(striped[i] == CubeColor.ORANGE && c == CubeColor.RED), "frame ${t / 100}: sticker $i shown red, is orange")
            }
            if (s.finished && !finished) {
                finished = true
                assertTrue((0 until Stickers.COUNT).all { s.stickers[it] == striped[it] }, "finishes with the true cube")
            }
        }
        assertTrue(finished, "finishes")
        return scan
    }

    @Test
    fun aClearCubeFinishesWhileNewFacesKeepComingIntoView() {
        // Third phone test (2026-10-08 09:11): the cube clear, yet it never finished while the cube was turned in
        // the hand: a new face track, its face known but its turn not yet, read against the cube in the turn it
        // was guessed in and revoked the finish before its half second came. Here the phone scan played until
        // clear, then its frames again with the faces somewhere new at once and every third frame (new tracks).
        val frames = VideoFixtures.load(VideoFixtures.PHONE_SCAN_1)
        val scan = VideoScan(engine = ScanEngine.RULES)
        var t = 0L
        val clearAt = frames.indexOfFirst { f -> scan.onFrame(f.faces, t).also { t += 100 }.complete }
        assertTrue(clearAt >= 0, "the scan becomes clear")
        val clearFrom = t - 100
        var finishedAt: Long? = null
        for ((k, faces) in frames.map { it.faces }.filter { it.isNotEmpty() }.withIndex()) {
            val s = scan.onFrame(faces.map { it.copy(centre = it.centre + Point((k / 3 + 1) * 400.0, 0.0)) }, t)
            t += 100
            if (s.finished) {
                finishedAt = t - 100
                assertTrue(s.stickers.joinToString("") { it?.letter?.toString() ?: "?" } == VideoFixtures.STRIPED_TRUTH, "finishes right")
                break
            }
        }
        val after = finishedAt?.minus(clearFrom)
        assertTrue(after != null && after <= VideoScan.FINISH_MILLIS + 100, "finishes half a second after the cube is clear (clear at frame $clearAt, finished after $after ms)")
    }

    @Test
    fun aLongScanReadsAsQuicklyLateAsEarly() {
        // Third phone test: the rules scanner fell from 14 to 5 pictures a second as face tracks piled up.
        // A recording that never finishes, played three times over: the tracks worked through stay bounded
        // and the last time through is not much slower than the one before.
        val frames = VideoFixtures.load(VideoFixtures.STRIPED_DIM).let { it + it + it }
        VideoScan(engine = ScanEngine.RULES).let { warm -> frames.take(200).forEachIndexed { i, f -> warm.onFrame(f.faces, i * 100L) } }
        val scan = VideoScan(engine = ScanEngine.RULES)
        val tracks = VideoScan::class.java.getDeclaredField("tracks").also { it.isAccessible = true }
        val third = frames.size / 3
        val nanos = LongArray(frames.size)
        var most = 0
        frames.forEachIndexed { i, f ->
            val start = System.nanoTime()
            scan.onFrame(f.faces, i * 100L)
            nanos[i] = System.nanoTime() - start
            most = maxOf(most, (tracks.get(scan) as FaceTracks).workingTracks)
        }
        // The second time through against the third: the first holds the cheap start, before faces are told.
        val second = nanos.drop(third).take(third).average()
        val last = nanos.drop(2 * third).average()
        assertTrue(most <= 40, "tracks worked through stay bounded: at most $most")
        assertTrue(last <= 1.5 * second, "the last time through (%.1f ms) is not much slower than the second (%.1f ms)".format(last / 1e6, second / 1e6))
    }

    @Test
    fun aFirstFaceThatCouldBeEitherOfTwoDoesNotFinishNorAskToTurnTheCube() {
        // A centre halfway between red and orange, the face only ever seen alone: red or orange stays open,
        // but a first face followed alone does not ask to turn the cube (it showed at once in the phone test
        // of 2026-10-08); the hint waits until most faces are read.
        val red = p.getValue(CubeColor.RED)
        val orange = p.getValue(CubeColor.ORANGE)
        val between = Rgb((red.r + orange.r) / 2, (red.g + orange.g) / 2, (red.b + orange.b) / 2)
        val scan = VideoScan(engine = ScanEngine.RULES)
        val face = SyntheticViews.straight(cube, Face.R, at = Point(100.0, 100.0)).let { r -> r.copy(colors = r.colors.toMutableList().also { it[4] = between }) }
        var t = 0L
        while (t <= 2 * FaceTracks.HINT_MILLIS) {
            val s = scan.onFrame(listOf(face), t)
            assertTrue(!s.complete, "never finishes")
            assertTrue(!s.undecided, "does not ask to turn the cube at ${t}")
            t += 100
        }
    }
}
