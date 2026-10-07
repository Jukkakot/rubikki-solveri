package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.beginner.Stage

/** Where the guide leads the real cube (`solve-to-target`). */
sealed interface SolveTarget {
    data object Solved : SolveTarget

    data class Pattern(val pattern: CubePattern) : SolveTarget

    /** Stop when the learn method has finished [stage]. */
    data class StageDone(val stage: Stage) : SolveTarget

    /** A cube painted by hand; it fits a start held any way (see [cubeFor]). */
    data class Painted(val cube: Cube) : SolveTarget {
        /** [cube] turned as a whole so its centres are [start]'s; as painted if no turn fits. */
        fun turnedFor(start: Cube): Cube {
            val centres = Face.entries.map { start.centre(it) }
            val seen = HashSet<Cube>()
            val queue = ArrayDeque(listOf(cube))
            while (queue.isNotEmpty()) {
                val next = queue.removeFirst()
                if (Face.entries.map { next.centre(it) } == centres) return next
                if (seen.add(next)) WHOLE_TURNS.forEach { queue.add(next.apply(it)) }
            }
            return cube
        }

        private companion object {
            val WHOLE_TURNS = listOf(Move(Layer.X, 1), Move(Layer.Y, 1))
        }
    }

    /** The whole target cube for [start] (its centres), or null for a stage target. */
    fun cubeFor(start: Cube): Cube? = when (this) {
        Solved -> CubePattern.solvedLike(start)
        is Pattern -> pattern.cube(start)
        is StageDone -> null
        is Painted -> turnedFor(start)
    }

    /** A short string for navigation; [decode] reads it back. */
    fun encode(): String = when (this) {
        Solved -> "solved"
        is Pattern -> "p:${pattern.name}"
        is StageDone -> "s:${stage.name}"
        is Painted -> "c:${cube.toColorString()}"
    }

    companion object {
        fun decode(text: String?): SolveTarget = when {
            text == null || text == "solved" -> Solved
            text.startsWith("p:") -> Pattern(CubePattern.valueOf(text.substring(2)))
            text.startsWith("s:") -> StageDone(Stage.valueOf(text.substring(2)))
            text.startsWith("c:") -> Painted(Cube.fromColorString(text.substring(2)))
            else -> Solved
        }
    }
}
