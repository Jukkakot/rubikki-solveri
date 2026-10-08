package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [VideoScanTest] with the rules scanner (`scan-rules`), and what only the rules scanner does. */
class RulesVideoScanTest : VideoScanTest() {
    override val engine = ScanEngine.RULES

    @Test
    fun aFaceHeldForAFewPicturesShowsItsReadColoursBeforeItIsPlaced() {
        // scan-feedback: the third phone test's striped cube (2026-10-08); the ring stood at 24/54 while faces
        // were read many times but not yet placed. A face whose track is open still shows what it was read as.
        val scan = VideoScan(engine = ScanEngine.RULES)
        val frames = VideoFixtures.load(VideoFixtures.PHONE_RULES_3)
        var readOpen = 0
        for ((i, f) in frames.withIndex()) {
            val s = scan.onFrame(f.faces, i * 100L)
            readOpen += s.found.count { face -> face.read != null && face.read!!.count { it != null } >= 8 && face.recognised.none { it } }
        }
        assertTrue(readOpen > 0, "faces read but not placed show their read colours")
    }

    @Test
    fun theSidesAreReadByTheTimeTheScanIsCompleteAndARestartForgetsThem() {
        val scan = VideoScan(engine = ScanEngine.RULES)
        val frames = VideoFixtures.load(VideoFixtures.PHONE_SCAN_1)
        val completeAt = frames.withIndex().indexOfFirst { (i, f) -> scan.onFrame(f.faces, i * 100L).complete }
        // The striped cube is clear with five sides read: the sixth follows from the others (the ring is full on complete).
        assertTrue(completeAt >= 0 && scan.state.readSides.size >= 5, "sides read by complete (frame $completeAt): ${scan.state.readSides}")
        scan.reset()
        assertEquals(emptySet(), scan.state.readSides)
    }
}
