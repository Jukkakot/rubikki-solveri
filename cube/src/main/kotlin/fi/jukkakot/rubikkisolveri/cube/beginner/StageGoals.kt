package fi.jukkakot.rubikkisolveri.cube.beginner

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Stickers

/**
 * What the cube looks like when [stage] is done, held as the method holds it during the stage.
 * [cube] is the solved cube in that hold, [inPlace] the stickers that are in place by then and
 * [added] the ones this stage puts in place. [twistMayBeWrong]: the stage places pieces without
 * fixing their twist.
 */
data class StageGoal(
    val stage: Stage,
    val cube: Cube,
    val inPlace: Set<Int>,
    val added: Set<Int>,
    val twistMayBeWrong: Boolean,
) {
    /** The colours to draw: null (grey) for every sticker not yet in place. */
    val colors: List<CubeColor?> get() = (0 until Stickers.COUNT).map { if (it in inPlace) cube[it] else null }
}

object StageGoals {
    /** Whole-cube turns from the white-up hold to the hold of [stage]: yellow on top from the middle layer on. */
    fun hold(stage: Stage): List<Move> = if (stage < Stage.MIDDLE_LAYER) emptyList() else Notation.parse("z2")

    fun of(stage: Stage): StageGoal {
        val cube = Cube.solved().apply(hold(stage))
        val inPlace = centres(cube, stage) + Stage.entries.filter { it <= stage }.flatMap { added(cube, it) }
        return StageGoal(stage, cube, inPlace, added(cube, stage), stage == Stage.YELLOW_CORNERS_PLACED)
    }

    /** The centres shown: the yellow one only once the cube is held yellow up. */
    private fun centres(cube: Cube, stage: Stage): Set<Int> {
        val yellow = Checks.yellowFace(cube)
        return Face.entries.filter { stage >= Stage.MIDDLE_LAYER || it != yellow }.map { Stickers.centre(it) }.toSet()
    }

    /** The stickers [stage] puts in place, on the solved [cube] (any hold). */
    private fun added(cube: Cube, stage: Stage): Set<Int> {
        val white = Checks.whiteFace(cube)
        val yellow = Checks.yellowFace(cube)
        return when (stage) {
            Stage.WHITE_CROSS -> Checks.edgesAround(white).flatMap { it.stickers }
            Stage.WHITE_CORNERS -> Checks.cornersAround(white).flatMap { it.stickers }
            Stage.MIDDLE_LAYER -> Checks.middleEdges(cube).flatMap { it.stickers }
            Stage.YELLOW_CROSS -> Checks.edgesAround(yellow).map { it.stickers[it.faces.indexOf(yellow)] }
            Stage.YELLOW_EDGES -> Checks.edgesAround(yellow).map { it.stickers[1 - it.faces.indexOf(yellow)] }
            Stage.YELLOW_CORNERS_PLACED, Stage.YELLOW_CORNERS_TURNED -> Checks.cornersAround(yellow).flatMap { it.stickers }
        }.toSet()
    }
}
