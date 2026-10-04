package fi.jukkakot.rubikkisolveri.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.time.Clock

@Serializable
data class TimedSolve(
    val id: Long = 0,
    val finishedAt: Long,
    val millis: Long,
    val penalty: Penalty = Penalty.NONE,
    val scramble: String,
) {
    val result: TimedResult get() = TimedResult(millis, penalty)
}

@Serializable
data class GuidedSolve(
    val id: Long = 0,
    val finishedAt: Long,
    val method: String,
    val moves: Int,
    val durationMillis: Long,
)

@Serializable
data class PracticeSession(
    val id: Long = 0,
    val finishedAt: Long,
    val stage: Int,
    val durationMillis: Long,
)

/** The user's solves and practice, stored on the device. */
interface ProgressRepository {
    val timedSolves: Flow<List<TimedSolve>>
    val guidedSolves: Flow<List<GuidedSolve>>
    val practiceSessions: Flow<List<PracticeSession>>

    /** Stage ordinal → times practised to the end. */
    val practiceCounts: Flow<Map<Int, Int>>

    suspend fun addTimed(millis: Long, scramble: String, finishedAt: Long = now()): Long
    suspend fun setPenalty(solve: TimedSolve, penalty: Penalty)
    suspend fun deleteTimed(id: Long)
    suspend fun addGuided(method: String, moves: Int, durationMillis: Long, finishedAt: Long = now())
    suspend fun addPractice(stage: Int, durationMillis: Long, finishedAt: Long = now())
}

private fun now() = Clock.System.now().toEpochMilliseconds()
