package info.mqtt.android.service.ping

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import info.mqtt.android.service.MqttService
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender(val service: MqttService) : MqttPingSender {

    private var comms: ClientComms? = null
    private val workManager = WorkManager.getInstance(service)

    companion object {
        // Identifier for Intents, log messages, etc..
        const val TAG = "AlarmPingSender"
        const val PING_JOB = "PING_JOB"
    }

    override fun init(comms: ClientComms?) {
        this.comms = comms
    }

    override fun start() {
        comms?.let { clientComms ->
            schedule(clientComms.keepAlive)
        }
    }

    override fun stop() {
        workManager.cancelUniqueWork(PING_JOB)
    }

    override fun schedule(delayInMilliseconds: Long) {
        Timber.d("Schedule next alarm at " + System.currentTimeMillis() + delayInMilliseconds)
        workManager.enqueueUniqueWork(
            PING_JOB,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest
                .Builder(PingWorker::class.java)
                .setInitialDelay(delayInMilliseconds, TimeUnit.MILLISECONDS)
                .build()
        )
    }

    inner class PingWorker(context: Context, workerParams: WorkerParameters) :
        CoroutineWorker(context, workerParams) {
        override suspend fun doWork(): Result =
            suspendCancellableCoroutine { continuation ->
                Timber.d(TAG, "Sending Ping at: ${System.currentTimeMillis()}")
                comms?.checkForActivity(object : IMqttActionListener{
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Timber.d(TAG, "Success.")
                        continuation.resume(Result.success())
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Timber.d(TAG, "Failure.")
                        continuation.resume(Result.failure())
                    }
                }) ?: kotlin.run {
                    continuation.resume(Result.failure())
                }
            }
    }
}