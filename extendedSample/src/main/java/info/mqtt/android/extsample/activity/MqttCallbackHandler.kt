package info.mqtt.android.extsample.activity

import android.content.Context
import info.mqtt.android.extsample.internal.Connections.Companion.getInstance
import info.mqtt.android.extsample.activity.Notify.notification
import org.eclipse.paho.client.mqttv3.MqttCallback
import timber.log.Timber
import info.mqtt.android.extsample.R
import android.content.Intent
import kotlin.Throws
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import java.lang.Exception

internal class MqttCallbackHandler(private val context: Context, private val clientHandle: String) : MqttCallback {

    override fun connectionLost(cause: Throwable?) {
        Timber.d("Connection Lost: ${cause?.message}")
        val connection = getInstance(context).getConnection(clientHandle)
        connection?.addAction("Connection Lost")
        connection?.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED)
        val message = context.getString(R.string.connection_lost, connection?.id, connection?.hostName)

        val intent = Intent()
        intent.setClassName(context, activityClass)
        intent.putExtra("handle", clientHandle)

        notification(context, message, intent, R.string.notifyTitle_connectionLost)
    }

    @Throws(Exception::class)
    override fun messageArrived(topic: String, message: MqttMessage) {

        //Get connection object associated with this object
        val connection = getInstance(context).getConnection(clientHandle)
        connection?.messageArrived(topic, message)
        //get the string from strings.xml and format
        val messageString = "${message.payload} $topic qos=${message.qos} retained:${message.isRetained}"
        Timber.i(messageString)

        //update client history
        connection?.addAction(messageString)
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        // Do nothing
    }

    companion object {
        private val activityClass = MainActivity::class.java.name
    }
}
