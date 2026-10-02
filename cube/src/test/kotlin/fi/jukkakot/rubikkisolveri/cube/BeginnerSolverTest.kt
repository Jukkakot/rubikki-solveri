package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.Checks
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StepNote
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BeginnerSolverTest {

    @Test
    fun checksOnKnownStates() {
        val solved = Cube.solved()
        for (stage in Stage.entries) assertTrue(Checks.stageDone(solved, stage), "$stage on solved")
        assertTrue(Checks.crossDone(solved.apply("D")))
        assertTrue(!Checks.crossDone(solved.apply("R")))
        assertTrue(Checks.firstLayerDone(solved.apply("D")))
        assertTrue(!Checks.firstLayerDone(solved.apply("D E")), "E moves the side centres")
        assertEquals(0, Checks.yellowCornersPlaced(solved.apply("D")))
        assertTrue(!Checks.twoLayersDone(solved.apply("R")))
    }

    @Test
    fun anyCube() {
        var total = 0
        var worst = 0L
        val runs = 500
        repeat(runs) { seed ->
            val cube = Cube.solved().apply(Scramble.random(25, Random(seed)))
            val start = System.nanoTime()
            val solution = BeginnerSolver.solve(cube)
            worst = maxOf(worst, System.nanoTime() - start)
            total += solution.moves.size
            // Stages come in order and each leaves its pieces solved.
            var state = cube
            var lastStage = -1
            for ((i, step) in solution.steps.withIndex()) {
                assertTrue(step.stage.ordinal >= lastStage, "seed $seed: stage order")
                state = state.apply(step.moves)
                val nextStage = solution.steps.getOrNull(i + 1)?.stage
                if (nextStage != step.stage) assertTrue(Checks.stageDone(state, step.stage), "seed $seed: ${step.stage} done")
                lastStage = step.stage.ordinal
            }
            assertTrue(state.isSolved, "seed $seed")
        }
        println("beginner: average ${total / runs} moves, worst ${worst / 1_000_000} ms")
        assertTrue(worst / 1_000_000 < 3000, "worst ${worst / 1_000_000} ms")
    }

    @Test
    fun alreadySolvedStage() {
        // Only the last (yellow, bottom) layer is scrambled: the first three stages have no moves.
        val cube = Cube.solved().apply("z2 R U R' U R U2 R' z2")
        val stages = BeginnerSolver.solve(cube).steps.filter { it.note !is StepNote.TurnOver }.map { it.stage }.toSet()
        assertTrue(Stage.WHITE_CROSS !in stages && Stage.WHITE_CORNERS !in stages && Stage.MIDDLE_LAYER !in stages, "$stages")
        assertEquals(emptyList(), BeginnerSolver.solve(Cube.solved()).steps.filter { it.note !is StepNote.TurnOver })
    }

    @Test
    fun crossStepNamesItsEdge() {
        val cube = Cube.solved().apply("F R2 B' L D2")
        val step = BeginnerSolver.solve(cube).steps.first { it.note is StepNote.CrossEdge }
        val color = (step.note as StepNote.CrossEdge).color
        val after = cube.apply(step.moves)
        val face = Checks.faceOf(after, color)
        val edge = Checks.edgesAround(Checks.whiteFace(after)).first { face in it.faces }
        assertTrue(Checks.edgeSolved(after, edge))
    }

    @Test
    fun middleEdge() {
        repeat(20) { seed ->
            val solution = BeginnerSolver.solve(Cube.solved().apply(Scramble.random(25, Random(1000 + seed))))
            for (step in solution.steps.filter { it.note is StepNote.MiddleEdge }) {
                val tail = step.moves.takeLast(8)
                assertTrue(tail == BeginnerSolver.MIDDLE_RIGHT || tail == BeginnerSolver.MIDDLE_LEFT, "seed $seed: $tail")
            }
        }
    }
}
