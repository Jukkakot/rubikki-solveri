package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Face readings of any cube as a camera would find them, for tests: the faces of a corner or an edge
 * seen from outside, the picture rolled, each reading turned, colours in the default palette.
 */
object SyntheticViews {
    private class V(val x: Double, val y: Double, val z: Double) {
        operator fun plus(o: V) = V(x + o.x, y + o.y, z + o.z)
        operator fun times(k: Double) = V(x * k, y * k, z * k)
        infix fun dot(o: V) = x * o.x + y * o.y + z * o.z
        infix fun cross(o: V) = V(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
        fun unit() = this * (1 / sqrt(this dot this))
    }

    private fun v(a: Vec3) = V(a.x.toDouble(), a.y.toDouble(), a.z.toDouble())

    /**
     * The three faces of [corner] (in [Corner.faces] order) as the camera sees them looking at that
     * corner from outside; see [view].
     */
    fun corner(cube: Cube, corner: Corner, roll: Double = 0.0, turns: List<Int> = listOf(0, 0, 0), centre: Map<Face, Rgb> = emptyMap(), at: Point = Point(200.0, 300.0)): List<FaceReading> =
        view(cube, corner.faces, roll, turns, centre, at)

    /** The two faces of [edge] (in [Edge.faces] order) seen from outside across their common edge; see [view]. */
    fun edge(cube: Cube, edge: Edge, roll: Double = 0.0, turns: List<Int> = listOf(0, 0), centre: Map<Face, Rgb> = emptyMap(), at: Point = Point(200.0, 300.0)): List<FaceReading> =
        view(cube, edge.faces, roll, turns, centre, at)

    /**
     * [faces] of [cube] looking at the cube along the sum of their normals, the picture rolled by
     * [roll] radians, each face's reading turned by its entry in [turns] (reading index
     * `RotationSearch.turnIndex(net index, turn)`); [centre] overrides a face's centre colour; the
     * cube's middle lands at [at].
     */
    fun view(cube: Cube, faces: List<Face>, roll: Double = 0.0, turns: List<Int> = List(faces.size) { 0 }, centre: Map<Face, Rgb> = emptyMap(), at: Point = Point(200.0, 300.0)): List<FaceReading> {
        val d = faces.map { v(it.normal) }.reduce(V::plus).unit()
        val any = if (abs(d.y) < 0.9) V(0.0, 1.0, 0.0) else V(1.0, 0.0, 0.0)
        val e0 = (any cross d).unit()
        val f0 = d cross e0
        val ex = e0 * cos(roll) + f0 * sin(roll)
        // Screen y down, looking at the cube along -d: ex × ey = -d.
        val ey = d * -1.0 cross ex
        val scale = 30.0
        fun screen(p: V) = Point(at.x + (p dot ex) * scale, at.y + (p dot ey) * scale)
        fun step(p: V) = Point((p dot ex) * scale, (p dot ey) * scale)
        return faces.mapIndexed { i, face ->
            val k = turns[i]
            val c = v(face.normal) * 1.5
            fun pos(n: Int) = c + v(face.right) * (n % 3 - 1.0) + v(face.down) * (n / 3 - 1.0)
            // Reading index j holds net index n with turnIndex(n, k) == j.
            fun netOf(j: Int) = (0 until 9).first { RotationSearch.turnIndex(it, k) == j }
            val u = step(pos(netOf(5)) + c * -1.0)
            val vv = step(pos(netOf(7)) + c * -1.0)
            val colors = (0 until 9).map { j ->
                val n = netOf(j)
                if (n == 4 && face in centre) centre.getValue(face) else ColorClassifier.DEFAULT_PALETTE.getValue(cube[face.ordinal * 9 + n])
            }
            FaceReading(colors, screen(c), u, vv)
        }
    }

    /** [face] of [cube] straight on, read turned [turn] (as in [view]), centred at [at] with cells [size] across. */
    fun straight(cube: Cube, face: Face, turn: Int = 0, at: Point = Point(100.0, 100.0), size: Double = 30.0): FaceReading {
        val colors = (0 until 9).map { j ->
            val n = (0 until 9).first { RotationSearch.turnIndex(it, turn) == j }
            ColorClassifier.DEFAULT_PALETTE.getValue(cube[face.ordinal * 9 + n])
        }
        return FaceReading(colors, at, Point(size, 0.0), Point(0.0, size))
    }

    /** [from] mixed towards [to] until the default palette names the centre [until] (a look-alike centre). */
    fun fadedCentre(from: Rgb, to: Rgb, until: CubeColor): Rgb? = (1..100).map { t ->
        Rgb(from.r + (to.r - from.r) * t / 100, from.g + (to.g - from.g) * t / 100, from.b + (to.b - from.b) * t / 100)
    }.firstOrNull { ColorClassifier.rankedCentre(it).first() == until }

    /** A red centre faded towards orange until the palette names it orange. */
    val orangeRed: Rgb get() = ColorClassifier.DEFAULT_PALETTE.let { fadedCentre(it.getValue(CubeColor.RED), it.getValue(CubeColor.ORANGE), CubeColor.ORANGE)!! }
}
