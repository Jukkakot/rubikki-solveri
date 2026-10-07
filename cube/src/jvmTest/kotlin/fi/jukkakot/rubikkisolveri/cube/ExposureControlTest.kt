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
        c.frame(0)
        assertEquals(Phase.SEARCHING, c.phase)
        assertNull(c.settings.meter)
        c.frame(100, face(x = 20.0, y = 30.0))
        assertEquals(Phase.METERING, c.phase)
        assertEquals(Point(0.2, 0.3), c.settings.meter)
        assertEquals(Point(0.2, 0.3), c.settings.focus)
        assertFalse(c.settings.lock)
    }

    @Test
    fun locksWhenTheStickersReadWell() {
        val c = ExposureControl()
        c.frame(0, face())
        assertNull(c.frame(500, face()).lockedWashed)
        assertEquals(Phase.METERING, c.phase)
        val f = c.frame(600, face(washedCount = 2))
        assertEquals(2.0 / 9, f.lockedWashed)
        assertEquals(Phase.LOCKED, c.phase)
        assertTrue(c.settings.lock)
        assertEquals(0, c.settings.darker)
    }

    @Test
    fun stepsDarkerWhileWashedOut() {
        val c = ExposureControl()
        c.frame(0, face())
        c.frame(600, face(washedCount = 5))
        assertEquals(1, c.settings.darker)
        c.frame(1_000, face(washedCount = 5))
        assertEquals(1, c.settings.darker, "waits for the new step")
        c.frame(1_200, face(washedCount = 5))
        assertEquals(2, c.settings.darker, "a darker step runs past the time limit")
        c.frame(1_800, face(washedCount = 1))
        assertEquals(Phase.LOCKED, c.phase)
        assertEquals(2, c.settings.darker)
    }

    @Test
    fun stopsAtTheDarkestStepAndLocks() {
        val c = ExposureControl(maxDarker = 1)
        c.frame(0, face())
        c.frame(600, face(washedCount = 9))
        assertEquals(1, c.settings.darker)
        c.frame(1_200, face(washedCount = 9))
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
        c.frame(600, face(washedCount = 9))
        assertEquals(Phase.LOCKED, c.phase)
        assertEquals(0, c.settings.darker)
    }

    @Test
    fun withoutFacesAfterSettlingItWaits() {
        val c = ExposureControl()
        c.frame(0, face())
        c.frame(700)
        c.frame(2_000)
        assertEquals(Phase.METERING, c.phase)
    }

    @Test
    fun relocksWhenWashedOutForTwoSeconds() {
        val c = ExposureControl()
        c.frame(0, face())
        c.frame(600, face())
        assertEquals(Phase.LOCKED, c.phase)
        c.frame(1_000, face(washedCount = 6))
        c.frame(2_000, face(washedCount = 6))
        c.frame(2_500, face())
        c.frame(3_000, face(washedCount = 6))
        assertEquals(Phase.LOCKED, c.phase, "a good frame broke the run")
        c.frame(5_000, face(washedCount = 6))
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
        c.frame(2_300, face())
        assertEquals(Phase.METERING, c.phase)
        c.frame(2_600, face())
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
        c.frame(100)
        assertEquals(Phase.SEARCHING, c.phase)
    }

    @Test
    fun pointFollowsTheFaceWhileUnlocked() {
        val c = ExposureControl()
        c.frame(0, face(x = 50.0))
        c.frame(300, face(x = 60.0))
        assertEquals(Point(0.5, 0.5), c.settings.meter, "a small move does not move the point")
        c.frame(400, face(x = 80.0))
        assertEquals(Point(0.8, 0.5), c.settings.meter)
        c.frame(700, face(x = 80.0))
        assertEquals(Phase.METERING, c.phase, "moving the point restarts the wait")
        c.frame(1_000, face(x = 80.0))
        assertEquals(Phase.LOCKED, c.phase)
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

    /** A face centred at ([x], [y]) with cells [size] across. */
    private fun sized(x: Double, y: Double, size: Double) = FaceReading(List(9) { good }, Point(x, y), Point(size, 0.0), Point(0.0, size))

    @Test
    fun metersAtTheMiddleOfAllFaces() {
        val c = ExposureControl()
        c.frame(0, sized(20.0, 40.0, 2.0), face(x = 60.0, y = 40.0))
        assertEquals(Point(0.4, 0.4), c.settings.meter)
    }

    @Test
    fun facesSwappingSizeDoNotMoveThePoint() {
        // Two faces in view as the cube turns in the hand: now one, now the other is the larger.
        val c = ExposureControl()
        c.frame(0, sized(20.0, 50.0, 8.0), sized(70.0, 50.0, 3.0))
        val point = c.settings.meter
        c.frame(100, sized(20.0, 50.0, 3.0), sized(70.0, 50.0, 8.0))
        c.frame(200, sized(20.0, 50.0, 8.0), sized(70.0, 50.0, 3.0))
        assertEquals(point, c.settings.meter)
        c.frame(600, sized(20.0, 50.0, 3.0), sized(70.0, 50.0, 8.0))
        assertEquals(Phase.LOCKED, c.phase, "the settle was not restarted")
    }

    @Test
    fun aCubeMovingEveryFrameLocksByTheTimeLimit() {
        val c = ExposureControl()
        var t = 0L
        var x = 20.0
        while (t < ExposureControl.METER_LIMIT_MILLIS) {
            c.frame(t, face(x = x))
            assertEquals(Phase.METERING, c.phase, "at $t")
            x = if (x == 20.0) 80.0 else 20.0
            t += 100
        }
        c.frame(t, face(x = x))
        assertEquals(Phase.LOCKED, c.phase)
    }

    @Test
    fun washedOutFramesStillStepDarkerAfterTheTimeLimit() {
        val c = ExposureControl()
        var x = 20.0
        for (t in 0L..ExposureControl.METER_LIMIT_MILLIS step 100) {
            c.frame(t, face(x = x, washedCount = 6))
            x = if (x == 20.0) 80.0 else 20.0
        }
        assertEquals(Phase.METERING, c.phase)
        assertEquals(1, c.settings.darker)
        c.frame(ExposureControl.METER_LIMIT_MILLIS + ExposureControl.SETTLE_MILLIS, face(x = x))
        assertEquals(Phase.LOCKED, c.phase)
        assertEquals(1, c.settings.darker)
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
