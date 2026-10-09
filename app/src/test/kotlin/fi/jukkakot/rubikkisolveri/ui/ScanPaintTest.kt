package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.ui.scan.Glide
import fi.jukkakot.rubikkisolveri.ui.scan.MotionFade
import fi.jukkakot.rubikkisolveri.ui.scan.PAINT_FADE_MILLIS
import fi.jukkakot.rubikkisolveri.ui.scan.PAINT_GONE_MILLIS
import fi.jukkakot.rubikkisolveri.ui.scan.ScanPaint
import fi.jukkakot.rubikkisolveri.ui.scan.paintAlpha
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The scan paint. */
class ScanPaintTest {
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun reading(face: Face, centre: Point) = FaceReading(
        (0 until 9).map { ColorClassifier.DEFAULT_PALETTE.getValue(cube[face.ordinal * 9 + it]) },
        centre, Point(30.0, 0.0), Point(0.0, 30.0),
    )

    /**
     * F with U above it, four times: both settle, a projection is built. The red and orange faces are
     * seen first elsewhere, so that red and orange stickers are known (the rules scanner holds them until then).
     */
    private fun cornerView(): VideoScanState {
        val scan = VideoScan()
        var s = VideoScanState.EMPTY
        var t = 0L
        for ((k, warm) in listOf(Face.R, Face.L).withIndex()) repeat(4) { scan.onFrame(listOf(reading(warm, Point(700.0 + 300 * k, 600.0))), t); t += 100 }
        repeat(4) { s = scan.onFrame(listOf(reading(Face.F, Point(100.0, 200.0)), reading(Face.U, Point(100.0, 110.0))), t); t += 100 }
        return s
    }

    @Test
    fun knownStickersGetADotAndNeededOnesAVeil() {
        val state = cornerView()
        val all = ScanPaint.of(state)
        assertTrue(all.tiles.isEmpty(), "the two faces found, every sticker known: no veil")
        assertEquals(18, all.dots.size, "a dot on every sticker of both faces")
        val topLeft = all.dots.single { (it.centre - Point(70.0, 170.0)).length < 1e-6 }
        assertEquals(cube[Face.F.ordinal * 9], topLeft.color, "F's first sticker in its colour")
        // F's first sticker no longer known: a veil on it, the top-left of the face at (100,200) one step up and left.
        val f = state.found.first()
        val needed = state.copy(found = listOf(f.copy(recognised = listOf(false) + f.recognised.drop(1), read = listOf(null) + f.read!!.drop(1))) + state.found.drop(1))
        val paint = ScanPaint.of(needed)
        val tile = paint.tiles.single()
        assertTrue((tile.centre - Point(70.0, 170.0)).length < 1e-6, "$tile")
        assertEquals(17, paint.dots.size, "the needed sticker has no dot")
        assertTrue(paint.dots.all { it.sure }, "known stickers: filled dots")
    }

    @Test
    fun aFaceReadButNotPlacedShowsRingsAndNoVeils() {
        val state = cornerView()
        val f = state.found.first()
        val open = state.copy(found = listOf(f.copy(recognised = List(9) { false }, known = List(9) { null })), projection = null)
        val paint = ScanPaint.of(open)
        assertTrue(paint.tiles.isEmpty(), "read stickers are not veiled")
        assertEquals(9, paint.dots.size)
        assertTrue(paint.dots.none { it.sure }, "read only: hollow rings")
    }

    @Test
    fun anOpenFaceGetsOnlyItsOwnMarks() {
        val state = cornerView()
        val f = state.found.first()
        // F found but its side not told yet (no centre name).
        val open = state.copy(found = listOf(f.copy(names = f.names.mapIndexed { n, c -> if (n == 4) null else c })))
        val paint = ScanPaint.of(open)
        assertEquals(9, paint.tiles.size + paint.dots.size, "the face's own nine marks")
    }

    @Test
    fun aKnownColourWinsOverADifferentReadOne() {
        val state = cornerView()
        val f = state.found.first()
        val other = CubeColor.entries.first { it != f.known[0] }
        val paint = ScanPaint.of(state.copy(found = listOf(f.copy(read = listOf(other) + f.read!!.drop(1))), projection = null))
        val dot = paint.dots.single { (it.centre - Point(70.0, 170.0)).length < 1e-6 }
        assertEquals(f.known[0], dot.color)
        assertTrue(dot.sure)
    }

