package fi.jukkakot.rubikkisolveri.progress

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A repository kept in memory: for previews and tests. */
class InMemoryProgressRepository : ProgressRepository {
    private var nextId = 1L
    private val timed = MutableStateFlow<List<TimedSolve>>(emptyList())
    private val guided = MutableStateFlow<List<GuidedSolve>>(emptyList())
    private val practice = MutableStateFlow<List<PracticeSession>>(emptyList())

    override val timedSolves = timed
    override val guidedSolves = guided
    override val practiceSessions = practice
    override val practiceCounts = practice.map { list -> list.groupingBy { it.stage }.eachCount() }

    override suspend fun addTimed(millis: Long, scramble: String, finishedAt: Long): Long {
        val id = nextId++
        timed.value = listOf(TimedSolve(id, finishedAt, millis, Penalty.NONE, scramble)) + timed.value
        return id
    }

    override suspend fun setPenalty(solve: TimedSolve, penalty: Penalty) {
        timed.value = timed.value.map { if (it.id == solve.id) it.copy(penalty = penalty) else it }
    }

    override suspend fun deleteTimed(id: Long) {
        timed.value = timed.value.filter { it.id != id }
    }

    override suspend fun addGuided(method: String, moves: Int, durationMillis: Long, finishedAt: Long) {
        guided.value = listOf(GuidedSolve(nextId++, finishedAt, method, moves, durationMillis)) + guided.value
    }

    override suspend fun addPractice(stage: Int, durationMillis: Long, finishedAt: Long) {
        practice.value = listOf(PracticeSession(nextId++, finishedAt, stage, durationMillis)) + practice.value
    }
}
