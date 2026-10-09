package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecorder
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The scan recording's text format, its 90 s window, and that a replay of it is exact (`scan-recording`). */
class ScanRecordingTest {
    private val face = FaceReading(
        List(9) { i -> if (i == 3) null else Rgb(200, 10 * i, 30) },
        Point(120.25, 80.5), Point(20.125, 1.0), Point(-1.5, 19.75),
    )

    private fun recorder(keep: Long = ScanRecording.KEEP_MILLIS) = ScanRecorder("android", "1.4 · 9.10.2026", "2026-10-09T11:54:00Z", keep)

    @Test
    fun roundTrip() {
        val r = recorder()
        r.picture(5_000, listOf(face), 360, 640, 12)
        r.picture(5_040, emptyList(), 360, 640)
        r.reset(5_050)
        r.picture(5_080, listOf(face, face), 360, 640)
        val rec = ScanRecording.read(r.text(ScanRecording.End.Finished("W".repeat(54))))
        assertEquals("android", rec.platform)
        assertEquals("1.4", rec.version)
        assertEquals("2026-10-09T11:54:00Z", rec.started)
        assertEquals(5_000, rec.t0)
        assertNull(rec.cut)
        assertEquals(listOf(0L, 40L, 50L, 80L), rec.entries.map { it.ms })
        assertIs<ScanRecording.Reset>(rec.entries[2])
        val first = assertIs<ScanRecording.Picture>(rec.entries[0])
        assertEquals(listOf(face), first.found.faces)
        assertEquals(360 to 640, first.found.width to first.found.height)
        assertEquals(2, (rec.entries[3] as ScanRecording.Picture).found.faces.size)
        assertEquals(ScanRecording.End.Finished("W".repeat(54)), rec.end)
    }

    @Test
    fun endLines() {
        for (end in listOf(ScanRecording.End.Left, ScanRecording.End.Restart, ScanRecording.End.Finished("..."))) {
            val r = recorder()
            r.picture(0, listOf(face), 10, 10)
            assertEquals(end, ScanRecording.read(r.text(end)).end)
        }
    }

    @Test
    fun keepsTheLast90Seconds() {
        val r = recorder()
        for (t in 0L..180_000L step 100) r.picture(t, listOf(face), 360, 640)
        val rec = ScanRecording.read(r.text(ScanRecording.End.Left))
        assertEquals(90_000, rec.cut)
        assertEquals(90_000, rec.entries.first().ms)
        assertEquals(180_000, rec.entries.last().ms)
        assertEquals(901, r.pictures)
    }

    @Test
    fun shortScanNotCut() {
        val r = recorder()
        for (t in 0L..30_000L step 100) r.picture(t, listOf(face), 360, 640)
        val rec = ScanRecording.read(r.text(ScanRecording.End.Left))
        assertNull(rec.cut)
        assertEquals(301, rec.entries.size)
    }

    @Test
    fun malformedLineNamesItsNumber() {
        val text = "# scan-recording 1 platform=web ver=1 started=x t0=0\n0 360,640,0\n40 1,2,3,4\n# end left\n"
        val e = assertFailsWith<IllegalArgumentException> { ScanRecording.read(text) }
        assertTrue(e.message!!.startsWith("line 3:"), e.message)
        val time = assertFailsWith<IllegalArgumentException> { ScanRecording.read("# scan-recording 1\nabc reset\n") }
        assertTrue(time.message!!.startsWith("line 2:"), time.message)
        assertFailsWith<IllegalArgumentException> { ScanRecording.read("hello") }
    }

    /** A fixture recorded as the app records it, read back and replayed, reaches what the fixture itself reaches. */
    @Test
    fun replayIsExact() {
        val frames = VideoFixtures.load(VideoFixtures.STRAIGHT)
        val t0 = 1_234_567L
        val direct = VideoScan()
        val directStates = ArrayList<Int>()
        var directFinished = false
        val recorded = VideoScan()
        val r = recorder(keep = Long.MAX_VALUE)
        for ((i, f) in frames.withIndex()) {
            val at = t0 + i * 100L
            val s = direct.onFrame(f.faces, at)
            directStates += s.recognised
            recorded.onFrame(r.picture(at, f.faces, 360, 640), at)
            if (s.finished) {
                directFinished = true
                break
            }
        }
        val end = if (directFinished) ScanRecording.End.Finished(recorded.outcome().editor.encode()) else ScanRecording.End.Left
        val replay = VideoFixtures.replay(ScanRecording.read(r.text(end)))
        assertEquals(directStates, replay.states.map { it.recognised })
        assertEquals(directFinished, replay.finished)
        assertEquals(direct.outcome().editor.encode(), replay.scan.outcome().editor.encode())
        if (directFinished) assertEquals(end, ScanRecording.End.Finished(replay.scan.outcome().editor.encode()))
    }
}
