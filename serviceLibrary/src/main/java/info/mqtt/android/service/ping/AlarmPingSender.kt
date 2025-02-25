package info.mqtt.android.service.ping

import androidx.work.*
import info.mqtt.android.service.MqttService
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit


/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * This class implements the [MqttPingSender] ping interface
 * allowing applications to send ping packet to server every keep alive interval.
 *
 * @see MqttPingSender
 */
internal class AlarmPingSender(val service: MqttService,val id: String) : MqttPingSender {

    private val workManager = WorkManager.getInstance(service)



    override fun init(comms: ClientComms) {
        clientCommsMap[id] = comms
        Timber.w("Init ping job $id")

    }

    override fun start() {
        Timber.d("Start ping job $id")


            if (clientCommsMap.containsKey(id)) {
                schedule(clientCommsMap[id]!!.keepAlive)
            }

    }

    private fun getName(): String = id

    override fun stop() {
        workManager.cancelUniqueWork("PING_JOB_${getName()}")
        //remove the clientComms from the map
        Timber.d("Stop ping job ${getName()}")
        clientCommsMap.remove(id)
    }

    override fun schedule(delayInMilliseconds: Long) {

        val name = getName()
        Timber.d("${name}: Schedule next alarm at ${System.currentTimeMillis() + delayInMilliseconds} ")

        val d: Data = Data.Builder().putString("id", name)

            .build()

       workManager.enqueueUniqueWork(
            "PING_JOB_$name",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequest
                .Builder(PingWorker::class.java)
                .setInputData(d)
                .setInitialDelay(delayInMilliseconds, TimeUnit.MILLISECONDS)
                .build()
        )
    }

    companion object {
        internal var clientCommsMap: ConcurrentMap<String,ClientComms> =  ConcurrentHashMap()

    }

}
