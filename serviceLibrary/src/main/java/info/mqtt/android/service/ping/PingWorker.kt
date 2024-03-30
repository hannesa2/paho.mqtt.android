package info.mqtt.android.service.ping

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import info.mqtt.android.service.ping.AlarmPingSender.Companion.sdf
import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import timber.log.Timber
import java.util.Date
import kotlin.coroutines.resume

class PingWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result =
        suspendCancellableCoroutine { continuation ->
            Timber.d("Sending Ping at: ${sdf.format(Date(System.currentTimeMillis()))}")
            AlarmPingSender.clientComms?.checkForActivity(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Success.")
                    continuation.resume(Result.success())
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e("Failure $exception")
                    continuation.resume(Result.failure())
                }
            }) ?: kotlin.run {
                continuation.resume(Result.failure())
            }
        }
}
