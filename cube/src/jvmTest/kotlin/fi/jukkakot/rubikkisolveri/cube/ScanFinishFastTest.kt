package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** `scan-finish-fast`: the video scan finishes soon after the cube is clear, still with the true cube. */
class ScanFinishFastTest {
    /** The time (recording ms) and cube of the first finished picture of recording [name], or null. */
    private fun finish(name: String): Pair<Long, String>? {
        var end: Pair<Long, String>? = null
        VideoFixtures.replay(VideoFixtures.loadRecording(name)) { entry, _, s ->
            if (end == null && entry is ScanRecording.Picture && s != null && s.finished) {
                end = entry.ms to s.stickers.joinToString("") { it?.letter?.toString() ?: "?" }
            }
        }
        return end
    }

    /**
     * Browser, 2026-10-10 10:25: the cube right and clear at 5.0 s, two misread white faces held it until 15.6 s;
     * now it finishes at 5.3 s (limit with a margin: 6 s).
     */
    @Test
    fun clearCubeFinishesWithinAFewTenthsOfASecond() {
        val end = finish("web_20261010_102548")
        assertNotNull(end, "the scan never finished")
        println("web_20261010_102548 finished at ${end.first} ms")
        assertEquals(ScanFinishUnblockTest.TRUTH_1010, end.second)
        assertTrue(end.first <= 6_000, "finished at ${end.first} ms")
    }

    /** Every recording still finishes with its true cube. */
    @Test
    fun everyRecordingFinishesRight() {
        for ((name, truth) in FinishDelayHarness.RECORDINGS) assertEquals(truth, finish(name)?.second, name)
    }
}
