package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.BestCube
import fi.jukkakot.rubikkisolveri.cube.scan.StickerEvidence
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BestCubeTest {
    private val truth = Cube.fromColorString(VideoFixtures.TRUTH)

    /** [votes] readings of each sticker of [cube] except [unseen]; [misread] stickers read as another colour. */
    private fun evidence(cube: Cube, votes: Int = 5, unseen: Set<Int> = emptySet(), misread: Map<Int, CubeColor> = emptyMap()) =
        StickerEvidence(
            List(Stickers.COUNT) { s ->
                DoubleArray(6).also { if (s !in unseen) it[(misread[s] ?: cube[s]).ordinal] = votes.toDouble() }
            },
        )

    @Test
    fun clearVotesCostLittle() {
        val e = evidence(truth)
        val s = Stickers.index(Face.U, 1)
        val right = e.cost(s, truth[s])
        for (c in CubeColor.entries) if (c != truth[s]) assertTrue(e.cost(s, c) > right + 1, "$c")
    }

    @Test
    fun redVotesLeaveOrangeCheaperThanBlue() {
        val e = StickerEvidence(List(Stickers.COUNT) { DoubleArray(6).also { a -> a[CubeColor.RED.ordinal] = 6.0 } })
        assertTrue(e.cost(0, CubeColor.RED) < e.cost(0, CubeColor.ORANGE))
        assertTrue(e.cost(0, CubeColor.ORANGE) < e.cost(0, CubeColor.BLUE))
        assertEquals(null, StickerEvidence(List(Stickers.COUNT) { DoubleArray(6).also { a -> a[CubeColor.RED.ordinal] = 3.0 } }).sure(0), "red needs more readings")
        assertEquals(CubeColor.RED, e.sure(0))
    }

    @Test
    fun fullRightEvidenceGivesThatCube() {
        val random = Random(7)
        repeat(20) {
            val cube = Cube.solved().apply(Scramble.random(random = random))
            val best = assertNotNull(BestCube.solve(evidence(cube)))
            assertEquals(cube, best.cube)
            assertTrue(best.minMargin > 2, "${best.minMargin}")
        }
    }

    @Test
    fun oneOrangeReadAsRedStillGivesTheRightCube() {
        val s = (0 until Stickers.COUNT).first { truth[it] == CubeColor.ORANGE && it % 9 != 4 }
        val best = assertNotNull(BestCube.solve(evidence(truth, misread = mapOf(s to CubeColor.RED))))
        assertEquals(truth, best.cube)
    }

    @Test
    fun twoStickersOfACornerTellTheThird() {
        val third = Corner.URF.stickers[2]
        val best = assertNotNull(BestCube.solve(evidence(truth, unseen = setOf(third))))
        assertEquals(truth[third], best.cube[third])
        assertTrue(best.margin(third) > 3, "${best.margin(third)}")
    }

    @Test
    fun anUnseenFaceIsKnownFromTheOthersWhereTheyDecideIt() {
        val random = Random(3)
        var decided = 0
        repeat(10) {
            val cube = Cube.solved().apply(Scramble.random(random = random))
            for (face in Face.entries) {
                val unseen = (0 until 9).map { face.ordinal * 9 + it }.toSet()
                val best = assertNotNull(BestCube.solve(evidence(cube, votes = 20, unseen = unseen)))
                // Five sides fix the sixth unless several of its edges show only its own colour.
                for (s in 0 until Stickers.COUNT) if (best.margin(s) > 1) assertEquals(cube[s], best.cube[s], "sticker $s")
                if (best.minMargin > 1) decided++
            }
        }
        assertTrue(decided > 40, "$decided of 60")
    }

    @Test
    fun marginsWorkedOutOnlyAsFarAsTheScanLooksDecideAlike() {
        // `scan-speed-up-4`: the scan works margins out to MARGIN_LOOK only; under it they are the same numbers.
        val random = Random(5)
        val cap = fi.jukkakot.rubikkisolveri.cube.scan.VideoScan.MARGIN_LOOK
        repeat(20) {
            val cube = Cube.solved().apply(Scramble.random(random = random))
            val unseen = (0 until Stickers.COUNT).filter { random.nextDouble() < 0.3 }.toSet()
            val misread = (0 until 3).associate { random.nextInt(Stickers.COUNT) to CubeColor.entries[random.nextInt(6)] }
            val e = evidence(cube, votes = 1 + random.nextInt(6), unseen = unseen, misread = misread)
            val full = assertNotNull(BestCube.solve(e))
            val capped = assertNotNull(BestCube.solve(e, cap = cap))
            assertEquals(full.cube, capped.cube)
            for (s in 0 until Stickers.COUNT) assertEquals(minOf(full.margin(s), cap), minOf(capped.margin(s), cap), 1e-9, "sticker $s")
        }
    }

    @Test
    fun emptyEvidenceIsNotClear() {
        val best = assertNotNull(BestCube.solve(StickerEvidence.EMPTY))
        assertTrue(best.minMargin < 0.01, "${best.minMargin}")
    }
}
