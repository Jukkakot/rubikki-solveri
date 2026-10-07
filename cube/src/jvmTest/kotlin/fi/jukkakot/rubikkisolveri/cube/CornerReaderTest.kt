package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.CornerReader
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CornerReaderTest {
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private class V(val x: Double, val y: Double, val z: Double) {
        operator fun plus(o: V) = V(x + o.x, y + o.y, z + o.z)
        operator fun times(k: Double) = V(x * k, y * k, z * k)
        infix fun dot(o: V) = x * o.x + y * o.y + z * o.z
        infix fun cross(o: V) = V(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
        fun unit() = this * (1 / sqrt(this dot this))
    }

    private fun v(a: Vec3) = V(a.x.toDouble(), a.y.toDouble(), a.z.toDouble())

    /**
     * The three faces of [corner] as the camera sees them looking at that corner from outside, the
     * picture rolled by [roll] radians, each face's reading turned by its entry in [turns]; colours
     * from [cube] in the default palette, [centre] overriding a face's centre colour.
     */
    private fun cornerView(corner: Corner, roll: Double = 0.0, turns: List<Int> = listOf(0, 0, 0), centre: Map<Face, Rgb> = emptyMap()): List<FaceReading> {
        val d = corner.faces.map { v(it.normal) }.reduce(V::plus).unit()
        val any = if (kotlin.math.abs(d.y) < 0.9) V(0.0, 1.0, 0.0) else V(1.0, 0.0, 0.0)
        val e0 = (any cross d).unit()
        val f0 = d cross e0
        val ex = e0 * cos(roll) + f0 * sin(roll)
        // Screen y down, looking at the cube along -d: ex × ey = -d.
        val ey = d * -1.0 cross ex
        val scale = 30.0
        fun screen(p: V) = Point(200 + (p dot ex) * scale, 300 + (p dot ey) * scale)
        fun step(p: V) = Point((p dot ex) * scale, (p dot ey) * scale)
        return corner.faces.mapIndexed { i, face ->
            val k = turns[i]
            val c = v(face.normal) * 1.5
            fun at(n: Int) = c + v(face.right) * (n % 3 - 1.0) + v(face.down) * (n / 3 - 1.0)
            // Reading index j holds net index n with turnIndex(n, k) == j.
            fun netOf(j: Int) = (0 until 9).first { RotationSearch.turnIndex(it, k) == j }
            val u = step(at(netOf(5)) + c * -1.0)
            val vv = step(at(netOf(7)) + c * -1.0)
            val colors = (0 until 9).map { j ->
                val n = netOf(j)
                if (n == 4 && face in centre) centre.getValue(face) else ColorClassifier.DEFAULT_PALETTE.getValue(cube[face.ordinal * 9 + n])
            }
            FaceReading(colors, screen(c), u, vv)
        }
    }

    @Test
    fun everyCornerIsNamedAndTurnedRight() {
        for (corner in Corner.entries) for (roll in listOf(0.0, 1.7, 3.5, 5.2)) for (turn in 0 until 4) {
            val turns = listOf(turn, (turn + 1) % 4, (turn + 3) % 4)
            val faces = cornerView(corner, roll, turns)
            val r = assertNotNull(CornerReader.read(faces), "$corner roll $roll")
            for ((k, i) in r.faces.withIndex()) {
                assertEquals(corner.faces[i], r.sides[k], "$corner roll $roll face $i")
                assertEquals(ColorScheme.STANDARD[corner.faces[i]], r.names[k])
                assertEquals(turns[i], r.turns[k], "$corner roll $roll turn of ${corner.faces[i]}")
            }
        }
    }

    @Test
    fun aRedCentreLookingOrangeBesideWhiteAndGreenIsNamedRedByHandedness() {
        val p = ColorClassifier.DEFAULT_PALETTE
        val red = p.getValue(CubeColor.RED)
        val orange = p.getValue(CubeColor.ORANGE)
        val orangeRed = (1..100).map { t -> Rgb(red.r + (orange.r - red.r) * t / 100, red.g + (orange.g - red.g) * t / 100, red.b + (orange.b - red.b) * t / 100) }
            .first { ColorClassifier.rankedCentre(it).first() == CubeColor.ORANGE }
        // URF: white, red, green; the red centre reads more orange than red.
        val r = assertNotNull(CornerReader.read(cornerView(Corner.URF, centre = mapOf(Face.R to orangeRed))))
        assertEquals(Face.R, r.sides[r.names.indexOf(CubeColor.RED)])
    }

    @Test
    fun twoFacesAreNoCorner() {
        assertNull(CornerReader.read(cornerView(Corner.URF).take(2)))
    }

    @Test
    fun aLatticeSlippedByARowIsNoCorner() {
        val faces = cornerView(Corner.URF).toMutableList()
        val f = faces[0]
        faces[0] = f.copy(centre = Point(f.centre.x + f.v.x * 2, f.centre.y + f.v.y * 2))
        assertNull(CornerReader.read(faces))
    }
}
