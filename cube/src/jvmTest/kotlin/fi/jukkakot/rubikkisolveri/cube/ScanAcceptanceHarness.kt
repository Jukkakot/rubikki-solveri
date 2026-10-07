package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `scan-rules` acceptance: replays every fixture with a known cube, as recorded and in a robustness
 * variant (red centres faded towards orange, blue ones towards white), through both scanners
 * ([ScanEngine]), and prints per run the frame it finishes, whether the cube is right, and the time
 * per frame. Fails when the rules scanner breaks the bar of `scan-rules` design 6. Runs only with
 * ACCEPTANCE=1 (slow); the report also goes to `build/acceptance-report.txt`.
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
    )

    /** One replay: the frame the scan finished at (null: never), whether its cube was the true one, ms per frame. */
    data class Run(val finishedAt: Int?, val right: Boolean, val msPerFrame: Double) {
        override fun toString() = (finishedAt?.let { "$it ${if (right) "right" else "WRONG"}" } ?: "never") + " (%.1f ms)".format(java.util.Locale.ROOT, msPerFrame)
    }

    private val out = StringBuilder()

    private fun say(line: String) {
        println(line)
        out.appendLine(line)
    }

    @Test
    fun rulesScannerMeetsTheBar() {
        assumeTrue("set ACCEPTANCE=1 to run", System.getenv("ACCEPTANCE") == "1")
        val engines = (System.getenv("ACCEPTANCE_ENGINES") ?: "look,rules").split(",").map { name -> ScanEngine.entries.first { it.logName == name } }
        val runs = HashMap<Triple<String, Boolean, ScanEngine>, Run>()
        for ((title, harden) in listOf("as recorded" to false, "robustness: red centres towards orange, blue towards white" to true)) {
            say("== $title")
            say("video | frames | " + engines.joinToString(" | ") { it.logName })
            for ((video, truth) in fixtures) {
                val frames = VideoFixtures.load(video).map { f -> if (harden) f.copy(faces = f.faces.map(::hardened)) else f }
                val row = engines.map { e -> replay(frames, truth, e).also { runs[Triple(video, harden, e)] = it } }
                say("$video | ${frames.size} | ${row.joinToString(" | ")}")
            }
        }
        File("build/acceptance-report.txt").writeText(out.toString())
        if (ScanEngine.RULES !in engines || ScanEngine.LOOK !in engines) return
        val problems = barProblems { video, harden, engine -> runs.getValue(Triple(video, harden, engine)) }
        problems.forEach { say("BAR: $it") }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    /** Where the rules scanner falls short of the bar (`scan-rules` design 6), given each [run]. */
    private fun barProblems(run: (String, Boolean, ScanEngine) -> Run): List<String> {
        val problems = ArrayList<String>()
        for ((video, _) in fixtures) for (harden in listOf(false, true)) {
            val r = run(video, harden, ScanEngine.RULES)
            if (r.finishedAt != null && !r.right) problems += "$video${if (harden) " (robustness)" else ""}: finished WRONG"
        }
        for ((video, _) in fixtures) {
            val look = run(video, false, ScanEngine.LOOK)
            val rules = run(video, false, ScanEngine.RULES)
            val limit = look.finishedAt?.let { (it * 1.2).toInt() } ?: continue
            if (!look.right) continue
            if (rules.finishedAt == null || rules.finishedAt > limit) problems += "$video: finished at ${rules.finishedAt} (earlier scanner ${look.finishedAt}, limit $limit)"
        }
        val robust = fixtures.count { (video, _) -> run(video, true, ScanEngine.RULES).let { it.finishedAt != null && it.right } }
        if (robust < 6) problems += "robustness: finished right on $robust of ${fixtures.size} (need 6)"
        val ms = fixtures.flatMap { (video, _) -> listOf(false, true).map { run(video, it, ScanEngine.RULES).msPerFrame } }.average()
        if (ms >= 10.0) problems += "%.1f ms per frame on average (need under 10)".format(java.util.Locale.ROOT, ms)
        return problems
    }

    /** [frames] through a scanner of [engine] at 10 fps until it finishes. */
    private fun replay(frames: List<VideoFixtures.Frame>, truth: String, engine: ScanEngine): Run {
        val scan = VideoScan(engine = engine)
        val start = System.nanoTime()
        frames.forEachIndexed { i, f ->
            val s = scan.onFrame(f.faces, i * 100L)
            if (s.finished) {
                val ms = (System.nanoTime() - start) / 1e6 / (i + 1)
                return Run(i, s.stickers.joinToString("") { it?.letter?.toString() ?: "?" } == truth, ms)
            }
        }
        return Run(null, false, (System.nanoTime() - start) / 1e6 / frames.size.coerceAtLeast(1))
    }

    /** A red centre faded towards orange until the palette names it orange; a blue one towards white until it names it white. */
    private fun hardened(face: FaceReading): FaceReading {
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
