package info.mqtt.android.extsample.repository

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import info.mqtt.android.service.room.MqMessageDatabase
import info.mqtt.android.service.room.entity.PingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class PingViewModel(application: Application) : AndroidViewModel(application) {

    private var parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Main
    private val scope = CoroutineScope(coroutineContext)

    private val repository: PingRepository
    val listLiveData: LiveData<List<PingEntity>>

    init {
        val pingDao = MqMessageDatabase.getDatabase(application).pingDao() // TODO scope
        repository = PingRepository(pingDao)
        listLiveData = repository.listLiveData
    }

    fun insert(pingEntity: PingEntity) = scope.launch(Dispatchers.IO) {
        repository.insert(pingEntity)
    }

    override fun onCleared() {
        super.onCleared()
        parentJob.cancel()
    }
}
