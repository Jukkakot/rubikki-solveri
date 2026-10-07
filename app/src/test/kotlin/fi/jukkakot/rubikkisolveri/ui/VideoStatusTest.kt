package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FoundFace
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.scan.videoStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/** The video scan's one status line (`scan-rules`: the turn-the-cube hint). */
class VideoStatusTest {
    private val inView = VideoScanState.EMPTY.copy(found = listOf(FoundFace(FaceReading(List(9) { null }, Point(0.0, 0.0), Point(1.0, 0.0), Point(0.0, 1.0)), List(9) { null }, List(9) { false })))

    @Test
    fun theLineAsksToTurnTheCubeWhileTwoFacesCouldBeEitherWay() {
        assertEquals(Res.string.video_status_grey, videoStatus(inView))
        assertEquals(Res.string.video_status_turn, videoStatus(inView.copy(undecided = true)))
        assertEquals(Res.string.video_status_find, videoStatus(VideoScanState.EMPTY.copy(undecided = true)))
    }
}
