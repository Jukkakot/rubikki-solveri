package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ExposureControl
import fi.jukkakot.rubikkisolveri.cube.scan.ExposureControl.Phase
import fi.jukkakot.rubikkisolveri.cube.scan.ExposureSteps
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExposureControlTest {
    private val good = Rgb(180, 40, 30)
    private val washed = Rgb(255, 250, 240)

    /** A face centred at ([x], [y]) of a 100×100 picture with [washedCount] of its nine readings washed out. */
    private fun face(x: Double = 50.0, y: Double = 50.0, washedCount: Int = 0) =
        FaceReading(List(9) { if (it < washedCount) washed else good }, Point(x, y), Point(5.0, 0.0), Point(0.0, 5.0))

    private fun ExposureControl.frame(now: Long, vararg faces: FaceReading) = onFrame(faces.toList(), 100, 100, now)

    @Test
    fun searchingUntilAFaceThenMetersAtIt() {
        val c = ExposureControl()
        assertTrue(c.frame(0).read)
        assertEquals(Phase.SEARCHING, c.phase)
        assertNull(c.settings.meter)
        assertFalse(c.frame(100, face(x = 20.0, y = 30.0)).read, "the camera adjusts first")
        assertEquals(Phase.METERING, c.phase)
        assertEquals(Point(0.2, 0.3), c.settings.meter)
        assertEquals(Point(0.2, 0.3), c.settings.focus)
        assertFalse(c.settings.lock)
    }

    @Test
    fun locksWhenTheStickersReadWell() {
        val c = ExposureControl()
        c.frame(0, face())
        assertFalse(c.frame(500, face()).read)
        val f = c.frame(600, face(washedCount = 2))
        assertTrue(f.read)
        assertEquals(2.0 / 9, f.lockedWashed)
        assertEquals(Phase.LOCKED, c.phase)
        assertTrue(c.settings.lock)
        assertEquals(0, c.settings.darker)
    }

    @Test
    fun stepsDarkerWhileWashedOut() {
        val c = ExposureControl()
        c.frame(0, face())
        assertFalse(c.frame(600, face(washedCount = 5)).read)
        assertEquals(1, c.settings.darker)
        assertFalse(c.frame(1_000, face(washedCount = 5)).read, "waits for the new step")
        assertEquals(1, c.settings.darker)
        c.frame(1_200, face(washedCount = 5))
        assertEquals(2, c.settings.darker)
        assertTrue(c.frame(1_800, face(washedCount = 1)).read)
        assertEquals(Phase.LOCKED, c.phase)
        assertEquals(2, c.settings.darker)
    }

    @Test
    fun stopsAtTheDarkestStepAndLocks() {
        val c = ExposureControl(maxDarker = 1)
        c.frame(0, face())
        c.frame(600, face(washedCount = 9))
        assertEquals(1, c.settings.darker)
        assertTrue(c.frame(1_200, face(washedCount = 9)).read)
        assertEquals(Phase.LOCKED, c.phase)
        assertEquals(1, c.settings.darker)
        // At the darkest step it does not meter again however long it stays washed out.
        c.frame(2_000, face(washedCount = 9))
        c.frame(5_000, face(washedCount = 9))
        assertEquals(Phase.LOCKED, c.phase)
    }

    @Test
    fun cameraThatCannotBeMadeDarkerLocksAsBefore() {
        val c = ExposureControl(maxDarker = 0)
        c.frame(0, face())
        assertTrue(c.frame(600, face(washedCount = 9)).read)
        assertEquals(Phase.LOCKED, c.phase)
        assertEquals(0, c.settings.darker)
    }

    @Test
    fun withoutFacesAfterSettlingFramesAreReadAndItWaits() {
        val c = ExposureControl()
        c.frame(0, face())
        assertTrue(c.frame(700).read)
        assertEquals(Phase.METERING, c.phase)
    }

    @Test
    fun relocksWhenWashedOutForTwoSeconds() {
        val c = ExposureControl()
        c.frame(0, face())
        c.frame(600, face())
        assertEquals(Phase.LOCKED, c.phase)
        assertTrue(c.frame(1_000, face(washedCount = 6)).read)
        c.frame(2_000, face(washedCount = 6))
        assertTrue(c.frame(2_500, face()).read, "a good frame breaks the run")
        c.frame(3_000, face(washedCount = 6))
        assertEquals(Phase.LOCKED, c.phase)
        assertFalse(c.frame(5_000, face(washedCount = 6)).read)
        assertEquals(Phase.METERING, c.phase)
        assertFalse(c.settings.lock)
        c.frame(5_600, face(washedCount = 6))
        assertEquals(1, c.settings.darker, "goes on from the current step")
    }

    @Test
    fun torchChangeMetersAgain() {
        val c = ExposureControl()
        c.frame(0, face())
        c.frame(600, face(washedCount = 6))
        c.frame(1_200, face())
        assertEquals(Phase.LOCKED, c.phase)
        assertEquals(1, c.settings.darker)
        c.onTorch(true, 2_000)
        assertEquals(Phase.METERING, c.phase)
        assertFalse(c.settings.lock)
        assertEquals(1, c.settings.darker, "the torch on keeps the step")
        assertFalse(c.frame(2_300, face()).read)
        assertTrue(c.frame(2_600, face()).read)
        assertEquals(Phase.LOCKED, c.phase)
        c.onTorch(false, 3_000)
        assertEquals(0, c.settings.darker, "the torch off goes back to the camera's own exposure")
        assertEquals(Phase.METERING, c.phase)
    }

    @Test
    fun torchBeforeAnyFaceChangesNothing() {
        val c = ExposureControl()
        c.onTorch(true, 0)
        assertEquals(Phase.SEARCHING, c.phase)
        assertTrue(c.frame(100).read)
    }

    @Test
    fun pointFollowsTheFaceWhileUnlocked() {
        val c = ExposureControl()
        c.frame(0, face(x = 50.0))
        c.frame(300, face(x = 60.0))
        assertEquals(Point(0.5, 0.5), c.settings.meter, "a small move does not move the point")
        assertFalse(c.frame(400, face(x = 80.0)).read)
        assertEquals(Point(0.8, 0.5), c.settings.meter)
        assertFalse(c.frame(700, face(x = 80.0)).read, "moving the point restarts the wait")
        assertTrue(c.frame(1_000, face(x = 80.0)).read)
    }

    @Test
    fun onceLockedOnlyFocusFollowsAtMostOnceASecond() {
        val c = ExposureControl()
        c.frame(0, face(x = 20.0))
        c.frame(600, face(x = 20.0))
        assertEquals(Phase.LOCKED, c.phase)
        c.frame(1_000, face(x = 70.0))
        assertEquals(Point(0.2, 0.5), c.settings.focus, "not yet a second since the lock")
        c.frame(1_600, face(x = 70.0))
        assertEquals(Point(0.7, 0.5), c.settings.focus)
        assertEquals(Point(0.2, 0.5), c.settings.meter, "measuring stays where it was locked")
        assertTrue(c.settings.lock)
    }

    @Test
    fun followsTheLargestFace() {
        val c = ExposureControl()
        val small = FaceReading(List(9) { good }, Point(10.0, 10.0), Point(2.0, 0.0), Point(0.0, 2.0))
        c.frame(0, small, face(x = 60.0, y = 40.0))
        assertEquals(Point(0.6, 0.4), c.settings.meter)
    }

    @Test
    fun stepsInCameraUnits() {
        // A third of an EV per camera step (common on Android): two per half EV, −2 EV at index −12.
        assertEquals(2, ExposureSteps.per(1.0 / 3))
        assertEquals(4, ExposureSteps.maxDarker(1.0 / 3, -12))
        assertEquals(-4, ExposureSteps.index(2, 1.0 / 3, -12))
        // A small range limits the steps.
        assertEquals(1, ExposureSteps.maxDarker(1.0 / 3, -3))
        assertEquals(-3, ExposureSteps.index(2, 1.0 / 3, -3))
        // No compensation at all.
        assertEquals(0, ExposureSteps.maxDarker(0.0, 0))
        assertEquals(0, ExposureSteps.index(2, 0.0, 0))
        // Whole-EV steps: one camera step per controller step.
        assertEquals(1, ExposureSteps.per(1.0))
        assertEquals(2, ExposureSteps.maxDarker(1.0, -2))
    }
}
