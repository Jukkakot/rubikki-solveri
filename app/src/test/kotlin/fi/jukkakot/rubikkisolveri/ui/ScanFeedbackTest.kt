package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.Corner
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
import fi.jukkakot.rubikkisolveri.ui.scan.CornerLook
import fi.jukkakot.rubikkisolveri.ui.scan.DEMO_COLORS
import fi.jukkakot.rubikkisolveri.ui.scan.VideoStatus
import fi.jukkakot.rubikkisolveri.ui.scan.cornerLooks
import fi.jukkakot.rubikkisolveri.ui.scan.shouldBuzz
import fi.jukkakot.rubikkisolveri.ui.scan.turnDemo
import fi.jukkakot.rubikkisolveri.ui.scan.videoStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The scan's feedback (`scan-feedback`, `scan-next-view`): the corner row, the status line, the buzz and the turn demo. */
class ScanFeedbackTest {
    private val inView = VideoScanState.EMPTY.copy(found = listOf(FoundFace(FaceReading(List(9) { null }, Point(0.0, 0.0), Point(1.0, 0.0), Point(0.0, 1.0)), List(9) { null }, List(9) { false })))
    private val all = CubeColor.entries.toSet()

    @Test
    fun cornerRowShowsEightFromTheStartReadOnesDimmedAndTheNextMarked() {
        val start = cornerLooks(VideoScanState.EMPTY)
        assertEquals(8, start.size)
        assertTrue(start.all { it.second == CornerLook.OPEN }, "none dimmed at the start")
        val s = inView.copy(readCorners = setOf(Corner.UFL), nextCorner = Corner.UBR)
        val looks = cornerLooks(s)
        assertEquals(start.map { it.first }, looks.map { it.first }, "every corner keeps its place")
        assertEquals(CornerLook.READ, looks.toMap()[Corner.UFL])
        assertEquals(CornerLook.NEXT, looks.toMap()[Corner.UBR])
        assertEquals(CornerLook.OPEN, looks.toMap()[Corner.URF])
        assertTrue(cornerLooks(s.copy(complete = true)).all { it.second == CornerLook.READ })
        assertTrue(cornerLooks(s, done = true).all { it.second == CornerLook.READ })
    }

    @Test
    fun theLineCountsCornersOnceEverySideIsRead() {
        assertEquals(VideoStatus.Line(Res.string.video_status_grey), videoStatus(inView.copy(readSides = all - CubeColor.BLUE)))
        assertEquals(VideoStatus.CornersLeft(8), videoStatus(inView.copy(readSides = all)))
        assertEquals(VideoStatus.Line(Res.string.video_status_done), videoStatus(inView.copy(readSides = all, complete = true)))
        assertEquals(VideoStatus.Line(Res.string.video_status_find), videoStatus(VideoScanState.EMPTY.copy(readSides = all)))
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
    fun anUnreadSideTurnsTowardsTheCameraFromHowTheCubeIsHeld() {
        val held = inView.copy(readSides = all - CubeColor.ORANGE, pose = Pose(Face.F, Face.U))
        assertNull(turnDemo(held, DEMO_IDLE_MILLIS - 1), "not before two seconds without progress")
        val demo = turnDemo(held, DEMO_IDLE_MILLIS)!!
        assertEquals(CubeColor.ORANGE, demo.side)
        assertTrue(demo.from.angleTo(holdFor(Pose(Face.F, Face.U))) < 1e-3f, "starts as held")
        // Orange is the left side (L) in the standard scheme: it ends facing the camera, by a quarter turn.
        assertTrue(close(demo.to.rotate(V3.of(Face.L.normal)), V3(0f, 0f, 1f)), "orange faces the camera")
        assertEquals((Math.PI / 2).toFloat(), demo.from.angleTo(demo.to), 1e-3f)
        assertNull(turnDemo(held.copy(complete = true), DEMO_IDLE_MILLIS))
    }

    @Test
    fun everySideReadOrNoHoldTiltsToACornerView() {
        val read = turnDemo(inView.copy(readSides = all, pose = Pose(Face.F, Face.U)), DEMO_IDLE_MILLIS)!!
        assertNull(read.side)
        assertEquals(Quat.IDENTITY, read.from)
        assertEquals(CubeScene.DEFAULT_VIEW, read.to)
        val noHold = turnDemo(inView.copy(readSides = setOf(CubeColor.WHITE)), DEMO_IDLE_MILLIS)!!
        assertNull(noHold.side)
        assertEquals(CubeScene.DEFAULT_VIEW, noHold.to)
    }

    @Test
    fun everySideReadTurnsTheNextCornerForwardWithItsNeededStickers() {
        val solved = Cube.solved()
        // Every sticker known but those of the white–red–blue corner (UBR).
        val stickers = List(Stickers.COUNT) { i -> solved[i].takeIf { i !in Corner.UBR.stickers } }
        val s = inView.copy(readSides = all, pose = Pose(Face.F, Face.U), stickers = stickers, nextCorner = Corner.UBR)
        val demo = turnDemo(s, DEMO_IDLE_MILLIS)!!
        assertEquals(Corner.UBR, demo.corner)
        assertNull(demo.side)
        assertEquals(Corner.UBR.stickers.toSet(), demo.needed, "the corner's unknown stickers blink")
        assertEquals(CubeColor.WHITE, demo.colors[0], "known stickers in colour")
        assertNull(demo.colors[Corner.UBR.stickers[0]], "unknown ones grey")
        assertTrue(demo.from.angleTo(holdFor(Pose(Face.F, Face.U))) < 1e-3f, "starts as held")
        val towards = V3.of(Face.U.normal) + V3.of(Face.B.normal) + V3.of(Face.R.normal)
        assertTrue(close(demo.to.rotate(towards.normalized()), V3(0f, 0f, 1f)), "the corner faces the camera")
        assertTrue(turnDemo(s.copy(pose = Pose(Face.R, Face.U)), DEMO_IDLE_MILLIS + 100, demo) === demo, "the same loop while the corner stays")
        // Before every side is read: grey with coloured centres, as before.
        assertEquals(DEMO_COLORS, turnDemo(s.copy(readSides = all - CubeColor.ORANGE), DEMO_IDLE_MILLIS)!!.colors)
    }

    @Test
    fun theMovementStaysWhileTheSideToShowStays() {
        val held = inView.copy(readSides = all - CubeColor.ORANGE, pose = Pose(Face.F, Face.U))
        val first = turnDemo(held, DEMO_IDLE_MILLIS)!!
        val moved = turnDemo(held.copy(pose = Pose(Face.R, Face.U)), DEMO_IDLE_MILLIS + 100, first)
        assertTrue(moved === first, "the same loop while orange is still to show")
        val next = turnDemo(held.copy(readSides = all - CubeColor.BLUE, pose = Pose(Face.R, Face.U)), DEMO_IDLE_MILLIS, first)!!
        assertEquals(CubeColor.BLUE, next.side)
    }
}
