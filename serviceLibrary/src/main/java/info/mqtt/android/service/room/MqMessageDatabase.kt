package info.mqtt.android.service.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import info.mqtt.android.service.QoS
import info.mqtt.android.service.room.MqMessageDatabase.Companion.MQ_DB_VERSION
import info.mqtt.android.service.room.entity.MqMessageEntity
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.*

@Database(entities = [MqMessageEntity::class], version = MQ_DB_VERSION)
@TypeConverters(Converters::class)
abstract class MqMessageDatabase : RoomDatabase() {

    abstract fun persistenceDao(): MqMessageDao

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
        persistenceDao().insert(messageArrived)
        return id
    }

    fun discardArrived(clientHandle: String, id: String): Boolean {
        return persistenceDao().deleteId(clientHandle, id) == 1
    }

    @Suppress("SimpleRedundantLet")
    companion object {

        const val MQ_DB_VERSION = 1
        private var db: MqMessageDatabase? = null

        @Synchronized
        fun getDatabase(context: Context, storageName: String = "messageMQ"): MqMessageDatabase {
            return db?.let {
                it
            } ?: run {
                db = Room.databaseBuilder(
                    context.applicationContext,
                    MqMessageDatabase::class.java,
                    storageName
                ).allowMainThreadQueries()
                    .build()
                db!!
            }
        }
    }
}
