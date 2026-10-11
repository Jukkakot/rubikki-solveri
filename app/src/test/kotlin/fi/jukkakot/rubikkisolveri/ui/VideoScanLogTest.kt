package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.Stall
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.ui.scan.FinderWait
import fi.jukkakot.rubikkisolveri.ui.scan.VideoScanLog
import org.junit.Test
import kotlin.test.assertEquals

class VideoScanLogTest {
    @Test
    fun snapshotTellsWhatIsKnownPerSideAndHowSure() {
        val cube = Cube.solved()
        // U and R fully known, F's first three, nothing else.
        val stickers = cube.toList().mapIndexed { i, c -> if (i < 18 || i in 18..20) c else null }
        val state = VideoScanState.EMPTY.copy(stickers = stickers, clearness = 1.234, brightness = 142, stall = Stall.STUCK, confirmed = setOf(Face.U))
        val fields = VideoScanLog.snapshot(state, facesPerFrame = 1.26, finderMs = 17.6, fps = 14.66, torch = true, darker = 2, centres = "Ub0b4b8 ?9a2b1c", showMs = 3.26).toMap()
        assertEquals("Ub0b4b8 ?9a2b1c", fields["centres"])
        assertEquals("snapshot", fields["kind"])
        assertEquals("U9 R9 F3 D0 L0 B0", fields["known"])
        assertEquals(cube.toColorString().take(21) + "?".repeat(33), fields["cube"])
        assertEquals(1.2, fields["margin"])
        assertEquals(142, fields["light"])
        assertEquals(1.3, fields["faces"])
        assertEquals(18, fields["finderMs"])
        assertEquals(14.7, fields["fps"])
        assertEquals(3.3, fields["showMs"])
        assertEquals(true, fields["torch"])
        assertEquals(2, fields["darker"])
        assertEquals("stuck", fields["stall"])
        // R is known but not confirmed by the rest of the cube: no tick.
        assertEquals(setOf(Face.U), VideoScanLog.doneSides(state))
        // Not measured: no picture preparation or finder wait.
        assertEquals(null, fields["prepMs"])
        assertEquals(null, fields["idleMs"])
    }

    @Test
    fun browserSnapshotTellsWhereTheFinderLosesTime() {
        val fields = VideoScanLog.snapshot(VideoScanState.EMPTY, facesPerFrame = 1.0, finderMs = 17.0, worker = true, prepMs = 15.26, idleMs = 0.04).toMap()
        assertEquals(15.3, fields["prepMs"])
        assertEquals(0.0, fields["idleMs"])
    }

    @Test
    fun phoneSnapshotTellsWhereTheFinderLosesTime() {
        val fields = VideoScanLog.snapshot(VideoScanState.EMPTY, facesPerFrame = 1.0, finderMs = 12.0, fps = 29.8, prepMs = 6.04, idleMs = 21.36).toMap()
        assertEquals(6.0, fields["prepMs"])
        assertEquals(21.4, fields["idleMs"])
        assertEquals(29.8, fields["fps"])
    }

    @Test
    fun finderWaitRunsFromAnswerToNextPicture() {
        val wait = FinderWait()
        assertEquals(0.0, wait.taken(100.0))
        wait.answered(112.5)
        assertEquals(20.0, wait.taken(132.5))
        wait.answered(140.0)
        assertEquals(0.0, wait.taken(140.0))
    }
}
