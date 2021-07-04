package info.mqtt.android.service.ping

import android.os.SystemClock
import androidx.work.*
import info.mqtt.android.service.MqttService
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * This class implements the [MqttPingSender] ping interface
 * allowing applications to send ping packet to server every keep alive interval.
 *
 * @see MqttPingSender
 */
internal class AlarmPingSender(val service: MqttService) : MqttPingSender {
    private var continuation: Operation? = null
    private var clientComms: ClientComms? = null
    private val workManager = WorkManager.getInstance(service)

    override fun init(comms: ClientComms) {
        this.clientComms = comms
    }

    override fun start() {
        val pingRepeatWorkRequest = PeriodicWorkRequest
            .Builder(PingWorker::class.java, clientComms!!.keepAlive, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.METERED)
                    .build()
            )
            .build()

        continuation = workManager.enqueueUniquePeriodicWork(PING_JOB, ExistingPeriodicWorkPolicy.REPLACE, pingRepeatWorkRequest)
    }

    override fun stop() {
        Timber.d("Unregister AlarmReceiver to MqttService ${clientComms!!.client.clientId}")
        workManager.cancelUniqueWork(PING_JOB)
    }

    override fun schedule(delayInMilliseconds: Long) {
        val nextAlarmInMilliseconds = SystemClock.elapsedRealtime() + delayInMilliseconds
        Timber.d("Pointless Schedule next alarm at $nextAlarmInMilliseconds ms")
    }

    companion object {
        private const val PING_JOB = "PING_JOB"
    }

}
