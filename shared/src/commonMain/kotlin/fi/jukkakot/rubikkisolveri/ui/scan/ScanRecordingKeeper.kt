package fi.jukkakot.rubikkisolveri.ui.scan

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecorder
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.log.NoScanRecordings
import fi.jukkakot.rubikkisolveri.log.ScanRecordingStore
import kotlin.time.Clock

/**
 * Where the video scan's recordings go (`scan-recording`): the [store], and the header's [platform]
 * (`android`, `web`) and app [version]; [now] gives the start time as an ISO text.
 */
class ScanRecordingSetup(
    val store: ScanRecordingStore = NoScanRecordings,
    val platform: String = "android",
    val version: String = "",
    val now: () -> String = { Clock.System.now().toString() },
)

/**
 * The current recording of one video scan screen: pictures go in as the scan gets them, and [end]
 * writes it (finished, left, restarted; also on going to the background, when it stays open and is
 * written again under the same name later). Runs on the screen's thread.
 */
internal class ScanRecordingKeeper(private val setup: ScanRecordingSetup) {
    private var recorder: ScanRecorder? = null

    private fun current(): ScanRecorder = recorder ?: ScanRecorder(setup.platform, setup.version, setup.now()).also { recorder = it }

    /** A picture the scan here gets at [at]: answers the faces as the recording keeps them, which the scan is then given. */
    fun picture(f: FoundFaces, at: Long): List<FaceReading> = current().picture(at, f.faces, f.width, f.height, f.finderMs)

    /**
     * A picture the browser's worker scanned ([scanned], at its own time): a fresh scan there (its
     * first picture after earlier ones) is recorded as a reset.
     */
    fun remote(f: FoundFaces, scanned: Scanned, now: Long) {
        val r = current()
        val at = if (scanned.at >= 0) scanned.at else now
        if (scanned.pictures == 1 && r.hasPicturesSinceReset) r.reset(at)
        r.picture(at, f.faces, f.width, f.height, f.finderMs)
    }

    /** Writes the recording, ending with [end]; [close] starts the next picture a new one. */
    fun end(end: ScanRecording.End, close: Boolean = true) {
        val r = recorder ?: return
        if (close) recorder = null
        if (r.isEmpty) return
        val name = ScanRecordingStore.nameOf(r.started)
        setup.store.write(name, r.text(end))
        val kind = when (end) {
            is ScanRecording.End.Finished -> "finished"
            ScanRecording.End.Left -> "left"
            ScanRecording.End.Restart -> "restart"
        }
        AppLog.info(Evt.SCAN_VIDEO, null, "kind" to "recording", "name" to name, "pictures" to r.pictures, "end" to kind)
    }
}
