package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
    private val saved = ArrayList<String>()
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun scan() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                ScanContent(frames, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = { outcome = it }, holdMillis = 0, savePicture = { saved += it; "$it.png" }, preview = {})
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
    fun noCubeInTheGrid() {
        scan()
        repeat(3) {
            compose.runOnIdle { frames.tryEmit(List(9) { Rgb(35, 35, 40) }) }
            compose.waitForIdle()
        }
        compose.onNodeWithText("Ruudukossa ei näy kuutiota. Tuo kuution puoli ruudukkoon.").assertIsDisplayed()
        compose.onNodeWithText("Etupuoli (1/6)").assertIsDisplayed()
        assertTrue(saved.isEmpty())
        // The capture button still takes it, and its picture is saved.
        compose.onNodeWithText("Ota kuva").performClick()
        compose.onNodeWithText("Hyvä, seuraava").assertIsDisplayed()
        assertEquals(listOf("F"), saved)
    }

    @Test
    fun otherCentreIsAHint() {
        scan()
        show(FaceView.RIGHT, 2)
        compose.onNodeWithText("Keskiö näyttää: punainen. Jos tämä on oikea puoli, pidä paikallaan.").assertIsDisplayed()
        show(FaceView.RIGHT, 1)
        compose.onNodeWithText("Keskiö luettiin: punainen. Jos tämä silti on oikea puoli, jatka vain.").assertIsDisplayed()
        compose.onNodeWithText("Etupuoli (1/6)").assertIsDisplayed()
    }

    @Test
    fun previousFaceStillInView() {
        scan()
        confirm(FaceView.FRONT)
        show(FaceView.FRONT, 3)
        compose.onNodeWithText("Käännä kuutiota: kamera näkee vielä edellisen puolen.").assertIsDisplayed()
        compose.onNodeWithText("Oikea puoli (2/6)").assertIsDisplayed()
    }

    /** Holds [view] until it is captured and accepts it. */
    private fun confirm(view: FaceView) {
        show(view, 3)
        compose.onNodeWithText("Hyvä, seuraava").performClick()
        compose.waitForIdle()
    }

    @Test
    fun heldStill() {
        scan()
        show(FaceView.FRONT, 3)
        compose.onNodeWithText("Näin kamera näki tämän puolen.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Etupuoli (1/6)").assertIsDisplayed()
        compose.onNodeWithText("Hyvä, seuraava").performClick()
        compose.onNodeWithText("Oikea puoli (2/6)").assertIsDisplayed()
        compose.onNodeWithText("Etupuoli luettu.").assertIsDisplayed()
    }

    @Test
    fun scanAgain() {
        scan()
        show(FaceView.FRONT, 3)
        compose.onNodeWithText("Kuvaa uudelleen").performClick()
        compose.onNodeWithText("Etupuoli (1/6)").assertIsDisplayed()
        compose.onNodeWithText("Valmiina 0/6").assertIsDisplayed()
        confirm(FaceView.FRONT)
        compose.onNodeWithText("Oikea puoli (2/6)").assertIsDisplayed()
    }

    @Test
    fun redo() {
        scan()
        confirm(FaceView.FRONT)
        compose.onNodeWithText("Edellinen uudelleen").performClick()
        compose.onNodeWithText("Etupuoli (1/6)").assertIsDisplayed()
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
        compose.onNodeWithContentDescription("punainen").performClick()
        compose.onNodeWithContentDescription("Etupuoli, tarra 1").performClick()
        assertTrue(compose.onAllNodesWithText("Skannaus ei ollut varma", substring = true).fetchSemanticsNodes().isEmpty())
    }
}
