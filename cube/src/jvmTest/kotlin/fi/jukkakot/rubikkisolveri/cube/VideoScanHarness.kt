package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceLattice
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FinderResult
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.ScanSession
import org.junit.Assume.assumeTrue
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * `video-scan-spike`: runs [FaceFinder] on the frames of the test videos of 2026-10-05 and prints
 * the numbers for `findings.md`. The frames are the committed JPEG stills (`testdata/video/2026-10-05/stills/`,
 * extracted with ffmpeg at 10 fps, 360 px wide); it runs only with the environment variable
 * VIDEO_HARNESS=1 (about 20 s). The table also goes to `frames/report.txt`, and
 * what was found is drawn into `frames/overlay/<video>/`.
 */
class VideoScanHarness {
    /** Reports, overlays and sweep files (git-ignored). */
    private val root = File("../testdata/video/2026-10-05/frames").apply { mkdirs() }

    /** The videos' frames, 10 fps, 360 px wide, as JPEG (quality 90, no chroma subsampling), committed so any checkout can regenerate the fixtures. */
    private val stills = File("../testdata/video/2026-10-05/stills")
    private val videos = listOf("20261005_151828" to "free angles, fingers", "20261005_151903" to "straight on, table")

    /** The evening videos of 2026-10-05 (light: table by the window, dark room, dim ceiling light, another room); fixtures only. */
    private val evening = listOf("20261005_213729", "20261005_213817", "20261005_213850", "20261005_213929")

    /** Camera videos of 2026-10-07 whose stills are committed (360×640, 10 fps); fixtures only. */
    private val stills1007 = File("../testdata/video/2026-10-07/stills")
    private val later = listOf("20261007_152753", "web_181940", "20261007_202058", "20261007_202156", "20261007_202318", "20261007_202403")
    private val stills1008 = File("../testdata/video/2026-10-08/stills")
    private val later1008 = listOf("web_084657")

    /** The scan right after the videos (scan-log.txt, 12:19:57Z), URFDLB. */
    private val truth = "YWRBWWGYRWGYWRGBOWWRBGGBWBYGROOYYRGBOYROORGBOBRGYBWOOY"
    private val trueFaces = truth.chunked(9).map { face -> face.map { CubeColor.fromLetter(it) } }

    private val out = StringBuilder()

    private fun println(line: String) {
        kotlin.io.println(line)
        out.appendLine(line)
    }

    private class Setting(val get: () -> Double, val set: (Double) -> Unit)

    private class Frame(val name: String, val image: BufferedImage, val result: FinderResult, val millis: Double)

    @Test
    fun printNumbers() {
        assumeTrue("set VIDEO_HARNESS=1 to run", System.getenv("VIDEO_HARNESS") == "1")
        assumeTrue("frames not extracted", videos.all { File(stills, it.first).isDirectory })
        println("video | frames | ≥1 face | faces/frame | 2 faces | 3 faces | partial (7–8) | stickers right | exact faces | faces seen | assembled | ms/frame")
        for ((video, note) in videos) {
            val frames = run(video)
            report(video, note, frames)
            overlay(video, frames)
        }
        File(root, "report.txt").writeText(out.toString())
    }

