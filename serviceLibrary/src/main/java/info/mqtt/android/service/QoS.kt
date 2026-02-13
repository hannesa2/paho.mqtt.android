package info.mqtt.android.service

enum class QoS(val value: Int) {

    AtMostOnce(0),
    AtLeastOnce(1),
    ExactlyOnce(2);

    companion object {
        fun fromValue(qos: Int): QoS {
            return entries[qos]
        }
    }

}
