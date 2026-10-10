package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import org.junit.Assume.assumeTrue
import java.io.File
import java.util.Locale
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `scan-finish-fast`: for every fixture (as recorded and the robustness variant), recording and synthetic
 * scramble, the time the best cube first became right and clear, the finish, the delay between them and what
 * held the finish meanwhile (frames the cube was not clear, a track read against it, the turns were not clear).
 * Fails on a wrong finish. Runs only with FINISH_DELAY=1 (slow); the table also goes to `build/finish-delay.txt`.
 */
class FinishDelayHarness {
    private val tracksField = VideoScan::class.java.getDeclaredField("tracks").also { it.isAccessible = true }

    private fun VideoScan.faceTracks() = tracksField.get(this) as FaceTracks

    private fun VideoScanState.cube() = stickers.joinToString("") { it?.letter?.toString() ?: "?" }

    /** One run's measures (times in ms of the run's clock). */
    data class Delay(val clearAt: Long?, val finishAt: Long?, val right: Boolean, val unclear: Int, val loud: Int, val turns: Int, val wrongClear: Int = 0, val wrongComplete: Long = 0, val wrongTurnsClear: Int = 0) {
        val delay: Long? get() = if (clearAt != null && finishAt != null) finishAt - clearAt else null
        override fun toString(): String = listOf(
            clearAt?.let { "%.1f".format(Locale.ROOT, it / 1000.0) } ?: "-",
            finishAt?.let { "%.1f".format(Locale.ROOT, it / 1000.0) + if (right) "" else " WRONG" } ?: "never",
            delay?.let { "%.1f".format(Locale.ROOT, it / 1000.0) } ?: "-",
            "$unclear/$loud/$turns",
            "wrong clear $wrongClear (turns clear $wrongTurnsClear), complete ${wrongComplete} ms",
        ).joinToString(" | ")
    }

    /** Follows one scan picture by picture against [truth]. */
    private inner class Meter(private val truth: String) {
        var clearAt: Long? = null
        var finishAt: Long? = null
        var right = false
        var unclear = 0
        var loud = 0
        var turns = 0
        var wrongClear = 0
        var wrongTurnsClear = 0
        var wrongSince: Long? = null
        var wrongComplete = 0L

        /** After [scan] took a picture at [t] giving [s]. */
        fun on(scan: VideoScan, s: VideoScanState, t: Long) {
            val ft = scan.faceTracks()
            val best = ft.best
            if (clearAt == null && best != null && best.cube.toString() == truth && s.clearness >= VideoScan.CLEAR_MARGIN) clearAt = t
            if (best != null && best.cube.toString() != truth && s.clearness >= VideoScan.CLEAR_MARGIN) {
                wrongClear++
                if (ft.turnsClear()) wrongTurnsClear++
            }
            if (s.complete && s.cube() != truth) {
                val from = wrongSince ?: t.also { wrongSince = it }
                wrongComplete = maxOf(wrongComplete, t - from)
            } else {
                wrongSince = null
            }
            // After the finish only the wrong-cube measures go on (to the end: how close a wrong cube came).
            if (finishAt != null) return
            if (s.finished) {
                finishAt = t
                right = s.cube() == truth
                return
            }
            if (clearAt != null && !s.complete) {
                when {
                    best == null || s.clearness < VideoScan.CLEAR_MARGIN -> unclear++
                    !ft.quietFor(best.cube) -> loud++
                    else -> turns++
                }
            }
        }

        fun delay() = Delay(clearAt, finishAt, right, unclear, loud, turns, wrongClear, wrongComplete, wrongTurnsClear)
    }

    private fun fixture(video: String, truth: String, harden: Boolean): Delay {
        val scan = VideoScan()
        val m = Meter(truth)
        VideoFixtures.load(video).forEachIndexed { i, f ->
            val faces = if (harden) f.faces.map(ScanAcceptanceHarness::hardened) else f.faces
            m.on(scan, scan.onFrame(faces, i * 100L), i * 100L)
        }
        return m.delay()
    }

