package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `scan-rules` acceptance: replays every fixture with a known cube, as recorded and in a robustness
 * variant (red centres faded towards orange, blue ones towards white), and prints per run the frame
 * the scan finishes, whether the cube is right, and the time
 * per frame. Fails when the scan breaks the bar of `scan-rules` design 6: each fixture's finish within
 * 1.2 times the frame stored in [FINISHED]. Runs only with ACCEPTANCE=1 (slow); the report also goes
 * to `build/acceptance-report.txt`.
 */
class ScanAcceptanceHarness {
    private val fixtures = listOf(
        VideoFixtures.ANGLED to VideoFixtures.TRUTH,
        VideoFixtures.STRAIGHT to VideoFixtures.TRUTH,
        "20261005_213729" to VideoFixtures.EVENING_TRUTH,
        "20261005_213817" to VideoFixtures.EVENING_TRUTH,
        "20261005_213850" to VideoFixtures.EVENING_TRUTH,
        "20261005_213929" to VideoFixtures.EVENING_TRUTH,
        VideoFixtures.CAMERA_1007 to VideoFixtures.TRUTH_1007,
        VideoFixtures.TOUR_1007 to VideoFixtures.TRUTH_1007,
        VideoFixtures.BLUE_FIRST_1007 to VideoFixtures.TRUTH_1007,
        VideoFixtures.WEB_1007 to VideoFixtures.TRUTH_1007,
        VideoFixtures.STRIPED to VideoFixtures.STRIPED_TRUTH,
        VideoFixtures.STRIPED_DIM to VideoFixtures.STRIPED_TRUTH,
        VideoFixtures.STRIPED_TABLE to VideoFixtures.STRIPED_TRUTH,
        VideoFixtures.STRIPED_U2_TABLE to VideoFixtures.STRIPED_U2_TRUTH,
        VideoFixtures.STRIPED_U2_DIM to VideoFixtures.STRIPED_U2_TRUTH,
        VideoFixtures.PHONE_SCAN_1 to VideoFixtures.STRIPED_TRUTH,
        VideoFixtures.PHONE_RULES_3 to VideoFixtures.TRUTH_1008C,
        VideoFixtures.PHONE_LOOK_3 to VideoFixtures.TRUTH_1008C,
    )

    /** One replay: the frame the scan finished at (null: never), whether its cube was the true one, ms per frame. */
    data class Run(val finishedAt: Int?, val right: Boolean, val msPerFrame: Double, val cube: String = "") {
        override fun toString() = (finishedAt?.let { "$it ${if (right) "right" else "WRONG"}" } ?: "never") + " (%.1f ms)".format(java.util.Locale.ROOT, msPerFrame) +
            if (System.getenv("ACCEPTANCE_CUBES") == "1") " $cube" else ""
    }

    private val out = StringBuilder()

    private fun say(line: String) {
        println(line)
        out.appendLine(line)
    }

