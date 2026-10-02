package fi.jukkakot.rubikkisolveri.cube

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorTest {

    @Test
    fun eachHoldBringsTheFaceToTheFrontWithTheNetTopUp() {
        for (view in FaceView.entries) {
            // Follow every sticker of the face through the hold rotation.
            var places = (1..9).map { Stickers.index(view.face, it) }
            for (move in view.hold) places = places.map { move.permutation[it] }
            assertEquals((1..9).map { Stickers.index(Face.F, it) }, places, view.name)
            val held = Cube.solved().apply(view.hold)
            assertEquals(view.centreColor(), held.centre(Face.F))
            assertEquals(view.topColor(), held.centre(Face.U))
        }
        assertEquals(CubeColor.BLUE, FaceView.TOP.topColor())
        assertEquals(CubeColor.GREEN, FaceView.BOTTOM.topColor())
        assertEquals(FaceView.RIGHT, FaceView.FRONT.next)
        assertNull(FaceView.BOTTOM.next)
    }

    @Test
    fun paintASticker() {
        val f1 = Stickers.index(Face.F, 1)
        val editor = CubeEditor.empty().paint(f1, CubeColor.RED)
        assertEquals(CubeColor.RED, editor[f1])
    }

    @Test
    fun centresAreFixed() {
        val centre = Stickers.centre(Face.F)
        val editor = CubeEditor.empty().paint(centre, CubeColor.RED)
        assertEquals(CubeColor.GREEN, editor[centre])
    }

    @Test
    fun clear() {
        val editor = CubeEditor.of(Cube.solved()).clear()
        for (i in 0 until 54) {
            if (i % 9 == 4) assertEquals(Cube.solved()[i], editor[i]) else assertNull(editor[i])
        }
        assertFalse(editor.isComplete)
        assertNull(editor.toCube())
    }

    @Test
    fun tooManyOfAColour() {
        var editor = CubeEditor.empty()
        for (i in listOf(0, 1, 2, 3, 5, 6, 7, 8, 9)) editor = editor.paint(i, CubeColor.WHITE)
        assertEquals(10, editor.counts()[CubeColor.WHITE])
    }

    @Test
    fun completeEditorGivesTheCubeAndSurvivesEncoding() {
        val cube = Cube.solved().apply("R U F'")
        val editor = CubeEditor.of(cube)
        assertTrue(editor.isComplete)
        assertEquals(cube, editor.toCube())
        assertEquals(editor, CubeEditor.decode(editor.encode()))
        assertEquals(CubeEditor.empty(), CubeEditor.decode(CubeEditor.empty().encode()))
        assertNull(CubeEditor.decode("nope"))
    }

    @Test
    fun withFaceReplacesAllButTheCentre() {
        val nine = List(9) { CubeColor.YELLOW }
        val editor = CubeEditor.empty().withFace(Face.F, nine)
        assertTrue(editor.isFaceComplete(Face.F))
        assertEquals(CubeColor.GREEN, editor[Stickers.centre(Face.F)])
        assertEquals(9, editor.counts()[CubeColor.YELLOW]) // 8 painted + the D centre
    }
}
