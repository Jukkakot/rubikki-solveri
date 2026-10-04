package fi.jukkakot.rubikkisolveri.progress

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Table and column names are the schema (app/schemas): they must not change without a migration.

@Entity(tableName = "timed_solve")
data class TimedSolveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val millis: Long,
    val penalty: Penalty = Penalty.NONE,
    val scramble: String,
) {
    fun toModel() = TimedSolve(id, finishedAt, millis, penalty, scramble)

    companion object {
        fun of(s: TimedSolve) = TimedSolveEntity(s.id, s.finishedAt, s.millis, s.penalty, s.scramble)
    }
}

@Entity(tableName = "guided_solve")
data class GuidedSolveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val method: String,
    val moves: Int,
    val durationMillis: Long,
) {
    fun toModel() = GuidedSolve(id, finishedAt, method, moves, durationMillis)
}

@Entity(tableName = "practice")
data class PracticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val stage: Int,
    val durationMillis: Long,
) {
    fun toModel() = PracticeSession(id, finishedAt, stage, durationMillis)
}

class Converters {
    @TypeConverter
    fun penaltyToText(p: Penalty): String = p.name

    @TypeConverter
    fun textToPenalty(s: String): Penalty = Penalty.valueOf(s)
}

/** One row per stage that has been practised: how many times. */
data class StageCount(val stage: Int, val count: Int)

@Dao
interface ProgressDao {
    @Insert
    suspend fun insert(solve: TimedSolveEntity): Long

    @Update
    suspend fun update(solve: TimedSolveEntity)

    @Query("DELETE FROM timed_solve WHERE id = :id")
    suspend fun deleteTimed(id: Long)

    @Query("SELECT * FROM timed_solve ORDER BY finishedAt DESC, id DESC")
    fun timedSolves(): Flow<List<TimedSolveEntity>>

    @Insert
    suspend fun insert(solve: GuidedSolveEntity): Long

    @Query("SELECT * FROM guided_solve ORDER BY finishedAt DESC, id DESC")
    fun guidedSolves(): Flow<List<GuidedSolveEntity>>

    @Insert
    suspend fun insert(session: PracticeEntity): Long

    @Query("SELECT * FROM practice ORDER BY finishedAt DESC, id DESC")
    fun practiceSessions(): Flow<List<PracticeEntity>>

    @Query("SELECT stage, COUNT(*) AS count FROM practice GROUP BY stage")
    fun practiceCounts(): Flow<List<StageCount>>
}

@Database(entities = [TimedSolveEntity::class, GuidedSolveEntity::class, PracticeEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun dao(): ProgressDao

    companion object {
        @Volatile
        private var instance: ProgressDatabase? = null

        fun get(context: Context): ProgressDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ProgressDatabase::class.java, "progress.db")
                .build().also { instance = it }
        }
    }
}

/** The phone's [ProgressRepository]: Room rows mapped to the shared model. */
class RoomProgressRepository(private val dao: ProgressDao) : ProgressRepository {
    override val timedSolves = dao.timedSolves().map { rows -> rows.map { it.toModel() } }
    override val guidedSolves = dao.guidedSolves().map { rows -> rows.map { it.toModel() } }
    override val practiceSessions = dao.practiceSessions().map { rows -> rows.map { it.toModel() } }
    override val practiceCounts = dao.practiceCounts().map { rows -> rows.associate { it.stage to it.count } }

    override suspend fun addTimed(millis: Long, scramble: String, finishedAt: Long) =
        dao.insert(TimedSolveEntity(finishedAt = finishedAt, millis = millis, scramble = scramble))

    override suspend fun setPenalty(solve: TimedSolve, penalty: Penalty) =
        dao.update(TimedSolveEntity.of(solve.copy(penalty = penalty)))

    override suspend fun deleteTimed(id: Long) = dao.deleteTimed(id)

    override suspend fun addGuided(method: String, moves: Int, durationMillis: Long, finishedAt: Long) {
        dao.insert(GuidedSolveEntity(finishedAt = finishedAt, method = method, moves = moves, durationMillis = durationMillis))
    }

    override suspend fun addPractice(stage: Int, durationMillis: Long, finishedAt: Long) {
        dao.insert(PracticeEntity(finishedAt = finishedAt, stage = stage, durationMillis = durationMillis))
    }
}
