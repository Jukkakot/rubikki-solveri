package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.ScanCheck
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.ui.manual.ManualInputScreen
import fi.jukkakot.rubikkisolveri.ui.nav.SolveRoute
import fi.jukkakot.rubikkisolveri.ui.nav.afterScan
import fi.jukkakot.rubikkisolveri.ui.scan.ScanContent
import fi.jukkakot.rubikkisolveri.ui.scan.ScanScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import fi.jukkakot.rubikkisolveri.res.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ScanScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = MutableSharedFlow<List<Rgb>>(extraBufferCapacity = 64)
    private var outcome: ScanOutcome? = null
    private val saved = ArrayList<String>()
    private var cubeInView = true
    private val locks = ArrayList<Boolean>()
    private val cube = Cube.solved().apply("R U F' D2 L B")

    private fun scan(autoAccept: Int? = null) {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                ScanContent(frames, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = { outcome = it }, holdMillis = 0, autoAcceptMillis = autoAccept, savePicture = { saved += it; "$it.png" }, gridCheck = { FrameSampler.GridCheck(List(9) { 30.0 }, List(9) { cubeInView }) }, onLockExposure = { locks += it }, preview = {})
            }
        }
    }

    @Test
    fun switchesToVideo() {
        var switched = false
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                ScanContent(frames, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = {}, onSwitch = { switched = true }, preview = {})
            }
        }
        compose.onNodeWithText("Videolla").performClick()
        assertTrue(switched)
    }

    private fun faceSamples(view: FaceView, turn: Int = 0) =
        RotationSearch.turned((1..9).map { ColorClassifier.DEFAULT_PALETTE.getValue(cube.colorAt(view.face, it)) }, turn)

    private fun show(view: FaceView, times: Int, turn: Int = 0) {
        repeat(times) {
            compose.runOnIdle { frames.tryEmit(faceSamples(view, turn)) }
            compose.waitForIdle()
        }
    }

    @Test
    fun exposureLocksOnTheFirstCapture() {
        scan()
        compose.waitForIdle()
        assertEquals(listOf(false), locks)
        show(FaceView.TOP, 3)
        // Captured, not yet accepted: already locked.
        assertEquals(listOf(false, true), locks)
        compose.onNodeWithText("Kuvaa uudelleen").performClick()
        compose.waitForIdle()
        assertEquals(listOf(false, true, false), locks)
    }

    @Test
    fun noCubeInTheGrid() {
        scan()
        cubeInView = false
        repeat(3) {
            compose.runOnIdle { frames.tryEmit(List(9) { Rgb(150, 145, 138) }) }
            compose.waitForIdle()
        }
        compose.onNodeWithText("Tuo kuutio ruudukkoon", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Kuvattu 0/6").assertIsDisplayed()
        assertTrue(saved.isEmpty())
        // The capture button still takes it, and its picture is saved.
        compose.onNodeWithContentDescription("Ota kuva").performClick()
        compose.onNodeWithText("Hyvä, seuraava").assertIsDisplayed()
        assertEquals(1, saved.size)
    }

    @Test
    fun firstFaceAsksForAnyFace() {
        scan()
        compose.onNodeWithText("Kuvattu 0/6").assertIsDisplayed()
        compose.onNodeWithText("Näytä mikä tahansa kuvaamaton puoli, missä asennossa tahansa.").assertIsDisplayed()
    }

    @Test
    fun anotherFaceFirst() {
        // The right face, turned a quarter, first: captured, and no face is named while scanning.
        scan()
        show(FaceView.RIGHT, 2, turn = 1)
        compose.onNodeWithText("Pidä paikallaan…").assertIsDisplayed()
        compose.onNodeWithText("Oikea puoli", substring = true).assertDoesNotExist()
        show(FaceView.RIGHT, 1, turn = 1)
        compose.onNodeWithText("Tunnistettu", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Oikea puoli", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Hyvä, seuraava").performClick()
        compose.onNodeWithText("Kuvattu 1/6").assertIsDisplayed()
        compose.onNodeWithContentDescription("Puoli luettu.").assertIsDisplayed()
    }

    @Test
    fun alreadyScannedFace() {
        scan()
        confirm(FaceView.FRONT)
        show(FaceView.FRONT, 3, turn = 2)
        compose.onNodeWithText("Tämä puoli on jo kuvattu – käännä kuutiota toiseen puoleen.").assertIsDisplayed()
        compose.onNodeWithText("Kuvattu 1/6").assertIsDisplayed()
    }

    /** Holds [view] until it is captured and accepts it. */
    private fun confirm(view: FaceView, turn: Int = 0) {
        show(view, 3, turn)
        compose.onNodeWithText("Hyvä, seuraava").performClick()
        compose.waitForIdle()
    }

    @Test
    fun heldStill() {
        scan()
        show(FaceView.FRONT, 3)
        compose.onNodeWithText("Tunnistettu", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Kuvattu 0/6").assertIsDisplayed()
        compose.onNodeWithText("Hyvä, seuraava").performClick()
        compose.onNodeWithText("Kuvattu 1/6").assertIsDisplayed()
        compose.onNodeWithText("Puoli luettu.").assertIsDisplayed()
    }

    @Test
    fun aCapturedFaceIsAcceptedByItself() {
        compose.mainClock.autoAdvance = false
        scan(autoAccept = 2_000)
        show(FaceView.FRONT, 3)
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("Kuvattu 0/6").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("Kuvattu 1/6").assertIsDisplayed()
    }

    @Test
    fun aTouchOnTheReviewStopsTheAutomaticAccept() {
        compose.mainClock.autoAdvance = false
        scan(autoAccept = 2_000)
        show(FaceView.FRONT, 3)
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Värit tulkitaan lopuksi", substring = true).performClick()
        compose.mainClock.advanceTimeBy(3_000)
        compose.onNodeWithText("Kuvattu 0/6").assertIsDisplayed()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("Hyvä, seuraava").performClick()
        compose.onNodeWithText("Kuvattu 1/6").assertIsDisplayed()
    }

    @Test
    fun scanAgain() {
        scan()
        show(FaceView.FRONT, 3)
        compose.onNodeWithText("Kuvaa uudelleen").performClick()
        compose.onNodeWithText("Kuvattu 0/6").assertIsDisplayed()
        confirm(FaceView.FRONT)
        compose.onNodeWithText("Kuvattu 1/6").assertIsDisplayed()
    }

    @Test
    fun facePipsShowDoneCurrentAndEmpty() {
        scan()
        confirm(FaceView.TOP)
        confirm(FaceView.FRONT)
        compose.onAllNodesWithContentDescription("Puoli luettu.").assertCountEquals(2)
        compose.onAllNodesWithContentDescription("Kuvattava puoli").assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Kuvaamatta").assertCountEquals(3)
    }

    @Test
    fun redo() {
        scan()
        confirm(FaceView.FRONT)
        compose.onNodeWithText("Uudelleen").performClick()
        compose.onNodeWithText("Kuvattu 0/6").assertIsDisplayed()
    }

    @Test
    fun confidentScan() {
        // Any order, some faces turned.
        scan()
        val order = listOf(FaceView.TOP, FaceView.BACK, FaceView.FRONT, FaceView.BOTTOM, FaceView.LEFT, FaceView.RIGHT)
        for ((i, view) in order.withIndex()) confirm(view, turn = i % 4)
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
    fun oneFaceScan() {
        val faces = ArrayList<Pair<FaceView, List<Rgb>>>()
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                ScanContent(
                    frames, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = { outcome = it }, holdMillis = 0, autoAcceptMillis = null,
                    only = FaceView.TOP, onFace = { view, samples -> faces += view to samples }, preview = {},
                )
            }
        }
        compose.onNodeWithText("Yläpuoli").assertIsDisplayed()
        compose.onNodeWithText("Keskiö on valkoinen. Näytä tämä puoli missä asennossa tahansa.").assertIsDisplayed()
        compose.onNodeWithText("Vain tämä puoli").assertIsDisplayed()
        compose.onNodeWithText("Uudelleen").assertDoesNotExist()
        confirm(FaceView.TOP)
        show(FaceView.TOP, 3)
        assertEquals(listOf(FaceView.TOP), faces.map { it.first })
        assertEquals(faceSamples(FaceView.TOP), faces.single().second)
        assertEquals(null, outcome)
    }

    /** The check of [colors] after a scan with [marked] stickers; the readings are the true colours. */
    private fun check(
        colors: Cube,
        marked: Set<Int>,
        onValid: (Cube) -> Unit = {},
        onScanFace: (FaceView) -> Unit = {},
        onScanAgain: () -> Unit = {},
        confident: Boolean = false,
    ) {
        val readings = (0 until 54).map { ColorClassifier.DEFAULT_PALETTE.getValue(cube[it]) }
        val editor = CubeEditor.of(colors)
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                ManualInputScreen(
                    onBack = {}, onValid = onValid, initial = editor, initialMarked = marked,
                    title = Res.string.check_title, note = if (confident) Res.string.check_note_ok else Res.string.check_note,
                    pictures = mapOf(Face.F to IntArray(120 * 120) { 0xff808080.toInt() }),
                    onScanAgain = onScanAgain,
                    check = ScanCheck.start(editor, marked, readings),
                    onScanFace = onScanFace,
                    confident = confident,
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "fi-w411dp-h891dp")
    fun openedFromAScan() {
        val f1 = Stickers.index(Face.F, 1)
        var scanAgain = false
        var rescan: FaceView? = null
        check(cube, setOf(f1), onScanFace = { rescan = it }, onScanAgain = { scanAgain = true })
        compose.onNodeWithText("Tarkista värit").assertIsDisplayed()
        compose.onNodeWithText("Vertaa kameran kuvaan.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Tarkistamatta vielä 1 puoli").assertIsDisplayed()
        // The camera's picture of the front face is beside the editable face, and the palette and
        // the check's actions are in view without scrolling.
        compose.onNodeWithContentDescription("Kamera näki").assertIsDisplayed()
        compose.onNodeWithContentDescription("punainen").assertIsDisplayed().performClick()
        compose.onNodeWithText("Näyttää oikealta").assertIsDisplayed()
        compose.onNodeWithContentDescription("Etupuoli, tarra 1").performClick()
        compose.onNodeWithText("Kuvaa uudelleen").assertIsDisplayed().performClick()
        assertEquals(FaceView.FRONT, rescan)
        compose.onNodeWithContentDescription("Lisää").performClick()
        compose.onNodeWithText("Skannaa koko kuutio uudelleen").performClick()
        assertTrue(scanAgain)
    }

    @Test
    @Config(qualifiers = "fi-w411dp-h891dp")
    fun looksRightGoesToTheNextUncheckedFace() {
        check(cube, setOf(Stickers.index(Face.R, 1), Stickers.index(Face.U, 3)))
        compose.onNodeWithText("Oikea puoli (2/6)").assertIsDisplayed()
        compose.onNodeWithContentDescription("Etupuoli, tarkistettu").assertIsDisplayed()
        compose.onNodeWithText("Näyttää oikealta").performClick()
        compose.onNodeWithText("Yläpuoli (5/6)").assertIsDisplayed()
        compose.onNodeWithContentDescription("Oikea puoli, tarkistettu").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fi-w411dp-h891dp")
    fun anImpossibleCubeNamesTheFaces() {
        // A top and a right sticker read the wrong way round.
        val top = (1..9).map { Stickers.index(Face.U, it) }.filter { it % 9 != 4 }
        val right = (1..9).map { Stickers.index(Face.R, it) }.filter { it % 9 != 4 }
        val (a, b) = top.flatMap { x -> right.map { x to it } }.first { (x, y) ->
            cube[x] != cube[y] && !fi.jukkakot.rubikkisolveri.cube.CubeCheck.validity(cube.with(x, cube[y]).with(y, cube[x])).isValid
        }
        check(cube.with(a, cube[b]).with(b, cube[a]), setOf(a))
        compose.onNodeWithText("Näyttää oikealta").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tällaista kuutiota ei voi olla", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Katso vielä: Oikea puoli ja Yläpuoli.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Oikea puoli (2/6)").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fi-w411dp-h891dp")
    fun aSolvableCubeOpensTheSolution() {
        var solved: Cube? = null
        check(cube, setOf(Stickers.index(Face.D, 2)), onValid = { solved = it })
        compose.onNodeWithText("Alapuoli (6/6)").assertIsDisplayed()
        compose.onNodeWithText("Näyttää oikealta").performClick()
        compose.waitUntil(5_000) { solved != null }
        assertEquals(cube, solved)
    }

    @Test
    fun aSureScanGoesStraightToTheSolutionWithTheCheckBehindIt() {
        val sure = ScanOutcome(CubeEditor.of(cube), emptySet(), CubeCheck.validity(cube))
        val (check, solve) = afterScan(sure)
        assertEquals(SolveRoute(cube.toColorString()), solve)
        assertTrue(check.fromScan && check.confident)
        val unsure = ScanOutcome(CubeEditor.of(cube), setOf(Stickers.index(Face.F, 1)), CubeCheck.validity(cube))
        assertEquals(null, afterScan(unsure).second)
    }

    @Test
    @Config(qualifiers = "fi-w411dp-h891dp")
    fun theCheckOfASureScanWaitsForTheUser() {
        // Opened again from the solution: nothing continues by itself, "Looks right" goes back.
        var solved: Cube? = null
        check(cube, emptySet(), onValid = { solved = it }, confident = true)
        compose.mainClock.advanceTimeBy(6_000)
        compose.waitForIdle()
        assertEquals(null, solved)
        compose.onNodeWithText("Näyttää oikealta").performClick()
        compose.waitUntil(5_000) { solved != null }
        assertEquals(cube, solved)
    }

    @Test
    @Config(qualifiers = "fi-w411dp-h891dp")
    fun scanAgainFromAConfidentCheck() {
        var scanAgain = false
        check(cube, emptySet(), onScanAgain = { scanAgain = true }, confident = true)
        compose.onNodeWithText("Skannaa koko kuutio uudelleen").performClick()
        assertTrue(scanAgain)
    }
}
