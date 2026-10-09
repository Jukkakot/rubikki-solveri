package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.Tracker
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
        val scan = VideoScan()
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
        val scan = VideoScan()
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
        val scan = VideoScan()
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
        val scan = VideoScan()
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
    fun aLongScanStaysWithinItsBudgetPerPicture() {
        // Browser 1.0.333: the scan logic grew from 9 to 37 ms a picture as a long scan went on (`scan-speed-up-3`).
        // The long phone recordings (neither finishes): every 100 pictures average at most 5 ms a picture (11–13 before), and a
        // picture without faces late in the scan costs almost nothing. Tripled on CI (slower, shared runners).
        val scale = if (System.getenv("CI") != null) 3.0 else 1.0
        val videos = listOf("web_121505", VideoFixtures.PHONE_SCAN_2)
        // Warmed up on the same recordings: the JIT reaches every path the timing goes through.
        for (video in listOf(VideoFixtures.STRIPED) + videos) VideoScan().let { warm -> VideoFixtures.load(video).forEachIndexed { i, f -> warm.onFrame(f.faces, i * 100L) } }
        val tracks = VideoScan::class.java.getDeclaredField("tracks").also { it.isAccessible = true }
        for (video in videos) {
            val frames = VideoFixtures.load(video)
            val scan = VideoScan()
            // The windows by the thread's CPU time, not the clock: a parallel build (lint, the app's tests) made the
            // clock time four times longer. It ticks coarsely (15.6 ms on Windows), which a window's average evens out.
            val cpu = java.lang.management.ManagementFactory.getThreadMXBean()
            val cpuNanos = LongArray(frames.size)
            val nanos = LongArray(frames.size)
            var most = 0
            frames.forEachIndexed { i, f ->
                val startCpu = cpu.currentThreadCpuTime
                val start = System.nanoTime()
                scan.onFrame(f.faces, i * 100L)
                nanos[i] = System.nanoTime() - start
                cpuNanos[i] = cpu.currentThreadCpuTime - startCpu
                most = maxOf(most, (tracks.get(scan) as FaceTracks).workingTracks)
            }
            assertTrue(most <= 40, "$video: tracks worked through stay bounded: at most $most")
            val windows = cpuNanos.toList().chunked(100).map { it.average() / 1e6 }
            assertTrue(windows.all { it <= 5.0 * scale }, "$video: every 100 pictures at most %.1f ms a picture: %s".format(5.0 * scale, windows.joinToString { "%.2f".format(it) }))
            val empty = frames.indices.filter { it >= 200 && frames[it].faces.isEmpty() }.map { nanos[it] / 1e6 }.sorted()
            // The median by the clock: most such pictures skip the work (a load slows that little); the few that still work
            // it out (a state still moving) are in the windows.
            val median = empty[empty.size / 2]
            assertTrue(median <= 0.5 * scale, "$video: a picture without faces late in the scan at most %.1f ms (median): %.2f ms".format(0.5 * scale, median))
        }
    }

    @Test
    fun aTrackLeftAloneStillEndsOnTimeInPicturesWithoutFaces() {
        // Pictures that read nothing skip the scan's work (`scan-speed-up-3`), but its time limits still fall on
        // the first picture past them: a face out of view stops being live, a glimpse leaves the work.
        val tracks = FaceTracks()
        var t = 0L
        repeat(15) {
            tracks.onFrame(listOf(SyntheticViews.straight(cube, Face.F)), t)
            t += 100
        }
        val seenLast = t - 100
        val face = tracks.snapshot().single()
        assertTrue(face.face == Face.F && face.live, "settled and live: $face")
        while (t - seenLast <= Tracker.GAP_MILLIS) {
            tracks.onFrame(emptyList(), t)
            assertTrue(tracks.snapshot().single().live, "live ${t - seenLast} ms after")
            t += 100
        }
        tracks.onFrame(emptyList(), t)
        assertTrue(!tracks.snapshot().single().live, "not live ${t - seenLast} ms after")
        // A glimpse elsewhere (two readings, never counted) leaves the work on the first picture past the gap.
        repeat(2) {
            tracks.onFrame(listOf(SyntheticViews.straight(cube, Face.U, at = Point(600.0, 600.0))), t)
            t += 100
        }
        assertTrue(tracks.workingTracks == 2, "glimpse followed")
        val glimpsed = t - 100
        while (t - glimpsed <= Tracker.GAP_MILLIS) {
            tracks.onFrame(emptyList(), t)
            t += 100
        }
        assertTrue(tracks.workingTracks == 2, "kept until past the gap")
        tracks.onFrame(emptyList(), t)
        assertTrue(tracks.workingTracks == 1, "left at ${t - glimpsed} ms")
    }

    @Test
    fun aFirstFaceThatCouldBeEitherOfTwoDoesNotFinishNorAskToTurnTheCube() {
        // A centre halfway between red and orange, the face only ever seen alone: red or orange stays open,
        // but a first face followed alone does not ask to turn the cube (it showed at once in the phone test
        // of 2026-10-08); the hint waits until most faces are read.
        val red = p.getValue(CubeColor.RED)
        val orange = p.getValue(CubeColor.ORANGE)
        val between = Rgb((red.r + orange.r) / 2, (red.g + orange.g) / 2, (red.b + orange.b) / 2)
        val scan = VideoScan()
        val face = SyntheticViews.straight(cube, Face.R, at = Point(100.0, 100.0)).let { r -> r.copy(colors = r.colors.toMutableList().also { it[4] = between }) }
        var t = 0L
        while (t <= 2 * FaceTracks.HINT_MILLIS) {
            val s = scan.onFrame(listOf(face), t)
            assertTrue(!s.complete, "never finishes")
            assertTrue(!s.undecided, "does not ask to turn the cube at ${t}")
            t += 100
        }
    }

    @Test
    fun aTrackDoesNotSwitchBackAndForthWithoutNewReadings() {
        // `scan-track-settle`: a track's face and turn change only when something new speaks for it. A track that
        // goes A, B, A over three pictures without a new reading (`213929` #19 went `=none` ↔ `U?U2` for 20
        // pictures) fails; the same with new readings is counted for the log only.
        val field = VideoScan::class.java.getDeclaredField("tracks").also { it.isAccessible = true }
        fun state(x: FaceTracks.TrackInfo) = listOf(x.face, x.option, x.assigned, x.otherFace)
        val bare = ArrayList<String>()
        var withReadings = 0
        for (video in FLIP_FIXTURES) {
            val frames = if (video == "blueFirst") VideoFixtures.blueFirst(30) else VideoFixtures.load(video)
            val scan = VideoScan()
            val history = HashMap<Int, ArrayDeque<Pair<Int, FaceTracks.TrackInfo>>>()
            frames.forEachIndexed { i, f ->
                scan.onFrame(f.faces, i * 100L)
                for (info in (field.get(scan) as FaceTracks).snapshot()) {
                    val h = history.getOrPut(info.id) { ArrayDeque() }
                    h.addLast(i to info)
                    if (h.size > 3) h.removeFirst()
                    if (h.size < 3 || h.last().first - h.first().first != 2) continue
                    val (a, b, c) = h.map { it.second }
                    if (state(a) != state(c) || state(a) == state(b)) continue
                    if (a.newest == b.newest && b.newest == c.newest) bare += "$video #${info.id} at $i: ${state(a)} / ${state(b)}" else withReadings++
                }
            }
        }
        println("Track flips with new readings: $withReadings")
        assertTrue(bare.isEmpty(), "${bare.size} flips without new readings, first: ${bare.take(5)}")
    }

    private companion object {
        /** Every recording the scan tests use, and slices of the long ones (`scan-speed-up-3`'s replay list). */
        val FLIP_FIXTURES = listOf(
            "20261005_151828", "20261005_151903", "20261005_213729", "20261005_213817", "20261005_213850", "20261005_213929",
            "20261007_132049", "20261007_132721", "20261007_152753", "20261007_202058", "20261007_202156", "20261007_202318",
            "20261007_202403", "20261007_web", "web_084657", "web_121505", "web_181940",
            "web_084657:66-455", "web_084657:499-998", "web_121505:66-373", "web_121505:448-751", "blueFirst",
        )
    }
}
