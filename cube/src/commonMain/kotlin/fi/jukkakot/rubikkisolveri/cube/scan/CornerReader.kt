package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Three faces of one picture meeting at a corner (`corner-scan-spike`), named and turned by the
 * corner's geometry. [faces] are indices into the picture's faces, clockwise as seen from outside the
 * cube; [names] their colours, [sides] the cube's faces they are, and [turns] each face's turn
 * (reading index = `RotationSearch.turnIndex(net index, turn)`). [margin] is how much worse the next
 * possible naming fits.
 */
data class CornerReading(
    val faces: List<Int>,
    val names: List<CubeColor>,
    val sides: List<Face>,
    val turns: List<Int>,
    val margin: Double,
)

/**
 * Reads a corner from the faces found in one picture: three full faces whose common corner (the mean
 * of their centres, where the corner of the cube shows) lies, for each face, about one and a half
 * steps out along both of its lattice axes, i.e. just beyond one of its corner stickers. The three
 * centres must be the
 * colours of one of the cube's eight corners in that corner's clockwise order, so the naming is
 * one of 24 (8 corners × 3 starting faces); white–green–red and white–green–orange run opposite
 * ways round and are told apart by the picture, not by how red the centre looks.
 */
object CornerReader {
    /** How far the common corner may lie from a face's centre along each axis, in steps (1.5 exactly; perspective stretches it to about 2.7 on the fixtures). */
    private val CORNER_STEPS = 1.0..3.0

    /** The reading index of [face]'s corner sticker towards [at], or null when [at] is not near one of its corners. */
    private fun cornerSticker(face: FaceReading, at: Point): Int? {
        val d = at - face.centre
        val det = face.u.cross(face.v)
        if (det == 0.0) return null
        val a = d.cross(face.v) / det
        val b = face.u.cross(d) / det
        if (abs(a) !in CORNER_STEPS || abs(b) !in CORNER_STEPS) return null
        return (if (b > 0) 2 else 0) * 3 + (if (a > 0) 2 else 0)
    }

    /** The corner in [faces] (the best fitting if several), or null when none, or its naming is not clear by [minMargin]. */
    fun read(faces: List<FaceReading>, scheme: ColorScheme = ColorScheme.STANDARD, minMargin: Double = MIN_MARGIN): CornerReading? {
        val full = faces.indices.filter { faces[it].isFull }
        var best: CornerReading? = null
        for (a in full) for (b in full) for (c in full) {
            if (!(a < b && b < c)) continue
            val r = readTriple(faces, listOf(a, b, c), scheme) ?: continue
            if (best == null || r.margin > best.margin) best = r
        }
        return best?.takeIf { it.margin >= minMargin }
    }

    private fun readTriple(faces: List<FaceReading>, triple: List<Int>, scheme: ColorScheme): CornerReading? {
        // The cube's corner shows where the three centres' mean is; each face's corner sticker points to it.
        val mx = triple.sumOf { faces[it].centre.x } / 3
        val my = triple.sumOf { faces[it].centre.y } / 3
        val corner = triple.associateWith { i -> cornerSticker(faces[i], Point(mx, my)) ?: return null }
        // Clockwise on screen (y down): by angle about that point.
        val order = triple.sortedBy { atan2(faces[it].centre.y - my, faces[it].centre.x - mx) }
        val distances = order.map { ColorClassifier.centreDistances(faces[it].colors[CENTRE]!!) }
        val hypotheses = Corner.entries.flatMap { piece ->
            (0 until 3).map { s ->
                val sides = List(3) { piece.faces[(it + s) % 3] }
                val cost = (0 until 3).sumOf { distances[it].getValue(scheme[sides[it]]) }
                Triple(cost, piece, sides)
            }
        }.sortedBy { it.first }
        val (cost, piece, sides) = hypotheses[0]
        val margin = hypotheses[1].first - cost
        val turns = order.mapIndexed { k, i ->
            // The net index of this face's sticker at the corner, and the turn that puts it where the reading has it.
            val net = piece.stickers[piece.faces.indexOf(sides[k])] % 9
            (0 until 4).first { t -> RotationSearch.turnIndex(net, t) == corner.getValue(i) }
        }
        return CornerReading(order, sides.map { scheme[it] }, sides, turns, margin)
    }

    private const val CENTRE = 4

    /** A corner's naming counts when the next possible one fits at least this much worse (palette distances). */
    const val MIN_MARGIN = 10.0
}
