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
import java.util.UUID
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

    private var comms: ClientComms? = null

    /**
     * Identifies the ping jobs scheduled by this sender. A new process (or a new sender for the
     * same client id) gets a new session, so [PingWorker] can tell its own jobs from leftovers.
     */
    private val session = UUID.randomUUID().toString()

    override fun init(comms: ClientComms) {
        // Runs from the ClientComms constructor, before connect(), so this can't hit a live chain.
        // Clears jobs a killed process left behind for this client id.
        workManager.cancelAllWorkByTag("${PING_JOB}_$id")
        this.comms = comms
        clientCommsMap[id] = comms
        activeSessions[id] = session
        messageDatabase = service.messageDatabase
        Timber.w("Init ping job $id")
    }

    override fun start() {
        Timber.d("Start ping job $id")
        clientCommsMap[id]?.clientState?.let {
            schedule(clientCommsMap[id]!!.keepAlive)
        } ?: Timber.e("FIXME: try to start ping schedule, but clientState null, not able to get keepAlive")
    }

    /**
     * The connection was shut down for good. Drops this sender's ClientComms and its pending ping
     * jobs, but only if they are still its own: a newer connection with the same client id may
     * already have replaced them, and its jobs share the same tag.
     */
    fun release() {
        val own = comms ?: return
        if (clientCommsMap.remove(id, own)) {
            activeSessions.remove(id, session)
            workManager.cancelAllWorkByTag("${PING_JOB}_$id")
        }
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
        data.putInt(PingWorker.KEEP_RECORDS_COUNT, keepPingRecords)
        data.putString("id", id)
        data.putString(PingWorker.SESSION, session)

        pingWork
            .setInitialDelay(delayInMilliseconds, TimeUnit.MILLISECONDS)
            .setInputData(data.build())
            .addTag("${PING_JOB}_$id")

        // Plain enqueue, not unique work: Paho calls schedule() from inside the running PingWorker
        // (ClientState.checkForActivity), so REPLACE on a stable name would cancel that worker,
        // and APPEND would hold the next ping until the current one gets its PINGRESP.
        // Leftover jobs are handled by the session check in PingWorker instead.
        workManager.enqueue(pingWork.build())

        Timber.d("$id: Successfully scheduled new ping job")
    }

    companion object {
        internal var clientCommsMap: ConcurrentHashMap<String, ClientComms> = ConcurrentHashMap()

        /** Client id -> session of the sender that currently owns that connection. */
        internal val activeSessions: ConcurrentHashMap<String, String> = ConcurrentHashMap()

        private const val PING_JOB = "PING_JOB"

        internal var messageDatabase: MqMessageDatabase? = null

        @SuppressLint("ConstantLocale")
        internal val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.getDefault())
    }

}
