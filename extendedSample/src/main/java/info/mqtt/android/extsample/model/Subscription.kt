package info.mqtt.android.extsample.model

class Subscription(var topic: String, var qos: Int, var clientHandle: String, var isEnableNotifications: Boolean) {
    var lastMessage: String? = null
    var persistenceId: Long = 0
    override fun toString(): String {
        return "Subscription{" +
                "topic='" + topic + '\'' +
                ", qos=" + qos +
                ", clientHandle='" + clientHandle + '\'' +
                ", persistenceId='" + persistenceId + '\'' +
                ", enableNotifications='" + isEnableNotifications + '\'' +
                '}'
    }
}
