package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Hungarian
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEvent
import fi.jukkakot.rubikkisolveri.cube.scan.ScanSession
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** "True" camera readings of a stickerless cube, deliberately not equal to the default palette. */
private val TRUE_COLORS = mapOf(
    CubeColor.WHITE to Rgb(210, 215, 220),
    CubeColor.YELLOW to Rgb(235, 220, 60),
    CubeColor.GREEN to Rgb(20, 160, 95),
    CubeColor.BLUE to Rgb(35, 95, 205),
    CubeColor.RED to Rgb(205, 35, 50),
    CubeColor.ORANGE to Rgb(250, 120, 35),
)

/** A reading of [color] under a light that scales each channel, with noise. */
private fun reading(color: CubeColor, light: Triple<Double, Double, Double>, random: Random, noise: Int = 12): Rgb {
    val base = TRUE_COLORS.getValue(color)
    fun ch(v: Int, k: Double) = (v * k + random.nextInt(-noise, noise + 1)).toInt().coerceIn(0, 255)
    return Rgb(ch(base.r, light.first), ch(base.g, light.second), ch(base.b, light.third))
}

class ColorMathTest {
    @Test
    fun knownLabValues() {
        val white = Rgb(255, 255, 255).toLab()
        assertTrue(abs(white.l - 100) < 0.1 && abs(white.a) < 0.1 && abs(white.b) < 0.1, "$white")
        val black = Rgb(0, 0, 0).toLab()
        assertTrue(abs(black.l) < 0.1)
        val red = Rgb(255, 0, 0).toLab()
        assertTrue(abs(red.l - 53.24) < 0.2 && abs(red.a - 80.09) < 0.3 && abs(red.b - 67.20) < 0.3, "$red")
        assertEquals("ff8000", Rgb(255, 128, 0).toHex())
        assertEquals(Rgb(255, 128, 0), Rgb.fromHex("ff8000"))
    }

    @Test
    fun lightnessCountsHalf() {
        val a = Lab(50.0, 0.0, 0.0)
        assertEquals(10.0, a.distance(Lab(70.0, 0.0, 0.0)), 1e-9)
        assertEquals(20.0, a.distance(Lab(50.0, 20.0, 0.0)), 1e-9)
    }

    @Test
    fun hungarianFindsTheCheapestAssignment() {
        val cost = arrayOf(doubleArrayOf(4.0, 1.0, 3.0), doubleArrayOf(2.0, 0.0, 5.0), doubleArrayOf(3.0, 2.0, 2.0))
        val assignment = Hungarian.solve(cost)
        assertEquals(5.0, assignment.indices.sumOf { cost[it][assignment[it]] })
        assertEquals(setOf(0, 1, 2), assignment.toSet())
    }
}

class FrameSamplerTest {
    private val faceColors = listOf(
        Rgb(255, 0, 0), Rgb(0, 255, 0), Rgb(0, 0, 255),
        Rgb(255, 255, 0), Rgb(255, 255, 255), Rgb(0, 255, 255),
        Rgb(255, 0, 255), Rgb(128, 64, 0), Rgb(0, 0, 0),
    )

    /** The upright picture: grey background, the 3×3 face in the grid square. */
    private fun upright(u: Float, v: Float, w: Int, h: Int): Rgb {
        val side = FrameSampler.GRID_SIZE * minOf(w, h)
        val x = u * w - (w - side) / 2
        val y = v * h - (h - side) / 2
        if (x < 0 || y < 0 || x >= side || y >= side) return Rgb(90, 90, 90)
        return faceColors[(y / side * 3).toInt() * 3 + (x / side * 3).toInt()]
    }

    /** A sensor frame that, turned [rotation] degrees clockwise, shows the upright picture. */
    private fun sensorFrame(rotation: Int, uprightW: Int = 300, uprightH: Int = 400): RgbaFrame {
        val (w, h) = if (rotation % 180 == 0) uprightW to uprightH else uprightH to uprightW
        val bytes = ByteArray(w * h * 4)
        for (y in 0 until h) for (x in 0 until w) {
            val nx = (x + 0.5f) / w
            val ny = (y + 0.5f) / h
            val (u, v) = when (rotation) {
                0 -> nx to ny
                90 -> 1 - ny to nx
                180 -> 1 - nx to 1 - ny
                else -> ny to 1 - nx
            }
            val c = upright(u, v, uprightW, uprightH)
            val i = (y * w + x) * 4
            bytes[i] = c.r.toByte(); bytes[i + 1] = c.g.toByte(); bytes[i + 2] = c.b.toByte(); bytes[i + 3] = -1
        }
        return RgbaFrame(w, h, w * 4, bytes, rotation)
    }

    @Test
    fun readsTheSameNineColoursAtEveryRotation() {
        for (rotation in listOf(0, 90, 180, 270)) {
            assertEquals(faceColors, FrameSampler.sample(sensorFrame(rotation)), "rotation $rotation")
        }
    }

    @Test
    fun cropRectLimitsTheGrid() {
        // The same face drawn in the right half of a frame twice as wide, cropped to that half.
        val half = sensorFrame(0)
        val w = half.width * 2
        val bytes = ByteArray(w * half.height * 4)
        for (y in 0 until half.height) System.arraycopy(half.bytes, y * half.rowStride, bytes, y * w * 4 + half.rowStride, half.rowStride)
        val frame = RgbaFrame(w, half.height, w * 4, bytes, 0, cropLeft = half.width, cropRight = w)
        assertEquals(faceColors, FrameSampler.sample(frame))
    }
}

