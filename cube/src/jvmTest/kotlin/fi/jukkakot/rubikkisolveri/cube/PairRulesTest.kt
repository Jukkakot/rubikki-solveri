package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceOption
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.PairRules
import fi.jukkakot.rubikkisolveri.cube.scan.Track
import fi.jukkakot.rubikkisolveri.cube.scan.Tracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PairRulesTest {
    private val cube = Cube.solved().apply("R U F' D2 L B")
    private val n = FaceOption.COUNT

    /** [picture] shown [times] times to a tracker and the rules; the tracks of its faces in order. */
    private fun observe(rules: PairRules, picture: List<FaceReading>, times: Int = 2): List<Track> {
        val tracker = Tracker()
        var tracks: List<Track> = emptyList()
        repeat(times) { k ->
            val r = tracker.onFrame(picture, k * 100L) { ColorClassifier.live(it) }
            tracks = r.map { it!!.first }
            rules.observe(r.map { it!! })
        }
        return tracks
    }

    @Test
    fun everyEdgeAllowsItsTrueFacesAndTurnsAndOnlyTheCubesTurnsOfThem() {
        for (edge in Edge.entries) for (roll in listOf(0.3, 2.0, 4.1)) for (turn in 0 until 4) {
            val turns = listOf(turn, (turn + 1) % 4)
            val rules = PairRules()
            val (a, b) = observe(rules, SyntheticViews.edge(cube, edge, roll, turns))
            val allowed = assertNotNull(rules.allowed(a, b))
            val truth = FaceOption.of(edge.faces[0], turns[0]) * n + FaceOption.of(edge.faces[1], turns[1])
            assertTrue(allowed[truth], "$edge roll $roll turn $turn")
            // One allowed pair per way of holding the cube (24), no other.
            val pairs = (0 until FaceOption.FACES).sumOf { oa -> (0 until FaceOption.FACES).count { ob -> allowed[oa * n + ob] } }
            assertEquals(24, pairs, "$edge roll $roll turn $turn")
            // The other way round (b first) gives the same rule.
            assertTrue(assertNotNull(rules.allowed(b, a))[FaceOption.of(edge.faces[1], turns[1]) * n + FaceOption.of(edge.faces[0], turns[0])])
        }
    }

    @Test
    fun everyCornerAllowsOnlyRealCornersTheRightWayRound() {
        for (corner in Corner.entries) for (roll in listOf(0.0, 1.7, 3.5, 5.2)) for (turn in 0 until 4) {
            val turns = listOf(turn, (turn + 1) % 4, (turn + 3) % 4)
            val rules = PairRules()
            val (a, b, c) = observe(rules, SyntheticViews.corner(cube, corner, roll, turns))
            val ab = rules.allowed(a, b)!!
            val bc = rules.allowed(b, c)!!
            val ac = rules.allowed(a, c)!!
            val triples = ArrayList<Triple<Int, Int, Int>>()
            for (oa in 0 until FaceOption.FACES) for (ob in 0 until FaceOption.FACES) {
                if (!ab[oa * n + ob]) continue
                for (oc in 0 until FaceOption.FACES) if (bc[ob * n + oc] && ac[oa * n + oc]) triples += Triple(oa, ob, oc)
            }
            val truth = Triple(FaceOption.of(corner.faces[0], turns[0]), FaceOption.of(corner.faces[1], turns[1]), FaceOption.of(corner.faces[2], turns[2]))
            assertTrue(truth in triples, "$corner roll $roll turn $turn")
            // One per way of holding the cube: never a mirrored corner.
            assertEquals(24, triples.size, "$corner roll $roll turn $turn")
            val mirrored = Triple(FaceOption.of(corner.faces[0], turns[0]), FaceOption.of(corner.faces[2], turns[1]), FaceOption.of(corner.faces[1], turns[2]))
            assertTrue(mirrored !in triples)
        }
    }

    @Test
    fun facesSeenTogetherAreNeverTheSameOrOpposite() {
        val rules = PairRules()
        val (a, b) = observe(rules, SyntheticViews.edge(cube, Edge.UF), times = 1)
        val allowed = rules.allowed(a, b)!!
        for (oa in 0 until FaceOption.FACES) for (ob in 0 until FaceOption.FACES) {
            val fa = FaceOption.face(oa)
            val fb = FaceOption.face(ob)
            if (fa == fb || fa.opposite == fb) assertTrue(!allowed[oa * n + ob], "$fa $fb")
        }
        // Seen once: no side rule yet, any neighbour in any turn.
        assertTrue(allowed[FaceOption.of(Face.U, 2) * n + FaceOption.of(Face.R, 1)])
        assertTrue(allowed[FaceOption.NONE * n + FaceOption.of(Face.U, 0)])
    }

    @Test
    fun tracksNeverSeenTogetherHaveNoRule() {
        val rules = PairRules()
        val tracker = Tracker()
        val a = tracker.onFrame(listOf(SyntheticViews.straight(cube, Face.U)), 0) { ColorClassifier.live(it) }.single()!!
        val b = tracker.onFrame(listOf(SyntheticViews.straight(cube, Face.R, at = fi.jukkakot.rubikkisolveri.cube.scan.Point(400.0, 400.0))), 100) { ColorClassifier.live(it) }.single()!!
        rules.observe(listOf(a))
        rules.observe(listOf(b))
        assertNull(rules.allowed(a.first, b.first))
    }
}
