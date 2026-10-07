package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/**
 * For tuning the rules scanner: replays the fixture TIMELINE=<video> (TIMELINE_HARD=1: robustness
 * variant) and prints every TIMELINE_EVERY-th frame the tracks, the clearness and the known stickers.
 */
class RulesTimeline {
    @Test
    fun print() {
        val video = System.getenv("TIMELINE")
        assumeTrue("set TIMELINE=<video> to run", video != null)
        val every = System.getenv("TIMELINE_EVERY")?.toInt() ?: 10
        val frames = VideoFixtures.load(video!!)
        val scan = VideoScan(engine = ScanEngine.RULES)
        val tracks = VideoScan::class.java.getDeclaredField("tracks").apply { isAccessible = true }
        frames.forEachIndexed { i, f ->
            val faces = if (System.getenv("TIMELINE_HARD") == "1") f.faces.map { ScanAcceptanceHarness.hardened(it) } else f.faces
            val s = scan.onFrame(faces, i * 100L)
            if (i % every == 0 || s.finished) {
                val ft = tracks.get(scan) as FaceTracks
                println("$i faces=${faces.size} known=${s.recognised} clear=%.2f complete=${s.complete} undecided=${s.undecided} | ${ft.describe()}".format(s.clearness))
            }
            if (s.finished) return
        }
    }
}
