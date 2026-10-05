package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

/**
 * The face readings of the test videos of 2026-10-05, one line per frame (written by
 * [VideoScanHarness.writeFixtures]): `<frame>` then per full face found
 * ` | cx cy ux uy vx vy rrggbb,…` (centre, steps, nine colours in reading order).
 */
object VideoFixtures {
    /** URFDLB colours of the cube in both videos (the scan right after them). */
    const val TRUTH = "YWRBWWGYRWGYWRGBOWWRBGGBWBYGROOYYRGBOYROORGBOBRGYBWOOY"

    /** Free angles, fingers in view; many corner views. */
    const val ANGLED = "20261005_151828"

    /** Straight on, from a table; hardly any corner views. */
    const val STRAIGHT = "20261005_151903"

    data class Frame(val name: String, val faces: List<FaceReading>)

    fun line(name: String, faces: List<FaceReading>): String = buildString {
        append(name)
        for (f in faces) {
            append(" | ")
            append(listOf(f.centre.x, f.centre.y, f.u.x, f.u.y, f.v.x, f.v.y).joinToString(" ") { "%.1f".format(java.util.Locale.ROOT, it) })
            append(' ')
            append(f.colors.joinToString(",") { it.toHex() })
        }
    }

    fun load(video: String): List<Frame> {
        val text = VideoFixtures::class.java.getResource("/video/$video.txt")!!.readText()
        return text.lines().filter { it.isNotBlank() }.map { line ->
            val parts = line.split(" | ")
            Frame(
                parts[0],
                parts.drop(1).map { face ->
                    val tokens = face.trim().split(" ")
                    val n = tokens.take(6).map { it.toDouble() }
                    FaceReading(tokens[6].split(",").map { Rgb.fromHex(it) }, Point(n[0], n[1]), Point(n[2], n[3]), Point(n[4], n[5]))
                },
            )
        }
    }
}
