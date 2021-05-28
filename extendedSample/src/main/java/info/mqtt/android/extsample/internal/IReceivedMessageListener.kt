package info.mqtt.android.extsample.internal

import info.mqtt.android.extsample.model.ReceivedMessage

interface IReceivedMessageListener {
    fun onMessageReceived(message: ReceivedMessage?)
}