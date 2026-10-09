package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FoundFace
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Pose
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.Quat
import fi.jukkakot.rubikkisolveri.ui.cube3d.V3
import fi.jukkakot.rubikkisolveri.ui.cube3d.holdFor
import fi.jukkakot.rubikkisolveri.ui.scan.BUZZ_MILLIS
import fi.jukkakot.rubikkisolveri.ui.scan.DEMO_IDLE_MILLIS
import fi.jukkakot.rubikkisolveri.ui.scan.FoundFaces
import fi.jukkakot.rubikkisolveri.ui.scan.HOLD_PICTURE_MILLIS
import fi.jukkakot.rubikkisolveri.ui.scan.holdPicture
import fi.jukkakot.rubikkisolveri.ui.scan.SideLook
import fi.jukkakot.rubikkisolveri.ui.scan.DEMO_COLORS
import fi.jukkakot.rubikkisolveri.ui.scan.VideoStatus
import fi.jukkakot.rubikkisolveri.ui.scan.sideLooks
import fi.jukkakot.rubikkisolveri.ui.scan.shouldBuzz
import fi.jukkakot.rubikkisolveri.ui.scan.turnDemo
import fi.jukkakot.rubikkisolveri.ui.scan.videoStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The scan's feedback (`scan-feedback`, `scan-side-balls`): the side row, the status line, the buzz and the turn demo. */
class ScanFeedbackTest {
    private val inView = VideoScanState.EMPTY.copy(found = listOf(FoundFace(FaceReading(List(9) { null }, Point(0.0, 0.0), Point(1.0, 0.0), Point(0.0, 1.0)), List(9) { null }, List(9) { false })))
    private val all = CubeColor.entries.toSet()

    @Test
    fun sideRowShowsSixFromTheStartDoneOnesDimmedAndTheNextMarked() {
        val start = sideLooks(VideoScanState.EMPTY)
        assertEquals(6, start.size)
        assertTrue(start.all { it.second == SideLook.OPEN }, "none dimmed at the start")
        val s = inView.copy(doneSides = setOf(Face.F), nextSide = Face.B)
        val looks = sideLooks(s)
        assertEquals(start.map { it.first }, looks.map { it.first }, "every side keeps its place")
        assertEquals(SideLook.DONE, looks.toMap()[Face.F])
        assertEquals(SideLook.NEXT, looks.toMap()[Face.B])
        assertEquals(SideLook.OPEN, looks.toMap()[Face.U])
        assertTrue(sideLooks(s.copy(complete = true)).all { it.second == SideLook.DONE })
        assertTrue(sideLooks(s, done = true).all { it.second == SideLook.DONE })
    }

    @Test
    fun theLineNamesTheNextSideByItsColour() {
        assertEquals(VideoStatus.ShowSide(CubeColor.BLUE), videoStatus(inView.copy(nextSide = Face.B)))
        assertEquals(VideoStatus.ShowSide(CubeColor.ORANGE), videoStatus(inView.copy(readSides = all, undecided = true, nextSide = Face.L)), "also once every side is read")
        assertEquals(VideoStatus.Line(Res.string.video_status_done), videoStatus(inView.copy(nextSide = Face.B, complete = true)))
        assertEquals(VideoStatus.Line(Res.string.video_status_find), videoStatus(VideoScanState.EMPTY.copy(nextSide = Face.U)))
    }

    @Test
    fun aNewSideBuzzesWithinTheSpacing() {
        val before = inView.copy(readSides = setOf(CubeColor.WHITE))
        val after = before.copy(readSides = setOf(CubeColor.WHITE, CubeColor.BLUE))
        assertTrue(shouldBuzz(before, after, 1_000, 0))
        assertFalse(shouldBuzz(before, after, 1_000, 1_000 - BUZZ_MILLIS + 1), "too soon after the last buzz")
        assertFalse(shouldBuzz(before, before, 1_000, 0), "nothing new")
        assertTrue(shouldBuzz(before, before.copy(newStickers = 2), 1_000, 0))
    }

