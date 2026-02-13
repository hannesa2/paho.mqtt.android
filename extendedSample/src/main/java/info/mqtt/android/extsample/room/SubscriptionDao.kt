package info.mqtt.android.extsample.room

import androidx.room.*
import info.mqtt.android.extsample.room.entity.SubscriptionEntity

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM SubscriptionEntity")
    fun getAll(): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(subscriptionEntity: SubscriptionEntity): Long

    @Update
    fun updateAll(vararg entities: SubscriptionEntity)

    @Delete
    fun delete(subscriptionEntity: SubscriptionEntity)
}
