package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.CaseId
import fi.jukkakot.rubikkisolveri.cube.beginner.Checks
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StageCases
import fi.jukkakot.rubikkisolveri.cube.beginner.StageGoals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StageVisualsTest {
    private fun stickersOn(face: Face) = (1..9).map { Stickers.index(face, it) }.toSet()

    @Test
    fun crossGoal() {
        val goal = StageGoals.of(Stage.WHITE_CROSS)
        assertEquals(13, goal.inPlace.size, "white centre and 4 edges, 4 edge sides, 4 side centres")
        assertEquals(8, goal.added.size)
        assertTrue(goal.inPlace.filter { it in stickersOn(Face.U) }.all { goal.cube[it] == CubeColor.WHITE })
        assertTrue(goal.colors.count { it == null } == 54 - 13)
    }

    @Test
    fun middleLayerGoal() {
        val goal = StageGoals.of(Stage.MIDDLE_LAYER)
        assertEquals(CubeColor.YELLOW, goal.cube.centre(Face.U), "held yellow up")
        assertEquals(setOf(Stickers.centre(Face.U)), goal.inPlace.filter { it in stickersOn(Face.U) }.toSet())
        assertEquals(8, goal.added.size)
        assertTrue(goal.added.all { it !in stickersOn(Face.U) && it !in stickersOn(Face.D) })
        // First two layers: the whole bottom and the lower two rows of each side.
        assertTrue(stickersOn(Face.D).all { it in goal.inPlace })
        assertEquals(1 + 9 + 4 * 6, goal.inPlace.size)
    }

    @Test
    fun yellowCrossGoal() {
        val goal = StageGoals.of(Stage.YELLOW_CROSS)
        assertEquals(setOf(2, 4, 6, 8).map { Stickers.index(Face.U, it) }.toSet(), goal.added)
        assertEquals(5, goal.inPlace.count { it in stickersOn(Face.U) })
        assertTrue(StageGoals.of(Stage.YELLOW_CORNERS_PLACED).twistMayBeWrong)
        assertEquals(54, StageGoals.of(Stage.YELLOW_CORNERS_TURNED).inPlace.size)
    }

    @Test
    fun movedByTheTrigger() {
        val moved = Sequences.movedStickers(BeginnerSolver.TRIGGER)
        assertEquals(setOf(Stickers.index(Face.U, 9)), moved.filter { it in stickersOn(Face.U) }.toSet())
        assertTrue(Face.entries.none { Stickers.centre(it) in moved })
        assertTrue(moved.isNotEmpty())
    }

    @Test
    fun movedByTheYellowCross() {
        val moved = Sequences.movedStickers(BeginnerSolver.YELLOW_CROSS)
        assertTrue(Stickers.centre(Face.U) !in moved)
        assertTrue(stickersOn(Face.D).none { it in moved }, "the bottom stays")
        assertTrue(Stickers.index(Face.U, 8) in moved, "the front top edge moves")
        assertEquals(emptySet(), Sequences.movedStickers(BeginnerSolver.YELLOW_CROSS + Sequences.inverse(BeginnerSolver.YELLOW_CROSS)))
    }

    @Test
    fun everyStageButNoneHasCases() {
        for (stage in Stage.entries) assertTrue(StageCases.of(stage).size in 2..4, "$stage")
    }

    @Test
    fun casesReachWhatTheyPromise() {
        for (case in StageCases.all) {
            val position = case.position
            for (earlier in Stage.entries.filter { it < case.stage }) {
                assertTrue(Checks.stageDone(position, earlier), "${case.id}: $earlier done at the start")
            }
            assertTrue(!StageCases.reached(case.id, position), "${case.id}: not reached at the start")
            assertTrue(StageCases.reached(case.id, position.apply(case.moves)), "${case.id}: reached after its moves")
            assertTrue(case.highlight.isNotEmpty(), "${case.id}: something highlighted")
            assertEquals(Cube.solved().apply(StageGoals.hold(case.stage)).centre(Face.U), position.centre(Face.U), "${case.id}: held as the stage")
        }
    }

    private fun case(id: CaseId) = StageCases.all.first { it.id == id }

    /** The face that the [color] sticker of the highlighted [piece] faces. */
    private fun whiteOf(id: CaseId, color: CubeColor, piece: List<Int>): Face {
        val cube = case(id).position
        return Stickers.all[piece.first { cube[it] == color }].face
    }

    @Test
    fun casesShowTheirSituation() {
        val down = case(CaseId.EDGE_DOWN)
        assertTrue(down.highlight.any { it in stickersOn(Face.D) }, "edge down")
        assertEquals(Face.D, whiteOf(CaseId.EDGE_DOWN, CubeColor.WHITE, down.highlight.toList()))
        assertTrue(case(CaseId.EDGE_MIDDLE).highlight.none { it in stickersOn(Face.U) || it in stickersOn(Face.D) }, "edge in the middle")
        assertEquals(Edge.UF.stickers.toSet(), case(CaseId.EDGE_FLIPPED).highlight, "flipped in its place")

        for ((id, face) in listOf(CaseId.WHITE_RIGHT to Face.R, CaseId.WHITE_FRONT to Face.F, CaseId.WHITE_DOWN to Face.D)) {
            assertEquals(Corner.DFR.stickers.toSet(), case(id).highlight, "$id below its place")
            assertEquals(face, whiteOf(id, CubeColor.WHITE, case(id).highlight.toList()), "$id white side")
        }

        for (id in listOf(CaseId.GOES_RIGHT, CaseId.GOES_LEFT)) {
            assertEquals(Edge.UF.stickers.toSet(), case(id).highlight, "$id waits at the top front")
        }
        assertEquals(Edge.FR.stickers.toSet(), case(CaseId.STUCK).highlight, "stuck in its place")

        fun up(id: CaseId) = case(id).highlight.map { Stickers.all[it].let { s -> s.row to s.col } }.toSet()
        assertEquals(setOf(1 to 1), up(CaseId.DOT))
        assertEquals(setOf(1 to 1, 0 to 1, 1 to 0), up(CaseId.L_SHAPE), "L at the back left")
        assertEquals(setOf(1 to 1, 1 to 0, 1 to 2), up(CaseId.LINE), "line left to right")

        assertEquals((Edge.UR.stickers + Edge.UB.stickers).toSet(), case(CaseId.NEIGHBOURS).highlight, "right and back")
        assertEquals((Edge.UF.stickers + Edge.UB.stickers).toSet(), case(CaseId.OPPOSITE).highlight, "front and back")
        assertEquals(Corner.URF.stickers.toSet(), case(CaseId.ONE_PLACED).highlight, "the front right one")
        assertEquals(0, Checks.yellowCornersPlaced(case(CaseId.NONE_PLACED).position))
        for ((id, face) in listOf(CaseId.YELLOW_RIGHT to Face.R, CaseId.YELLOW_FRONT to Face.F)) {
            assertEquals(Corner.URF.stickers.toSet(), case(id).highlight)
            assertEquals(face, whiteOf(id, CubeColor.YELLOW, case(id).highlight.toList()), "$id yellow side")
            assertTrue(Checks.firstLayerDone(case(id).position), "$id bottom whole")
        }
    }
}
