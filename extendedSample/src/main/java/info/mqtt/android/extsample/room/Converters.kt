package info.mqtt.android.extsample.room

import androidx.room.TypeConverter
import org.eclipse.paho.android.service.QoS

class Converters {

    @TypeConverter
    fun toQoS(value: Int) = enumValues<QoS>()[value]

    @TypeConverter
    fun fromQoS(value: QoS) = value.value
}
