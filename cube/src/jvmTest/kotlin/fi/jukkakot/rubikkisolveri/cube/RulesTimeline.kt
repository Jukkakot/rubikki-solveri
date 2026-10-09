package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceOption
import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
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
        val tracks = VideoScan::class.java.getDeclaredField("tracks").apply { isAccessible = true }
        frames.forEachIndexed { i, f ->
            val faces = if (System.getenv("TIMELINE_HARD") == "1") f.faces.map { ScanAcceptanceHarness.hardened(it) } else f.faces
            val s = scan.onFrame(faces, i * 100L)
            if (i % every == 0 || s.finished) {
                val ft = tracks.get(scan) as FaceTracks
                println("$i faces=${faces.size} known=${s.recognised} clear=%.2f complete=${s.complete} undecided=${s.undecided}".format(s.clearness))
                for (t in ft.snapshot()) {
                    val o = t.option ?: t.assigned
                    val mine = if (o == FaceOption.NONE) "none" else "${FaceOption.face(o)}${FaceOption.turn(o)}"
                    val state = if (t.option != null) "=" else if (t.face != null) "${t.face}?" else "?"
                    val read = t.leading.joinToString("") { it?.letter?.toString() ?: "." }
                    val fit = truth?.let { best(it, t.leading) }
                    val flag = if (fit != null && o != FaceOption.NONE && fit.first != o) " !" else ""
                    if (System.getenv("TIMELINE_COSTS") == "1") println("      " + ft.costsOf(t.id))
                    println("   #${t.id}x${t.size}${if (t.live) "*" else ""} $state$mine $read" + (fit?.let { " true ${FaceOption.face(it.first)}${FaceOption.turn(it.first)} (${it.second}/8)$flag" } ?: ""))
                }
            }
            if (s.finished) return
        }
    }

    /** The true face and turn whose stickers [leading] (track frame) matches on the most outer stickers. */
    private fun best(truth: String, leading: List<CubeColor?>): Pair<Int, Int> =
        (0 until FaceOption.FACES).map { o ->
            val face = FaceOption.face(o)
            o to (0 until 9).count { n -> n != 4 && leading[RotationSearch.turnIndex(n, FaceOption.turn(o))]?.letter == truth[face.ordinal * 9 + n] }
        }.maxBy { it.second }
}
