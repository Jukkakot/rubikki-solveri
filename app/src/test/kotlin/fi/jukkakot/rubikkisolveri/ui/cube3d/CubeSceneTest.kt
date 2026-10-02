package fi.jukkakot.rubikkisolveri.ui.cube3d

import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Stickers
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CubeSceneTest {
    private val w = 1000f
    private val h = 1000f

    private fun visibleStickers(view: Quat, move: Move? = null, progress: Float = 0f) =
        CubeScene.project(CubeScene.quads(move, progress), view, w, h).filter { it.sticker >= 0 }

    private fun close(a: V3, b: V3) = (a - b).length < 1e-4f

    @Test
    fun quaternionRotatesLikeTheCubeModel() {
        // A quarter turn clockwise from the axis tip equals Vec3.quarterTurn.
        for (layer in Layer.entries) {
            val q = Quat.axisAngle(V3.of(layer.axis), CubeScene.moveAngle(Move(layer, 1), 1f))
            for (p in listOf(fi.jukkakot.rubikkisolveri.cube.Vec3(1, 1, 0), fi.jukkakot.rubikkisolveri.cube.Vec3(0, -1, 1))) {
                assertTrue(close(V3.of(p.quarterTurn(layer.axis)), q.rotate(V3.of(p))), "$layer $p")
            }
        }
    }

    @Test
    fun defaultView() {
        val faces = visibleStickers(CubeScene.DEFAULT_VIEW).map { Stickers.all[it.sticker].face }
        assertEquals(27, faces.size)
        assertEquals(setOf(Face.U, Face.F, Face.R), faces.toSet())
    }

    @Test
    fun animatedTurnMovesOnlyTheTurnedLayerAwayAtTheTop() {
        val r = Notation.parse("R").single()
        val before = CubeScene.quads(null, 0f).associateBy { it.sticker }
        val half = CubeScene.quads(r, 0.5f).associateBy { it.sticker }
        for (s in Stickers.all) {
            val moved = !close(before.getValue(s.index).corners[0], half.getValue(s.index).corners[0])
            assertEquals(s.position.x == 1, moved, "sticker ${s.index}")
        }
        // U9 is on top at the front-right; during R it goes up and back (away from the viewer).
        val u9 = Stickers.index(Face.U, 9)
        val z0 = before.getValue(u9).corners.map { it.z }.average()
        val z1 = half.getValue(u9).corners.map { it.z }.average()
        assertTrue(z1 < z0, "U9 moves back: $z0 -> $z1")
        // At the end of the move the geometry matches the cube model's permutation.
        val end = CubeScene.quads(r, 1f).associateBy { it.sticker }
        for (s in Stickers.all) {
            val target = Stickers.all[r.permutation[s.index]]
            val centre = end.getValue(s.index).corners.reduce(V3::plus) * 0.25f
            val expected = V3.of(target.position) + V3.of(target.normal) * 0.505f
            assertTrue(close(centre, expected), "sticker ${s.index}")
        }
    }

    @Test
    fun midTurnStillShowsACoherentCube() {
        val visible = visibleStickers(CubeScene.DEFAULT_VIEW, Notation.parse("U").single(), 0.5f)
        assertTrue(visible.size in 27..40)
    }

    @Test
    fun tapASticker() {
        val front = Quat.IDENTITY
        val projected = CubeScene.project(CubeScene.quads(null, 0f), front, w, h)
        assertEquals(Stickers.centre(Face.F), CubeScene.hitTest(projected, w / 2, h / 2))
        assertEquals(Stickers.centre(Face.F), CubeScene.hitTest(CubeScene.project(CubeScene.quads(null, 0f), CubeScene.DEFAULT_VIEW, w, h), centreOf(Stickers.centre(Face.F)).first, centreOf(Stickers.centre(Face.F)).second))
        assertNull(CubeScene.hitTest(projected, 5f, 5f))
    }

    @Test
    fun hiddenStickersAreNeverHit() {
        val projected = CubeScene.project(CubeScene.quads(null, 0f), CubeScene.DEFAULT_VIEW, w, h)
        val hidden = Stickers.all.filter { it.face in setOf(Face.D, Face.L, Face.B) }.map { it.index }.toSet()
        for (x in 0 until 1000 step 20) for (y in 0 until 1000 step 20) {
            val hit = CubeScene.hitTest(projected, x.toFloat(), y.toFloat())
            assertTrue(hit == null || hit !in hidden, "hit $hit at $x,$y")
        }
    }

    @Test
    fun dragHalfAWidthTurnsTheViewAQuarter() {
        val state = CubeViewState(Quat.IDENTITY)
        state.drag(500f, 0f, 1000f)
        val front = state.rotation.rotate(V3(0f, 0f, 1f))
        assertTrue(abs(front.x - 1f) < 1e-4f, "front now faces right: $front")
        state.drag(500f, 0f, 1000f)
        val back = visibleStickers(state.rotation).map { Stickers.all[it.sticker].face }.toSet()
        assertEquals(setOf(Face.B), back)
    }

    @Test
    fun slerpEnds() {
        val a = Quat.IDENTITY
        val b = Quat.axisAngle(V3(0f, 1f, 0f), (PI / 2).toFloat())
        assertTrue(close(a.slerp(b, 0f).rotate(V3(1f, 0f, 0f)), V3(1f, 0f, 0f)))
        assertTrue(close(a.slerp(b, 1f).rotate(V3(1f, 0f, 0f)), b.rotate(V3(1f, 0f, 0f))))
    }

    private fun centreOf(sticker: Int): Pair<Float, Float> {
        val q = CubeScene.project(CubeScene.quads(null, 0f), CubeScene.DEFAULT_VIEW, w, h).first { it.sticker == sticker }
        return q.xs.average().toFloat() to q.ys.average().toFloat()
    }
}

