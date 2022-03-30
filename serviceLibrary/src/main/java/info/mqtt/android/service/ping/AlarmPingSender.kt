package info.mqtt.android.service.ping

import android.os.Handler
import info.mqtt.android.service.MqttService
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttPingSender
import org.eclipse.paho.client.mqttv3.internal.ClientComms
import timber.log.Timber


/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * This class implements the [MqttPingSender] ping interface
 * allowing applications to send ping packet to server every keep alive interval.
 *
 * @see MqttPingSender
 */
internal class AlarmPingSender(val service: MqttService) : MqttPingSender {
    private var clientComms: ClientComms? = null
    private val handler = Handler(service.mainLooper)

    override fun init(comms: ClientComms) {
        this.clientComms = comms
    }

    override fun start() {
        schedule(clientComms!!.keepAlive)
    }

    override fun stop() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun schedule(delayInMilliseconds: Long) {
        Timber.d("Schedule alarm at ${System.currentTimeMillis() + delayInMilliseconds}")
        handler.postDelayed({
            Timber.d("Running Scheduled ping at ${System.currentTimeMillis()}")
            clientComms?.checkForActivity(object : IMqttActionListener{
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Timber.d("Scheduled ping: Success.")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Timber.d("Scheduled ping: Failure.")
                }
            }) ?: kotlin.run {
                Timber.d("Token: null.")
            }
        }, delayInMilliseconds)
    }
}