    @Test
    fun aBrowserPictureWithoutAFaceWaitsBehindTheShownOneForAMoment() {
        val face = inView.found.single().reading
        val shown = FoundFaces(listOf(face), 360, 640, show = {})
        val empty = FoundFaces(emptyList(), 360, 640, show = {})
        assertTrue(holdPicture(empty, shown, 100), "held: the marks stay with their picture")
        assertFalse(holdPicture(empty, shown, HOLD_PICTURE_MILLIS), "not for longer than a moment")
        assertFalse(holdPicture(FoundFaces(listOf(face), 360, 640, show = {}), shown, 100), "a picture with a face shows at once")
        assertFalse(holdPicture(FoundFaces(emptyList(), 360, 640), shown, 100), "the live picture: nothing to hold")
        assertFalse(holdPicture(empty, empty, 100), "nothing with marks to hold")
    }

    private fun close(a: V3, b: V3) = kotlin.math.abs(a.x - b.x) + kotlin.math.abs(a.y - b.y) + kotlin.math.abs(a.z - b.z) < 1e-3f

    @Test
    fun theNextSideTurnsTowardsTheCameraFromHowTheCubeIsHeld() {
        val held = inView.copy(readSides = all - CubeColor.ORANGE, pose = Pose(Face.F, Face.U), nextSide = Face.L)
        assertNull(turnDemo(held, DEMO_IDLE_MILLIS - 1), "not before two seconds without progress")
        val demo = turnDemo(held, DEMO_IDLE_MILLIS)!!
        assertEquals(CubeColor.ORANGE, demo.side)
        assertTrue(demo.from.angleTo(holdFor(Pose(Face.F, Face.U))) < 1e-3f, "starts as held")
        // Orange is the left side (L) in the standard scheme: it ends facing the camera, by a quarter turn.
        assertTrue(close(demo.to.rotate(V3.of(Face.L.normal)), V3(0f, 0f, 1f)), "orange faces the camera")
        assertEquals((Math.PI / 2).toFloat(), demo.from.angleTo(demo.to), 1e-3f)
        assertEquals(DEMO_COLORS, demo.colors, "nothing known: grey with coloured centres")
        assertNull(turnDemo(held.copy(complete = true), DEMO_IDLE_MILLIS))
    }

    @Test
    fun noHoldStartsFaceOnAndNoNextSideTiltsToACornerView() {
        val noHold = turnDemo(inView.copy(nextSide = Face.B), DEMO_IDLE_MILLIS)!!
        assertEquals(CubeColor.BLUE, noHold.side)
        assertEquals(Quat.IDENTITY, noHold.from)
        assertTrue(close(noHold.to.rotate(V3.of(Face.B.normal)), V3(0f, 0f, 1f)), "blue faces the camera")
        val none = turnDemo(inView, DEMO_IDLE_MILLIS)!!
        assertNull(none.side)
        assertEquals(CubeScene.DEFAULT_VIEW, none.to)
    }

    @Test
    fun knownStickersInColourAndTheNeededOnesOnTheNextSideBlink() {
        val solved = Cube.solved()
        val blue = (0 until 9).map { Face.B.ordinal * 9 + it }
        // Every sticker known but two on the blue side; one more blue sticker known but not part of the clear cube.
        val stickers = List(Stickers.COUNT) { i -> solved[i].takeIf { i != blue[0] && i != blue[8] } }
        val clear = (0 until Stickers.COUNT).toSet() - blue[0] - blue[8] - blue[5]
        val s = inView.copy(readSides = all, pose = Pose(Face.F, Face.U), stickers = stickers, clear = clear, nextSide = Face.B)
        val demo = turnDemo(s, DEMO_IDLE_MILLIS)!!
        assertEquals(CubeColor.BLUE, demo.side)
        assertEquals(setOf(blue[0], blue[5], blue[8]), demo.needed, "the side's stickers still needed blink")
        assertEquals(CubeColor.WHITE, demo.colors[0], "known stickers in colour")
        assertNull(demo.colors[blue[0]], "unknown ones grey")
        assertTrue(turnDemo(s.copy(pose = Pose(Face.R, Face.U)), DEMO_IDLE_MILLIS + 100, demo) === demo, "the same loop while the side stays")
        val next = turnDemo(s.copy(nextSide = Face.R), DEMO_IDLE_MILLIS, demo)!!
        assertEquals(CubeColor.RED, next.side, "a new loop for a new side")
    }
}
