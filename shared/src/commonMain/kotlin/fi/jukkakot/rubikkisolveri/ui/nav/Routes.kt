package fi.jukkakot.rubikkisolveri.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Serializable
data object LogRoute

/**
 * Manual input, empty or prefilled: [cube] is a `CubeEditor.encode()` string, [marked] comma-separated
 * sticker indices to mark, [fromScan] when a scan hands over its result for checking, [confident]
 * when that scan was valid and sure (the check then goes on to the solution by itself).
 * With [targetStart] (a colour string) it paints a target for that starting cube instead
 * (`solve-to-target`); [targetFromSolve] as in [TargetRoute]. [replaceSolve]: opened from a solution's
 * menu, whose solution the checked cube replaces. [target] (a `SolveTarget.encode()` string) is where
 * the checked cube's solution leads, carried from a scan started from the target picker.
 */
@Serializable
data class ManualInputRoute(
    val cube: String? = null,
    val marked: String? = null,
    val fromScan: Boolean = false,
    val confident: Boolean = false,
    val targetStart: String? = null,
    val targetFromSolve: Boolean = false,
    val replaceSolve: Boolean = false,
    val target: String? = null,
)

/**
 * The scan; with [face] (a `FaceView` name) only that face, rescanned from the check. [target] as in
 * [ManualInputRoute], passed on to the solution.
 */
@Serializable
data class ScanRoute(val face: String? = null, val target: String? = null)

/** [cube] is a colour string (see Cube.toColorString), or null for a solved cube. */
@Serializable
data class FreeCubeRoute(val cube: String? = null)

/**
 * [cube] is a colour string (see Cube.toColorString); [target] a `SolveTarget.encode()` string (null:
 * solved); [fromScan] when a scan found the cube (its check is then the scan's, see `LastScan.check`).
 */
@Serializable
data class SolveRoute(val cube: String, val target: String? = null, val fromScan: Boolean = false)

/**
 * Choosing a target for [start] (colour string); [current] is the target now. [fromSolve]: opened
 * from a solution screen, which the chosen target's solution replaces.
 */
@Serializable
data class TargetRoute(val start: String, val current: String? = null, val fromSolve: Boolean = false)

@Serializable
data object LessonsRoute

/** [index] 0 = basics, 1..7 = the beginner stages. */
@Serializable
data class LessonRoute(val index: Int)

/** Practise beginner stage [stage] (its ordinal) on the position made from [seed]. */
@Serializable
data class PracticeRoute(val stage: Int, val seed: Long)

@Serializable
data object TimerRoute

@Serializable
data object HistoryRoute

/** Perform the scramble [moves] (notation) with the move guide. */
@Serializable
data class ScrambleGuideRoute(val moves: String)

@Serializable
data object AboutRoute

/** The scan from video (`video-scan`), the default scan; [target] as in [ManualInputRoute]. */
@Serializable
data class VideoScanRoute(val target: String? = null)
