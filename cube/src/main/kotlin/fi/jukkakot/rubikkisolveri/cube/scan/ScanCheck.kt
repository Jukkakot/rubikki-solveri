package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.Validity

/** What the check says once every face is checked. */
sealed interface Verdict {
    data class Solvable(val cube: Cube) : Verdict

    /**
     * The cube cannot be right: look at [faces] again (in face order); [marked] are the most likely
     * wrong stickers, if known. [validity] is the technical reason, for the log.
     */
    data class Impossible(val faces: List<FaceView>, val marked: Set<Int>, val validity: Validity) : Verdict
}

/**
 * The face-by-face check of a scan: the colours, which faces the user has found right, and the
 * stickers to look at per face. [readings] are the 54 camera readings (null after a restart); they
 * rank the likely misreads and classify a rescanned face. Immutable: every operation returns a new
 * check.
 */
data class ScanCheck(
    val editor: CubeEditor,
    val checked: Set<FaceView>,
    val marks: Set<Int>,
    val readings: List<Rgb>? = null,
) {
    val unchecked: List<FaceView> get() = FaceView.entries.filter { it !in checked }

    /** The face to show after [from]: the next unchecked one in face order, wrapping round; null when all are checked. */
    fun nextUnchecked(from: FaceView? = null): FaceView? {
        val left = unchecked
        return left.firstOrNull { from == null || it.ordinal > from.ordinal } ?: left.firstOrNull()
    }

    fun lookRight(view: FaceView): ScanCheck = copy(checked = checked + view)

    /** Paints a sticker; that face's marks clear, its checked state stays. */
    fun paint(index: Int, color: CubeColor): ScanCheck {
        val face = index / 9
        return copy(editor = editor.paint(index, color), marks = marks.filterTo(HashSet()) { it / 9 != face })
    }

    /**
     * The rescanned [view] ([samples], net order) replaces that face: classified against the rest of
     * this cube, unchecked, its doubtful stickers marked. Needs [readings].
     */
    fun replaceFace(view: FaceView, samples: List<Rgb>): ScanCheck {
        val known = requireNotNull(readings) { "No readings to classify against" }
        val face = view.face
        val result = ColorClassifier.classifyFace(face, samples, editor.colors, known, editor.scheme)
        val base = face.ordinal * 9
        val newReadings = known.toMutableList().also { for (n in 0 until 9) it[base + n] = samples[n] }
        return copy(
            editor = editor.withFace(face, result.colors),
            checked = checked - view,
            marks = marks.filterTo(HashSet()) { it / 9 != face.ordinal } + result.uncertain().map { base + it },
            readings = newReadings,
        )
    }

    /** Checks the cube: solvable, or the faces to look at again. */
    fun verdict(scheme: ColorScheme = editor.scheme): Verdict {
        val cube = requireNotNull(editor.toCube()) { "Check of an incomplete cube" }
        val validity = CubeCheck.validity(cube, scheme)
        if (validity.isValid) return Verdict.Solvable(cube)
        MisreadSearch.swaps(cube, readings, scheme).firstOrNull()?.let { swap ->
            return Verdict.Impossible(views(setOf(swap.a / 9, swap.b / 9)), setOf(swap.a, swap.b), validity)
        }
        val concerned = validity.markedStickers
        if (concerned.isNotEmpty()) return Verdict.Impossible(views(concerned.map { it / 9 }.toSet()), concerned, validity)
        return Verdict.Impossible(leastSureFaces(cube), emptySet(), validity)
    }

    /** After an impossible verdict: its faces go back to unchecked and its stickers are marked. */
    fun apply(verdict: Verdict.Impossible): ScanCheck = copy(checked = checked - verdict.faces.toSet(), marks = marks + verdict.marked)

    /** Colours, checked faces and marks as one string, for saving across screen rotation (readings are kept elsewhere). */
    fun encode(): String =
        editor.encode() + "|" + FaceView.entries.joinToString("") { if (it in checked) "1" else "0" } + "|" + marks.sorted().joinToString(",")

    /** The two faces whose readings fit their colours worst (all faces without readings). */
    private fun leastSureFaces(cube: Cube): List<FaceView> {
        val labs = readings?.map { it.toLab() } ?: return FaceView.entries
        val refs = CubeColor.entries.mapNotNull { color ->
            labs.filterIndexed { i, _ -> cube[i] == color }.takeIf { it.isNotEmpty() }?.let { color to Lab.mean(it) }
        }.toMap()
        fun sureness(i: Int): Double {
            val d = refs.values.map { it.distance(labs[i]) }.sorted()
            val own = refs.getValue(cube[i]).distance(labs[i])
            val other = if (own <= d[0]) d.getOrElse(1) { own } else d[0]
            return if (own + other == 0.0) 0.0 else (other - own) / (own + other)
        }
        val worst = Face.entries.sortedBy { face ->
            (0 until 9).filter { it != 4 }.map { sureness(face.ordinal * 9 + it) }.average()
        }.take(2)
        return views(worst.map { it.ordinal }.toSet())
    }

    private fun views(faces: Set<Int>): List<FaceView> = FaceView.entries.filter { it.face.ordinal in faces }

    companion object {
        /** The check right after a scan: faces without a marked sticker start as checked. */
        fun start(outcome: ScanOutcome): ScanCheck = start(outcome.editor, outcome.marked, outcome.samples)

        fun start(editor: CubeEditor, marked: Set<Int>, readings: List<Rgb>?): ScanCheck {
            val checked = FaceView.entries.filter { view -> marked.none { it / 9 == view.face.ordinal } }.toSet()
            return ScanCheck(editor, checked, marked, readings?.takeIf { it.size == Stickers.COUNT })
        }

        fun decode(text: String, readings: List<Rgb>? = null): ScanCheck? {
            val parts = text.split('|')
            if (parts.size != 3 || parts[1].length != FaceView.entries.size) return null
            val editor = CubeEditor.decode(parts[0]) ?: return null
            val checked = FaceView.entries.filter { parts[1][it.ordinal] == '1' }.toSet()
            val marks = parts[2].split(',').mapNotNull { it.toIntOrNull() }.toSet()
            return ScanCheck(editor, checked, marks, readings?.takeIf { it.size == Stickers.COUNT })
        }
    }
}
