package info.mqtt.android.service.ping

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber

class PingWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return if (backgroundCheck(comms))
            Result.success()
        else
            Result.failure()
    }

    private fun backgroundCheck(clientComms: ClientComms?): Boolean {
        var success = clientComms == null
        var count = 0

        val token: IMqttToken? = clientComms?.checkForActivity(object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                Timber.d("PingTask success $count")
                success = true
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Timber.e("PingTask Failed $count")
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
            token?.let {
                token.waitForCompletion()
            } ?: run {
                Timber.e("Ping command was not sent by the client. $count")
            }
        } catch (e: MqttException) {
            Timber.w("Ignore MQTT exception : ${e.message}  $count")
        } catch (ex: Exception) {
            Timber.d(ex)
        }
        Timber.d("PingTask=$success $count $clientComms")
        return success
    }

    companion object {
        var comms: ClientComms? = null
    }
}
