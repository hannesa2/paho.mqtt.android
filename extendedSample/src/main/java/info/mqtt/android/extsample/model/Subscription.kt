package info.mqtt.android.extsample.model

class Subscription(var topic: String, var qos: Int, var clientHandle: String, var isEnableNotifications: Boolean) {
    override fun toString(): String {
        return "Subscription{" +
                "topic='" + topic + '\'' +
                ", qos=" + qos +
                ", clientHandle='" + clientHandle + '\'' +
                ", enableNotifications='" + isEnableNotifications + '\'' +
                '}'
    }
}
