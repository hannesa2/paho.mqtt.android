package info.mqtt.android.service.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["timestamp"])])
data class PingEntity(
    @PrimaryKey val timestamp: Long,
    var clientId: String?,
    var serverURI: String?,
    val success: Boolean,
    var message: String? = null
)
