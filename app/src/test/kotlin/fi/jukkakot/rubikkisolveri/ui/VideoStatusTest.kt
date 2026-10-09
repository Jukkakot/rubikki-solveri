package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FoundFace
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.ui.scan.VideoStatus
import fi.jukkakot.rubikkisolveri.ui.scan.videoStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** The video scan's one status line (`scan-next-view`: the corners left once every side is read). */
class VideoStatusTest {
    private val inView = VideoScanState.EMPTY.copy(found = listOf(FoundFace(FaceReading(List(9) { null }, Point(0.0, 0.0), Point(1.0, 0.0), Point(0.0, 1.0)), List(9) { null }, List(9) { false })))

    @Test
    fun theLineCountsTheCornersLeftOnceEverySideIsRead() {
        val all = CubeColor.entries.toSet()
        assertEquals(VideoStatus.Line(Res.string.video_status_grey), videoStatus(inView))
        assertEquals(VideoStatus.Line(Res.string.video_status_grey), videoStatus(inView.copy(undecided = true)), "no separate turn-the-cube line")
        assertEquals(VideoStatus.Line(Res.string.video_status_find), videoStatus(VideoScanState.EMPTY.copy(undecided = true)))
        val read = inView.copy(readSides = all, readCorners = setOf(Corner.URF, Corner.UFL, Corner.ULB, Corner.UBR, Corner.DFR))
        assertEquals(VideoStatus.CornersLeft(3), videoStatus(read))
        assertEquals(VideoStatus.Line(Res.string.video_status_grey), videoStatus(read.copy(readCorners = Corner.entries.toSet())), "only edges left")
        assertEquals(VideoStatus.Line(Res.string.video_status_done), videoStatus(read.copy(complete = true)))
    }
}
