package fi.jukkakot.rubikkisolveri.progress

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ProgressDatabaseTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), ProgressDatabase::class.java)
        .allowMainThreadQueries().build()
    private val repo = RoomProgressRepository(db.dao())

    @After
    fun close() = db.close()

    @Test
    fun writesAndReadsEveryTable() = runTest {
        repo.addTimed(12_340, "R U F", finishedAt = 1)
        repo.addTimed(10_000, "L D B", finishedAt = 2)
        val timed = repo.timedSolves.first()
        assertEquals(listOf(10_000L, 12_340L), timed.map { it.millis }, "newest first")
        repo.setPenalty(timed[1], Penalty.PLUS_TWO)
        assertEquals(14_340L, repo.timedSolves.first()[1].result.effective)
        repo.deleteTimed(timed[0].id)
        assertEquals(1, repo.timedSolves.first().size)

        repo.addGuided("LEARN", 150, 600_000)
        assertEquals(150, repo.guidedSolves.first().single().moves)

        repo.addPractice(3, 60_000)
        repo.addPractice(3, 50_000)
        repo.addPractice(1, 40_000)
        assertEquals(mapOf(3 to 2, 1 to 1), repo.practiceCounts.first())
        assertEquals(3, repo.practiceSessions.first().size)
    }
}
