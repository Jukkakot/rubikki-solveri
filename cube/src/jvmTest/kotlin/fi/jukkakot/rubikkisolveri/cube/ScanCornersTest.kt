package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.NextCorner
import fi.jukkakot.rubikkisolveri.cube.scan.ScanCorners
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The corner row and the next corner (`scan-next-view`). */
class ScanCornersTest {
    private val solved = Cube.solved()
    private val all = List(Stickers.COUNT) { solved[it] }

    /**
     * A state knowing every sticker but those at [unknown], clear but for those and [unclear], every side
     * confirmed but those of [doubt] and those with a sticker not clear.
     */
    private fun state(unknown: Set<Int>, doubt: Set<Face> = emptySet(), open: Set<Face> = emptySet(), unclear: Set<Int> = emptySet()) = VideoScanState.EMPTY.copy(
        stickers = all.mapIndexed { i, c -> c.takeIf { i !in unknown } },
        confirmed = Face.entries.toSet() - doubt - Face.entries.filter { f -> (unknown + unclear).any { it / 9 == f.ordinal } },
        openTurns = open,
        clear = all.indices.toSet() - unknown - unclear,
    )

    @Test
    fun rowIsWhiteSideThenYellowSideInCentreColours() {
        val colors = ScanCorners.ROW.map { ScanCorners.colors(it) }
        assertEquals(8, colors.size)
        assertTrue(colors.take(4).all { it[0] == CubeColor.WHITE } && colors.drop(4).all { it[0] == CubeColor.YELLOW })
        assertEquals(listOf(CubeColor.WHITE, CubeColor.RED, CubeColor.GREEN), colors[0], "URF: top, right, left")
    }

    @Test
    fun readCornersFromAPartialState() {
        assertEquals(emptySet(), ScanCorners.read(VideoScanState.EMPTY))
        // One sticker of the white–red–green corner unknown: every corner but that one is read.
        val s = state(setOf(Corner.URF.stickers[1]))
        assertEquals(Corner.entries.toSet() - Corner.URF, ScanCorners.read(s))
        assertEquals(Corner.entries.toSet(), ScanCorners.read(s.copy(complete = true)), "all at finish")
        assertEquals(Corner.entries.toSet(), ScanCorners.ofMask(ScanCorners.mask(Corner.entries.toSet())))
    }

    @Test
    fun missingWhiteRedBlueCornerIsChosen() {
        // The white–red–blue corner (UBR) and the edges next to it are unknown.
        val unknown = Corner.UBR.stickers + Edge.UR.stickers + Edge.UB.stickers + Edge.BR.stickers
        val s = state(unknown.toSet())
        assertEquals(Corner.UBR, NextCorner.choose(s, null))
        assertEquals(Corner.UBR, NextCorner.choose(s, Corner.URF), "a read corner is not kept")
        assertNull(NextCorner.choose(state(emptySet()).copy(complete = true), null), "none when every corner is read")
    }

    @Test
    fun cornerStickersAloneAreNotEnough() {
        // The white–red–green corner's stickers known, its white–red edge known from its own votes only.
        val s = state(emptySet(), unclear = Edge.UR.stickers.toSet())
        assertTrue(Corner.URF.stickers.all { s.stickers[it] != null && it in s.clear })
        val read = ScanCorners.read(s)
        assertTrue(Corner.URF !in read && Corner.UBR !in read, "the corners at the edge in doubt are not read")
        assertEquals(6, read.size)
    }

    @Test
    fun allEightOnlyWhenComplete() {
        // Everything clear, the white–red–blue corner's sides still turning: that corner stays unread and pulses.
        val s = state(emptySet(), open = setOf(Face.B))
        val read = ScanCorners.read(s)
        assertEquals(7, read.size)
        val held = (Corner.entries - read).single()
        assertTrue(Face.B in held.faces, "the held corner touches the open doubt")
        assertEquals(held, NextCorner.choose(s, null))
        // The doubt elsewhere (only the complete flag missing): the pulsing corner is kept.
        val quiet = state(emptySet())
        assertEquals(Corner.DRB, NextCorner.choose(quiet, Corner.DRB))
        assertEquals(Corner.entries.toSet() - Corner.DRB, ScanCorners.read(quiet, Corner.DRB))
        assertEquals(Corner.entries.toSet(), ScanCorners.read(quiet.copy(complete = true)))
    }

