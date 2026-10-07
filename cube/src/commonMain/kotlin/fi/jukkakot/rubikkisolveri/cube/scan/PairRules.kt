package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Face

/**
 * A track's face and turn as one number: `face.ordinal * 4 + turn`, where the track index of the
 * face's net index n is `RotationSearch.turnIndex(n, turn)`; [NONE]: no face (a stray lattice).
 */
object FaceOption {
    const val FACES = 24
    const val COUNT = 25
    const val NONE = 24

    fun of(face: Face, turn: Int): Int = face.ordinal * 4 + turn.mod(4)
    fun face(option: Int): Face = Face.entries[option / 4]
    fun turn(option: Int): Int = option % 4
}

/**
 * The hard rules one picture gives for two tracks seen in it (`scan-rules` design 3): they are
 * different faces that are neighbours on the cube (never opposite colours); where the picture shows
 * them touching across a side, each lies across that side of the other in the net, which fixes their
 * turns; round a corner, their corner stickers and which follows which clockwise fix faces, turns
 * and handedness. A side or corner counts once seen [MIN_SEEN] times and in at least [SHARE] of the
 * pictures that showed one, so one bad lattice does not make a rule.
 */
class PairRules {
    private class Rule {
        val sideA = IntArray(4)
        val sideB = IntArray(4)
        val corners = HashMap<Int, Int>()
        var allowed: BooleanArray? = null
    }

    private val rules = HashMap<Long, Rule>()

    /** Records what one picture shows: its full readings with their tracks. */
    fun observe(picture: List<Pair<Track, TrackReading>>) {
        val full = picture.filter { it.second.face.isFull }
        val faces = full.map { it.second.face }
        for (i in full.indices) for (j in full.indices) {
            if (i >= j || full[i].first === full[j].first) continue
            val (a, b) = if (full[i].first.id < full[j].first.id) i to j else j to i
            val (ta, ra) = full[a]
            val (tb, rb) = full[b]
            val rule = rules.getOrPut(key(ta, tb)) { Rule() }
            faces[a].sideTowards(faces[b])?.let { rule.sideA[(it + ra.turn) % 4]++ }
            faces[b].sideTowards(faces[a])?.let { rule.sideB[(it + rb.turn) % 4]++ }
            rule.allowed = null
        }
        for (view in CornerReader.views(faces)) {
            for (x in 0 until 3) for (y in 0 until 3) {
                if (x == y) continue
                val i = view.faces[x]
                val j = view.faces[y]
                if (full[i].first.id > full[j].first.id || full[i].first === full[j].first) continue
                val (ta, ra) = full[i]
                val (tb, rb) = full[j]
                val after = y == (x + 1) % 3
                val code = cornerCode(trackIndex(view.stickers[x], ra.turn), trackIndex(view.stickers[y], rb.turn), after)
                val rule = rules.getOrPut(key(ta, tb)) { Rule() }
                rule.corners[code] = (rule.corners[code] ?: 0) + 1
                rule.allowed = null
            }
        }
    }

    /** Whether [a] and [b] were ever seen in one picture. */
    fun together(a: Track, b: Track): Boolean = rules.containsKey(key(a, b))

    /**
     * Which options of [a] and [b] the rules allow together, indexed `optionA * COUNT + optionB`; null
     * without any rule (never seen together).
     */
    fun allowed(a: Track, b: Track): BooleanArray? {
        if (a === b) return null
        val rule = rules[key(a, b)] ?: return null
        val table = rule.allowed ?: build(rule).also { rule.allowed = it }
        if (a.id < b.id) return table
        return BooleanArray(FaceOption.COUNT * FaceOption.COUNT) { k -> table[(k % FaceOption.COUNT) * FaceOption.COUNT + k / FaceOption.COUNT] }
    }

    fun clear() = rules.clear()

    private fun build(rule: Rule): BooleanArray {
        val sideA = dominant(rule.sideA.withIndex().associate { it.index to it.value })
        val sideB = dominant(rule.sideB.withIndex().associate { it.index to it.value })
        val corner = dominant(rule.corners)
        val n = FaceOption.COUNT
        return BooleanArray(n * n) { k ->
            val oa = k / n
            val ob = k % n
            if (oa == FaceOption.NONE || ob == FaceOption.NONE) return@BooleanArray true
            val fa = FaceOption.face(oa)
            val fb = FaceOption.face(ob)
            val ta = FaceOption.turn(oa)
            val tb = FaceOption.turn(ob)
            if (fa == fb || fa.opposite == fb) return@BooleanArray false
            if (sideA != null && VideoScan.neighbourAt(fa, sideA + ta) != fb) return@BooleanArray false
            if (sideB != null && VideoScan.neighbourAt(fb, sideB + tb) != fa) return@BooleanArray false
            corner == null || cornerAllows(corner, fa, ta, fb, tb)
        }
    }

    private fun dominant(counts: Map<Int, Int>): Int? {
        val total = counts.values.sum()
        val top = counts.maxByOrNull { it.value } ?: return null
        return top.key.takeIf { top.value >= MIN_SEEN && top.value >= SHARE * total }
    }

    companion object {
        /** Pictures a side or corner must be seen in before it is a rule. */
        const val MIN_SEEN = 2

        /** Share of the pictures showing a side (or corner) that must agree on it. */
        const val SHARE = 0.75

        private fun key(a: Track, b: Track): Long = if (a.id < b.id) a.id.toLong() * 1_000_000 + b.id else b.id.toLong() * 1_000_000 + a.id

        /** The track index whose reading index, read turned [turn], is [reading]. */
        private fun trackIndex(reading: Int, turn: Int): Int = (0 until 9).first { RotationSearch.turnIndex(it, turn) == reading }

        private fun cornerCode(stickerA: Int, stickerB: Int, after: Boolean): Int = (stickerA * 9 + stickerB) * 2 + if (after) 1 else 0

        /**
         * Whether face [fa] turned [ta] and [fb] turned [tb] can show the corner [code]: some corner of
         * the cube has both faces, [fb] following [fa] clockwise as the code says, each face's sticker at
         * it lying at the code's track index.
         */
        private fun cornerAllows(code: Int, fa: Face, ta: Int, fb: Face, tb: Int): Boolean {
            val after = code % 2 == 1
            val stickerA = code / 2 / 9
            val stickerB = code / 2 % 9
            return Corner.entries.any { piece ->
                val ia = piece.faces.indexOf(fa)
                val ib = piece.faces.indexOf(fb)
                ia >= 0 && ib >= 0 && (ib == (ia + 1) % 3) == after &&
                    RotationSearch.turnIndex(piece.stickers[ia] % 9, ta) == stickerA &&
                    RotationSearch.turnIndex(piece.stickers[ib] % 9, tb) == stickerB
            }
        }
    }
}
