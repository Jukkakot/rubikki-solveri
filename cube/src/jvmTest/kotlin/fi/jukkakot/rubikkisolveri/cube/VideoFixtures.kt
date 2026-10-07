package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

/**
 * The face readings of the test videos of 2026-10-05 and 2026-10-07, one line per frame (written by
 * [VideoScanHarness.writeFixtures]): `<frame>` then per full face found
 * ` | cx cy ux uy vx vy rrggbb,…` (centre, steps, nine colours in reading order, `-` for a sticker not found).
 */
object VideoFixtures {
    /** URFDLB colours of the cube in both videos (the scan right after them). */
    const val TRUTH = "YWRBWWGYRWGYWRGBOWWRBGGBWBYGROOYYRGBOYROORGBOBRGYBWOOY"

    /** Free angles, fingers in view; many corner views. */
    const val ANGLED = "20261005_151828"

    /** Straight on, from a table; hardly any corner views. */
    const val STRAIGHT = "20261005_151903"

    /** URFDLB colours of the cube in the evening videos (the video scan in good light just before them, log 18:36:56Z). */
    const val EVENING_TRUTH = "BRGBWRGRBOGWGRBGYGWWYOGWBROYBYGYOOBYRWRGOWWYROYWYBOROB"

    /** Evening videos, frames to clear when all 54 had to be confirmed one by one (null: never, before `video-scan-light`): table by the window, dark room, dim ceiling light, another room. */
    val EVENING = mapOf("20261005_213729" to 120, "20261005_213817" to 121, "20261005_213850" to null, "20261005_213929" to null)

    /** URFDLB colours of the cube in the recordings of 2026-10-07 (360×640, 10 fps; the videos stay with the user). */
    const val TRUTH_1007 = "RRRRWWRWGWRGRRGGGGWGRWGGWWWOOOYYOBYOBBBOOBYOBYYYYBBYBO"

    /** 14 clean frames of the failing web scan's start: red, white on top, blue on the right in shadow (`scan-centre-clash`). */
    const val WEB_1007 = "20261007_web"

    /** Camera video: white, red and green faces; white reads bluish. */
    const val CAMERA_1007 = "20261007_132049"

    /** Camera video, 42 s: white on top with blue beside it, then every face; a blue centre named white in a few frames. */
    const val TOUR_1007 = "20261007_132721"

    data class Frame(val name: String, val faces: List<FaceReading>)

    fun line(name: String, faces: List<FaceReading>): String = buildString {
        append(name)
        for (f in faces) {
            append(" | ")
            append(listOf(f.centre.x, f.centre.y, f.u.x, f.u.y, f.v.x, f.v.y).joinToString(" ") { "%.1f".format(java.util.Locale.ROOT, it) })
            append(' ')
            append(f.colors.joinToString(",") { it?.toHex() ?: "-" })
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
                    FaceReading(tokens[6].split(",").map { if (it == "-") null else Rgb.fromHex(it) }, Point(n[0], n[1]), Point(n[2], n[3]), Point(n[4], n[5]))
                },
            )
        }
    }
}
