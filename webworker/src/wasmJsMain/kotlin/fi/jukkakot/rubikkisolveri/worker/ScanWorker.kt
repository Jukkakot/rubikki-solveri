package fi.jukkakot.rubikkisolveri.worker

import fi.jukkakot.rubikkisolveri.cube.scan.FaceCodec
import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.ScanStateCodec
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import org.khronos.webgl.toByteArray

/**
 * The scan worker: each picture's faces ([FaceFinder], full and partial) back to the page as numbers
 * ([FaceCodec]), and the video scan itself run on them here, off the page's thread (`scan-speed-up-2`
 * design 5): its state goes back as text ([ScanStateCodec]). Commands: `reset` starts the
 * scan again, `outcome` answers the scan's outcome.
 *
 * The page may run two of them as a pipeline (`scan-speed-up-4`): `role:find` makes one only find faces, and
 * the other gets the faces as text ([scanFaces]) and only scans; `adopt:<resets>` starts its scan as
 * the finder's last reset left it.
 */
fun main() {
    var scan = VideoScan()
    var resets = 0
    var scanned = ""
    var scanning = true
    // Pictures the current scan has had: 1 tells the page a fresh scan began (`scan-recording`).
    var pictures = 0
    fun answer(state: VideoScanState, started: Double, at: Long) =
        listOf(ScanStateCodec.encode(state), scan.centreLog, (now() - started).toString(), resets.toString(), at.toString(), pictures.toString()).joinToString(SEPARATOR)
    // The scan gets the faces as the page receives and records them (the text, read back), so a replay is exact.
    fun scanOne(faces: List<FaceReading>, at: Long, started: Double): String {
        pictures++
        return answer(scan.onFrame(faces, at), started, at)
    }
    workerListen(
        find = { width, height, rgba ->
            val start = now()
            val bytes = rgba.toByteArray()
            val argb = IntArray(width * height) { i ->
                val k = i * 4
                (0xff shl 24) or ((bytes[k].toInt() and 0xff) shl 16) or ((bytes[k + 1].toInt() and 0xff) shl 8) or (bytes[k + 2].toInt() and 0xff)
            }
            val faces = FaceFinder.find(argb, width, height).let { it.faces + it.partial }.map(FaceReading::of)
            val found = now()
            val text = FaceCodec.encode(FaceCodec.Found(faces, width, height, (found - start).toLong()))
            scanned = if (scanning) scanOne(FaceCodec.decode(text).faces, found.toLong(), found) else ""
            text
        },
        scanned = { scanned },
        scanFaces = { text, at ->
            scanOne(FaceCodec.decode(text).faces, at.toLong(), now())
        },
        command = { text ->
            when {
                text == "reset" -> {
                    scan = VideoScan()
                    pictures = 0
                    resets++
                    resets.toString()
                }
                text.startsWith("adopt:") -> {
                    scan = VideoScan()
                    pictures = 0
                    resets = text.removePrefix("adopt:").toInt()
                    resets.toString()
                }
                text == "role:find" -> {
                    scanning = false
                    ""
                }
                text == "outcome" -> ScanStateCodec.encodeOutcome(scan.outcome())
                else -> ""
            }
        },
    )
}

/** Between the parts of the scan's answer: state, centre line, scan ms, resets so far, the picture's scan time, pictures in this scan. */
const val SEPARATOR = "\u0001"
