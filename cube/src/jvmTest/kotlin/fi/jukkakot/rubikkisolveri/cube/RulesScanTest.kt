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
        val striped = Cube.fromColorString(VideoFixtures.STRIPED_TRUTH)
        val o = p.getValue(CubeColor.ORANGE)
        val r = p.getValue(CubeColor.RED)
        val cameraOrange = Rgb((o.r + (r.r - o.r) * 0.7).toInt(), (o.g + (r.g - o.g) * 0.7).toInt(), (o.b + (r.b - o.b) * 0.7).toInt())
        assertTrue(ColorClassifier.rankedCentre(cameraOrange).first() == CubeColor.RED)
        val colors = { c: CubeColor -> if (c == CubeColor.ORANGE) cameraOrange else p.getValue(c) }
        val scan = VideoScan(engine = ScanEngine.RULES)
        var t = 0L
        var finished = false
        // Turning the cube in the hand: two faces at a time, then the corners round the top, each view
        // elsewhere in the picture (turning the cube turns the top face's lattice too).
        val edges = listOf(Edge.UF, Edge.UL, Edge.UB, Edge.UR, Edge.DF, Edge.DL)
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
        assertTrue(scan.centreLog.contains("L" + cameraOrange.toHex()), "the log carries the camera's orange: ${scan.centreLog}")
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
