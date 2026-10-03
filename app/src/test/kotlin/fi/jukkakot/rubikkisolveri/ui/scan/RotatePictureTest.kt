package fi.jukkakot.rubikkisolveri.ui.scan

import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import org.junit.Test
import kotlin.test.assertEquals

class RotatePictureTest {
    @Test
    fun picturesTurnLikeTheReadings() {
        // A 3×3 picture turns the same way as the nine readings of a face.
        val picture = IntArray(9) { it }
        for (k in 0 until 4) {
            assertEquals(RotationSearch.turned(picture.toList(), k), rotatePicture(picture, 3, k).toList(), "k=$k")
        }
    }
}
