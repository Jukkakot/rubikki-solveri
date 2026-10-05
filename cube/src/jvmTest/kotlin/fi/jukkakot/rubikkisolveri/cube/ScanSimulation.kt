package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.BestCube
import fi.jukkakot.rubikkisolveri.cube.scan.StickerEvidence
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `video-scan-progress` design 4: simulated video scans to choose the clear threshold
 * [VideoScan.CLEAR_MARGIN]. Each run scrambles a cube and shows its faces in a random order (one
 * possibly never), a face held for a while and a couple of faces at once while turning; each frame
 * reads the stickers in view with the misreads seen in `video-scan-spike` (bursts of a wrong colour
 * lasting up to five frames, red and orange mixed up, in dim light a whole face's orange read as
 * red). After every frame the best cube is solved; a threshold finishes the run once the clearness
 * stays over it for half a second (5 frames). The full sweep runs with SIMULATION=<runs> and writes
 * `frames/simulation.txt`; the small test checks the chosen threshold.
 */
class ScanSimulation {
    /** Per frame: the clearness and whether the best cube was the true one, and stickers read so far. */
    class Trace(val clearness: DoubleArray, val right: BooleanArray, val read: IntArray, val dim: Boolean, val biased: Boolean, val dropped: Boolean)

    private class Burst(var color: CubeColor, var left: Int)

    fun run(random: Random, stopAbove: Double): Trace {
        val cube = Cube.solved().apply(Scramble.random(random = random))
        val dim = random.nextDouble() < 0.4
        val votes = List(Stickers.COUNT) { IntArray(6) }
        val bursts = HashMap<Int, Burst>()
        // In dim light some faces read most of their orange as red, all through.
        val orangeAsRed = Face.entries.associateWith { dim && random.nextDouble() < 0.3 }
        val order = Face.entries.shuffled(random).let { if (random.nextDouble() < 0.2) it.dropLast(1) else it }
        val clearness = ArrayList<Double>()
        val right = ArrayList<Boolean>()
        val read = ArrayList<Int>()
        var over = 0

        fun read(face: Face, angled: Boolean) {
            for (n in 0 until 9) {
                val s = face.ordinal * 9 + n
                if (n == 4 || random.nextDouble() < 0.05) continue
                val truth = cube[s]
                val burst = bursts[s]
                val color = when {
                    burst != null -> burst.color.also { if (--burst.left == 0) bursts.remove(s) }
                    random.nextDouble() < (if (angled) 0.03 else 0.01) * (if (dim) 2 else 1) -> {
                        val wrong = if (truth.isRedOrOrange && random.nextDouble() < 0.7) truth.partner else CubeColor.entries.filter { it != truth }.random(random)
                        val length = 1 + random.nextInt(5)
                        if (length > 1) bursts[s] = Burst(wrong, length - 1)
                        wrong
                    }
                    truth == CubeColor.ORANGE && orangeAsRed.getValue(face) && random.nextDouble() < 0.6 -> CubeColor.RED
                    truth.isRedOrOrange && random.nextDouble() < (if (dim) 0.15 else 0.05) -> truth.partner
                    else -> truth
                }
                votes[s][color.ordinal]++
            }
        }

        fun frame(vararg faces: Pair<Face, Boolean>): Boolean {
            for ((face, angled) in faces) read(face, angled)
            val evidence = StickerEvidence(votes.map { it.copyOf() })
            val best = BestCube.solve(evidence)!!
            val c = best.clearness(evidence)
            clearness += c
            right += best.cube == cube
            read += (0 until Stickers.COUNT).count { it % 9 == 4 || votes[it].sum() > 0 }
            over = if (c >= stopAbove) over + 1 else 0
            return over >= FINISH_FRAMES
        }

        outer@ for ((i, face) in order.withIndex()) {
            repeat(8 + random.nextInt(18)) { if (frame(face to false)) break@outer }
            val next = order.getOrNull(i + 1) ?: break
            // Turning: the next face comes into view at an angle (only if it is a neighbour).
            if (next != face.opposite) repeat(2 + random.nextInt(5)) { if (frame(face to true, next to true)) break@outer }
        }
        // Then every face again in turn (the arrow asks for the one never shown), until clear or 45 s have passed.
        var k = 0
        while (clearness.size < 450 && over < FINISH_FRAMES) {
            val face = Face.entries[k++ / 15 % 6]
            frame(face to false)
        }
        return Trace(clearness.toDoubleArray(), right.toBooleanArray(), read.toIntArray(), dim, orangeAsRed.values.any { it }, order.size < 6)
    }

    /** Where [trace] finishes with threshold [t]: the frame, or null when it never does. */
    private fun finish(trace: Trace, t: Double): Int? {
        var over = 0
        for (i in trace.clearness.indices) {
            over = if (trace.clearness[i] >= t) over + 1 else 0
            if (over >= FINISH_FRAMES) return i
        }
        return null
    }

    private val CubeColor.isRedOrOrange get() = this == CubeColor.RED || this == CubeColor.ORANGE
    private val CubeColor.partner get() = if (this == CubeColor.RED) CubeColor.ORANGE else CubeColor.RED

    @Test
    fun sweep() {
        val runs = System.getenv("SIMULATION")?.toIntOrNull() ?: return
        val random = Random(2026_10_05)
        val thresholds = (1..18).map { it * 0.5 }
        val start = System.nanoTime()
        val traces = List(runs) { run(random, thresholds.last()) }
        val ms = (System.nanoTime() - start) / 1e6 / traces.sumOf { it.clearness.size }
        val out = StringBuilder("runs=$runs, ms per frame=${"%.2f".format(ms)}\nT | wrong | not finished | frames to finish (median, 90 %) | stickers read at finish (median) | not finished: biased dim, other dim, good | finished, one face first shown in the second round\n")
        for (t in thresholds) {
            val ends = traces.map { tr -> finish(tr, t)?.let { tr to it } }
            val done = ends.filterNotNull()
            val wrong = done.count { (tr, i) -> !tr.right[i] }
            val frames = done.map { it.second }.sorted()
            val stickers = done.map { (tr, i) -> tr.read[i] }.sorted()
            val open = traces.filterIndexed { i, _ -> ends[i] == null }
            out.appendLine(
                "%.1f | %d | %d | %d, %d | %d | %d, %d, %d | %d".format(
                    t, wrong, runs - done.size, frames.getOrElse(frames.size / 2) { -1 }, frames.getOrElse(frames.size * 9 / 10) { -1 },
                    stickers.getOrElse(stickers.size / 2) { -1 },
                    open.count { it.biased }, open.count { it.dim && !it.biased }, open.count { !it.dim },
                    done.count { it.first.dropped },
                ),
            )
        }
        println(out)
        File("../testdata/video").takeIf { it.isDirectory }?.let { File(it, "simulation.txt").writeText(out.toString()) }
    }

    @Test
    fun chosenThresholdNeverFinishesWrong() {
        val random = Random(5)
        var finished = 0
        repeat(60) {
            val trace = run(random, VideoScan.CLEAR_MARGIN)
            val end = finish(trace, VideoScan.CLEAR_MARGIN) ?: return@repeat
            finished++
            assertTrue(trace.right[end], "run $it finished on a wrong cube")
        }
        assertTrue(finished > 30, "$finished of 60 finished")
    }

    companion object {
        const val FINISH_FRAMES = 5
    }
}
