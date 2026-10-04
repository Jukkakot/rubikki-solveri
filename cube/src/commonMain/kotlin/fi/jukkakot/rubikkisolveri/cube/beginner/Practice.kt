package fi.jukkakot.rubikkisolveri.cube.beginner

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Scramble
import kotlin.random.Random

/** A position to practise one stage and the beginner steps that do that stage. */
data class Exercise(val stage: Stage, val position: Cube, val steps: List<Step>)

object Practice {
    /**
     * An exercise for [stage]: the earlier stages are solved (by the beginner method, so the cube
     * is held as the method holds it at the start of the stage) and this stage is not.
     */
    fun exercise(stage: Stage, random: Random = Random.Default): Exercise {
        repeat(100) {
            val scrambled = Cube.solved().apply(Scramble.random(25, random))
            val steps = BeginnerSolver.solve(scrambled).steps
            val position = scrambled.apply(steps.filter { it.stage < stage }.flatMap { it.moves })
            val practice = steps.filter { it.stage == stage }
            if (!Checks.stageDone(position, stage) && practice.isNotEmpty()) return Exercise(stage, position, practice)
        }
        error("no practice position for $stage")
    }
}
