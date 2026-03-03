package info.mqtt.android.service.room

import androidx.room.*
import info.mqtt.android.service.room.entity.PingEntity
import kotlinx.coroutines.flow.Flow

@Suppress("KotlinRedundantDiagnosticSuppress", "UNCHECKED_CAST")
@Dao
interface PingDao {

    @Query("SELECT * FROM PingEntity ORDER BY timestamp ASC")
    fun getAll(): Flow<List<PingEntity>>

    @Query("SELECT * FROM PingEntity WHERE success = :statePing ORDER BY timestamp ASC")
    fun allByState(statePing: Boolean): Flow<List<PingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(pingEntity: PingEntity): Long

    @Update
    fun updateAll(vararg pingEntities: PingEntity)

    @Delete
    fun delete(pingEntity: PingEntity)

    @Query("DELETE FROM PingEntity WHERE success = :statePing")
    fun deleteState(statePing: Boolean): Int

    @Query("DELETE FROM PingEntity WHERE timeStamp IN (SELECT timeStamp FROM PingEntity ORDER BY timeStamp DESC LIMIT 1 OFFSET :keepCount)")
    fun removeOldData(keepCount: Int)

}
