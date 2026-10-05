package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.CubeProjection
import fi.jukkakot.rubikkisolveri.cube.scan.Orientation
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CubeProjectionTest {
    private val centre = Point(200.0, 300.0)
    private val step = 30.0

    private fun times(a: List<Double>, b: List<Double>) = List(9) { i -> (0 until 3).sumOf { k -> a[i / 3 * 3 + k] * b[k * 3 + i % 3] } }
    private fun aboutX(deg: Double) = Math.toRadians(deg).let { t -> listOf(1.0, 0.0, 0.0, 0.0, cos(t), -sin(t), 0.0, sin(t), cos(t)) }
    private fun aboutY(deg: Double) = Math.toRadians(deg).let { t -> listOf(cos(t), 0.0, sin(t), 0.0, 1.0, 0.0, -sin(t), 0.0, cos(t)) }

    /** Where the real cube held as [truth] with [front] at [centre] shows the point [x], [y], [z] (in steps). */
    private fun shown(truth: Orientation, front: Face, x: Double, y: Double, z: Double): Point {
        val (sx, sy, _) = truth.apply(x - 1.5 * front.normal.x, y - 1.5 * front.normal.y, z - 1.5 * front.normal.z)
        return centre + Point(sx, -sy) * step
    }

    private fun vec(truth: Orientation, v: Vec3) = truth.apply(v.x.toDouble(), v.y.toDouble(), v.z.toDouble()).let { (x, y, _) -> Point(x, -y) * step }

    @Test
    fun stickersOfTheSidesInViewLieWhereTheRealCubeShowsThem() {
        for ((tiltX, turnY) in listOf(25.0 to -35.0, -20.0 to 30.0, -35.0 to 40.0)) {
            val truth = Orientation(times(aboutX(tiltX), aboutY(turnY)))
            val front = Face.F
            val a = front.right
            val b = front.down
            val u = vec(truth, a)
            val v = vec(truth, b)
            // Neighbouring faces seen in the same frame tell which way the face tilts.
            val others = listOf(Face.U, Face.R, Face.L, Face.D).map { it.normal to shown(truth, front, 1.5 * it.normal.x, 1.5 * it.normal.y, 1.5 * it.normal.z) }
            val chosen = assertNotNull(Orientation.choose(Orientation.candidates(u, v, a, b), front.normal, centre, others, null))
            val projection = assertNotNull(CubeProjection.of(chosen, front, centre, u, v, a, b))
            assertEquals(step, projection.step, 0.5)
            val facing = Face.entries.filter { truth.apply(it.normal.x.toDouble(), it.normal.y.toDouble(), it.normal.z.toDouble()).third >= CubeProjection.FACING }.toSet()
            assertEquals(facing, projection.facing, "$tiltX/$turnY")
            assertTrue(front in facing && facing.size >= 2, "$facing")
            for (sticker in Stickers.all.filter { it.face in facing }) {
                val p = sticker.position + sticker.normal * -1
                val expected = shown(truth, front, 1.5 * sticker.normal.x + p.x, 1.5 * sticker.normal.y + p.y, 1.5 * sticker.normal.z + p.z)
                val error = (projection.points[sticker.index] - expected).length
                assertTrue(error < 0.2 * step, "$tiltX/$turnY sticker ${sticker.index}: off by $error")
            }
        }
    }
}
