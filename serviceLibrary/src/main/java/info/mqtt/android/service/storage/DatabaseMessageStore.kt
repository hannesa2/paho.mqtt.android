package info.mqtt.android.service.storage

import android.database.sqlite.SQLiteDatabase
import org.eclipse.paho.client.mqttv3.MqttMessage
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import java.lang.UnsupportedOperationException
import kotlin.Throws
import android.database.sqlite.SQLiteOpenHelper
import info.mqtt.android.service.MqttService
import info.mqtt.android.service.MqttServiceConstants
import info.mqtt.android.service.MqttTraceHandler
import java.util.*

internal class DatabaseMessageStore(service: MqttService, context: Context) : MessageStore {

    private var db: SQLiteDatabase? = null

    private var mqttDb: MQTTDatabaseHelper

    private var traceHandler: MqttTraceHandler = service

    init {
        mqttDb = MQTTDatabaseHelper(traceHandler, context)

        // Android documentation suggests that this perhaps
        // could/should be done in another thread, but as the
        // database is only one table, I doubt it matters...
        traceHandler.traceDebug("DatabaseMessageStore<init> complete")
    }

    /**
     * Store an MQTT message
     *
     * @param clientHandle identifier for the client storing the message
     * @param Topic        The topic on which the message was published
     * @param message      the arrived MQTT message
     * @return an identifier for the message, so that it can be removed when appropriate
     */
    override fun storeArrived(clientHandle: String?, topic: String?, message: MqttMessage?): String {
        db = mqttDb.writableDatabase
        traceHandler.traceDebug("storeArrived{" + clientHandle + "}, {" + message.toString() + "}")
        val payload = message!!.payload
        val qos = message.qos
        val retained = message.isRetained
        val duplicate = message.isDuplicate
        val values = ContentValues()
        val id = UUID.randomUUID().toString()
        values.put(MqttServiceConstants.MESSAGE_ID, id)
        values.put(MqttServiceConstants.CLIENT_HANDLE, clientHandle)
        values.put(MqttServiceConstants.DESTINATION_NAME, topic)
        values.put(MqttServiceConstants.PAYLOAD, payload)
        values.put(MqttServiceConstants.QOS, qos)
        values.put(MqttServiceConstants.RETAINED, retained)
        values.put(MqttServiceConstants.DUPLICATE, duplicate)
        values.put(MTIMESTAMP, System.currentTimeMillis())
        try {
            db?.insertOrThrow(ARRIVED_MESSAGE_TABLE_NAME, null, values)
        } catch (e: SQLException) {
            traceHandler.traceException("onUpgrade", e)
            throw e
        }
        val count = getArrivedRowCount(clientHandle)
        traceHandler.traceDebug(
            "storeArrived: inserted message with id of {" + id
                    + "} - Number of messages in database for this clientHandle = " + count
        )
        return id
    }

    private fun getArrivedRowCount(clientHandle: String?): Int {
        var count = 0
        val projection = arrayOf(
            MqttServiceConstants.MESSAGE_ID
        )
        val selection = MqttServiceConstants.CLIENT_HANDLE + "=?"
        val selectionArgs = arrayOfNulls<String>(1)
        selectionArgs[0] = clientHandle
        val c = db!!.query(
            ARRIVED_MESSAGE_TABLE_NAME,  // Table Name
            projection,  // The columns to return;
            selection,  // Columns for WHERE Clause
            selectionArgs,  // The values for the WHERE Cause
            null,  //Don't group the rows
            null,  // Don't filter by row groups
            null // The sort order
        )
        if (c.moveToFirst()) {
            count = c.getInt(0)
        }
        c.close()
        return count
    }

    /**
     * Delete an MQTT message.
     *
     * @param clientHandle identifier for the client which stored the message
     * @param id           the identifying string returned when the message was stored
     * @return true if the message was found and deleted
     */
    override fun discardArrived(clientHandle: String?, id: String?): Boolean {
        db = mqttDb.writableDatabase
        traceHandler.traceDebug("discardArrived{$clientHandle}, {$id}")
        val rows: Int
        val selectionArgs = arrayOfNulls<String>(2)
        selectionArgs[0] = id
        selectionArgs[1] = clientHandle
        rows = try {
            db!!.delete(
                ARRIVED_MESSAGE_TABLE_NAME,
                MqttServiceConstants.MESSAGE_ID + "=? AND " + MqttServiceConstants.CLIENT_HANDLE + "=?",
                selectionArgs
            )
        } catch (e: SQLException) {
            traceHandler.traceException("discardArrived", e)
            throw e
        }
        if (rows != 1) {
            traceHandler.traceError("discardArrived - Error deleting message {$id} from database: Rows affected = $rows")
            return false
        }
        val count = getArrivedRowCount(clientHandle)
        traceHandler.traceDebug("discardArrived - Message deleted successfully. - messages in db for this clientHandle $count")
        return true
    }

