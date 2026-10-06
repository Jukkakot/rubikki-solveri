package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.ui.scan.Glide
import fi.jukkakot.rubikkisolveri.ui.scan.PAINT_FADE_MILLIS
import fi.jukkakot.rubikkisolveri.ui.scan.PAINT_GONE_MILLIS
import fi.jukkakot.rubikkisolveri.ui.scan.ScanPaint
import fi.jukkakot.rubikkisolveri.ui.scan.paintAlpha
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanPaintTest {
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun reading(face: Face, centre: Point) = FaceReading(
        (0 until 9).map { ColorClassifier.DEFAULT_PALETTE.getValue(cube[face.ordinal * 9 + it]) },
        centre, Point(30.0, 0.0), Point(0.0, 30.0),
    )

    /** F with U above it, four times: both settle, a projection is built. */
    private fun cornerView(): VideoScanState {
        val scan = VideoScan()
        var s = VideoScanState.EMPTY
        repeat(4) { s = scan.onFrame(listOf(reading(Face.F, Point(100.0, 200.0)), reading(Face.U, Point(100.0, 110.0))), it * 100L) }
        return s
    }

    @Test
    fun foundFacesInTheirColoursAndNeededStickersGrey() {
        val paint = ScanPaint.of(cornerView())
        val solid = paint.tiles.filter { it.color != null }
        assertEquals(18, solid.size, "the two faces found, every sticker known")
        // A found face's tile lies on its sticker: the top-left of the face at (100,200) is one step up and left.
        assertTrue(solid.any { (it.centre - Point(70.0, 170.0)).length < 1e-6 })
    }

    @Test
    fun aProjectedSideWithoutReadingsIsGreyWhereNeeded() {
        val state = cornerView()
        // The cube's pose known from before, no face in this picture and nothing known yet.
        val paint = ScanPaint.of(state.copy(found = emptyList(), stickers = List(54) { null }))
        assertEquals(9, paint.tiles.size, "the side facing the camera")
        assertTrue(paint.tiles.all { it.color == null && it.key < 54 })
    }

    @Test
    fun aConfirmedSideGetsAnOutline() {
        val state = cornerView()
        val outlines = ScanPaint.of(state.copy(confirmed = setOf(Face.F))).outlines
        assertEquals(1, outlines.size)
        assertEquals(listOf(Point(55.0, 155.0), Point(145.0, 155.0), Point(145.0, 245.0), Point(55.0, 245.0)), outlines.single())
        assertTrue(ScanPaint.of(state.copy(confirmed = emptySet())).outlines.isEmpty())
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
