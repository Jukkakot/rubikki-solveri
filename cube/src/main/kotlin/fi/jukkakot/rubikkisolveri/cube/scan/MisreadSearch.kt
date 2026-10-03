package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Stickers

/** Two stickers whose colours, swapped, make the cube solvable; [cost] is how much worse the readings fit. */
data class Swap(val a: Int, val b: Int, val cost: Double)

/**
 * Finds the likely misreads of a cube that cannot be right. A balanced scan keeps nine of each
 * colour, so a misread is typically two stickers read the wrong way round: every swap of two
 * non-centre stickers with different colours is tried, and the ones that give a solvable cube are
 * returned, the best fit to the camera [readings] first (sticker order without readings).
 */
object MisreadSearch {
    fun swaps(cube: Cube, readings: List<Rgb>? = null, scheme: ColorScheme = ColorScheme.STANDARD): List<Swap> {
        val labs = readings?.takeIf { it.size == Stickers.COUNT }?.map { it.toLab() }
        val refs: Map<CubeColor, Lab>? = labs?.let { l ->
            CubeColor.entries.associateWith { color ->
                Lab.mean(l.filterIndexed { i, _ -> cube[i] == color }.ifEmpty { listOf(Lab(50.0, 0.0, 0.0)) })
            }
        }
        fun fit(i: Int, color: CubeColor): Double = if (labs == null || refs == null) 0.0 else labs[i].distance(refs.getValue(color))

        val stickers = (0 until Stickers.COUNT).filter { it % 9 != 4 }
        val found = ArrayList<Swap>()
        for ((n, a) in stickers.withIndex()) {
            for (b in stickers.subList(n + 1, stickers.size)) {
                val ca = cube[a]
                val cb = cube[b]
                if (ca == cb) continue
                val swapped = cube.with(a, cb).with(b, ca)
                if (!CubeCheck.validity(swapped, scheme).isValid) continue
                found += Swap(a, b, fit(a, cb) + fit(b, ca) - fit(a, ca) - fit(b, cb))
            }
        }
        return found.sortedBy { it.cost }
    }
}
