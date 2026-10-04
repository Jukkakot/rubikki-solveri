package fi.jukkakot.rubikkisolveri.ui.scan

import org.junit.Test
import kotlin.test.assertEquals

/** The browser camera shows and reads the same part of the video as the phone's preview. */
class CoverCropTest {
    @Test
    fun landscapeVideoInAPortraitBoxCutsTheSides() {
        // 1280×720 into 400×600: the full height, a 480 px wide middle.
        assertEquals(CropRect(400, 0, 480, 720), coverCrop(1280, 720, 400, 600))
    }

    @Test
    fun portraitVideoInAWideBoxCutsTopAndBottom() {
        assertEquals(CropRect(0, 400, 720, 480), coverCrop(720, 1280, 600, 400))
    }

    @Test
    fun sameAspectKeepsEverything() {
        assertEquals(CropRect(0, 0, 1280, 720), coverCrop(1280, 720, 640, 360))
    }

    @Test
    fun emptyBoxKeepsEverything() {
        assertEquals(CropRect(0, 0, 640, 480), coverCrop(640, 480, 0, 0))
    }

    @Test
    fun gridSquareIsTheCentreOfTheVisiblePart() {
        assertEquals(CropRect(400, 120, 480, 480), centredSquare(CropRect(400, 0, 480, 720)))
        assertEquals(CropRect(120, 400, 480, 480), centredSquare(CropRect(0, 400, 720, 480)))
    }
}
