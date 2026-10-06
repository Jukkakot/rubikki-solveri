package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.scan.CameraSettings
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.ui.nav.afterScan
import fi.jukkakot.rubikkisolveri.ui.nav.ManualInputRoute
import fi.jukkakot.rubikkisolveri.ui.nav.SolveRoute
import fi.jukkakot.rubikkisolveri.ui.scan.FoundFaces
import fi.jukkakot.rubikkisolveri.ui.scan.VideoScanContent
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
    private val settings = ArrayList<CameraSettings>()
    private var now = 0L
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun scan() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                VideoScanContent(
                    found, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = { outcome = it },
                    onExposure = { settings += it }, clock = { now }, preview = {},
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
    fun switchesToOneFaceAtATime() {
        var switched = false
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                VideoScanContent(found, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = {}, onSwitch = { switched = true }, preview = {})
            }
        }
        compose.onNodeWithText("Kuva kerrallaan").performClick()
        assertTrue(switched)
    }

    @Test
    fun stickersFillInAndTheCameraMetersOnTheFaceThenLocks() {
        scan()
        compose.onNodeWithContentDescription("Valmiit sivut: 0/6").assertExists()
        compose.onNodeWithText("Korjaa värit").assertIsNotEnabled()
        // The first face: the camera meters and focuses at it, and its frames are not read while it adjusts.
        show(face(Face.U), times = 6)
        assertEquals(listOf(CameraSettings(meter = Point(0.5, 0.5), focus = Point(0.5, 0.5))), settings)
        compose.onNodeWithText("Korjaa värit").assertIsNotEnabled()
        show(face(Face.U), times = 3)
        // The face in view, read three times: its stickers are known, but no tick until the rest of the cube confirms it.
        compose.onNodeWithContentDescription("Valmiit sivut: 0/6").assertExists()
        compose.onNodeWithText("Korjaa värit").assertIsEnabled()
        assertEquals(CameraSettings(meter = Point(0.5, 0.5), focus = Point(0.5, 0.5), lock = true), settings.last())
    }

    @Test
    fun partialFacesCountAndAPartlyKnownCubeRenders() {
        scan()
        // A face with a sticker hidden, then the face in full: the full one starts it, both are drawn.
        val partial = face(Face.U).let { it.copy(colors = it.colors.toMutableList().also { c -> c[1] = null }) }
        show(face(Face.U), partial, face(Face.U).copy(centre = Point(180.0, 120.0)), times = 7)
        compose.onNodeWithContentDescription("Valmiit sivut: 0/6").assertExists()
        compose.onNodeWithText("Korjaa värit").assertIsEnabled()
    }

    @Test
    fun wholeCubeSeenOpensTheSolution() {
        scan()
        for (f in Face.entries) show(face(f), times = 10)
        show(times = 6)
        val result = assertNotNull(outcome)
        assertEquals(cube.toColorString(), result.editor.encode())
        assertTrue(afterScan(result) is SolveRoute, "sure: straight on to the solution")
    }

    @Test
    fun stopEarlyOpensTheCheckWithTheMissingMarked() {
        scan()
        show(face(Face.U), times = 9)
        compose.onNodeWithText("Korjaa värit").performClick()
        val result = assertNotNull(outcome)
        val missing = (0 until Stickers.COUNT).filter { it / 9 != Face.U.ordinal && it % 9 != 4 }
        assertTrue(result.marked.containsAll(missing))
        assertTrue(afterScan(result) is ManualInputRoute, "unsure: the check")
    }

    @Test
    fun aStallShowsTheReasonAndStartingOverClearsTheProgress() {
        scan()
        // The same face for over twenty seconds: nothing new becomes known.
        show(face(Face.U), times = 150)
        compose.onNodeWithText("Värit eivät täsmää").assertDoesNotExist()
        show(face(Face.U), times = 60)
        compose.onNodeWithText("Värit eivät täsmää").assertExists()
        compose.onNodeWithText("Taskulamppu").assertExists()
        compose.onNodeWithText("Aloita alusta").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Aloita alusta").assertDoesNotExist()
        compose.onNodeWithContentDescription("Valmiit sivut: 0/6").assertExists()
        compose.onNodeWithText("Korjaa värit").assertIsNotEnabled()
        assertNull(outcome)
    }

    @Test
    fun scanningGoesOnWithTheNoticeShown() {
        scan()
        show(face(Face.U), times = 210)
        compose.onNodeWithText("Värit eivät täsmää").assertExists()
        // The user keeps turning the cube: new stickers become known and the notice goes.
        show(face(Face.F), times = 3)
        compose.onNodeWithText("Värit eivät täsmää").assertDoesNotExist()
    }

    @Test
    fun aTapOutsideClosesTheNoticeForGood() {
        scan()
        show(face(Face.U), times = 210)
        // The picture's close action (in the test layout the picture is small and the notice covers its middle).
        compose.onNodeWithContentDescription("Sulje ilmoitus").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Värit eivät täsmää").assertDoesNotExist()
        // Still stuck, but the same reason does not come back in this scan.
        show(face(Face.U), times = 30)
        compose.onNodeWithText("Värit eivät täsmää").assertDoesNotExist()
        compose.onNodeWithContentDescription("Sulje ilmoitus").assertDoesNotExist()
    }
}
