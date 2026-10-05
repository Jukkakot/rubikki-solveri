package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.nav.afterScan
import fi.jukkakot.rubikkisolveri.ui.scan.FoundFaces
import fi.jukkakot.rubikkisolveri.ui.scan.VideoScanContent
import fi.jukkakot.rubikkisolveri.ui.scan.progressColors
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The video scan screen fed with faces as the finder would report them (no camera). */
@RunWith(RobolectricTestRunner::class)
class VideoScanScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val found = MutableSharedFlow<FoundFaces>(extraBufferCapacity = 64)
    private var outcome: ScanOutcome? = null
    private val locks = ArrayList<Boolean>()
    private var now = 0L
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun scan() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                VideoScanContent(
                    found, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = { outcome = it },
                    onLockExposure = { locks += it }, clock = { now }, preview = {},
                )
            }
        }
    }

    private fun face(face: Face) = FaceReading(
        (0 until 9).map { ColorClassifier.DEFAULT_PALETTE.getValue(cube[face.ordinal * 9 + it]) },
        Point(180.0, 320.0), Point(40.0, 0.0), Point(0.0, 40.0),
    )

    private fun show(vararg faces: FaceReading, times: Int = 1) {
        repeat(times) {
            compose.runOnIdle { found.tryEmit(FoundFaces(faces.toList(), 360, 640)) }
            compose.waitForIdle()
            now += 100
        }
    }

    @Test
    fun stickersFillInAndExposureLocksOnTheFirstFace() {
        scan()
        compose.onNodeWithText("0/54 tarraa").assertExists()
        compose.onNodeWithText("Tarkista nyt").assertIsNotEnabled()
        show(face(Face.U), times = 3)
        compose.onNodeWithText("9/54 tarraa").assertExists()
        compose.onNodeWithText("Tarkista nyt").assertIsEnabled()
        assertEquals(listOf(false, true), locks)
    }

    @Test
    fun aPartlyReadCubeShowsFaintColoursAndPartialFacesCount() {
        scan()
        // A face with a sticker hidden, then the face in full: the full one starts it, both are drawn.
        val partial = face(Face.U).let { it.copy(colors = it.colors.toMutableList().also { c -> c[1] = null }) }
        show(face(Face.U), partial, face(Face.U).copy(centre = Point(180.0, 120.0)))
        // Three votes for the eight it shows, two for the hidden one.
        compose.onNodeWithText("8/54 tarraa").assertExists()
        show(face(Face.U))
        compose.onNodeWithText("9/54 tarraa").assertExists()
        val state = VideoScanState.EMPTY.copy(leading = List(Stickers.COUNT) { if (it < 9) CubeColor.RED else null })
        val colors = progressColors(state)
        assertTrue(colors[0] != StickerColors.UNKNOWN && colors[0] != StickerColors.of(CubeColor.RED), "faint")
        assertEquals(StickerColors.UNKNOWN, colors[9])
    }

    @Test
    fun wholeCubeSeenOpensTheSolutionAfterHalfASecond() {
        scan()
        for (f in Face.entries) show(face(f), times = 3)
        assertNull(outcome, "not before half a second")
        show(times = 6)
        val result = assertNotNull(outcome)
        assertEquals(cube.toColorString(), result.editor.encode())
        assertNotNull(afterScan(result).second, "sure: straight on to the solution")
    }

    @Test
    fun stopEarlyOpensTheCheckWithTheMissingMarked() {
        scan()
        show(face(Face.U), times = 3)
        compose.onNodeWithText("Tarkista nyt").performClick()
        val result = assertNotNull(outcome)
        val missing = (0 until Stickers.COUNT).filter { it / 9 != Face.U.ordinal && it % 9 != 4 }
        assertTrue(result.marked.containsAll(missing))
        assertNull(afterScan(result).second, "unsure: the check")
    }
}
