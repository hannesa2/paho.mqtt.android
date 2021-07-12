package info.mqtt.android.extsample.utils

import android.content.Context
import info.mqtt.android.extsample.activity.Action
import info.mqtt.android.extsample.activity.ActionListener
import info.mqtt.android.extsample.activity.Connection
import info.mqtt.android.extsample.activity.MqttCallbackHandler

fun Connection.connect(context: Context) {
    val actionArgs = arrayOfNulls<String>(1)
    actionArgs[0] = this.id
    val callback = ActionListener(context, Action.CONNECT, this, *actionArgs)
    this.client.setCallback(MqttCallbackHandler(context, this.handle()))
    this.client.connect(this.connectionOptions, null, callback)
}