    /**
     * Threshold sweep for tuning (task 3.2): each line of `frames/sweep.txt` is a setting such as
     * `tolerance=0.3,stretch=0.45`; one summary line per setting goes to `frames/sweep-report.txt`.
     */
    @Test
    fun sweep() {
        val file = File(root, "sweep.txt")
        assumeTrue("no sweep.txt", file.isFile)
        val images = videos.associate { (video, _) ->
            video to File(stills, video).listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }.map { ImageIO.read(it) }
        }
        val settings = mapOf(
            "darkBelow" to Setting({ FaceFinder.darkBelow.toDouble() }, { FaceFinder.darkBelow = it.toInt() }),
            "joinWithin" to Setting({ FaceFinder.joinWithin.toDouble() }, { FaceFinder.joinWithin = it.toInt() }),
            "meanWithin" to Setting({ FaceFinder.meanWithin.toDouble() }, { FaceFinder.meanWithin = it.toInt() }),
            "minFill" to Setting({ FaceFinder.minFill }, { FaceFinder.minFill = it }),
            "maxAspect" to Setting({ FaceFinder.maxAspect }, { FaceFinder.maxAspect = it }),
            "tolerance" to Setting({ FaceFinder.tolerance }, { FaceFinder.tolerance = it }),
            "stretch" to Setting({ FaceFinder.stretch }, { FaceFinder.stretch = it }),
            "minSpan" to Setting({ FaceFinder.minSpan }, { FaceFinder.minSpan = it }),
            "compactWeight" to Setting({ FaceLattice.compactWeight }, { FaceLattice.compactWeight = it }),
        )
        val saved = settings.mapValues { it.value.get() }
        fun restore() = saved.forEach { (name, value) -> settings.getValue(name).set(value) }
        val report = StringBuilder()
        for (line in file.readLines().filter { it.isNotBlank() }) {
            for (setting in line.split(",").filter { it.isNotBlank() }) {
                val (name, value) = setting.trim().split("=")
                settings.getValue(name).set(value.toDouble())
            }
            val parts = images.map { (video, list) ->
                val results = list.map { find(it) }
                val faces = results.flatMap { it.faces }
                val named = name(faces)
                val matches = named.values.map { bestFace(it).second }
                val exact = matches.count { it == 9 }
                val spread = "9:$exact 8:${matches.count { it == 8 }} 7:${matches.count { it == 7 }} low:${matches.count { it <= 6 }}"
                "${video.takeLast(4)}: ≥1 ${results.count { it.faces.isNotEmpty() } * 100 / list.size}% exact $exact/${faces.size} ($spread) two ${results.count { it.faces.size >= 2 }} ${assemble(faces, named)}"
            }
            report.appendLine("$line | ${parts.joinToString(" | ")}")
            restore()
        }
        File(root, "sweep-report.txt").writeText(report.toString())
    }

    /**
     * `video-scan` task 1.1: the full and partial faces found in every frame, saved as the fixtures the
     * [VideoScanTest]s replay (`src/jvmTest/resources/video/<video>.txt`, see [VideoFixtures]).
     */
    @Test
    fun writeFixtures() {
        assumeTrue("set VIDEO_HARNESS=1 to run", System.getenv("VIDEO_HARNESS") == "1")
        assumeTrue("frames not extracted", videos.all { File(stills, it.first).isDirectory })
        val dir = File("src/jvmTest/resources/video").apply { mkdirs() }
        val sources = (videos.map { it.first } + evening).map { File(stills, it) } + later.map { File(stills1007, it) } + later1008.map { File(stills1008, it) }
        for (source in sources.filter { it.isDirectory }) {
            val video = source.name
            val files = source.listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }
            val text = files.joinToString("\n", postfix = "\n") { file ->
                VideoFixtures.line(file.name.removeSuffix(".jpg"), find(ImageIO.read(file)).let { it.faces + it.partial }.map { FaceReading.of(it) })
            }
            File(dir, "$video.txt").writeText(text)
        }
    }

    /** Every lattice considered in the frames named in `frames/debug.txt` (`<video>/<frame>.jpg`), without the face checks. */
    @Test
    fun debugFrames() {
        val file = File(root, "debug.txt")
        assumeTrue("no debug.txt", file.isFile)
        val saved = FaceFinder.minSpan
        FaceFinder.minSpan = 0.0
        val report = StringBuilder()
        for (name in file.readLines().filter { it.isNotBlank() }) {
            val image = ImageIO.read(File(stills, name.trim()))
            val result = find(image)
            mask(Frame(name, image, result, 0.0), File(root, "debug-" + name.replace("/", "-")))
            report.appendLine("$name blobs=${result.blobs.size}")
            for (b in result.blobs) report.appendLine("  blob %.0f,%.0f area=${b.area} aspect=%.2f".format(b.centre.x, b.centre.y, b.aspect))
            for (l in result.candidates.sortedByDescending { it.hits }) {
                val c = l.stickers[4]!!.centre
                report.appendLine("  lattice at %.0f,%.0f hits=${l.hits} span=%.2f likeness=%.2f quality=%.2f u=%.0f,%.0f v=%.0f,%.0f ".format(c.x, c.y, l.span, l.likeness, l.quality, l.u.x, l.u.y, l.v.x, l.v.y) + l.stickers.joinToString(" ") { s -> s?.let { "%.0f,%.0f".format(it.centre.x, it.centre.y) } ?: "-" })
            }
        }
        FaceFinder.minSpan = saved
        File(root, "debug-report.txt").writeText(report.toString())
    }

    private fun run(video: String): List<Frame> {
        val files = File(stills, video).listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }
        // Warm the JIT once so the timing is closer to steady state.
        files.take(5).forEach { find(ImageIO.read(it)) }
        return files.map { file ->
            val image = ImageIO.read(file)
            val start = System.nanoTime()
            val result = find(image)
            Frame(file.name, image, result, (System.nanoTime() - start) / 1e6)
        }
    }

    private fun find(image: BufferedImage): FinderResult {
        val argb = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        return FaceFinder.find(argb, image.width, image.height)
    }

    private fun report(video: String, note: String, frames: List<Frame>) {
        val faces = frames.flatMap { it.result.faces }
        val named = name(faces)
        var right = 0
        var exact = 0
        val seen = HashSet<Int>()
        for (colors in named.values) {
            val (face, matches) = bestFace(colors)
            right += matches
            if (matches == 9) {
                exact++
                seen += face
            }
        }
        val assembled = assemble(faces, named)
        val n = frames.size
        fun pct(k: Int, of: Int) = if (of == 0) "-" else "${k * 100 / of} %"
        println(
            listOf(
                "$video ($note)",
                "$n",
                pct(frames.count { it.result.faces.isNotEmpty() }, n),
                "%.2f".format(faces.size.toDouble() / n),
                "${frames.count { it.result.faces.size == 2 }}",
                "${frames.count { it.result.faces.size >= 3 }}",
                "${frames.sumOf { it.result.partial.size }} in ${frames.count { it.result.partial.isNotEmpty() }} frames",
                pct(right, faces.size * 9),
                "$exact / ${faces.size}",
                "${seen.size} (${seen.sorted().joinToString("") { Face.entries[it].name }})",
                assembled,
                "%.1f".format(frames.map { it.millis }.average()),
            ).joinToString(" | "),
        )
        for (f in frames) {
            val line = f.result.faces.joinToString("  ") { l ->
                val colors = named.getValue(l)
                val (face, matches) = bestFace(colors)
                "${colors.joinToString("") { it.letter.toString() }}~${Face.entries[face].name}$matches ${spans(l)}"
            }
            println("  ${f.name} blobs=${f.result.blobs.size} partial=${f.result.partial.size} $line")
        }
    }

    /**
     * Colour names for every sticker of every face found: the centres are named regardless of
     * brightness ([ColorClassifier.rankedCentre]); the mean of the centres named alike is then the
     * reference each sticker is named by (the cube's own colours, like the guided scan's live view).
     */
    private fun name(faces: List<FaceLattice>): Map<FaceLattice, List<CubeColor>> {
        val centreName = faces.associateWith { ColorClassifier.rankedCentre(it.colors[4]).first() }
        val known = centreName.entries.groupBy({ it.value }, { it.key.colors[4].toLab() })
        val refs = ColorClassifier.references(known)
        return faces.associateWith { l -> l.colors.mapIndexed { i, rgb -> if (i == 4) centreName.getValue(l) else ColorClassifier.live(rgb, refs) } }
    }

    /** The lattice measures [FaceFinder] judges a face by, for tuning. */
    private fun spans(l: FaceLattice): String {
        return "s%.2f l%.2f".format(l.span, l.likeness)
    }

    /** The true face (index, URFDLB) [colors] fit best in any rotation, and how many stickers match. */
    private fun bestFace(colors: List<CubeColor>): Pair<Int, Int> =
        trueFaces.indices.flatMap { f -> (0 until 4).map { k -> f to RotationSearch.turned(colors, k).indices.count { RotationSearch.turned(colors, k)[it] == trueFaces[f][it] } } }
            .maxBy { it.second }

    /**
     * The cube from the faces found: per centre colour, the reading most faces agree on (in any
     * rotation) goes into the guided scan's end ([ScanSession.outcome]), as if captured there.
     */
    private fun assemble(faces: List<FaceLattice>, named: Map<FaceLattice, List<CubeColor>>): String {
        val byCentre = faces.groupBy { named.getValue(it)[4] }
        if (byCentre.size < 6) return "no (${byCentre.size}/6 centres)"
        val session = ScanSession(holdMillis = 0, minFrames = 1)
        for ((color, group) in byCentre) {
            fun key(l: FaceLattice) = (0 until 4).minOf { k -> RotationSearch.turned(named.getValue(l), k).joinToString("") { it.letter.toString() } }
            val common = group.groupBy { key(it) }.maxBy { it.value.size }.value
            val reading = common.minBy { it.error }
            val view = FaceView.entries.first { it.centreColor() == color }
            session.onFrame(reading.colors, 0)
            session.captureNow()
            session.choose(view)
            session.accept()
        }
        val outcome = session.outcome()
        val cube = outcome.editor.encode()
        return if (cube == truth) "yes" else "no (${cube.indices.count { cube[it] != truth[it] }} stickers off, valid=${outcome.validity.isValid})"
    }

    /** Why each pixel's blob was kept or not: black dark, grey too big, blue too small, red wrong shape, green a sticker. */
    private fun mask(f: Frame, file: File) {
        val w = f.image.width
        val h = f.image.height
        val debug = IntArray(w * h)
        FaceFinder.blobs(f.image.getRGB(0, 0, w, h, null, 0, w), w, h, debug)
        val palette = intArrayOf(0x000000, 0x808080, 0x3060ff, 0xff3030, 0x30d040)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        out.setRGB(0, 0, w, h, IntArray(w * h) { palette[debug[it]] }, 0, w)
        ImageIO.write(out, "png", file)
    }

    private fun overlay(video: String, frames: List<Frame>) {
        val dir = File(root, "overlay/$video").apply { mkdirs() }
        for ((index, f) in frames.withIndex()) {
            if (index % 10 == 9) mask(f, File(dir, f.name.replace(".jpg", "-mask.png")))
            val g = f.image.createGraphics()
            g.stroke = BasicStroke(1f)
            g.color = Color.MAGENTA
            for (b in f.result.blobs) g.drawOval(b.centre.x.toInt() - 2, b.centre.y.toInt() - 2, 4, 4)
            fun draw(l: FaceLattice, color: Color) {
                g.color = color
                g.stroke = BasicStroke(2f)
                val pts = l.stickers.map { it?.centre }
                for (row in 0 until 3) for (col in 0 until 2) {
                    val a = pts[row * 3 + col] ?: continue
                    val b = pts[row * 3 + col + 1] ?: continue
                    g.drawLine(a.x.toInt(), a.y.toInt(), b.x.toInt(), b.y.toInt())
                }
                for (col in 0 until 3) for (row in 0 until 2) {
                    val a = pts[row * 3 + col] ?: continue
                    val b = pts[(row + 1) * 3 + col] ?: continue
                    g.drawLine(a.x.toInt(), a.y.toInt(), b.x.toInt(), b.y.toInt())
                }
                pts[0]?.let { g.fillOval(it.x.toInt() - 4, it.y.toInt() - 4, 8, 8) }
            }
            f.result.faces.forEach { draw(it, Color.CYAN) }
            f.result.partial.forEach { draw(it, Color.ORANGE) }
            g.dispose()
            ImageIO.write(f.image, "png", File(dir, f.name.replace(".jpg", ".png")))
        }
    }
}
