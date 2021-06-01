package info.mqtt.android.extsample.model

import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.*

class ReceivedMessage(val topic: String, val message: MqttMessage) {
    val timestamp: Date = Date()
    override fun toString(): String {
        return "ReceivedMessage{" +
                "topic='" + topic + '\'' +
                ", message=" + message +
                ", timestamp=" + timestamp +
                '}'
    }

}
