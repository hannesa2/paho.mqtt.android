package info.mqtt.android.service.ping

import android.annotation.SuppressLint
import androidx.work.*
import info.mqtt.android.service.MqttService
import info.mqtt.android.service.room.MqMessageDatabase
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit



/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * This class implements the [MqttPingSender] ping interface
 * allowing applications to send ping packet to server every keep alive interval.
 *
 * @see MqttPingSender
 */
internal class AlarmPingSender(
    val service: MqttService,
    val id: String,
    private val pingLogging: Boolean = false,
    private val keepPingRecords: Int = 1000
) : MqttPingSender {

    private val workManager = WorkManager.getInstance(service)

    override fun init(comms: ClientComms) {
        clientCommsMap[id] = comms
        messageDatabase = service.messageDatabase
        Timber.w("Init ping job $id")
    }

    override fun start() {
        Timber.d("Start ping job $id")
        clientCommsMap[id]?.clientState?.let {
            schedule(clientCommsMap[id]!!.keepAlive)
        } ?: Timber.e("FIXME: try to start ping schedule, but clientState null, not able to get keepAlive")
    }

    override fun stop() {
        //remove the clientComms from the map
        Timber.d("Stop ping job $id")
        workManager.cancelAllWorkByTag("${PING_JOB}_$id")
    }

    override fun schedule(delayInMilliseconds: Long) {
        Timber.d("$id: Schedule next alarm at ${sdf.format(Date(System.currentTimeMillis() + delayInMilliseconds))}")

        val pingWork = OneTimeWorkRequest.Builder(PingWorker::class.java)
        val data = Data.Builder()
        data.putBoolean("logging", pingLogging)
        data.putInt("keepRecordCount", keepPingRecords)
        data.putString("id", id)

        pingWork
            .setInitialDelay(delayInMilliseconds, TimeUnit.MILLISECONDS)
            .setInputData(data.build())
            .addTag("${PING_JOB}_$id")

        // we add the currentTimeMillis to keep the prev job running
        val uniqueWorkName = "${PING_JOB}_${id}_${System.currentTimeMillis()}"

        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            pingWork.build()
        )



        Timber.d("$id: Successfully scheduled new ping job")
    }

    companion object {
        internal var clientCommsMap: ConcurrentHashMap<String, ClientComms> = ConcurrentHashMap()

        private const val PING_JOB = "PING_JOB"

        internal var messageDatabase: MqMessageDatabase? = null

        @SuppressLint("ConstantLocale")
        internal val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.getDefault())
    }

}