class MoveGuideSceneTest {
    private val w = 1000f
    private val h = 1000f

    private fun move(text: String) = fi.jukkakot.rubikkisolveri.cube.Notation.parseMove(text)!!

    /** Shoelace sum of the arrow's screen points around its projected centre (y down: > 0 is clockwise). */
    private fun screenWinding(m: Move, view: Quat): Float {
        val (cx, cy) = CubeScene.projectPoint(V3.of(m.layer.axis) * 1.56f, view, w, h)
        val pts = CubeScene.arrow(m, view).map { CubeScene.projectPoint(it, view, w, h) }.map { (x, y) -> (x - cx) to (y - cy) }
        var sum = 0f
        for (i in 0 until pts.size - 1) sum += pts[i].first * pts[i + 1].second - pts[i + 1].first * pts[i].second
        return sum
    }

    @Test
    fun rightLayerHighlighted() {
        val r = move("R")
        val projected = CubeScene.project(CubeScene.quads(null, 0f), CubeScene.DEFAULT_VIEW, w, h, highlight = r)
        val stickers = projected.filter { it.sticker >= 0 }
        for (q in stickers) {
            val inLayer = Stickers.all[q.sticker].position.x == 1
            assertEquals(!inLayer, q.dimmed, "sticker ${q.sticker}")
        }
        assertTrue(stickers.any { !it.dimmed } && stickers.any { it.dimmed })
    }

    @Test
    fun clockwiseArrow() {
        assertTrue(screenWinding(move("F"), Quat.IDENTITY) > 0, "F seen from the front is clockwise")
        assertTrue(screenWinding(move("F'"), Quat.IDENTITY) < 0)
        assertTrue(screenWinding(move("R"), CubeScene.guideView(move("R"))) > 0)
        assertTrue(screenWinding(move("U'"), CubeScene.guideView(move("U'"))) < 0)
    }

    @Test
    fun halfTurnArrow() {
        val pts = CubeScene.arrow(move("F2"), Quat.IDENTITY)
        val a = pts.first()
        val b = pts.last()
        // Both ends lie on the circle around the front face's axis, opposite each other.
        assertTrue(abs(a.x + b.x) < 1e-3f && abs(a.y + b.y) < 1e-3f, "$a $b")
        val quarter = CubeScene.arrow(move("F"), Quat.IDENTITY)
        val dot = quarter.first().x * quarter.last().x + quarter.first().y * quarter.last().y
        assertTrue(abs(dot) < 1e-3f, "quarter arc ends are perpendicular")
    }

    @Test
    fun arrowSitsOnTheVisibleSideOfTheFace() {
        val view = CubeScene.guideView(move("U"))
        val mid = CubeScene.arrow(move("U"), view)[12]
        assertTrue(mid.z > 0.5f, "the middle of a top arrow is towards the front: $mid")
    }

    @Test
    fun backMove() {
        for (m in listOf("B", "L'", "D2")) {
            val face = move(m).layer.face!!
            val visible = CubeScene.project(CubeScene.quads(null, 0f), CubeScene.guideView(move(m)), w, h)
                .filter { it.sticker >= 0 && Stickers.all[it.sticker].face == face }
            assertEquals(9, visible.size, m)
        }
        assertEquals(CubeScene.DEFAULT_VIEW, CubeScene.guideView(move("U")))
        assertEquals(CubeScene.DEFAULT_VIEW, CubeScene.guideView(move("R2")))
    }
}
