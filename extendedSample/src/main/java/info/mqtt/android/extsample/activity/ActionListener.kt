package info.mqtt.android.extsample.activity

import info.mqtt.android.extsample.internal.Connections.Companion.getInstance
import info.mqtt.android.extsample.activity.Notify.toast
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import android.annotation.SuppressLint
import android.content.Context
import info.mqtt.android.extsample.R
import android.widget.Toast
import timber.log.Timber
import android.content.Intent

class ActionListener(
    private val context: Context,
    private val action: Action, private val connection: Connection, vararg additionalArgs: String?
) : IMqttActionListener {

    private val additionalArgs: Array<String?> = arrayOf(*additionalArgs)
    private val clientHandle: String = connection.handle()

    override fun onSuccess(asyncActionToken: IMqttToken) {
        when (action) {
            Action.CONNECT -> connect()
            Action.DISCONNECT -> disconnect()
            Action.SUBSCRIBE -> subscribe()
            Action.PUBLISH -> publish()
        }
    }

    /**
     * A publish action has been successfully completed, update connection
     * object associated with the client this action belongs to, then notify the
     * user of success
     */
    private fun publish() {
        val connection = getInstance(context).getConnection(clientHandle)
        @SuppressLint("StringFormatMatches")
        val actionTaken = context.getString(R.string.toast_pub_success, *additionalArgs)
        connection!!.addHistory(actionTaken)
        toast(context, actionTaken, Toast.LENGTH_SHORT)
        print("Published")
    }

    /**
     * A addNewSubscription action has been successfully completed, update the connection
     * object associated with the client this action belongs to and then notify
     * the user of success
     */
    private fun subscribe() {
        val connection = getInstance(context).getConnection(clientHandle)
        val actionTaken = context.getString(R.string.toast_sub_success, *additionalArgs)
        connection!!.addHistory(actionTaken)
        toast(context, actionTaken, Toast.LENGTH_SHORT)
        print(actionTaken)
    }

    /**
     * A disconnection action has been successfully completed, update the
     * connection object associated with the client this action belongs to and
     * then notify the user of success.
     */
    private fun disconnect() {
        val connection = getInstance(context).getConnection(clientHandle)
        connection!!.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED)
        val actionTaken = context.getString(R.string.toast_disconnected)
        connection.addHistory(actionTaken)
        Timber.i("${connection.handle()} disconnected")
        //build intent
        val intent = Intent()
        intent.setClassName(context, activityClass)
        intent.putExtra("handle", clientHandle)
    }

    /**
     * A connection action has been successfully completed, update the
     * connection object associated with the client this action belongs to and
     * then notify the user of success.
     */
    private fun connect() {
        val connection = getInstance(context).getConnection(clientHandle)
        connection!!.changeConnectionStatus(Connection.ConnectionStatus.CONNECTED)
        connection.addHistory("Client Connected")
        Timber.i("${connection.handle()} connected.")
        val subscriptions = this.connection.getSubscriptions()
        for (sub in subscriptions) {
            Timber.i("Auto-subscribing to: ${sub.topic} @ QoS: ${sub.qos}")
            this.connection.client.subscribe(sub.topic, sub.qos)
        }
    }

    /**
     * The action associated with the object was a failure
     *
     * @param token     This argument is not used
     * @param exception The exception which indicates why the action failed
     */
    override fun onFailure(token: IMqttToken?, exception: Throwable?) {
        Timber.e(exception, "token=$token")
        exception?.let {
            when (action) {
                Action.CONNECT -> connect(it)
                Action.DISCONNECT -> disconnect(it)
                Action.SUBSCRIBE -> subscribe(it)
                Action.PUBLISH -> publish(it)
            }
        }
    }

    /**
     * A publish action was unsuccessful, notify user and update client history
     */
    private fun publish(exception: Throwable) {
        val connection = getInstance(context).getConnection(clientHandle)
        @SuppressLint("StringFormatMatches")
        val action = context.getString(R.string.toast_pub_failed, *additionalArgs)
        connection!!.addHistory(action)
        toast(context, action, Toast.LENGTH_SHORT)
        Timber.e("Publish failed")
    }

    /**
     * A addNewSubscription action was unsuccessful, notify user and update client history
     *
     * @param exception This argument is not used
     */
    private fun subscribe(exception: Throwable) {
        val connection = getInstance(context).getConnection(clientHandle)
        val action = context.getString(R.string.toast_sub_failed, *additionalArgs)
        connection!!.addHistory(action)
        toast(context, action, Toast.LENGTH_SHORT)
        Timber.e(action)
    }

    /**
     * A disconnect action was unsuccessful, notify user and update client history
     *
     * @param exception This argument is not used
     */
    private fun disconnect(exception: Throwable) {
        val connection = getInstance(context).getConnection(clientHandle)
        connection!!.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED)
        connection.addHistory("Disconnect Failed - an error occured")
    }

    /**
     * A connect action was unsuccessful, notify the user and update client history
     *
     * @param exception This argument is not used
     */
    private fun connect(exception: Throwable) {
        val connection = getInstance(context).getConnection(clientHandle)
        connection!!.changeConnectionStatus(Connection.ConnectionStatus.ERROR)
        connection.addHistory("Client failed to connect")
        Timber.e("Client failed to connect")
    }

    companion object {
        private const val activityClass = "info.mqtt.android.extsample.activity.MainActivity"
    }

}
