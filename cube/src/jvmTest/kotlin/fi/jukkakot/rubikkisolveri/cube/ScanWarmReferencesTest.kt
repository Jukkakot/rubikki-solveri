package fi.jukkakot.rubikkisolveri.cube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** `scan-warm-references`: red and orange references that hold in the light of the 2026-10-11 room. */
class ScanWarmReferencesTest {
    /** The first finished picture's index and cube of recording [name], or null when it never finishes. */
    private fun finish(name: String): Pair<Int, String>? =
        VideoFixtures.replay(VideoFixtures.loadRecording(name)).states.withIndex().firstOrNull { it.value.finished }
            ?.let { it.index to it.value.stickers.joinToString("") { s -> s?.letter?.toString() ?: "?" } }

    /** Browser, 2026-10-11 10:04: a yellow face taken for the orange one gave a beige orange reference; every orange read red. */
    @Test
    fun yellowFaceTakenForOrangeIsNoOrangeReference() {
        val end = finish("web_20261011_100403")
        assertNotNull(end, "the scan never finished")
        println("web_20261011_100403 finished at picture ${end.first}")
        assertEquals(ColourSpikeHarness.TRUTH_1011, end.second)
    }

    /**
     * Browser, 2026-10-11 10:16: red and orange centres seen in different light (a dark red, a bright orange), the shaded
     * faces' oranges read red; never finished. Each face's own red and orange as further references finish it right.
     */
    @Test
    fun orangeOnAFaceInShadeIsReadOrange() {
        val end = finish("web_20261011_101618")
        assertNotNull(end, "the scan never finished")
        println("web_20261011_101618 finished at picture ${end.first}")
        assertEquals(ColourSpikeHarness.TRUTH_1011, end.second)
    }
}