    /**
     * Get an iterator over all messages stored (optionally for a specific client)
     *
     * @param clientHandle identifier for the client.<br></br>
     * If null, all messages are retrieved
     * @return iterator of all the arrived MQTT messages
     */
    override fun getAllArrivedMessages(clientHandle: String): Iterator<StoredMessage> {
        return object : MutableIterator<StoredMessage> {

            private val selectionArgs = arrayOf(clientHandle)
            private var c: Cursor? = null
            private var hasNext = false

            init {
                db = mqttDb.writableDatabase
                // anonymous initializer to start a suitable query
                // and position at the first row, if one exists
                c = db!!.query(
                    ARRIVED_MESSAGE_TABLE_NAME,
                    null,
                    MqttServiceConstants.CLIENT_HANDLE + "=?",
                    selectionArgs,
                    null,
                    null,
                    "mtimestamp ASC"
                )
                hasNext = c!!.moveToFirst()
            }

            override fun hasNext(): Boolean {
                if (!hasNext) {
                    c!!.close()
                }
                return hasNext
            }

            override fun next(): StoredMessage {
                val messageId = c!!.getString(c!!.getColumnIndex(MqttServiceConstants.MESSAGE_ID))
                val localClientHandle = c!!.getString(c!!.getColumnIndex(MqttServiceConstants.CLIENT_HANDLE))
                val topic = c!!.getString(c!!.getColumnIndex(MqttServiceConstants.DESTINATION_NAME))
                val payload = c!!.getBlob(c!!.getColumnIndex(MqttServiceConstants.PAYLOAD))
                val qos = c!!.getInt(c!!.getColumnIndex(MqttServiceConstants.QOS))
                val retained = java.lang.Boolean.parseBoolean(c!!.getString(c!!.getColumnIndex(MqttServiceConstants.RETAINED)))
                val dup = java.lang.Boolean.parseBoolean(c!!.getString(c!!.getColumnIndex(MqttServiceConstants.DUPLICATE)))

                // build the result
                val message = MqttMessageHack(payload)
                message.qos = qos
                message.isRetained = retained
                message.isDuplicate = dup

                // move on
                hasNext = c!!.moveToNext()
                return DbStoredData(messageId, localClientHandle, topic, message)
            }

            override fun remove() {
                throw UnsupportedOperationException()
            }

            /* (non-Javadoc)
             * @see java.lang.Object#finalize()
             */
            @Throws(Throwable::class)
            protected fun finalize() {
                c!!.close()
            }

        }
    }

    /**
     * Delete all messages (optionally for a specific client)
     *
     * @param clientHandle identifier for the client.<br></br>
     * If null, all messages are deleted
     */
    override fun clearArrivedMessages(clientHandle: String) {
        db = mqttDb.writableDatabase
        val selectionArgs = arrayOfNulls<String>(1)
        selectionArgs[0] = clientHandle
        val rows: Int
        rows = run {
            traceHandler.traceDebug("clearArrivedMessages: clearing the table of $clientHandle messages")
            db!!.delete(
                ARRIVED_MESSAGE_TABLE_NAME,
                MqttServiceConstants.CLIENT_HANDLE + "=?",
                selectionArgs
            )
        }
        traceHandler.traceDebug("clearArrivedMessages: rows affected = $rows")
    }

    override fun close() {
        if (db != null) {
            db!!.close()
        }
    }

    /**
     * We need a SQLiteOpenHelper to handle database creation and updating
     */
    private class MQTTDatabaseHelper(private val traceHandler: MqttTraceHandler, context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        /**
         * When the database is (re)created, create our table
         */
        override fun onCreate(database: SQLiteDatabase) {
            val createArrivedTableStatement = ("CREATE TABLE "
                    + ARRIVED_MESSAGE_TABLE_NAME + "("
                    + MqttServiceConstants.MESSAGE_ID + " TEXT PRIMARY KEY, "
                    + MqttServiceConstants.CLIENT_HANDLE + " TEXT, "
                    + MqttServiceConstants.DESTINATION_NAME + " TEXT, "
                    + MqttServiceConstants.PAYLOAD + " BLOB, "
                    + MqttServiceConstants.QOS + " INTEGER, "
                    + MqttServiceConstants.RETAINED + " TEXT, "
                    + MqttServiceConstants.DUPLICATE + " TEXT, " + MTIMESTAMP
                    + " INTEGER" + ");")
            traceHandler.traceDebug("onCreate {$createArrivedTableStatement}")
            try {
                database.execSQL(createArrivedTableStatement)
                traceHandler.traceDebug("created the table")
            } catch (e: SQLException) {
                traceHandler.traceException("onCreate", e)
                throw e
            }
        }

        /**
         * To upgrade the database, drop and recreate our table
         *
         * @param db         the database
         * @param oldVersion ignored
         * @param newVersion ignored
         */
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            traceHandler.traceDebug("onUpgrade")
            try {
                db.execSQL("DROP TABLE IF EXISTS " + ARRIVED_MESSAGE_TABLE_NAME)
            } catch (e: SQLException) {
                traceHandler.traceException("onUpgrade", e)
                throw e
            }
            onCreate(db)
            traceHandler.traceDebug("onUpgrade complete")
        }

        companion object {
            private const val DATABASE_NAME = "mqttAndroidService.db"

            // database version, used to recognise when we need to upgrade
            // (delete and recreate)
            private const val DATABASE_VERSION = 1
        }

    }

    private inner class DbStoredData(
        override val messageId: String,
        override val clientHandle: String,
        override val topic: String,
        override val message: MqttMessage
    ) : StoredMessage

    /**
     * A way to get at the "setDuplicate" method of MqttMessage
     */
    private inner class MqttMessageHack(payload: ByteArray?) : MqttMessage(payload) {
        public override fun setDuplicate(dup: Boolean) {
            super.setDuplicate(dup)
        }
    }

    companion object {
        // One "private" database column name
        // The other database column names are defined in MqttServiceConstants
        private const val MTIMESTAMP = "mtimestamp"

        private const val ARRIVED_MESSAGE_TABLE_NAME = "MqttArrivedMessageTable"
    }
}
