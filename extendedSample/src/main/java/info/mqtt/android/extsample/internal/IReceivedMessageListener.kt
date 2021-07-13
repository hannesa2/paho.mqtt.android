package info.mqtt.android.extsample.internal

import info.mqtt.android.extsample.model.ReceivedMessage

interface IReceivedMessageListener {
    var identifer : String
    fun onMessageReceived(message: ReceivedMessage?)
}
