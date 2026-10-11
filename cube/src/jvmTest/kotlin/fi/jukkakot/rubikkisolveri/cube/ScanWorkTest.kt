package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ScanWork
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The scan's work counters (`scan-work-counters`): the same on every run, growing with the pictures that read faces. */
class ScanWorkTest {
    private val frames = VideoFixtures.load("${VideoFixtures.STRIPED}:0-120")

    private fun run(): Pair<VideoScan, List<ScanWork>> {
        val scan = VideoScan()
        val samples = frames.mapIndexed { i, f ->
            scan.onFrame(f.faces, i * 100L)
            scan.work.copy()
        }
        return scan to samples
    }

    @Test
    fun theCountsAreTheSameInTwoRuns() {
        val a = run().second.map { it.toString() }
        val b = run().second.map { it.toString() }
        assertEquals(a, b)
        val last = run().second.last()
        assertTrue(last.namings > 0 && last.nodes > 0 && last.solves > 0 && last.tracks > 0, "all counted: $last")
    }

    @Test
    fun eachPictureWithFacesAddsNamings() {
        val samples = run().second
        val withFaces = frames.indices.filter { it > 0 && frames[it].faces.isNotEmpty() }
        assertTrue(withFaces.isNotEmpty())
        for (i in withFaces) assertTrue(samples[i].namings > samples[i - 1].namings, "picture $i: ${samples[i - 1]} -> ${samples[i]}")
    }

    @Test
    fun aPictureWithoutFacesAfterASteadyStateAddsNoNamings() {
        val (scan, _) = run()
        val t = frames.size * 100L
        // The first empty pictures may still work a moving state out; at the same time no limit is crossed anew.
        var before = scan.work.copy()
        repeat(5) {
            scan.onFrame(emptyList(), t)
            val now = scan.work.copy()
            if (now.namings == before.namings && now.tracks == before.tracks) return@repeat
            before = now
        }
        val steady = scan.work.copy()
        scan.onFrame(emptyList(), t)
        assertEquals(steady.toString(), scan.work.toString())
    }
}
