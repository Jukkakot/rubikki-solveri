package fi.jukkakot.rubikkisolveri.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The user's solves and practice, stored on the phone. */
interface ProgressRepository {
    val timedSolves: Flow<List<TimedSolve>>
    val guidedSolves: Flow<List<GuidedSolve>>
    val practiceSessions: Flow<List<PracticeSession>>

    /** Stage ordinal → times practised to the end. */
    val practiceCounts: Flow<Map<Int, Int>>

    suspend fun addTimed(millis: Long, scramble: String, finishedAt: Long = System.currentTimeMillis()): Long
    suspend fun setPenalty(solve: TimedSolve, penalty: Penalty)
    suspend fun deleteTimed(id: Long)
    suspend fun addGuided(method: String, moves: Int, durationMillis: Long, finishedAt: Long = System.currentTimeMillis())
    suspend fun addPractice(stage: Int, durationMillis: Long, finishedAt: Long = System.currentTimeMillis())
}

class RoomProgressRepository(private val dao: ProgressDao) : ProgressRepository {
    override val timedSolves = dao.timedSolves()
    override val guidedSolves = dao.guidedSolves()
    override val practiceSessions = dao.practiceSessions()
    override val practiceCounts = dao.practiceCounts().map { rows -> rows.associate { it.stage to it.count } }

    override suspend fun addTimed(millis: Long, scramble: String, finishedAt: Long) =
        dao.insert(TimedSolve(finishedAt = finishedAt, millis = millis, scramble = scramble))

    override suspend fun setPenalty(solve: TimedSolve, penalty: Penalty) = dao.update(solve.copy(penalty = penalty))

    override suspend fun deleteTimed(id: Long) = dao.deleteTimed(id)

    override suspend fun addGuided(method: String, moves: Int, durationMillis: Long, finishedAt: Long) {
        dao.insert(GuidedSolve(finishedAt = finishedAt, method = method, moves = moves, durationMillis = durationMillis))
    }

    override suspend fun addPractice(stage: Int, durationMillis: Long, finishedAt: Long) {
        dao.insert(PracticeSession(finishedAt = finishedAt, stage = stage, durationMillis = durationMillis))
    }
}
