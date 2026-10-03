package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.MisreadSearch
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.ScanCheck
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.cube.scan.Verdict
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Camera readings of a stickerless cube (not the default palette). */
private val CAMERA = mapOf(
    CubeColor.WHITE to Rgb(210, 215, 220),
    CubeColor.YELLOW to Rgb(235, 220, 60),
    CubeColor.GREEN to Rgb(20, 160, 95),
    CubeColor.BLUE to Rgb(35, 95, 205),
    CubeColor.RED to Rgb(205, 35, 50),
    CubeColor.ORANGE to Rgb(250, 120, 35),
)

private fun read(color: CubeColor, random: Random, gain: Double = 1.0, noise: Int = 5): Rgb {
    val c = CAMERA.getValue(color)
    fun ch(v: Int) = (v * gain + random.nextInt(-noise, noise + 1)).toInt().coerceIn(0, 255)
    return Rgb(ch(c.r), ch(c.g), ch(c.b))
}

private fun readings(cube: Cube, random: Random) = (0 until 54).map { read(cube[it], random) }

private fun swapped(cube: Cube, a: Int, b: Int) = cube.with(a, cube[b]).with(b, cube[a])

/** A top and a right sticker of [cube] (non-centre) with different colours, red and orange where possible. */
private fun orangeOnTopRedOnRight(cube: Cube): Pair<Int, Int> {
    fun of(face: Face) = (1..9).map { Stickers.index(face, it) }.filter { it % 9 != 4 }
    val pairs = of(Face.U).flatMap { a -> of(Face.R).map { b -> a to b } }
        .filter { (a, b) -> cube[a] != cube[b] && !CubeCheck.validity(cube.with(a, cube[b]).with(b, cube[a])).isValid }
    return pairs.firstOrNull { (a, b) -> cube[a] == CubeColor.ORANGE && cube[b] == CubeColor.RED } ?: pairs.first()
}

class MisreadSearchTest {
    private val cube = Cube.solved().apply("R U F' L2 D B' R2 U'")

    @Test
    fun aSwapReadWrongIsFoundFirst() {
        val (a, b) = orangeOnTopRedOnRight(cube)
        // The camera saw the true colours; the classification put them the wrong way round.
        val random = Random(2)
        val seen = readings(cube, random)
        val wrong = swapped(cube, a, b)
        assertTrue(!CubeCheck.validity(wrong).isValid)
        val best = MisreadSearch.swaps(wrong, seen).first()
        assertEquals(setOf(a, b), setOf(best.a, best.b))
    }

    @Test
    fun withoutReadingsEverySwapIsStillValid() {
        val (a, b) = orangeOnTopRedOnRight(cube)
        val swaps = MisreadSearch.swaps(swapped(cube, a, b))
        assertTrue(swaps.any { setOf(it.a, it.b) == setOf(a, b) })
        swaps.forEach { assertTrue(CubeCheck.validity(swapped(swapped(cube, a, b), it.a, it.b)).isValid) }
    }

    @Test
    fun aTwistedCornerHasNoSwap() {
        val urf = Corner.entries.first().stickers
        val twisted = cube.with(urf[0], cube[urf[1]]).with(urf[1], cube[urf[2]]).with(urf[2], cube[urf[0]])
        assertEquals(Validity.TwistedCorner, CubeCheck.validity(twisted))
        assertEquals(emptyList(), MisreadSearch.swaps(twisted))
    }
}

class ClassifyFaceTest {
    private val cube = Cube.solved().apply("F R' U2 B L D'")

    @Test
    fun aRescanUnderOtherExposureReadsBack() {
        val random = Random(4)
        val seen = readings(cube, random)
        for (face in Face.entries) {
            // The rescan is 25 % darker overall.
            val rescan = (1..9).map { read(cube.colorAt(face, it), random, gain = 0.75) }
            val result = ColorClassifier.classifyFace(face, rescan, cube.toList(), seen)
            assertEquals((1..9).map { cube.colorAt(face, it) }, result.colors, "$face")
        }
    }

    @Test
    fun aDoubtfulStickerIsUncertain() {
        val random = Random(6)
        val seen = readings(cube, random)
        val rescan = (1..9).map { read(cube.colorAt(Face.F, it), random) }.toMutableList()
        val red = CAMERA.getValue(CubeColor.RED)
        val orange = CAMERA.getValue(CubeColor.ORANGE)
        rescan[0] = Rgb((red.r + orange.r) / 2, (red.g + orange.g) / 2, (red.b + orange.b) / 2)
        val result = ColorClassifier.classifyFace(Face.F, rescan, cube.toList(), seen)
        assertTrue(0 in result.uncertain(), "confidence ${result.confidence[0]}")
    }
}

class ScanCheckTest {
    private val cube = Cube.solved().apply("R U F' L2 D B' R2 U'")
    private val random = Random(8)
    private val seen = readings(cube, random)

    private fun check(colors: Cube, marks: Set<Int> = emptySet()) =
        ScanCheck.start(ScanOutcome(CubeEditor.of(colors), marks, CubeCheck.validity(colors), seen))

    @Test
    fun onlyDoubtfulFacesToWalk() {
        val right = Stickers.index(Face.R, 1)
        val top = Stickers.index(Face.U, 3)
        val check = check(cube, setOf(right, top))
        assertEquals(listOf(FaceView.RIGHT, FaceView.TOP), check.unchecked)
        assertEquals(FaceView.RIGHT, check.nextUnchecked())
    }