    private fun recording(name: String, truth: String): Delay {
        val recording = VideoFixtures.loadRecording(name)
        var m = Meter(truth)
        VideoFixtures.replay(recording) { entry, scan, s ->
            if (entry is ScanRecording.Reset && m.finishAt == null) m = Meter(truth)
            if (entry is ScanRecording.Picture && s != null) m.on(scan, s, entry.ms)
        }
        return m.delay()
    }

    /** As `ScanNeverLockedTest.alone`: [seed]'s scramble, each face shown alone eight pictures at a time, four rounds. */
    private fun alone(seed: Int): Delay {
        val rnd = Random(seed)
        val cube = Cube.solved().apply((0 until 20).joinToString(" ") { MOVES[rnd.nextInt(MOVES.size)] })
        val turns = List(6) { rnd.nextInt(4) }
        val order = Face.entries.shuffled(rnd)
        val scan = VideoScan()
        val m = Meter(cube.toString())
        var t = 0L
        for (round in 0 until 4) for (f in order) repeat(8) {
            val faces: List<FaceReading> = listOf(SyntheticViews.straight(cube, f, turns[f.ordinal]))
            m.on(scan, scan.onFrame(faces, t), t)
            t += 100
        }
        return m.delay()
    }

    @Test
    fun table() {
        assumeTrue("set FINISH_DELAY=1 to run", System.getenv("FINISH_DELAY") == "1")
        val out = StringBuilder()
        fun say(line: String) {
            println(line)
            out.appendLine(line)
        }
        val wrong = ArrayList<String>()
        val delays = ArrayList<Long>()
        val t0 = System.nanoTime()
        say("run | clear s | finish s | delay s | held unclear/loud/turns (pictures)")
        for ((video, truth) in FIXTURES) for (harden in listOf(false, true)) {
            val name = video + if (harden) " (robust)" else ""
            val d = fixture(video, truth, harden)
            say("$name | $d")
            if (d.finishAt != null && !d.right) wrong += name
            d.delay?.let { delays += it }
        }
        for ((name, truth) in RECORDINGS) {
            val d = recording(name, truth)
            say("$name | $d")
            if (d.finishAt != null && !d.right) wrong += name
            d.delay?.let { delays += it }
        }
        var synthWrong = 0
        var synthNever = 0
        val synthDelays = ArrayList<Long>()
        for (seed in 0 until SEEDS) {
            val d = alone(seed)
            if (System.getenv("FINISH_DELAY_SEEDS") == "1") say("seed $seed | $d")
            if (d.finishAt != null && !d.right) {
                synthWrong++
                wrong += "seed $seed"
            }
            if (d.finishAt == null) synthNever++
            d.delay?.let { synthDelays += it }
        }
        say("synthetic $SEEDS seeds: wrong $synthWrong, never $synthNever, delay mean %.2f s, most %.1f s".format(Locale.ROOT, synthDelays.average() / 1000, (synthDelays.maxOrNull() ?: 0) / 1000.0))
        say("real runs: delay mean %.2f s, median %.1f s, most %.1f s".format(Locale.ROOT, delays.average() / 1000, delays.sorted()[delays.size / 2] / 1000.0, delays.max() / 1000.0))
        say("wrong finishes: ${wrong.size} ${wrong.joinToString()}; %.0f s".format(Locale.ROOT, (System.nanoTime() - t0) / 1e9))
        File("build/finish-delay.txt").writeText(out.toString())
        assertTrue(wrong.isEmpty(), "wrong finishes: $wrong")
    }

    companion object {
        const val SEEDS = 60

        private val MOVES = listOf("U", "R", "F", "D", "L", "B", "U'", "R'", "F'", "D'", "L'", "B'", "U2", "R2", "F2")

        val FIXTURES = listOf(
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

        val RECORDINGS = listOf(
            "web_20261009_100814" to ScanNeverLockedTest.TRUTH_1009,
            "web_20261009_100824" to ScanNeverLockedTest.TRUTH_1009,
            "web_20261010_102548" to ScanFinishUnblockTest.TRUTH_1010,
        )
    }
}
