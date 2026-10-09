package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.NextSide
import fi.jukkakot.rubikkisolveri.cube.scan.ScanSides
import fi.jukkakot.rubikkisolveri.cube.scan.ScanStateCodec
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The side row and the next side (`scan-side-balls`). */
class ScanSidesTest {
    private val solved = Cube.solved()
    private val all = List(Stickers.COUNT) { solved[it] }
    private val allColors = CubeColor.entries.toSet()

    /**
     * A state knowing every sticker but those at [unknown], clear but for those and [unclear], every
     * side read but [unread], with [open] turns.
     */
    private fun state(unknown: Set<Int> = emptySet(), unclear: Set<Int> = emptySet(), open: Set<Face> = emptySet(), unread: Set<CubeColor> = emptySet()) =
        VideoScanState.EMPTY.copy(
            stickers = all.mapIndexed { i, c -> c.takeIf { i !in unknown } },
            openTurns = open,
            clear = all.indices.toSet() - unknown - unclear,
            readSides = allColors - unread,
        )

    @Test
    fun rowIsSixCentreColoursWithOppositesApart() {
        assertEquals(
            listOf(CubeColor.WHITE, CubeColor.RED, CubeColor.GREEN, CubeColor.YELLOW, CubeColor.ORANGE, CubeColor.BLUE),
            ScanSides.ROW.map { ScanSides.color(it) },
        )
        assertEquals(Face.entries.toSet(), ScanSides.ofMask(ScanSides.mask(Face.entries.toSet())))
        assertEquals(setOf(Face.R, Face.B), ScanSides.ofMask(ScanSides.mask(setOf(Face.R, Face.B))))
    }

    @Test
    fun doneSideNeedsAllNineClear() {
        assertEquals(emptySet(), ScanSides.done(VideoScanState.EMPTY))
        // One green sticker known from its own votes only: every side but green is done.
        val s = state(unclear = setOf(Face.F.ordinal * 9 + 2))
        assertEquals(Face.entries.toSet() - Face.F, ScanSides.done(s))
        assertEquals(Face.entries.toSet(), ScanSides.done(s.copy(complete = true)), "all at finish")
    }

    @Test
    fun tickTakenBackWhenARecheckChangesTheSide() {
        val sure = state(unclear = setOf(Face.U.ordinal * 9))
        assertTrue(Face.F in ScanSides.done(sure))
        // A recheck finds the green side turned differently: its stickers are no longer part of the clear cube.
        val rechecked = sure.copy(clear = sure.clear - ScanSides.stickersOf(Face.F).toSet())
        assertTrue(Face.F !in ScanSides.done(rechecked), "the tick is taken back")
        assertTrue(Face.F in ScanSides.done(sure), "and comes back once sure again")
    }

    @Test
    fun allSixOnlyWhenComplete() {
        // Everything clear, the blue side's turn still open: blue stays undone and is the next side.
        val s = state(open = setOf(Face.B))
        assertEquals(Face.entries.toSet() - Face.B, ScanSides.done(s))
        assertEquals(Face.B, NextSide.choose(s, null))
        // No doubt left but the complete flag: the current pick is held back.
        val quiet = state()
        assertEquals(Face.entries.toSet() - Face.L, ScanSides.done(quiet, Face.L))
        assertEquals(Face.L, NextSide.choose(quiet, Face.L))
        assertNull(NextSide.choose(quiet.copy(complete = true), Face.L), "none when every side is done")
    }

    @Test
    fun unreadSideChosenFirst() {
        // Orange never read; blue read but with much more to settle.
        val orange = state(unclear = ScanSides.stickersOf(Face.B).toSet() + (Face.L.ordinal * 9), unread = setOf(CubeColor.ORANGE))
        assertEquals(Face.L, NextSide.choose(orange, null))
        assertEquals(Face.L, NextSide.choose(orange, Face.B), "an unread side wins over the current pick")
        assertEquals(Face.U, NextSide.choose(VideoScanState.EMPTY, null), "row order at the start")
    }

    @Test
    fun choiceHoldsUnderSmallScoreChanges() {
        // Green and blue each with three stickers to settle.
        val a = state(unclear = (ScanSides.stickersOf(Face.F).take(3) + ScanSides.stickersOf(Face.B).take(3)).toSet())
        val first = NextSide.choose(a, null)!!
        assertEquals(Face.F, first, "row order on a tie")
        val other = Face.B
        assertEquals(first, NextSide.choose(a, first))
        // Blue gains a sticker in doubt: a little more to settle, not enough to switch.
        val b = a.copy(clear = a.clear - ScanSides.stickersOf(Face.B)[5])
        assertTrue(NextSide.score(b, other) > NextSide.score(b, first))
        assertEquals(first, NextSide.choose(b, first))
        // Clearly more (1.5×): it switches.
        val c = b.copy(openTurns = setOf(Face.B))
        assertEquals(other, NextSide.choose(c, first))
    }

    @Test
    fun codecCarriesDoneAndNextSides() {
        val s = state(unclear = setOf(0)).let { it.copy(doneSides = ScanSides.done(it), nextSide = Face.U) }
        val back = ScanStateCodec.decode(ScanStateCodec.encode(s))
        assertEquals(s.doneSides, back.doneSides)
        assertEquals(Face.U, back.nextSide)
    }

    @Test
    fun onEveryReplayAllSixDoneOnlyWhenComplete() {
        val runs = listOf("web_20261009_100814", "web_20261009_100824").map { name ->
            name to VideoFixtures.replay(VideoFixtures.loadRecording(name)).states
        } + VIDEOS.map { video ->
            val scan = VideoScan()
            video to VideoFixtures.load(video).mapIndexed { i, f -> scan.onFrame(f.faces, i * 100L) }
        }
        for ((name, states) in runs) {
            for ((i, s) in states.withIndex()) {
                if (s.doneSides.size == 6) assertTrue(s.complete || s.finished, "$name frame $i: all six done before complete")
                if (s.complete) assertEquals(6, s.doneSides.size, "$name frame $i: every side done at finish")
                for (f in s.doneSides) {
                    if (!s.complete) assertTrue(ScanSides.stickersOf(f).all { it in s.clear }, "$name frame $i: $f done with a sticker not clear")
                }
                if (!s.complete) {
                    val next = s.nextSide
                    assertTrue(next != null && next !in s.doneSides, "$name frame $i: a next side not done")
                }
            }
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
