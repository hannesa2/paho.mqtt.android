package org.eclipse.paho.android.service

import org.eclipse.paho.client.mqttv3.MqttMessage

interface StoredMessage {
    val messageId: String
    val clientHandle: String
    val topic: String
    val message: MqttMessage
}
