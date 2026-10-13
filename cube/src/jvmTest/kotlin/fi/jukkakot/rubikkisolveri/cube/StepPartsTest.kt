package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.StepNote
import fi.jukkakot.rubikkisolveri.cube.beginner.StepPart
import fi.jukkakot.rubikkisolveri.cube.beginner.StepPieces
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StepPartsTest {

    @Test
    fun comboAnimationTurnsLayersOnly() {
        repeat(50) { seed ->
            val solution = BeginnerSolver.solve(Cube.solved().apply(Scramble.random(25, Random(3000 + seed))))
            for (step in solution.steps) {
                step.parts.forEachIndexed { i, part ->
                    if (part !is StepPart.Combo) return@forEachIndexed
                    // "Näytä" plays the step's moves from the part's start to the next part's start.
                    val played = step.moves.subList(step.partStart(i), step.partStart(i + 1))
                    assertEquals(part.moves, played, "seed $seed")
                    assertTrue(played.none { it.isRotation }, "seed $seed: whole-cube turn in ${part.algorithm}")
                }
            }
        }
    }

    @Test
    fun marksThePieceAndItsPlace() {
        repeat(30) { seed ->
            var cube = Cube.solved().apply(Scramble.random(25, Random(4000 + seed)))
            for (step in BeginnerSolver.solve(cube).steps) {
                val marks = StepPieces.marks(cube, step.note)
                when (step.note) {
                    is StepNote.WhiteCorner -> assertTrue(marks.size in 3..6, "seed $seed: $marks")
                    is StepNote.CrossEdge, is StepNote.MiddleEdge -> assertTrue(marks.size in 2..4, "seed $seed: $marks")
                    else -> assertTrue(marks.isEmpty())
                }
                cube = cube.apply(step.moves)
                // After the step the piece is in its place: piece and place are the same stickers.
                val after = StepPieces.marks(cube, step.note)
                if (step.note is StepNote.WhiteCorner) assertEquals(3, after.size, "seed $seed")
                if (step.note is StepNote.CrossEdge || step.note is StepNote.MiddleEdge) assertEquals(2, after.size, "seed $seed")
            }
        }
    }
}