    @Test
    fun looksRightMovesOn() {
        val check = check(cube, setOf(Stickers.index(Face.R, 1), Stickers.index(Face.U, 3)))
            .lookRight(FaceView.RIGHT)
        assertEquals(FaceView.TOP, check.nextUnchecked(FaceView.RIGHT))
        assertEquals(null, check.lookRight(FaceView.TOP).nextUnchecked())
    }

    @Test
    fun paintingKeepsTheFaceCheckedAndClearsItsMarks() {
        val r1 = Stickers.index(Face.R, 1)
        val check = check(cube, setOf(r1, Stickers.index(Face.U, 3))).lookRight(FaceView.RIGHT).paint(r1, cube[r1])
        assertTrue(FaceView.RIGHT in check.checked)
        assertEquals(setOf(Stickers.index(Face.U, 3)), check.marks)
    }

    @Test
    fun solvableVerdict() {
        assertEquals(Verdict.Solvable(cube), check(cube).verdict())
    }

    @Test
    fun twoStickersSwappedByTheReading() {
        val (a, b) = orangeOnTopRedOnRight(cube)
        // Every face has been found right before the verdict.
        var check = FaceView.entries.fold(check(swapped(cube, a, b))) { c, v -> c.lookRight(v) }
        val verdict = assertIs<Verdict.Impossible>(check.verdict())
        assertEquals(listOf(FaceView.RIGHT, FaceView.TOP), verdict.faces)
        assertEquals(setOf(a, b), verdict.marked)
        check = check.apply(verdict)
        assertEquals(FaceView.RIGHT, check.nextUnchecked())
        assertTrue(a in check.marks && b in check.marks)
    }

    @Test
    fun noSingleSwapNamesTheProblemFaces() {
        // A corner piece that cannot exist (two white stickers) on the front, top and left faces.
        val ufl = Corner.entries.first { c -> c.faces.toSet() == setOf(Face.U, Face.F, Face.L) }.stickers
        val other = ufl.first { cube[it] != CubeColor.WHITE && cube[it] != CubeColor.YELLOW }
        val withWhite = (0 until 54).first { it % 9 != 4 && it !in ufl && cube[it] == CubeColor.WHITE }
        // A white from elsewhere swapped into the corner, and a second misread on the bottom face:
        // no single swap mends both.
        val d = (1..9).map { Stickers.index(Face.D, it) }.filter { it % 9 != 4 && it != withWhite && it !in ufl }
        val d1 = d.first()
        val d2 = d.first { cube[it] != cube[d1] && it != withWhite }
        val broken = swapped(swapped(cube, other, withWhite), d1, d2)
        assertEquals(emptyList(), MisreadSearch.swaps(broken, seen))
        val verdict = assertIs<Verdict.Impossible>(check(broken).verdict())
        val concerned = CubeCheck.validity(broken).markedStickers
        assertEquals(FaceView.entries.filter { v -> concerned.any { it / 9 == v.face.ordinal } }, verdict.faces)
        assertEquals(concerned, verdict.marked)
    }

    @Test
    fun aTwistedCornerNamesTheLeastSureFaces() {
        val urf = Corner.entries.first().stickers
        val twisted = cube.with(urf[0], cube[urf[1]]).with(urf[1], cube[urf[2]]).with(urf[2], cube[urf[0]])
        val verdict = assertIs<Verdict.Impossible>(check(twisted).verdict())
        assertEquals(Validity.TwistedCorner, verdict.validity)
        assertEquals(2, verdict.faces.size)
    }

    @Test
    fun rescanReplacesOneFace() {
        val (a, b) = orangeOnTopRedOnRight(cube)
        val wrong = swapped(cube, a, b)
        val check = check(wrong).lookRight(FaceView.TOP).lookRight(FaceView.FRONT)
        val rescan = (1..9).map { read(cube.colorAt(Face.U, it), random, gain = 0.85) }
        val (next, rotation) = check.replaceFace(FaceView.TOP, rescan)
        assertEquals(0, rotation)
        assertEquals((1..9).map { cube.colorAt(Face.U, it) }, (0 until 9).map { next.editor[Stickers.index(Face.U, it + 1)] })
        assertTrue(FaceView.TOP !in next.checked)
        assertTrue(FaceView.FRONT in next.checked)
        // Other faces as before.
        assertEquals(wrong[b], next.editor[b])
    }

    @Test
    fun quarterTurnedRescanComesBackRight() {
        // Two top stickers misread the wrong way round; the top is rescanned turned a quarter.
        val top = (1..9).map { Stickers.index(Face.U, it) }.filter { it % 9 != 4 }
        val a = top.first()
        val b = top.first { cube[it] != cube[a] }
        val check = check(swapped(cube, a, b))
        val rescan = RotationSearch.turned((1..9).map { read(cube.colorAt(Face.U, it), random) }, 1)
        val (next, rotation) = check.replaceFace(FaceView.TOP, rescan)
        assertEquals(3, rotation)
        assertEquals(cube, next.editor.toCube())
        assertEquals(RotationSearch.turned(rescan, 3), next.readings!!.subList(Face.U.ordinal * 9, Face.U.ordinal * 9 + 9))
    }

    @Test
    fun encodeRoundTrip() {
        val check = check(cube, setOf(3, 20)).lookRight(FaceView.FRONT)
        val back = assertNotNull(ScanCheck.decode(check.encode(), seen))
        assertEquals(check, back)
    }
}
