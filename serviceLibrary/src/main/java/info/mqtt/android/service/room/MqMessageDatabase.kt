package info.mqtt.android.service.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import info.mqtt.android.service.QoS
import info.mqtt.android.service.room.MqMessageDatabase.Companion.MQ_DB_VERSION
import info.mqtt.android.service.room.entity.MqMessageEntity
import info.mqtt.android.service.room.entity.PingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.*

@Database(entities = [MqMessageEntity::class, PingEntity::class], version = MQ_DB_VERSION)
@TypeConverters(Converters::class)
abstract class MqMessageDatabase : RoomDatabase() {

    abstract fun persistenceDao(): MqMessageDao
    abstract fun pingDao(): PingDao

    fun storeArrived(clientHandle: String, topic: String, message: MqttMessage): String {
        val id = UUID.randomUUID().toString()
        val messageArrived = MqMessageEntity(
            id,
            clientHandle,
            topic,
            MqttMessage(message.payload),
            QoS.valueOf(message.qos),
            message.isRetained,
            message.isDuplicate,
            System.currentTimeMillis()
        )
        CoroutineScope(Dispatchers.IO).launch {
            persistenceDao().insert(messageArrived)
        }
        return id
    }

    fun discardArrived(clientHandle: String, id: String): Boolean {
        var result = false
        CoroutineScope(Dispatchers.IO).launch {
            val queue = async(Dispatchers.IO) {
                persistenceDao().deleteId(clientHandle, id) == 1
            }
            result = queue.await()
        }
        return result
    }

    companion object {

        const val MQ_DB_VERSION = 2

        @Volatile
        private var instance: MqMessageDatabase? = null

        @Synchronized
        fun getDatabase(context: Context, storageName: String = "messageMQ"): MqMessageDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext, storageName).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context, storageName: String): MqMessageDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MqMessageDatabase::class.java,
                storageName
            ).fallbackToDestructiveMigrationFrom(1, 2)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
        }
    }
}
