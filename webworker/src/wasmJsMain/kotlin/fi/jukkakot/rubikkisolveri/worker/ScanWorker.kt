package fi.jukkakot.rubikkisolveri.worker

import fi.jukkakot.rubikkisolveri.cube.scan.FaceCodec
import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
import fi.jukkakot.rubikkisolveri.cube.scan.ScanStateCodec
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.khronos.webgl.toByteArray

/**
 * The scan worker: each picture's faces ([FaceFinder], full and partial) back to the page as numbers
 * ([FaceCodec]), and the video scan itself run on them here, off the page's thread (`scan-speed-up-2`
 * design 5): its state goes back as text ([ScanStateCodec]). Commands: `reset:<engine>` starts the
 * scan again, `outcome` answers the scan's outcome.
 */
fun main() {
    var scan = VideoScan()
    var resets = 0
    var scanned = ""
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
            val state = scan.onFrame(faces, found.toLong())
            scanned = listOf(ScanStateCodec.encode(state), scan.centreLog, (now() - found).toString(), resets.toString()).joinToString(SEPARATOR)
            FaceCodec.encode(FaceCodec.Found(faces, width, height, (found - start).toLong()))
        },
        scanned = { scanned },
        command = { text ->
            when {
                text.startsWith("reset:") -> {
                    scan = VideoScan(engine = ScanEngine.entries.first { it.name == text.removePrefix("reset:") })
                    resets++
                    resets.toString()
                }
                text == "outcome" -> ScanStateCodec.encodeOutcome(scan.outcome())
                else -> ""
            }
        },
    )
}

/** Between the parts of the scan's answer: state, centre line, scan ms, resets so far. */
const val SEPARATOR = "\u0001"
