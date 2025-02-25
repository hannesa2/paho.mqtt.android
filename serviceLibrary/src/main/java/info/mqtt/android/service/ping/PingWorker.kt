package info.mqtt.android.service.ping

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import timber.log.Timber
import kotlin.coroutines.resume

class PingWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result =
        suspendCancellableCoroutine { continuation ->
            val key = this.inputData.getString("id");
            Timber.d("${key} Sending Ping at: ${System.currentTimeMillis()}")

            //check if id is not null
            if(key == null) {
                continuation.resume(Result.failure())
                return@suspendCancellableCoroutine
            }

            //check if there is a clients comm asociated with the key
            if(!AlarmPingSender.clientCommsMap.containsKey(key)) {
                continuation.resume(Result.failure())
                return@suspendCancellableCoroutine
            }


            AlarmPingSender.clientCommsMap[key]?.checkForActivity(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("$key Success.")
                    continuation.resume(Result.success())
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e("$key Failure $exception")
                    continuation.resume(Result.failure())
                }
            }) ?: kotlin.run {
                continuation.resume(Result.failure())
            }
        }
}