    @Test
    fun aFaceReadSteadilyGetsADimOutlineAStrayLatticeNone() {
        val state = cornerView()
        assertEquals(2, ScanPaint.of(state).found.size)
        val stray = state.copy(found = listOf(state.found.first().copy(read = null)) + state.found.drop(1))
        assertEquals(1, ScanPaint.of(stray).found.size, "a lattice not read steadily gets no outline")
    }

    @Test
    fun aLatticeInOnePictureAloneGetsNoMarksAndAFollowedOneIsVeiled() {
        // scan-rules-only: a face found in one blurred picture of a quickly turned cube showed grey veils beside it.
        val scan = VideoScan()
        val once = scan.onFrame(listOf(reading(Face.U, Point(100.0, 100.0))), 0)
        assertFalse(once.found.single().followed)
        assertEquals(ScanPaint.EMPTY, ScanPaint.of(once), "found in one picture: nothing")
        val again = scan.onFrame(listOf(reading(Face.U, Point(102.0, 100.0))), 100)
        assertTrue(again.found.single().followed)
        assertEquals(9, ScanPaint.of(again).tiles.size, "followed from the picture before: veiled")
    }

    @Test
    fun noFaceFoundPaintsNothingEvenWithAProjection() {
        // scan-paint-found-only: the guessed sides floated beside the cube; only faces found are painted.
        val state = cornerView()
        assertTrue(state.projection != null)
        val paint = ScanPaint.of(state.copy(found = emptyList(), stickers = List(54) { null }))
        assertEquals(ScanPaint(emptyList(), emptyList()), paint)
    }

    @Test
    fun aConfirmedSideGetsAnOutlineAndATick() {
        val state = cornerView()
        val paint = ScanPaint.of(state.copy(confirmed = setOf(Face.F)))
        assertEquals(listOf(Point(55.0, 155.0), Point(145.0, 155.0), Point(145.0, 245.0), Point(55.0, 245.0)), paint.outlines.single())
        assertEquals(Point(100.0, 200.0), paint.ticks.single().centre)
        assertEquals(1, paint.found.size, "the other face found keeps its dim outline")
        val none = ScanPaint.of(state.copy(confirmed = emptySet()))
        assertTrue(none.outlines.isEmpty() && none.ticks.isEmpty())
    }

    @Test
    fun marksHideWhileTheCubeMovesAndComeBackAfterARest() {
        val fade = MotionFade()
        // One side 90 px wide; pictures every 100 ms.
        assertTrue(fade.step(Point(100.0, 100.0), 90.0, 0), "shown from the first picture")
        assertTrue(fade.step(Point(101.0, 100.0), 90.0, 100), "still: a pixel in a tenth of a second")
        assertTrue(fade.step(Point(105.0, 100.0), 90.0, 200), "a hand holding it: half a side a second")
        assertFalse(fade.step(Point(160.0, 100.0), 90.0, 300), "moving: two thirds of a side in a tenth of a second")
        assertFalse(fade.step(Point(160.0, 100.0), 90.0, 400), "rested only just")
        assertFalse(fade.step(Point(160.0, 100.0), 90.0, 600))
        assertTrue(fade.step(Point(160.0, 100.0), 90.0, 700), "back after the rest time")
        assertTrue(fade.step(null, 0.0, 800), "no centre known: as it was")
    }

    @Test
    fun tilesGlidePartWayAndSnapOnABigJump() {
        val glide = Glide(tauMillis = 60f)
        glide.step(mapOf(1 to Point(0.0, 0.0)), 16f) { 45.0 }
        val half = glide.step(mapOf(1 to Point(30.0, 0.0)), 16f) { 45.0 }.getValue(1)
        assertTrue(half.x > 0 && half.x < 30, "moved part of the way: $half")
        val jumped = glide.step(mapOf(1 to Point(200.0, 0.0)), 16f) { 45.0 }.getValue(1)
        assertEquals(Point(200.0, 0.0), jumped, "a jump of more than the snap distance is not slid")
    }

    @Test
    fun paintFadesWithTheProjectionsAge() {
        assertEquals(1f, paintAlpha(0))
        assertEquals(1f, paintAlpha(PAINT_FADE_MILLIS))
        assertEquals(0.5f, paintAlpha((PAINT_FADE_MILLIS + PAINT_GONE_MILLIS) / 2), 0.01f)
        assertEquals(0f, paintAlpha(PAINT_GONE_MILLIS))
    }
}
