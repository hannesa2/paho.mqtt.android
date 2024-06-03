package info.mqtt.android.service.ping

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import info.mqtt.android.service.ping.AlarmPingSender.Companion.sdf
import info.mqtt.android.service.room.entity.PingEntity
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
            val logging = inputData.getBoolean(LOGGING, false)
            val keepRecords = inputData.getInt(KEEP_RECORDS_COUNT, 1000)
            AlarmPingSender.clientComms?.checkForActivity(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Success ${asyncActionToken?.client?.clientId}")
                    if (logging) {
                        val pingMQ = PingEntity(
                            System.currentTimeMillis(),
                            asyncActionToken?.client?.clientId,
                            asyncActionToken?.client?.serverURI,
                            true
                        )
                        AlarmPingSender.messageDatabase?.pingDao()?.insert(pingMQ)
                        AlarmPingSender.messageDatabase?.pingDao()?.removeOldData(keepRecords)
                    }
                    continuation.resume(Result.success())
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.e("Failure $exception ${asyncActionToken?.client?.clientId}")
                    if (logging) {
                        val pingMQ = PingEntity(
                            System.currentTimeMillis(),
                            asyncActionToken?.client?.clientId,
                            asyncActionToken?.client?.serverURI,
                            false,
                            exception?.message
                        )
                        AlarmPingSender.messageDatabase?.pingDao()?.insert(pingMQ)
                        AlarmPingSender.messageDatabase?.pingDao()?.removeOldData(keepRecords)
                    }
                    continuation.resume(Result.failure())
                }
            }) ?: kotlin.run {
                continuation.resume(Result.failure())
            }
        }

    companion object {
        const val LOGGING = "logging"
        const val KEEP_RECORDS_COUNT = "keepCount"
    }
}