    @Test
    fun scannerMeetsTheBar() {
        assumeTrue("set ACCEPTANCE=1 to run", System.getenv("ACCEPTANCE") == "1")
        val runs = HashMap<Pair<String, Boolean>, Run>()
        for ((title, harden) in listOf("as recorded" to false, "robustness: red centres towards orange, blue towards white" to true)) {
            say("== $title")
            say("video | frames | finish")
            for ((video, truth) in fixtures) {
                val frames = VideoFixtures.load(video).map { f -> if (harden) f.copy(faces = f.faces.map(::hardened)) else f }
                val run = replay(frames, truth).also { runs[video to harden] = it }
                say("$video | ${frames.size} | $run")
            }
        }
        val problems = barProblems { video, harden -> runs.getValue(video to harden) }
        problems.forEach { say("BAR: $it") }
        File("build/acceptance-report.txt").writeText(out.toString())
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    /** Where the scan falls short of the bar (`scan-rules` design 6, held by [FINISHED] since `scan-rules-only`), given each [run]. */
    private fun barProblems(run: (String, Boolean) -> Run): List<String> {
        val problems = ArrayList<String>()
        for ((video, _) in fixtures) for (harden in listOf(false, true)) {
            val r = run(video, harden)
            if (r.finishedAt != null && !r.right) problems += "$video${if (harden) " (robustness)" else ""}: finished WRONG"
        }
        // Speed and the robustness count as confirmed (2026-10-07): on the first eleven fixtures. The later
        // ones are reported only.
        val confirmed = fixtures.take(CONFIRMED)
        for ((video, _) in fixtures) {
            val bar = FINISHED[video] ?: continue
            val limit = (bar * 1.2).toInt()
            val r = run(video, false)
            if (r.finishedAt == null || r.finishedAt > limit) {
                val line = "$video: finished at ${r.finishedAt} (stored $bar, limit $limit)"
                if (confirmed.any { it.first == video }) problems += line else say("NOTE: $line")
            }
        }
        val robust = confirmed.count { (video, _) -> run(video, true).let { it.finishedAt != null && it.right } }
        if (robust < 6) problems += "robustness: finished right on $robust of ${confirmed.size} (need 6)"
        val ms = fixtures.flatMap { (video, _) -> listOf(false, true).map { run(video, it).msPerFrame } }.average()
        if (ms >= 10.0) problems += "%.1f ms per frame on average (need under 10)".format(java.util.Locale.ROOT, ms)
        return problems
    }

    /** [frames] through a scan at 10 fps until it finishes. */
    private fun replay(frames: List<VideoFixtures.Frame>, truth: String): Run {
        val scan = VideoScan()
        val start = System.nanoTime()
        frames.forEachIndexed { i, f ->
            val s = scan.onFrame(f.faces, i * 100L)
            if (s.finished) {
                val ms = (System.nanoTime() - start) / 1e6 / (i + 1)
                val cube = s.stickers.joinToString("") { it?.letter?.toString() ?: "?" }
                return Run(i, cube == truth, ms, cube)
            }
        }
        return Run(null, false, (System.nanoTime() - start) / 1e6 / frames.size.coerceAtLeast(1))
    }

    companion object {
        /** The fixtures the bar was confirmed on (2026-10-07). */
        const val CONFIRMED = 11

        /**
         * The frame each fixture finished at as recorded, when `scan-rules-only` removed the earlier scanner the
         * bar compared with (2026-10-09); fixtures that did not finish then have none.
         */
        val FINISHED = mapOf(
            VideoFixtures.ANGLED to 228,
            VideoFixtures.STRAIGHT to 119,
            "20261005_213729" to 124,
            "20261005_213817" to 125,
            "20261005_213850" to 168,
            "20261005_213929" to 192,
            VideoFixtures.TOUR_1007 to 158,
            VideoFixtures.BLUE_FIRST_1007 to 73,
            VideoFixtures.STRIPED to 202,
            VideoFixtures.STRIPED_TABLE to 159,
            VideoFixtures.STRIPED_U2_TABLE to 170,
            VideoFixtures.STRIPED_U2_DIM to 143,
            VideoFixtures.PHONE_SCAN_1 to 105,
        )

        /** A red centre faded towards orange until the palette names it orange; a blue one towards white until it names it white. */
        fun hardened(face: FaceReading): FaceReading {
            val centre = face.colors[4] ?: return face
            val p = ColorClassifier.DEFAULT_PALETTE
            val changed = when (ColorClassifier.rankedCentre(centre).first()) {
                CubeColor.RED -> SyntheticViews.fadedCentre(centre, p.getValue(CubeColor.ORANGE), CubeColor.ORANGE)
                CubeColor.BLUE -> SyntheticViews.fadedCentre(centre, Rgb(255, 255, 255), CubeColor.WHITE)
                else -> null
            } ?: return face
            return face.copy(colors = face.colors.toMutableList().also { it[4] = changed })
        }
    }
}
