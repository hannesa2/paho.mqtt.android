package info.mqtt.android.extsample.activity

import android.annotation.SuppressLint
import android.content.Context
import info.mqtt.android.extsample.activity.Notify.notification
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.extsample.internal.IReceivedMessageListener
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import info.mqtt.android.extsample.R
import org.eclipse.paho.client.mqttv3.MqttException
import info.mqtt.android.extsample.internal.Persistence
import info.mqtt.android.extsample.internal.PersistenceException
import org.eclipse.paho.client.mqttv3.MqttMessage
import android.content.Intent
import info.mqtt.android.extsample.model.ReceivedMessage
import info.mqtt.android.extsample.model.Subscription
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a [MqttAndroidClient] and the actions it has performed
 */
class Connection private constructor(
    private val clientHandle: String,
    var id: String,
    var hostName: String,
    var port: Int,
    private val context: Context,
    var client: MqttAndroidClient,
    private var tlsConnection: Boolean
) {
    private val listeners = ArrayList<PropertyChangeListener>()
    private val subscriptions: MutableMap<String, Subscription> = HashMap()
    val messages = ArrayList<ReceivedMessage>()
    val history: ArrayList<String> = ArrayList()
    private val receivedMessageListeners = ArrayList<IReceivedMessageListener>()

    private var status = ConnectionStatus.NONE

    var connectionOptions: MqttConnectOptions? = null
        private set

    var persistenceId: Long = -1

    init {
        addHistory("Client: $id created")
    }

    fun updateConnection(clientId: String, host: String, port: Int, tlsConnection: Boolean) {
        val uri: String = if (tlsConnection) {
            "ssl://$host:$port"
        } else {
            "tcp://$host:$port"
        }
        id = clientId
        hostName = host
        this.port = port
        this.tlsConnection = tlsConnection
        client = MqttAndroidClient(context, uri, clientId)
    }

    @SuppressLint("SimpleDateFormat")
    fun addHistory(action: String) {
        val timestamp = SimpleDateFormat("HH:mm.ss.SSS").format(Date(System.currentTimeMillis()))
        history.add(action + timestamp)
        notifyListeners(PropertyChangeEvent(this, ActivityConstants.historyProperty, null, null))
    }

    fun handle() = clientHandle

    val isConnected: Boolean
        get() = status == ConnectionStatus.CONNECTED

    fun changeConnectionStatus(connectionStatus: ConnectionStatus) {
        status = connectionStatus
        notifyListeners(PropertyChangeEvent(this, ActivityConstants.ConnectionStatusProperty, null, null))
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(id)
        sb.append("\n ")
        when (status) {
            ConnectionStatus.CONNECTED -> sb.append(context.getString(R.string.connection_connected_to))
            ConnectionStatus.DISCONNECTED -> sb.append(context.getString(R.string.connection_disconnected_from))
            ConnectionStatus.NONE -> sb.append(context.getString(R.string.connection_unknown_status))
            ConnectionStatus.CONNECTING -> sb.append(context.getString(R.string.connection_connecting_to))
            ConnectionStatus.DISCONNECTING -> sb.append(context.getString(R.string.connection_disconnecting_from))
            ConnectionStatus.ERROR -> sb.append(context.getString(R.string.connection_error_connecting_to))
        }
        sb.append(" ")
        sb.append(hostName)
        return sb.toString()
    }

    /**
     * Compares two connection objects for equality
     * this only takes account of the client handle
     *
     * @param other The object to compare to
     * @return true if the client handles match
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Connection) {
            return false
        }
        return clientHandle == other.clientHandle
    }

    /**
     * Add the connectOptions used to connect the client to the server
     *
     * @param connectOptions the connectOptions used to connect to the server
     */
    fun addConnectionOptions(connectOptions: MqttConnectOptions?) {
        connectionOptions = connectOptions
    }

    /**
     * Register a [PropertyChangeListener] to this object
     *
     * @param listener the listener to register
     */
    fun registerChangeListener(listener: PropertyChangeListener) {
        listeners.add(listener)
    }

    /**
     * Notify [PropertyChangeListener] objects that the object has been updated
     *
     * @param propertyChangeEvent - The property Change event
     */
    private fun notifyListeners(propertyChangeEvent: PropertyChangeEvent) {
        listeners.forEach {
            it.propertyChange(propertyChangeEvent)
        }
    }

    /**
     * Determines if the connection is secured using SSL, returning a C style integer value
     *
     * @return 1 if SSL secured 0 if plain text
     */
    val isSSL: Int
        get() = if (tlsConnection) 1 else 0

    fun addNewSubscription(subscription: Subscription) {
        if (!subscriptions.containsKey(subscription.topic)) {
            try {
                val actionArgs = arrayOfNulls<String>(1)
                actionArgs[0] = subscription.topic
                val callback = ActionListener(context, Action.SUBSCRIBE, this, *actionArgs)
                client.subscribe(subscription.topic, subscription.qos, null, callback)
                val persistence = Persistence(context)
                val rowId = persistence.persistSubscription(subscription)
                subscription.persistenceId = rowId
                subscriptions[subscription.topic] = subscription
            } catch (pe: PersistenceException) {
                throw MqttException(pe)
            }
        }
    }

    fun unsubscribe(subscription: Subscription) {
        if (subscriptions.containsKey(subscription.topic)) {
            client.unsubscribe(subscription.topic)
            subscriptions.remove(subscription.topic)
            val persistence = Persistence(context)
            persistence.deleteSubscription(subscription)
        }
    }

    fun getSubscriptions(): ArrayList<Subscription> {
        return ArrayList(subscriptions.values)
    }

    fun setSubscriptions(newSubs: ArrayList<Subscription>) {
        for (sub in newSubs) {
            subscriptions[sub.topic] = sub
        }
    }

    fun addReceivedMessageListener(listener: IReceivedMessageListener) {
        receivedMessageListeners.add(listener)
    }

    @SuppressLint("SimpleDateFormat")
    fun addMessage(topic: String, message: MqttMessage) {
        val msg = ReceivedMessage(topic, message)
        messages.add(0, msg)
        if (subscriptions.containsKey(topic)) {
            subscriptions[topic]!!.lastMessage = String(message.payload)
            if (subscriptions[topic]!!.isEnableNotifications) {
                //create intent to start activity
                val intent = Intent()
                intent.setClassName(context, MainActivity::class.java.name)
                intent.putExtra("handle", clientHandle)

                SimpleDateFormat("HH:mm.ss.SSS").format(Date(System.currentTimeMillis()))
                notification(context, context.getString(R.string.notification, String(message.payload), topic), intent, R.string.notifyTitle)
            }
        }
        for (listener in receivedMessageListeners) {
            listener.onMessageReceived(msg)
        }
    }

    override fun hashCode(): Int {
        var result = clientHandle.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    enum class ConnectionStatus {
        CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, ERROR, NONE
    }

    companion object {
        private const val FOREGROUND = true

        fun createConnection(clientHandle: String, clientId: String, host: String, port: Int, context: Context, tlsConnection: Boolean): Connection {
            val uri: String = if (tlsConnection) {
                "ssl://$host:$port"
            } else {
                "tcp://$host:$port"
            }
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val foregroundNotification = Notify.foregroundNotification(context, clientId, intent, R.string.notifyForeground)
            val client = MqttAndroidClient(context, uri, clientId).apply {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && FOREGROUND)
                    setForegroundService(foregroundNotification, 77)
            }
            return Connection(clientHandle, clientId, host, port, context, client, tlsConnection)
        }
    }

}
