package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
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

/** The scan paint, run for both scanners ([RulesScanPaintTest] runs it with [ScanEngine.RULES]). */
open class ScanPaintTest {
    protected open val engine: ScanEngine = ScanEngine.LOOK

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
        val scan = VideoScan(engine = engine)
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
        val needed = state.copy(found = listOf(f.copy(recognised = listOf(false) + f.recognised.drop(1))) + state.found.drop(1))
        val paint = ScanPaint.of(needed)
        val tile = paint.tiles.single()
        assertTrue((tile.centre - Point(70.0, 170.0)).length < 1e-6, "$tile")
        assertEquals(17, paint.dots.size, "the needed sticker has no dot")
    }

    @Test
    fun aFaceFoundGetsADimOutline() {
        assertEquals(2, ScanPaint.of(cornerView()).found.size)
    }

    @Test
    fun aProjectedSideWithoutReadingsIsGreyWhereNeeded() {
        val state = cornerView()
        // The cube's pose known from before, no face in this picture and nothing known yet.
        val paint = ScanPaint.of(state.copy(found = emptyList(), stickers = List(54) { null }))
        assertEquals(9, paint.tiles.size, "the side facing the camera")
        assertTrue(paint.tiles.all { it.key < 54 })
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
