package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.Stall
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
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
        val fields = VideoScanLog.snapshot(state, facesPerFrame = 1.26, finderMs = 17.6, fps = 14.66, torch = true, darker = 2, centres = "Ub0b4b8 ?9a2b1c").toMap()
        assertEquals("Ub0b4b8 ?9a2b1c", fields["centres"])
        assertEquals("snapshot", fields["kind"])
        assertEquals("U9 R9 F3 D0 L0 B0", fields["known"])
        assertEquals(cube.toColorString().take(21) + "?".repeat(33), fields["cube"])
        assertEquals(1.2, fields["margin"])
        assertEquals(142, fields["light"])
        assertEquals(1.3, fields["faces"])
        assertEquals(18, fields["finderMs"])
        assertEquals(14.7, fields["fps"])
        assertEquals(true, fields["torch"])
        assertEquals(2, fields["darker"])
        assertEquals("stuck", fields["stall"])
        // R is known but not confirmed by the rest of the cube: no tick.
        assertEquals(setOf(Face.U), VideoScanLog.doneSides(state))
    }
}
