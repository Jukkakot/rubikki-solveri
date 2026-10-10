package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** `scan-finish-unblock`: a clear cube is not held back by a followed face with an open turn or an outvoted misread. */
class ScanFinishUnblockTest {
    private fun VideoScanState.cube() = stickers.joinToString("") { it?.letter?.toString() ?: "?" }

    /** The first finished picture's index and cube of recording [name], or null when it never finishes. */
    private fun finish(name: String): Pair<Int, String>? =
        VideoFixtures.replay(VideoFixtures.loadRecording(name)).states.withIndex().firstOrNull { it.value.finished }?.let { it.index to it.value.cube() }

    /** Browser, 2026-10-10 10:25: the cube clear for 33 s, held by an old white face with an open turn and a short misread one. */
    @Test
    fun clearCubeHeldByOpenAndOutvotedTracksFinishes() {
        val end = finish("web_20261010_102548")
        assertNotNull(end, "the scan never finished")
        println("web_20261010_102548 finished at picture ${end.first}")
        assertEquals(TRUTH_1010, end.second)
    }

    /** The earlier recordings finish right, no later than before this change (pictures 151 and 202). */
    @Test
    fun earlierRecordingsKeepTheirFinish() {
        for ((name, before) in listOf("web_20261009_100814" to 151, "web_20261009_100824" to 202)) {
            val end = finish(name)
            assertNotNull(end, "$name never finished")
            assertEquals(ScanNeverLockedTest.TRUTH_1009, end.second, name)
            assertTrue(end.first <= before, "$name finished at ${end.first}, before $before")
        }
    }

    companion object {
        const val TRUTH_1010 = "OYGYWWRORWRWGRBRYYGGBOGROGGYWYRYGBOGBRWOOBRBBOBWWBWOYY"
    }
}
