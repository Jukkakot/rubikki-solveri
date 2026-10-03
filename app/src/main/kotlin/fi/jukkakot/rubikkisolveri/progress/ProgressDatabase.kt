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

@Entity(tableName = "timed_solve")
data class TimedSolve(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val millis: Long,
    val penalty: Penalty = Penalty.NONE,
    val scramble: String,
) {
    val result: TimedResult get() = TimedResult(millis, penalty)
}

@Entity(tableName = "guided_solve")
data class GuidedSolve(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val method: String,
    val moves: Int,
    val durationMillis: Long,
)

@Entity(tableName = "practice")
data class PracticeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val stage: Int,
    val durationMillis: Long,
)

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
    suspend fun insert(solve: TimedSolve): Long

    @Update
    suspend fun update(solve: TimedSolve)

    @Query("DELETE FROM timed_solve WHERE id = :id")
    suspend fun deleteTimed(id: Long)

    @Query("SELECT * FROM timed_solve ORDER BY finishedAt DESC, id DESC")
    fun timedSolves(): Flow<List<TimedSolve>>

    @Insert
    suspend fun insert(solve: GuidedSolve): Long

    @Query("SELECT * FROM guided_solve ORDER BY finishedAt DESC, id DESC")
    fun guidedSolves(): Flow<List<GuidedSolve>>

    @Insert
    suspend fun insert(session: PracticeSession): Long

    @Query("SELECT * FROM practice ORDER BY finishedAt DESC, id DESC")
    fun practiceSessions(): Flow<List<PracticeSession>>

    @Query("SELECT stage, COUNT(*) AS count FROM practice GROUP BY stage")
    fun practiceCounts(): Flow<List<StageCount>>
}

@Database(entities = [TimedSolve::class, GuidedSolve::class, PracticeSession::class], version = 1, exportSchema = true)
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
