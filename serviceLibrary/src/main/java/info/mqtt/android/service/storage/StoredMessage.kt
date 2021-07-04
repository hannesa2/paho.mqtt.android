package info.mqtt.android.service.storage

import org.eclipse.paho.client.mqttv3.MqttMessage

interface StoredMessage {
    val messageId: String
    val clientHandle: String
    val topic: String
    val message: MqttMessage
}
