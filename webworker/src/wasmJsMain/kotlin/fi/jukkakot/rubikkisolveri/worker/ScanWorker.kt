package fi.jukkakot.rubikkisolveri.worker

import fi.jukkakot.rubikkisolveri.cube.scan.FaceCodec
import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import org.khronos.webgl.toByteArray

/** The scan worker: each picture's faces ([FaceFinder], full and partial) back to the page as numbers ([FaceCodec]). */
fun main() {
    workerListen { width, height, rgba ->
        val start = now()
        val bytes = rgba.toByteArray()
        val argb = IntArray(width * height) { i ->
            val k = i * 4
            (0xff shl 24) or ((bytes[k].toInt() and 0xff) shl 16) or ((bytes[k + 1].toInt() and 0xff) shl 8) or (bytes[k + 2].toInt() and 0xff)
        }
        val faces = FaceFinder.find(argb, width, height).let { it.faces + it.partial }.map(FaceReading::of)
        FaceCodec.encode(FaceCodec.Found(faces, width, height, (now() - start).toLong()))
    }
}
