package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch

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

    /** Camera video, 9 s, brighter light, blue on top first: the orange centre is named red until the red face is seen at frame 78 (`scan-centre-naming`). */
    const val BLUE_FIRST_1007 = "20261007_152753"

    /** Screen recording of the web test of 2026-10-07 18:19, the camera part (360×600, 10 fps): the striped pattern cube, slow first scan then a quick one (`corner-scan-spike`). */
    const val STRIPED = "web_181940"
    const val STRIPED_TRUTH = "WWWWWWWWWBRGBRGBRGOGROGROGRYYYYYYYYYGOBGOBGOBRBORBORBO"

    data class Frame(val name: String, val faces: List<FaceReading>)

    /** [blue] mixed towards white until the default palette names it white, as the phone's camera saw a blue centre in shadow. */
    fun paleBlue(blue: Rgb): Rgb = (1..100).map { t ->
        Rgb(blue.r + (255 - blue.r) * t / 100, blue.g + (255 - blue.g) * t / 100, blue.b + (255 - blue.b) * t / 100)
    }.first { ColorClassifier.rankedCentre(it).first() == CubeColor.WHITE }

    /**
     * `scan-centre-naming`: [TOUR_1007] with the blue face's centre made pale throughout (named white
     * by the palette, as in the web test of 14:55), its frames whose only full face is the blue face
     * replayed for [frames] frames first: the scan starts with the blue face alone, the white face seen
     * only later.
     */
    fun blueFirst(frames: Int = 30): List<Frame> {
        val blueFace = TRUTH_1007.substring(45, 54).map { CubeColor.fromLetter(it) }
        fun isBlue(r: FaceReading): Boolean {
            if (!r.isFull) return false
            val names = r.colors.map { ColorClassifier.live(it!!) }
            return (0 until 4).any { k -> RotationSearch.turned(names, k).withIndex().count { (n, c) -> n != 4 && c == blueFace[n] } >= 6 }
        }
        val tour = load(TOUR_1007).map { f -> f.copy(faces = f.faces.map { r -> if (isBlue(r)) r.copy(colors = r.colors.toMutableList().also { it[4] = paleBlue(it[4]!!) }) else r }) }
        val blue = tour.filter { f -> f.faces.count { it.isFull } == 1 && f.faces.any { isBlueCentre(it) } }.map { f -> f.copy(faces = f.faces.filter { it.isFull }) }
        return List(frames) { blue[it % blue.size] } + tour
    }

    /** A full face whose centre [paleBlue] made pale (it now sits just on the white side of blue). */
    private fun isBlueCentre(r: FaceReading): Boolean = r.isFull && ColorClassifier.centreDistances(r.colors[4]!!).let { d -> d.getValue(CubeColor.BLUE) - d.getValue(CubeColor.WHITE) in 0.0..3.0 }

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