    @Test
    fun onEveryReplayAllEightReadOnlyWhenComplete() {
        val runs = listOf("web_20261009_100814", "web_20261009_100824").map { name ->
            name to VideoFixtures.replay(VideoFixtures.loadRecording(name)).states
        } + VIDEOS.map { video ->
            val scan = VideoScan()
            video to VideoFixtures.load(video).mapIndexed { i, f -> scan.onFrame(f.faces, i * 100L) }
        }
        for ((name, states) in runs) {
            for ((i, s) in states.withIndex()) {
                if (s.readCorners.size == 8) assertTrue(s.complete, "$name frame $i: all eight read before complete")
                if (s.complete) assertEquals(8, s.readCorners.size, "$name frame $i: every corner read at finish")
                for (c in s.readCorners) {
                    if (!s.complete) assertTrue(ScanCorners.stickersOf(c).all { it in s.clear }, "$name frame $i: $c read with a sticker not clear")
                }
            }
        }
    }

    @Test
    fun choiceHoldsUnderSmallScoreChanges() {
        // Two unread corners on opposite sides of the cube, about the same to settle.
        val a = state((Corner.URF.stickers + Corner.DBL.stickers).toSet())
        val first = NextCorner.choose(a, null)!!
        val other = if (first == Corner.URF) Corner.DBL else Corner.URF
        // The other corner's side gains an open turn: a little more to settle, not enough to switch.
        val b = a.copy(openTurns = setOf(other.faces[0]))
        assertTrue(NextCorner.score(b, other) > NextCorner.score(b, first))
        assertEquals(first, NextCorner.choose(b, first))
        // Clearly more (1.5×): only one sticker of the chosen corner left, it switches.
        val c = state(setOf(first.stickers[1]) + other.stickers)
        assertEquals(other, NextCorner.choose(c, first))
    }

    @Test
    fun onTheRecordingsTheChosenCornerAlwaysHasSomethingToSettle() {
        for (name in listOf("web_20261009_100814", "web_20261009_100824")) {
            var chosen = 0
            VideoFixtures.replay(VideoFixtures.loadRecording(name)) { _, _, s ->
                val next = s?.nextCorner ?: return@replay
                chosen++
                assertTrue(next !in s.readCorners, "$name: the next corner is unread")
                // The last corner held back while only the complete flag is missing may have nothing left on its sides.
                if (s.readCorners.size == 7) return@replay
                val view = next.faces.flatMap { f -> (0 until 9).map { f.ordinal * 9 + it } }
                assertTrue(view.any { s.stickers[it] == null || Face.entries[it / 9] !in s.confirmed }, "$name: $next has an unknown or doubtful sticker")
            }
            assertTrue(chosen > 0, "$name: a next corner was chosen")
        }
    }

    private companion object {
        /** The video fixtures replayed whole, at 10 pictures a second. */
        val VIDEOS = listOf(
            VideoFixtures.ANGLED, VideoFixtures.STRAIGHT, VideoFixtures.WEB_1007, VideoFixtures.CAMERA_1007, VideoFixtures.TOUR_1007,
            VideoFixtures.STRIPED, VideoFixtures.STRIPED_DIM, VideoFixtures.STRIPED_TABLE, VideoFixtures.STRIPED_U2_TABLE,
            VideoFixtures.STRIPED_U2_DIM, VideoFixtures.PHONE_SCAN_1, VideoFixtures.PHONE_SCAN_2, VideoFixtures.PHONE_RULES_3,
            VideoFixtures.PHONE_LOOK_3,
        ) + VideoFixtures.EVENING.keys
    }
}
