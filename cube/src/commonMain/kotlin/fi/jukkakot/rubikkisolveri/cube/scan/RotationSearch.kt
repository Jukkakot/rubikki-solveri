package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.Validity

/**
 * How the faces of a scan were turned. [source] maps each sticker in net order to the index of the
 * reading it came from (as seen); [rotations] is, per face, how many quarter turns clockwise its
 * readings were turned; [from] is the face whose readings ended up on each face (itself, unless the
 * colours of an opposite pair were read the wrong way round). [ambiguous] faces could also be
 * turned another way that gives a different solvable cube.
 */
data class RotationResult(
    val colors: List<CubeColor>,
    val source: List<Int>,
    val rotations: Map<Face, Int>,
    val from: Map<Face, Face>,
    val ambiguous: Set<Face>,
    val validity: Validity,
)

/**
 * Finds how each face of a scan was turned, on the assumption that the real cube is solvable: the
 * 4⁶ rotations of the six faces are tried and the one giving a solvable cube is kept (several
 * different ones: the fewest quarter turns, the faces in doubt marked). None solvable: the one with
 * the most real pieces, after trying the colours of one opposite pair the other way round.
 */
object RotationSearch {
    /** The reading index (0…8, as seen) that lands on net position [n] when the face is turned [k] quarter turns clockwise. */
    fun turnIndex(n: Int, k: Int): Int {
        var at = n
        repeat(k.mod(4)) { at = (2 - at % 3) * 3 + at / 3 }
        return at
    }

    /** [nine] (row by row) turned [k] quarter turns clockwise. */
    fun <T> turned(nine: List<T>, k: Int): List<T> {
        require(nine.size == 9)
        return List(9) { nine[turnIndex(it, k)] }
    }

    /**
     * [colors] are the 54 classified readings (URFDLB, each face as seen); [samples] are the readings
     * themselves, used to tell which opposite pair was named the wrong way round.
     *
     * Only one pair at a time is renamed: naming one pair the wrong way round mirrors the cube, and
     * a mirror is undone by renaming any one pair (two mirrors make a turn of the whole cube), so
     * every single pair gives a solvable cube. The pair whose readings fit the default palette better
     * renamed is taken; without readings, red/orange before white/yellow before green/blue.
     */
    fun search(colors: List<CubeColor>, scheme: ColorScheme = ColorScheme.STANDARD, samples: List<Rgb>? = null): RotationResult {
        require(colors.size == Stickers.COUNT)
        val plain = best(colors, emptyList(), scheme)
        if (plain.validity.isValid) return plain
        val pairs = listOf(Face.R, Face.U, Face.F).sortedByDescending { renameGain(colors, samples, scheme[it], scheme[it.opposite]) }
        for (pair in pairs) {
            val renamed = best(colors, listOf(pair), scheme)
            if (renamed.validity.isValid) return renamed
        }
        return plain
    }

    /** How much better the readings named [a] and [b] fit the default palette with the names swapped (0 without readings). */
    private fun renameGain(colors: List<CubeColor>, samples: List<Rgb>?, a: CubeColor, b: CubeColor): Double {
        if (samples == null || samples.size != Stickers.COUNT) return 0.0
        fun mean(color: CubeColor) = Lab.mean(samples.filterIndexed { i, _ -> colors[i] == color }.map { it.toLab() })
        val palette = ColorClassifier.references()
        val ma = mean(a)
        val mb = mean(b)
        val pa = palette.getValue(a)
        val pb = palette.getValue(b)
        return ma.distance(pa) + mb.distance(pb) - ma.distance(pb) - mb.distance(pa)
    }

    /** The best rotations with the faces of [swapped] and their opposites exchanged (readings and colour names). */
    private fun best(colors: List<CubeColor>, swapped: List<Face>, scheme: ColorScheme): RotationResult {
        val from = Face.entries.associateWith { face -> if (face in swapped || face.opposite in swapped) face.opposite else face }
        val rename = HashMap<CubeColor, CubeColor>()
        for (face in swapped) {
            rename[scheme[face]] = scheme[face.opposite]
            rename[scheme[face.opposite]] = scheme[face]
        }
        fun colorAt(index: Int) = colors[index].let { rename[it] ?: it }

        // Per face and turn: the nine colours in net order (centre fixed).
        val options = Face.entries.map { face ->
            val base = from.getValue(face).ordinal * 9
            (0 until 4).map { k -> List(9) { n -> colorAt(base + turnIndex(n, k)) } }
        }
        val valid = ArrayList<Pair<IntArray, List<CubeColor>>>()
        var bestScore = -1
        var bestTurns: IntArray? = null
        val turns = IntArray(6)
        val buffer = ArrayList<CubeColor>(Stickers.COUNT)
        for (combo in 0 until 4096) {
            for (f in 0 until 6) turns[f] = (combo shr (2 * f)) and 3
            buffer.clear()
            for (f in 0 until 6) buffer.addAll(options[f][turns[f]])
            val cube = Cube.of(buffer)
            val score = CubeCheck.realPieceCount(cube)
            if (score == REAL_PIECES && CubeCheck.validity(cube, scheme).isValid) {
                val list = cube.toList()
                if (valid.none { it.second == list }) {
                    valid += turns.copyOf() to list
                } else {
                    // Same cube, fewer turns: keep the simpler way of holding.
                    val i = valid.indexOfFirst { it.second == list }
                    if (cost(turns) < cost(valid[i].first)) valid[i] = turns.copyOf() to list
                }
            }
            if (score > bestScore || (score == bestScore && cost(turns) < cost(bestTurns!!))) {
                bestScore = score
                bestTurns = turns.copyOf()
            }
        }
        val chosen = valid.minByOrNull { cost(it.first) }?.first ?: bestTurns!!
        val result = Face.entries.flatMap { options[it.ordinal][chosen[it.ordinal]] }
        val ambiguous = Face.entries.filter { face ->
            val range = face.ordinal * 9 until face.ordinal * 9 + 9
            valid.any { (_, other) -> range.any { other[it] != result[it] } }
        }.toSet()
        val source = List(Stickers.COUNT) { i ->
            val face = Face.entries[i / 9]
            from.getValue(face).ordinal * 9 + turnIndex(i % 9, chosen[face.ordinal])
        }
        return RotationResult(
            colors = result,
            source = source,
            rotations = Face.entries.associateWith { chosen[it.ordinal] },
            from = from,
            ambiguous = ambiguous,
            validity = CubeCheck.validity(Cube.of(result), scheme),
        )
    }

    /** Quarter turns away from upright, summed over the faces. */
    private fun cost(turns: IntArray): Int = turns.sumOf { minOf(it, 4 - it) }

    private const val REAL_PIECES = 20
}
