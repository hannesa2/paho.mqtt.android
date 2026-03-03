package info.mqtt.android.extsample.repository

import androidx.annotation.WorkerThread
import info.mqtt.android.service.room.PingDao
import info.mqtt.android.service.room.entity.PingEntity
import kotlinx.coroutines.flow.Flow

class PingRepository(private val pingDao: PingDao) {

    val listFlow: Flow<List<PingEntity>> = pingDao.getAll()

    @WorkerThread
    fun insert(pingEntity: PingEntity) {
        pingDao.insert(pingEntity)
    }
}
