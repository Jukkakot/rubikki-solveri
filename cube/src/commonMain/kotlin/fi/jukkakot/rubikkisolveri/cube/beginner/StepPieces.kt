package fi.jukkakot.rubikkisolveri.cube.beginner

import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Edge

/** The piece a step moves and its place, as sticker indices to outline on the cube. */
object StepPieces {
    /** The colours of the piece a step with [note] moves; null for steps about a whole layer. */
    fun colors(note: StepNote): Set<CubeColor>? = when (note) {
        is StepNote.CrossEdge -> setOf(CubeColor.WHITE, note.color)
        is StepNote.WhiteCorner -> setOf(CubeColor.WHITE, note.a, note.b)
        is StepNote.MiddleEdge -> setOf(note.a, note.b)
        else -> null
    }

    /**
     * Stickers of the step's piece where it is in [cube] and of its place (the slot between the
     * centres of its colours), whichever way the cube is held. Empty for steps without one piece.
     */
    fun marks(cube: Cube, note: StepNote): Set<Int> {
        val colors = colors(note) ?: return emptySet()
        val stickers: List<List<Int>> = if (colors.size == 3) Corner.entries.map { it.stickers } else Edge.entries.map { it.stickers }
        val slots: List<List<Int>> =
            if (colors.size == 3) Corner.entries.filter { c -> c.faces.map { cube.centre(it) }.toSet() == colors }.map { it.stickers }
            else Edge.entries.filter { e -> e.faces.map { cube.centre(it) }.toSet() == colors }.map { it.stickers }
        val piece = stickers.filter { s -> s.map { cube[it] }.toSet() == colors }
        return (piece + slots).flatten().toSet()
    }
}
