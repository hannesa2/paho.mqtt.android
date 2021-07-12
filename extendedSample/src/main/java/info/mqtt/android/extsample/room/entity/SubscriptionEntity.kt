package info.mqtt.android.extsample.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SubscriptionEntity(
    @PrimaryKey var clientHandle: String,
    var topic: String,
    var notify: Int,
    var qos: Int
)
