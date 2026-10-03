package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.ui.manual.ManualInputScreen
import fi.jukkakot.rubikkisolveri.ui.scan.ScanContent
import fi.jukkakot.rubikkisolveri.ui.scan.ScanScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import fi.jukkakot.rubikkisolveri.R
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ScanScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = MutableSharedFlow<List<Rgb>>(extraBufferCapacity = 64)
    private var outcome: ScanOutcome? = null
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun scan() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                ScanContent(frames, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = { outcome = it }, holdMillis = 0, preview = {})
            }
        }
    }

    private fun faceSamples(view: FaceView) = (1..9).map { ColorClassifier.DEFAULT_PALETTE.getValue(cube.colorAt(view.face, it)) }

    private fun show(view: FaceView, times: Int) {
        repeat(times) {
            compose.runOnIdle { frames.tryEmit(faceSamples(view)) }
            compose.waitForIdle()
        }
    }

    @Test
    fun wrongFace() {
        scan()
        show(FaceView.RIGHT, 3)
        compose.onNodeWithText("Väärä puoli: käännä vihreä keskiö kameraa kohti.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Etupuoli (1/6)").performScrollTo().assertIsDisplayed()
    }

    /** Holds [view] until it is captured and accepts it. */
    private fun confirm(view: FaceView) {
        show(view, 3)
        compose.onNodeWithText("Näyttää oikealta").performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun heldStill() {
        scan()
        show(FaceView.FRONT, 3)
        compose.onNodeWithText("Luettiin näin. Ovatko värit oikein?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Etupuoli (1/6)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Näyttää oikealta").performScrollTo().performClick()
        compose.onNodeWithText("Oikea puoli (2/6)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Etupuoli luettu.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun scanAgain() {
        scan()
        show(FaceView.FRONT, 3)
        compose.onNodeWithText("Skannaa uudelleen").performScrollTo().performClick()
        compose.onNodeWithText("Etupuoli (1/6)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Valmiina 0/6").performScrollTo().assertIsDisplayed()
        confirm(FaceView.FRONT)
        compose.onNodeWithText("Oikea puoli (2/6)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun redo() {
        scan()
        confirm(FaceView.FRONT)
        compose.onNodeWithText("Edellinen uudelleen").performScrollTo().performClick()
        compose.onNodeWithText("Etupuoli (1/6)").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun confidentScan() {
        scan()
        for (view in FaceView.entries) confirm(view)
        compose.waitUntil(5_000) { outcome != null }
        assertTrue(outcome!!.isConfident)
        assertEquals(cube, outcome!!.editor.toCube())
    }

    @Test
    fun permissionDenied() {
        var manual = false
        compose.setContent {
            RubikkiTheme(dynamicColor = false) { ScanScreen(onBack = {}, onManual = { manual = true }, onResult = {}) }
        }
        compose.onNodeWithText("Kamera tarvitaan").assertIsDisplayed()
        compose.onNodeWithText("Syötä värit käsin").performClick()
        assertTrue(manual)
    }

    @Test
    fun openedFromAScan() {
        val f1 = Stickers.index(Face.F, 1)
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                ManualInputScreen(
                    onBack = {}, onValid = {}, initial = CubeEditor.of(cube), initialMarked = setOf(f1),
                    title = R.string.check_title, note = R.string.check_note,
                )
            }
        }
        compose.onNodeWithText("Tarkista värit").assertIsDisplayed()
        compose.onNodeWithText("Skannaus ei ollut varma", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("punainen").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Etupuoli, tarra 1").performScrollTo().performClick()
        assertTrue(compose.onAllNodesWithText("Skannaus ei ollut varma", substring = true).fetchSemanticsNodes().isEmpty())
    }
}
