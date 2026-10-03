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
 * sticker indices to mark, [fromScan] when a scan hands over its result for checking.
 */
@Serializable
data class ManualInputRoute(val cube: String? = null, val marked: String? = null, val fromScan: Boolean = false)

@Serializable
data object ScanRoute

/** [cube] is a colour string (see Cube.toColorString), or null for a solved cube. */
@Serializable
data class FreeCubeRoute(val cube: String? = null)

/** [cube] is a colour string (see Cube.toColorString). */
@Serializable
data class SolveRoute(val cube: String)

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
