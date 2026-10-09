package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** `scan-never-locked`: the faces' turns are rechecked as a whole, so turns settled wrong together do not lock the scan. */
class ScanNeverLockedTest {
    private val tracks = VideoScan::class.java.getDeclaredField("tracks").also { it.isAccessible = true }

    private fun VideoScan.faceTracks() = tracks.get(this) as FaceTracks

    private fun VideoScanState.cube() = stickers.joinToString("") { it?.letter?.toString() ?: "?" }

    /** The cube of the first finished picture of recording [name], or null when it never finishes. */
    private fun finishedCube(name: String): String? =
        VideoFixtures.replay(VideoFixtures.loadRecording(name)).states.firstOrNull { it.finished }?.cube()

    /** Every face read right, the turns settled wrong together in the first pictures (browser, 2026-10-09 10:08). */
    @Test
    fun turnsSettledWrongTogetherAreCorrected() {
        val cube = finishedCube("web_20261009_100824")
        assertNotNull(cube, "the scan never finished")
        assertEquals(TRUTH_1009, cube)
    }

    /** The recording that finished before still finishes with the true cube. */
    @Test
    fun finishedRecordingStillFinishes() {
        assertEquals(TRUTH_1009, finishedCube("web_20261009_100814"))
    }

    /**
     * Each face shown alone, its reading turned, round the cube four times: only the best cube tells the turns,
     * and for these scrambles the faces' turns settled wrong together and never finished before the recheck.
     */
    @Test
    fun facesSeenAloneWithTurnsSettledWrongTogetherFinishRight() {
        for (seed in LOCKED_SEEDS) {
            val (cube, scan, states) = alone(seed)
            val finished = states.firstOrNull { it.finished }
            assertNotNull(finished, "seed $seed never finished: ${scan.faceTracks().describe()}")
            assertEquals(cube.toString(), finished.cube(), "seed $seed")
            assertTrue(scan.faceTracks().wholeChanges > 0, "seed $seed: finished by a recheck")
        }
    }

    /** Once the cube is clear, no recheck changes anything. */
    @Test
    fun aClearCubeIsNeverChanged() {
        val runs = LOCKED_SEEDS.map { seed -> alone(seed).let { (_, scan, states) -> scan to states } } +
            listOf("web_20261009_100824", "web_20261009_100814").map { name -> replayWithScan(name) }
        for ((scan, states) in runs) {
            val changes = changesPerPicture(scan, states)
            for (i in 1 until states.size) {
                if (states[i - 1].clearness >= VideoScan.CLEAR_MARGIN) assertEquals(changes[i - 1], changes[i], "a change after a clear picture at $i")
            }
        }
    }

    /** The recheck stays within its budget (about 2 ms on the JVM; tripled on CI). */
    @Test
    fun theRecheckStaysWithinItsBudget() {
        val scale = if (System.getenv("CI") != null) 3.0 else 1.0
        repeat(2) { replayWithScan("web_20261009_100824") } // warm-up
        val nanos = ArrayList<Long>()
        val recording = VideoFixtures.loadRecording("web_20261009_100824")
        VideoFixtures.replay(recording) { _, scan, s -> if (s != null) scan.faceTracks().recheckNanos.takeIf { it > 0 }?.let { nanos += it } }
        assertTrue(nanos.size >= 10, "rechecks ran: ${nanos.size}")
        val ms = nanos.sorted().map { it / 1e6 }
        val median = ms[ms.size / 2]
        println("recheck: ${ms.size} runs, median %.2f ms, 90th %.2f ms, most %.2f ms".format(median, ms[ms.size * 9 / 10], ms.last()))
        assertTrue(median <= 2.0 * scale, "median %.2f ms".format(median))
    }

    /** [FaceTracks.wholeChanges] after each picture of [states] (as the scan stands at the end, the replay is redone). */
    private fun changesPerPicture(scan: VideoScan, states: List<VideoScanState>): List<Int> = recorded.getValue(scan).also { assertEquals(states.size, it.size) }

    private val recorded = HashMap<VideoScan, List<Int>>()

    private fun replayWithScan(name: String): Pair<VideoScan, List<VideoScanState>> {
        val changes = ArrayList<Int>()
        var last: VideoScan? = null
        val replay = VideoFixtures.replay(VideoFixtures.loadRecording(name)) { _, scan, s ->
            if (scan !== last) changes.clear()
            last = scan
            if (s != null) changes += scan.faceTracks().wholeChanges
        }
        // A recording with resets: only its last scan's pictures.
        val states = replay.states.takeLast(changes.size)
        recorded[replay.scan] = changes.toList()
        return replay.scan to states
    }

    /** [seed]'s scramble, each face shown alone (straight, its reading turned) eight pictures at a time, four rounds. */
    private fun alone(seed: Int): Triple<Cube, VideoScan, List<VideoScanState>> {
        val rnd = Random(seed)
        val cube = Cube.solved().apply((0 until 20).joinToString(" ") { MOVES[rnd.nextInt(MOVES.size)] })
        val turns = List(6) { rnd.nextInt(4) }
        val order = Face.entries.shuffled(rnd)
        val scan = VideoScan()
        val states = ArrayList<VideoScanState>()
        val changes = ArrayList<Int>()
        var t = 0L
        for (round in 0 until 4) for (f in order) repeat(8) {
            states += scan.onFrame(listOf(SyntheticViews.straight(cube, f, turns[f.ordinal])), t)
            changes += scan.faceTracks().wholeChanges
            t += 100
        }
        recorded[scan] = changes
        return Triple(cube, scan, states)
    }

    companion object {
        const val TRUTH_1009 = "RGYGWBWOGWWGGRRYWOGWROGYBYROBGRYRWOWBWOOOBRBYORYYBGBYB"

        private val MOVES = listOf("U", "R", "F", "D", "L", "B", "U'", "R'", "F'", "D'", "L'", "B'", "U2", "R2", "F2")

        /** The seeds of [alone] (of the first 60) that never finished before the recheck. */
        private val LOCKED_SEEDS = listOf(2, 10, 17, 21, 26)
    }
}