class ClassifierTest {
    private val warm = Triple(1.0, 0.82, 0.6)

    @Test
    fun liveReadingOfTheDefaultPalette() {
        val random = Random(1)
        for ((color, rgb) in ColorClassifier.DEFAULT_PALETTE) {
            assertEquals(color, ColorClassifier.live(rgb))
            repeat(20) { assertEquals(color, ColorClassifier.live(reading(color, Triple(1.0, 1.0, 1.0), random, 10))) }
        }
    }

    @Test
    fun differentLighting() {
        for (seed in 0 until 200) {
            val random = Random(seed)
            val cube = Cube.solved().apply(Scramble.random(25, random))
            val light = if (seed % 2 == 0) warm else Triple(0.85, 0.9, 1.0)
            // Each face is captured separately, so its brightness differs a little too.
            val faceGain = List(6) { 0.8 + random.nextDouble() * 0.3 }
            val samples = (0 until 54).map { i ->
                val g = faceGain[i / 9]
                reading(cube[i], Triple(light.first * g, light.second * g, light.third * g), random)
            }
            val result = ColorClassifier.classify(samples)
            assertEquals(cube.toList(), result.colors, "seed $seed")
        }
    }

    @Test
    fun doubtfulSticker() {
        val random = Random(5)
        val cube = Cube.solved().apply("R U F' L2 D B'")
        val samples = (0 until 54).map { reading(cube[it], Triple(1.0, 1.0, 1.0), random, 4) }.toMutableList()
        // F1 reads halfway between red and orange.
        val f1 = Stickers.index(Face.F, 1)
        val red = TRUE_COLORS.getValue(CubeColor.RED)
        val orange = TRUE_COLORS.getValue(CubeColor.ORANGE)
        samples[f1] = Rgb((red.r + orange.r) / 2, (red.g + orange.g) / 2, (red.b + orange.b) / 2)
        val result = ColorClassifier.classify(samples)
        assertTrue(f1 in result.uncertain(), "confidence ${result.confidence[f1]}")
        assertTrue(result.uncertain().size <= 3, "only the doubtful one (and its swap partner): ${result.uncertain()}")
    }
}

class ScanSessionTest {
    private val cube = Cube.solved().apply("R U R' F2 D' L B2")
    private val random = Random(3)

    /** The nine readings of [view] held the right way (with a little noise). */
    private fun face(view: FaceView, of: Cube = cube): List<Rgb> =
        (1..9).map { reading(of.colorAt(view.face, it), Triple(1.0, 1.0, 1.0), random, 6) }

    @Test
    fun firstFace() {
        val session = ScanSession()
        assertEquals(FaceView.FRONT, session.current)
        assertEquals(CubeColor.GREEN, session.current!!.centreColor())
        assertEquals(CubeColor.WHITE, session.current!!.topColor())
    }

    @Test
    fun wrongFace() {
        val session = ScanSession()
        repeat(10) {
            assertEquals(ScanEvent.WrongFace(CubeColor.GREEN, CubeColor.RED), session.onFrame(face(FaceView.RIGHT)))
        }
        assertEquals(FaceView.FRONT, session.current)
    }

    @Test
    fun heldStill() {
        val session = ScanSession(stableFrames = 6)
        val front = face(FaceView.FRONT)
        repeat(5) { assertEquals(ScanEvent.Waiting, session.onFrame(front)) }
        assertEquals(ScanEvent.Captured(FaceView.FRONT), session.onFrame(front))
        assertEquals(FaceView.RIGHT, session.current)
        // Still showing the front: wrong face now, nothing captured twice.
        assertIs<ScanEvent.WrongFace>(session.onFrame(front))
    }

    @Test
    fun redo() {
        val session = ScanSession(stableFrames = 1)
        session.onFrame(face(FaceView.FRONT))
        session.onFrame(face(FaceView.RIGHT))
        assertEquals(FaceView.BACK, session.current)
        session.redo()
        assertEquals(FaceView.RIGHT, session.current)
        assertEquals(null, session.capturedSamples(FaceView.RIGHT))
    }

    private fun scanAll(of: Cube): ScanSession {
        val session = ScanSession(stableFrames = 2)
        for (view in FaceView.entries) {
            val samples = face(view, of)
            session.onFrame(samples)
            assertEquals(ScanEvent.Captured(view), session.onFrame(samples))
        }
        assertTrue(session.isDone)
        return session
    }

    @Test
    fun confidentScan() {
        val outcome = scanAll(cube).outcome()
        assertEquals(cube, outcome.editor.toCube())
        assertTrue(outcome.isConfident)
    }

    @Test
    fun unsureScan() {
        // A cube with a flipped edge scans fine colour-wise but is invalid: not confident.
        val uf = Edge.UF.stickers
        val flipped = cube.with(uf[0], cube[uf[1]]).with(uf[1], cube[uf[0]])
        val outcome = scanAll(flipped).outcome()
        assertEquals(Validity.FlippedEdge, outcome.validity)
        assertFalse(outcome.isConfident)
    }

    @Test
    fun captureButtonTakesTheLatestFrame() {
        val session = ScanSession()
        session.onFrame(face(FaceView.FRONT))
        assertEquals(ScanEvent.Captured(FaceView.FRONT), session.captureNow())
    }
}
