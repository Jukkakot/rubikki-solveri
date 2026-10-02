package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.beginner.Checks
import fi.jukkakot.rubikkisolveri.cube.beginner.Practice
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PracticeTest {
    @Test
    fun practicePositions() {
        for (stage in Stage.entries) {
            repeat(50) { seed ->
                val exercise = Practice.exercise(stage, Random(seed))
                val position = exercise.position
                for (earlier in Stage.entries.filter { it < stage }) {
                    assertTrue(Checks.stageDone(position, earlier), "$stage seed $seed: $earlier should be done")
                }
                assertTrue(!Checks.stageDone(position, stage), "$stage seed $seed")
                val steps = exercise.steps
                assertTrue(steps.isNotEmpty() && steps.all { it.stage == stage })
                assertTrue(Checks.stageDone(position.apply(steps.flatMap { it.moves }), stage))
            }
        }
        assertEquals(Practice.exercise(Stage.YELLOW_CROSS, Random(1)), Practice.exercise(Stage.YELLOW_CROSS, Random(1)))
    }
}
