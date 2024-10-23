package info.mqtt.android.extsample.internal

import android.content.Context
import android.content.Intent
import info.mqtt.android.extsample.MainActivity
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.internal.Connections.Companion.getInstance
import info.mqtt.android.extsample.internal.Notify.notification
import info.mqtt.android.extsample.utils.connect
import info.mqtt.android.service.room.MqMessageDatabase
import info.mqtt.android.service.room.entity.PingEntity
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import timber.log.Timber

internal class MqttCallbackHandler(private val context: Context, private val clientHandle: String) : MqttCallback {

    override fun connectionLost(cause: Throwable?) {
        val connection = getInstance(context).getConnection(clientHandle)

        connection?.addHistory("Connection Lost [${cause?.message}]")
        connection?.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED)

        cause?.let {
            Timber.w("${it.javaClass.simpleName} ${it.message}")
            val pingMQ = PingEntity(
                System.currentTimeMillis(),
                connection?.client?.clientId,
                connection?.client?.serverURI,
                false,
                message = "${it.javaClass.simpleName} ${it.message}"
            )
            val pingDao = MqMessageDatabase.getDatabase(context).pingDao()
            pingDao.insert(pingMQ)
            if (connection?.connectionOptions?.isAutomaticReconnect == true) {
                Timber.i("Try to reconnect")
                connection.connect(context)
            }
        } ?: run {
            Timber.d("isAutomaticReconnect=${connection?.connectionOptions?.isAutomaticReconnect}")
        }

        cause?.let {
            val intent = Intent()
            intent.setClassName(context, activityClass)
            intent.putExtra("handle", clientHandle)
            notification(context, "id=${connection?.id} host=${connection?.hostName}", intent, R.string.notifyTitle_connectionLost)
        }
    }

    @Throws(Exception::class)
    override fun messageArrived(topic: String, message: MqttMessage) {
        val messageString = "'${String(message.payload)}' $topic qos=${message.qos} retained:${message.isRetained}"
        Timber.i(messageString)

        //Get connection object associated with this object
        getInstance(context).getConnection(clientHandle)?.apply {
            addMessage(topic, message)
        }
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        getInstance(context).getConnection(clientHandle)?.apply {
            addHistory("deliveryComplete ${token.message}")
        }
    }

    companion object {
        private val activityClass = MainActivity::class.java.name
    }
}
