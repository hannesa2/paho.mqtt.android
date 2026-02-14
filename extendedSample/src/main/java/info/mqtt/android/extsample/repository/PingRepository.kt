package info.mqtt.android.extsample.repository

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import info.mqtt.android.service.room.PingDao
import info.mqtt.android.service.room.entity.PingEntity

class PingRepository(private val pingDao: PingDao) {

    val listLiveData: LiveData<List<PingEntity>> = pingDao.getAll()

    @WorkerThread
    fun insert(pingEntity: PingEntity) {
        pingDao.insert(pingEntity)
    }
}
