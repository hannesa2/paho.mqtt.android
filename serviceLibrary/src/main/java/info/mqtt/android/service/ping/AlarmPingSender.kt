package info.mqtt.android.service.ping

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import info.mqtt.android.service.MqttService
import info.mqtt.android.service.MqttServiceConstants
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * This class implements the [MqttPingSender] pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 *
 * @see MqttPingSender
 */
internal class AlarmPingSender(val service: MqttService) : MqttPingSender {
    private var clientComms: ClientComms? = null
    private var alarmReceiver: BroadcastReceiver? = null
    private var pendingIntent: PendingIntent? = null

    @Volatile
    private var hasStarted = false

    override fun init(comms: ClientComms) {
        this.clientComms = comms
        alarmReceiver = AlarmReceiver()
    }

    override fun start() {
        val action = MqttServiceConstants.PING_SENDER + clientComms!!.client.clientId
        Timber.d("Register AlarmReceiver to MqttService$action")
        service.registerReceiver(alarmReceiver, IntentFilter(action))
        pendingIntent = PendingIntent.getBroadcast(service, 0, Intent(action), PendingIntent.FLAG_UPDATE_CURRENT)
        schedule(clientComms!!.keepAlive)
        hasStarted = true
    }

    override fun stop() {
        Timber.d("Unregister AlarmReceiver to MqttService ${clientComms!!.client.clientId}")
        if (hasStarted) {
            if (pendingIntent != null) {
                // Cancel Alarm.
                val alarmManager = service.getSystemService(Service.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(pendingIntent)
            }
            hasStarted = false
            try {
                service.unregisterReceiver(alarmReceiver)
            } catch (e: IllegalArgumentException) {
                //Ignore unregister errors.
            }
        }
    }

    override fun schedule(delayInMilliseconds: Long) {
        val nextAlarmInMilliseconds = SystemClock.elapsedRealtime() + delayInMilliseconds
        Timber.d("Schedule next alarm at $nextAlarmInMilliseconds ms")
        val alarmManager = service.getSystemService(Service.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 23) {
            // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
            // the device to run this task whilst dosing.
            Timber.d("Alarm schedule using setExactAndAllowWhileIdle, next: $delayInMilliseconds")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextAlarmInMilliseconds, pendingIntent)
        } else
            Timber.d("Alarm schedule using setExact, delay: $delayInMilliseconds")
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextAlarmInMilliseconds, pendingIntent)
    }

    private inner class PingAsyncTask : AsyncTask<ClientComms?, Void?, Boolean>() {
        var success = false
        var count = 0

        override fun doInBackground(vararg comms: ClientComms?): Boolean {
            Timber.d("start")
            val token: IMqttToken? = comms[0]?.checkForActivity(object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    success = true
                    Timber.d("Ping async task success. $count")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.d("Ping async task failed. $count")
                    success = false
                }
            })
            try {
                while (token == null && count < 10) {
                    try {
                        Thread.sleep(100)
                        count++
                    } catch (e: Exception) {
                    }
                }
                if (token != null) {
                    token.waitForCompletion()
                } else {
                    Timber.d("Ping command was not sent by the client. $count")
                }
            } catch (e: MqttException) {
                Timber.d("Ignore MQTT exception : ${e.message} $count")
            } catch (ex: Exception) {
                Timber.d("Ignore exception : ${ex.message} $count")
            }
            Timber.d("Completed Success=$success $count")
            return success
        }

        override fun onPostExecute(success: Boolean) {
            if (!success) {
                Timber.d("Success=$success")
            }
        }

        override fun onCancelled(success: Boolean) {
            Timber.d("Canceled=$success")
        }
    }

    /*
     * This class sends PingReq packet to MQTT broker
     */
    internal inner class AlarmReceiver : BroadcastReceiver() {
        private val wakeLockTag = MqttServiceConstants.PING_WAKELOCK + clientComms!!.client.clientId
        private var pingRunner: PingAsyncTask? = null

        @SuppressLint("Wakelock")
        override fun onReceive(context: Context, intent: Intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.
            val pm = service.getSystemService(Service.POWER_SERVICE) as PowerManager
            val wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)
            wakelock.acquire(10 * 60 * 1000L /*10 minutes*/)
            pingRunner?.let {
                if (it.cancel(true)) {
                    Timber.d("Previous ping async task was cancelled at:${System.currentTimeMillis()}")
                }
            }
            pingRunner = PingAsyncTask()
            pingRunner!!.execute(clientComms)
            if (wakelock.isHeld) {
                wakelock.release()
            }
        }
    }

}
