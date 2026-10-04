package fi.jukkakot.rubikkisolveri.store

import fi.jukkakot.rubikkisolveri.progress.GuidedSolve
import fi.jukkakot.rubikkisolveri.progress.Penalty
import fi.jukkakot.rubikkisolveri.progress.PracticeSession
import fi.jukkakot.rubikkisolveri.progress.ProgressRepository
import fi.jukkakot.rubikkisolveri.progress.TimedSolve
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Solves and practice as one JSON object under [StoreKeys.PROGRESS], rewritten after each change
 * (a few hundred solves are a few tens of kB). Lists are kept newest first, as Room returns them.
 */
class StoredProgressRepository(private val store: KeyValueStore) : ProgressRepository {

    @Serializable
    private data class Data(
        val nextId: Long = 1,
        val timed: List<TimedSolve> = emptyList(),
        val guided: List<GuidedSolve> = emptyList(),
        val practice: List<PracticeSession> = emptyList(),
    )

    private val state = MutableStateFlow(load())

    override val timedSolves = state.map { it.timed }
    override val guidedSolves = state.map { it.guided }
    override val practiceSessions = state.map { it.practice }
    override val practiceCounts = state.map { d -> d.practice.groupingBy { it.stage }.eachCount() }

    private fun load(): Data = store.get(StoreKeys.PROGRESS)
        ?.let { runCatching { JSON.decodeFromString(Data.serializer(), it) }.getOrNull() }
        ?: Data()

    private fun update(change: (Data) -> Data) {
        val next = change(state.value)
        state.value = next
        store.set(StoreKeys.PROGRESS, JSON.encodeToString(Data.serializer(), next))
    }

    override suspend fun addTimed(millis: Long, scramble: String, finishedAt: Long): Long {
        val id = state.value.nextId
        update { it.copy(nextId = id + 1, timed = sorted(listOf(TimedSolve(id, finishedAt, millis, Penalty.NONE, scramble)) + it.timed)) }
        return id
    }

    override suspend fun setPenalty(solve: TimedSolve, penalty: Penalty) =
        update { d -> d.copy(timed = d.timed.map { if (it.id == solve.id) it.copy(penalty = penalty) else it }) }

    override suspend fun deleteTimed(id: Long) = update { d -> d.copy(timed = d.timed.filter { it.id != id }) }

    override suspend fun addGuided(method: String, moves: Int, durationMillis: Long, finishedAt: Long) {
        val id = state.value.nextId
        update { d -> d.copy(nextId = id + 1, guided = listOf(GuidedSolve(id, finishedAt, method, moves, durationMillis)) + d.guided) }
    }

    override suspend fun addPractice(stage: Int, durationMillis: Long, finishedAt: Long) {
        val id = state.value.nextId
        update { d -> d.copy(nextId = id + 1, practice = listOf(PracticeSession(id, finishedAt, stage, durationMillis)) + d.practice) }
    }

    private fun sorted(list: List<TimedSolve>) = list.sortedWith(compareByDescending<TimedSolve> { it.finishedAt }.thenByDescending { it.id })

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
