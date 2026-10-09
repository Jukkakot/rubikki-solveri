package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/**
 * For tuning the rules scanner: replays the fixture TIMELINE=<video> (TIMELINE_HARD=1: robustness
 * variant) and prints every TIMELINE_EVERY-th frame the tracks, the clearness and the known stickers;
 * with TIMELINE_TRUTH=<URFDLB> each track also with the true face and turn its leading colours fit
 * best (`!` where the solver's differs).
 */
class RulesTimeline {
    @Test
    fun print() {
        val video = System.getenv("TIMELINE")
        assumeTrue("set TIMELINE=<video> to run", video != null)
        val every = System.getenv("TIMELINE_EVERY")?.toInt() ?: 10
        val truth = System.getenv("TIMELINE_TRUTH")
        val frames = VideoFixtures.load(video!!)
        val scan = VideoScan()
        frames.forEachIndexed { i, f ->
            val faces = if (System.getenv("TIMELINE_HARD") == "1") f.faces.map { ScanAcceptanceHarness.hardened(it) } else f.faces
            val s = scan.onFrame(faces, i * 100L)
            if (i % every == 0 || s.finished) ScanTimeline.print("$i", scan, faces.size, s, truth)
            if (s.finished) return
        }
    }
}
