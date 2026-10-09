package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ScanStateCodec
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import kotlin.test.Test
import kotlin.test.assertEquals

/** The scan state and outcome through text, as between the browser's worker and the page (`scan-speed-up-2`). */
class ScanStateCodecTest {
    @Test
    fun stateAndOutcomeComeBackTheSame() {
        val scan = VideoScan()
        val frames = VideoFixtures.load(VideoFixtures.PHONE_SCAN_1)
        var checked = 0
        var followed = 0
        for ((i, f) in frames.withIndex()) {
            val s = scan.onFrame(f.faces, i * 100L)
            if (i % 10 != 0 && !s.complete) continue
            // The projection is left out: the screen does not use it.
            assertEquals(s.copy(projection = null, projectionAge = 0), ScanStateCodec.decode(ScanStateCodec.encode(s)), "frame $i")
            checked++
            followed += s.found.count { it.followed }
            if (s.complete) break
        }
        assertEquals(true, checked > 5)
        assertEquals(true, followed > 0, "followed faces go through too")
        val outcome = scan.outcome()
        val back = ScanStateCodec.decodeOutcome(ScanStateCodec.encodeOutcome(outcome))
        assertEquals(outcome.copy(from = emptyMap()), back)
    }
}
