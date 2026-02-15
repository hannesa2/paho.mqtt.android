package info.mqtt.android.extsample.room

import androidx.room.*
import info.mqtt.android.extsample.room.entity.ConnectionEntity

@Suppress("KotlinRedundantDiagnosticSuppress", "UNCHECKED_CAST")
@Dao
interface PersistenceDao {

    @Query("SELECT * FROM ConnectionEntity")
    fun getAll(): List<ConnectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(connectionEntity: ConnectionEntity): Long

    @Update
    fun updateAll(vararg connectionEntities: ConnectionEntity)

    @Delete
    fun delete(connectionEntity: ConnectionEntity)
}
