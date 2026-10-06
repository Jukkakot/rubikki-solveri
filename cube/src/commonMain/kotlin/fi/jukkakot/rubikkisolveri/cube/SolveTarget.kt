package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.beginner.Stage

/** Where the guide leads the real cube (`solve-to-target`). */
sealed interface SolveTarget {
    data object Solved : SolveTarget

    data class Pattern(val pattern: CubePattern) : SolveTarget

    /** Stop when the learn method has finished [stage]. */
    data class StageDone(val stage: Stage) : SolveTarget

    /** A cube painted by hand. */
    data class Painted(val cube: Cube) : SolveTarget

    /** The whole target cube for [start] (its centres), or null for a stage target. */
    fun cubeFor(start: Cube): Cube? = when (this) {
        Solved -> CubePattern.solvedLike(start)
        is Pattern -> pattern.cube(start)
        is StageDone -> null
        is Painted -> cube
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
