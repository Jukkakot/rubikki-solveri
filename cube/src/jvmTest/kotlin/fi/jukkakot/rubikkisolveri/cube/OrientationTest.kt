package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.Orientation
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrientationTest {
    private fun deg(d: Double) = d * PI / 180

    private fun rotX(a: Double) = Orientation(listOf(1.0, 0.0, 0.0, 0.0, cos(a), -sin(a), 0.0, sin(a), cos(a)))
    private fun rotY(a: Double) = Orientation(listOf(cos(a), 0.0, sin(a), 0.0, 1.0, 0.0, -sin(a), 0.0, cos(a)))
    private fun rotZ(a: Double) = Orientation(listOf(cos(a), -sin(a), 0.0, sin(a), cos(a), 0.0, 0.0, 0.0, 1.0))

    private operator fun Orientation.times(o: Orientation) =
        Orientation(List(9) { i -> (0 until 3).sumOf { k -> m[i / 3 * 3 + k] * o.m[k * 3 + i % 3] } })

    /** Where cube vector [v] lands on screen (pixels, y down) at [scale] pixels per sticker. */
    private fun Orientation.screen(v: Vec3, scale: Double = 40.0): Point {
        val (x, y, _) = apply(v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
        return Point(x * scale, -y * scale)
    }

    /** The front face's steps as the finder would report them, the candidates from them. */
    private fun candidatesFor(r: Orientation) = Orientation.candidates(r.screen(Face.F.right), r.screen(Face.F.down), Face.F.right, Face.F.down)

    private val cases = mapOf(
        "straight" to rotX(0.0),
        "rolled 30°" to rotZ(deg(30.0)),
        "tilted 25°" to rotX(deg(25.0)),
        "turned and tilted" to rotZ(deg(-15.0)) * rotX(deg(-20.0)) * rotY(deg(25.0)),
    )

    @Test
    fun oneOfTheCandidatesIsTheTrueRotation() {
        for ((name, r) in cases) {
            val best = candidatesFor(r).minOf { it.angleTo(r) }
            assertTrue(best < deg(2.0), "$name: off by ${best * 180 / PI}°")
        }
    }

    @Test
    fun aNeighbouringFaceInViewPicksTheTilt() {
        for ((name, r) in cases) {
            val centre = r.screen(Face.F.normal) * 1.5
            val others = listOf(Face.U, Face.R).map { it.normal to r.screen(it.normal) * 1.5 }
            val chosen = Orientation.choose(candidatesFor(r), Face.F.normal, centre, others, previous = null)!!
            assertTrue(chosen.angleTo(r) < deg(2.0), "$name: off by ${chosen.angleTo(r) * 180 / PI}°")
        }
    }

    @Test
    fun withoutANeighbourTheLastOrientationPicksTheTilt() {
        for ((name, r) in cases) {
            val previous = r * rotY(deg(5.0))
            val chosen = Orientation.choose(candidatesFor(r), Face.F.normal, Point(0.0, 0.0), emptyList(), previous)!!
            assertTrue(chosen.angleTo(r) < deg(2.0), "$name: off by ${chosen.angleTo(r) * 180 / PI}°")
        }
    }

    @Test
    fun parallelStepsAreNoFace() {
        assertEquals(emptyList(), Orientation.candidates(Point(10.0, 0.0), Point(20.0, 0.0), Face.F.right, Face.F.down))
    }
}
