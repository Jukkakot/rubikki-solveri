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
    fun aFaceThatCouldBeEitherOfTwoDoesNotFinishAndAsksToTurnTheCube() {
        // A centre halfway between red and orange, the face only ever seen alone: red or orange stays open.
        val red = p.getValue(CubeColor.RED)
        val orange = p.getValue(CubeColor.ORANGE)
        val between = Rgb((red.r + orange.r) / 2, (red.g + orange.g) / 2, (red.b + orange.b) / 2)
        val scan = VideoScan(engine = ScanEngine.RULES)
        val face = SyntheticViews.straight(cube, Face.R, at = Point(100.0, 100.0)).let { r -> r.copy(colors = r.colors.toMutableList().also { it[4] = between }) }
        var t = 0L
        var raisedAt: Long? = null
        while (t <= 4_000) {
            val s = scan.onFrame(listOf(face), t)
            assertTrue(!s.complete, "never finishes")
            if (s.undecided && raisedAt == null) raisedAt = t
            t += 100
        }
        val at = raisedAt
        assertTrue(at != null && at >= FaceTracks.HINT_MILLIS && at <= FaceTracks.HINT_MILLIS + 1_000, "asks to turn the cube after about two seconds: $at")
    }
}
