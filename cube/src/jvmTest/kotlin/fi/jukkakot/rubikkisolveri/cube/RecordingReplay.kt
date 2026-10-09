package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/**
 * For a failed scan shared with the log (`scan-recording`): replays RECORDING=<name>
 * (`src/jvmTest/resources/recordings/<name>.txt`) with its real times and resets, printing the
 * timeline every TIMELINE_EVERY-th picture (TIMELINE_TRUTH=<URFDLB> as in [RulesTimeline]), then the
 * end the device recorded beside the replay's.
 */
class RecordingReplay {
    @Test
    fun print() {
        val name = System.getenv("RECORDING")
        assumeTrue("set RECORDING=<name> to run", name != null)
        val every = System.getenv("TIMELINE_EVERY")?.toInt() ?: 10
        val truth = System.getenv("TIMELINE_TRUTH")
        val recording = VideoFixtures.loadRecording(name!!)
        println("${recording.platform} ${recording.version} started ${recording.started}" + (recording.cut?.let { ", start cut at $it ms" } ?: ""))
        var i = 0
        val replay = VideoFixtures.replay(recording) { entry, scan, s ->
            if (entry is ScanRecording.Reset) println("${entry.ms} ms reset")
            if (entry is ScanRecording.Picture && s != null) {
                if (i % every == 0 || s.finished) ScanTimeline.print("${entry.ms} ms #$i", scan, entry.found.faces.size, s, truth)
                i++
            }
        }
        println("recorded end: ${recording.end?.let(ScanRecording::endLine) ?: "(none)"}")
        println("replay end: " + if (replay.finished) "finished ${replay.scan.outcome().editor.encode()}" else "not finished, known ${replay.scan.state.recognised}")
    }
}
