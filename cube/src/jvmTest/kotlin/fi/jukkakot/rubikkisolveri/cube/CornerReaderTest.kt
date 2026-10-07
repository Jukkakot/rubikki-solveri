package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.CornerReader
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CornerReaderTest {
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun cornerView(corner: Corner, roll: Double = 0.0, turns: List<Int> = listOf(0, 0, 0), centre: Map<Face, Rgb> = emptyMap()) =
        SyntheticViews.corner(cube, corner, roll, turns, centre)

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
        val orangeRed = SyntheticViews.orangeRed
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
