package fi.jukkakot.rubikkisolveri.cube.scan

/**
 * The faces found in one picture as a line of numbers, for the browser's scan worker to send back to
 * the page (`camera-exposure` design 8): the picture's width and height, the finder's time (ms), then
 * per face its centre, u and v (x and y each) and its nine colours as `0xRRGGBB` (−1 where none was
 * found).
 */
object FaceCodec {
    private const val PER_FACE = 15

    /** What the worker found: the faces in a [width]×[height] picture, in [finderMs]. */
    class Found(val faces: List<FaceReading>, val width: Int, val height: Int, val finderMs: Long)

    fun encode(found: Found): String {
        val numbers = ArrayList<String>(3 + found.faces.size * PER_FACE)
        numbers += found.width.toString()
        numbers += found.height.toString()
        numbers += found.finderMs.toString()
        for (f in found.faces) {
            for (p in listOf(f.centre, f.u, f.v)) {
                numbers += short(p.x)
                numbers += short(p.y)
            }
            for (c in f.colors) numbers += (c?.let { (it.r shl 16) or (it.g shl 8) or it.b } ?: -1).toString()
        }
        return numbers.joinToString(",")
    }

    fun decode(text: String): Found {
        val n = text.split(',')
        require(n.size >= 3 && (n.size - 3) % PER_FACE == 0) { "faces: ${n.size} numbers" }
        val faces = (0 until (n.size - 3) / PER_FACE).map { k ->
            val at = 3 + k * PER_FACE
            fun d(i: Int) = n[at + i].toDouble()
            FaceReading(
                (0 until 9).map { i ->
                    val c = n[at + 6 + i].toInt()
                    if (c < 0) null else Rgb((c shr 16) and 0xff, (c shr 8) and 0xff, c and 0xff)
                },
                Point(d(0), d(1)), Point(d(2), d(3)), Point(d(4), d(5)),
            )
        }
        return Found(faces, n[0].toInt(), n[1].toInt(), n[2].toLong())
    }

    /** A coordinate to a thousandth of a pixel (plenty for the marks and the scan). */
    private fun short(x: Double): String {
        val r = kotlin.math.round(x * 1000) / 1000
        return if (r == kotlin.math.floor(r)) r.toLong().toString() else r.toString()
    }
}
