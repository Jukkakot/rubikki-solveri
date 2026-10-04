package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Hungarian
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
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

    @Test
    fun pictureShowsTheGridUpright() {
        val size = 30
        for (rotation in listOf(0, 90, 180, 270)) {
            val picture = FrameSampler.picture(sensorFrame(rotation), size)
            assertEquals(size * size, picture.size)
            // The middle of each cell has that cell's colour.
            for (cell in 0 until 9) {
                val x = (cell % 3) * 10 + 5
                val y = (cell / 3) * 10 + 5
                val c = faceColors[cell]
                assertEquals((0xff shl 24) or (c.r shl 16) or (c.g shl 8) or c.b, picture[y * size + x], "rotation $rotation cell $cell")
            }
        }
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
    fun referencesFromTheCube() {
        val cubeRed = Rgb(225, 70, 30)
        assertEquals(CubeColor.ORANGE, ColorClassifier.live(cubeRed))
        val refs = ColorClassifier.references(mapOf(CubeColor.RED to listOf(cubeRed.toLab())))
        assertEquals(CubeColor.RED, ColorClassifier.live(cubeRed, refs))
        assertEquals(listOf(CubeColor.ORANGE, CubeColor.RED), ColorClassifier.ranked(cubeRed).take(2))
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

    /** The nine readings of [view] turned [turn] quarter turns (with a little noise). */
    private fun face(view: FaceView, of: Cube = cube, turn: Int = 0): List<Rgb> =
        RotationSearch.turned((1..9).map { reading(of.colorAt(view.face, it), Triple(1.0, 1.0, 1.0), random, 6) }, turn)

    /** Shows [samples] every 100 ms from [from] until [until] (exclusive); returns the last event. */
    private fun ScanSession.show(samples: List<Rgb>, from: Long, until: Long): ScanEvent {
        var event: ScanEvent = ScanEvent.Waiting
        for (t in from until until step 100) event = onFrame(samples, t)
        return event
    }

    /** Captures [view] (held for the hold time) and accepts it. */
    private fun ScanSession.scanFace(view: FaceView, of: Cube = cube, samples: List<Rgb> = face(view, of)) {
        capture(view, samples)
        accept()
    }

    private fun ScanSession.capture(view: FaceView, samples: List<Rgb>) {
        show(samples, 0, ScanSession.HOLD_MILLIS)
        assertEquals(ScanEvent.Captured(view), onFrame(samples, ScanSession.HOLD_MILLIS))
    }

    /** A cube whose red the default palette reads as orange. */
    private val reddish = ColorClassifier.DEFAULT_PALETTE + mapOf(CubeColor.RED to Rgb(225, 70, 30), CubeColor.ORANGE to Rgb(250, 150, 40))

    private fun reddishFace(view: FaceView, of: Cube) = (1..9).map { reddish.getValue(of.colorAt(view.face, it)) }

    @Test
    fun firstFace() {
        val session = ScanSession()
        assertEquals(FaceView.entries, session.remaining)
        assertEquals(0, session.index)
    }

    @Test
    fun anotherFaceFirst() {
        // The top face, turned a quarter, before any other: captured and recognised as the top.
        val session = ScanSession()
        val top = face(FaceView.TOP, turn = 1)
        assertEquals(ScanEvent.Holding(0f, FaceView.TOP), session.onFrame(top, 0))
        session.show(top, 100, 1_500)
        assertEquals(ScanEvent.Captured(FaceView.TOP), session.onFrame(top, 1_500))
        assertEquals(FaceView.TOP, session.reviewFace)
        session.accept()
        assertTrue(session.isAccepted(FaceView.TOP))
        assertEquals(5, session.remaining.size)
    }

    @Test
    fun heldStill() {
        val session = ScanSession()
        val front = face(FaceView.FRONT)
        assertEquals(ScanEvent.Holding(0f, FaceView.FRONT), session.onFrame(front, 0))
        assertEquals(ScanEvent.Holding(7 / 15f, FaceView.FRONT), session.show(front, 100, 800))
        assertEquals(ScanEvent.Holding(14 / 15f, FaceView.FRONT), session.show(front, 800, 1_500))
        assertEquals(ScanEvent.Captured(FaceView.FRONT), session.onFrame(front, 1_500))
        // Under review: nothing more is read, the front is not yet done.
        assertEquals(ScanEvent.Waiting, session.onFrame(front, 1_600))
        assertFalse(session.isAccepted(FaceView.FRONT))
        assertEquals(null, session.capturedSamples(FaceView.FRONT))
    }

    @Test
    fun stillTurning() {
        val session = ScanSession()
        val front = face(FaceView.FRONT)
        val other = CubeColor.entries.first { it != ColorClassifier.live(front[0]) && it != CubeColor.GREEN }
        val turning = listOf(reading(other, Triple(1.0, 1.0, 1.0), random, 6)) + front.drop(1)
        session.show(front, 0, 1_000)
        // One cell changes just before the hold time is up: the hold starts again.
        assertEquals(ScanEvent.Holding(0f, FaceView.FRONT), session.onFrame(turning, 1_000))
        assertIs<ScanEvent.Holding>(session.show(turning, 1_100, 2_500))
        assertEquals(ScanEvent.Captured(FaceView.FRONT), session.onFrame(turning, 2_500))
    }

    @Test
    fun changingTheRecognisedFace() {
        // Recognised as the left face; the user says it is the right face.
        val session = ScanSession()
        val left = face(FaceView.LEFT)
        session.capture(FaceView.LEFT, left)
        session.choose(FaceView.RIGHT)
        assertEquals(FaceView.RIGHT, session.reviewFace)
        session.accept()
        assertTrue(session.isAccepted(FaceView.RIGHT))
        assertFalse(session.isAccepted(FaceView.LEFT))
        // A face already done cannot be chosen.
        session.capture(FaceView.FRONT, face(FaceView.FRONT))
        session.choose(FaceView.RIGHT)
        assertEquals(FaceView.FRONT, session.reviewFace)
    }

    @Test
    fun alreadyScannedInAnyRotation() {
        val session = ScanSession()
        val front = face(FaceView.FRONT)
        session.scanFace(FaceView.FRONT, samples = front)
        // Still showing the front, now turned: nothing is captured twice, the user is asked to turn the cube.
        val turned = RotationSearch.turned(front, 1)
        repeat(30) { assertEquals(ScanEvent.AlreadyScanned, session.onFrame(turned, it * 100L)) }
        assertEquals(1, session.index)
    }

    @Test
    fun warmRed() {
        // Red that reads as the default orange: recognised as the left face, changed to the right one.
        val session = ScanSession()
        session.scanFace(FaceView.FRONT, samples = reddishFace(FaceView.FRONT, cube))
        val right = reddishFace(FaceView.RIGHT, cube)
        session.capture(FaceView.LEFT, right)
        session.choose(FaceView.RIGHT)
        session.accept()
        // The red centre is known now: this cube's red reads as red.
        assertEquals(ScanEvent.AlreadyScanned, session.onFrame(right, 5_000))
        assertEquals(CubeColor.RED, session.live!![4])
        for (view in listOf(FaceView.BACK, FaceView.LEFT, FaceView.TOP, FaceView.BOTTOM)) {
            session.scanFace(view, samples = reddishFace(view, cube))
        }
        assertEquals(cube, session.outcome().editor.toCube())
    }

    @Test
    fun warmRedLeftAsTheLeftFace() {
        // The user does not fix it: the red face is stored as the left one. The search undoes it.
        val session = ScanSession()
        session.scanFace(FaceView.FRONT, samples = reddishFace(FaceView.FRONT, cube))
        session.scanFace(FaceView.LEFT, samples = reddishFace(FaceView.RIGHT, cube))
        session.scanFace(FaceView.RIGHT, samples = reddishFace(FaceView.LEFT, cube))
        for (view in listOf(FaceView.BACK, FaceView.TOP, FaceView.BOTTOM)) {
            session.scanFace(view, samples = reddishFace(view, cube))
        }
        val outcome = session.outcome()
        assertEquals(cube, outcome.editor.toCube())
        assertEquals(Face.L, outcome.from[Face.R])
    }

    @Test
    fun scanAgain() {
        val session = ScanSession()
        val front = face(FaceView.FRONT)
        session.show(front, 0, 1_600)
        assertEquals(FaceView.FRONT, session.reviewFace)
        session.retake()
        assertEquals(null, session.review)
        assertEquals(null, session.capturedSamples(FaceView.FRONT))
        assertEquals(ScanEvent.Holding(0f, FaceView.FRONT), session.onFrame(front, 2_000))
    }

    @Test
    fun redo() {
        val session = ScanSession()
        session.scanFace(FaceView.FRONT)
        session.scanFace(FaceView.RIGHT)
        assertEquals(2, session.index)
        session.redo()
        assertEquals(1, session.index)
        assertEquals(null, session.capturedSamples(FaceView.RIGHT))
        assertTrue(session.isAccepted(FaceView.FRONT))
    }

    private fun scanAll(of: Cube, turns: Map<FaceView, Int> = emptyMap(), order: List<FaceView> = FaceView.entries): ScanSession {
        val session = ScanSession()
        for (view in order) session.scanFace(view, of, face(view, of, turns[view] ?: 0))
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
    fun facesTurnedInAnyOrder() {
        // The top turned a quarter, the back upside down, the faces in another order.
        val turns = mapOf(FaceView.TOP to 1, FaceView.BACK to 2)
        val order = listOf(FaceView.BOTTOM, FaceView.TOP, FaceView.LEFT, FaceView.BACK, FaceView.FRONT, FaceView.RIGHT)
        val outcome = scanAll(cube, turns, order).outcome()
        assertEquals(cube, outcome.editor.toCube())
        assertTrue(outcome.isConfident)
        // Seen readings turned k quarter turns clockwise give net order: k undoes the hold.
        assertEquals(3, outcome.rotations[Face.U])
        assertEquals(2, outcome.rotations[Face.B])
    }

    @Test
    fun unsureScan() {
        // A cube with a flipped edge scans fine colour-wise but is invalid: not confident.
        val uf = Edge.UF.stickers
        val flipped = cube.with(uf[0], cube[uf[1]]).with(uf[1], cube[uf[0]])
        val outcome = scanAll(flipped).outcome()
        assertFalse(outcome.validity.isValid)
        assertFalse(outcome.isConfident)
    }

    @Test
    fun outcomeKeepsTheReadingsInNetOrder() {
        val session = scanAll(cube, mapOf(FaceView.TOP to 1))
        val outcome = session.outcome()
        assertEquals(54, outcome.samples.size)
        assertEquals(
            RotationSearch.turned(session.capturedSamples(FaceView.TOP)!!, 3),
            outcome.samples.subList(Face.U.ordinal * 9, Face.U.ordinal * 9 + 9),
        )
    }

    @Test
    fun oneFaceOnly() {
        val session = ScanSession(only = FaceView.TOP)
        assertEquals(listOf(FaceView.TOP), session.remaining)
        val top = face(FaceView.TOP, turn = 2)
        session.capture(FaceView.TOP, top)
        session.retake()
        session.capture(FaceView.TOP, top)
        session.accept()
        assertTrue(session.isDone)
        assertEquals(top, session.capturedSamples(FaceView.TOP))
        // Nothing else is asked for, and redo does not step back.
        session.redo()
        assertTrue(session.isDone)
        assertEquals(ScanEvent.Waiting, session.onFrame(face(FaceView.BOTTOM), 9_000))
    }

    @Test
    fun oneFaceIsTheOnlyRecognisedFace() {
        // Whatever centre is shown, the one-face scan takes it as its face.
        val session = ScanSession(only = FaceView.RIGHT)
        assertEquals(ScanEvent.Holding(0f, FaceView.RIGHT), session.onFrame(face(FaceView.FRONT), 0))
    }

    @Test
    fun captureButtonTakesTheLatestFrame() {
        val session = ScanSession()
        session.onFrame(face(FaceView.BACK), 0)
        assertEquals(ScanEvent.Captured(FaceView.BACK), session.captureNow())
        assertEquals(FaceView.BACK, session.reviewFace)
    }

    @Test
    fun noCubeInTheGrid() {
        // Something that is not a cube, held still: never captured automatically.
        val session = ScanSession()
        val samples = List(9) { Rgb(150, 145, 138) }
        repeat(30) { assertEquals(ScanEvent.NoCube, session.onFrame(samples, it * 100L, looksLikeCube = false)) }
        assertEquals(null, session.review)
        // The capture button still takes it.
        assertIs<ScanEvent.Captured>(session.captureNow())
    }

    @Test
    fun aCubeAgainStartsTheHold() {
        val session = ScanSession()
        val front = face(FaceView.FRONT)
        session.onFrame(front, 0, looksLikeCube = false)
        assertEquals(ScanEvent.Holding(0f, FaceView.FRONT), session.onFrame(front, 100))
        session.show(front, 200, 1_600)
        assertEquals(ScanEvent.Captured(FaceView.FRONT), session.onFrame(front, 1_600))
    }
}
