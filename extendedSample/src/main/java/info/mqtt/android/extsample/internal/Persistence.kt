package info.mqtt.android.extsample.internal

import info.mqtt.android.extsample.activity.Connection.Companion.createConnection
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.database.sqlite.SQLiteDatabase
import kotlin.Throws
import android.content.ContentValues
import android.content.Context
import info.mqtt.android.extsample.activity.Connection
import info.mqtt.android.extsample.model.Subscription
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import timber.log.Timber
import java.util.ArrayList

class Persistence(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION), BaseColumns {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
        db.execSQL(SQL_CREATE_SUBSCRIPTION_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        db.execSQL(SQL_DELETE_SUBSCRIPTION_ENTRIES)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    @Throws(PersistenceException::class)
    fun persistConnection(connection: Connection) {
        val db = writableDatabase

        //insert the values into the tables, returns the ID for the row
        val newRowId = db.insert(TABLE_CONNECTIONS, null, getValues(connection))
        db.close() //close the db then deal with the result of the query
        if (newRowId == -1L) {
            throw PersistenceException("Failed to persist connection: " + connection.handle())
        } else { //Successfully persisted assigning persistenceID
            connection.persistenceId = newRowId
        }
    }

    fun updateConnection(connection: Connection) {
        val db = writableDatabase
        val whereClause = BaseColumns._ID + "=?"
        val whereArgs = arrayOfNulls<String>(1)
        whereArgs[0] = connection.persistenceId.toString()
        db.update(TABLE_CONNECTIONS, getValues(connection), whereClause, whereArgs)
    }

    private fun getValues(connection: Connection): ContentValues {
        val conOpts = connection.connectionOptions
        val lastWill = conOpts!!.willMessage
        val values = ContentValues()

        //put the column values object
        values.put(COLUMN_CLIENT_HANDLE, connection.handle())
        values.put(COLUMN_HOST, connection.hostName)
        values.put(COLUMN_port, connection.port)
        values.put(COLUMN_client_ID, connection.id)
        values.put(COLUMN_ssl, connection.isSSL)
        values.put(COLUMN_KEEP_ALIVE, conOpts.keepAliveInterval)
        values.put(COLUMN_TIME_OUT, conOpts.connectionTimeout)
        values.put(COLUMN_USER_NAME, conOpts.userName)
        values.put(COLUMN_TOPIC, conOpts.willDestination)

        //uses "condition ? trueValue: falseValue" for in line converting of values
        val password = conOpts.password
        values.put(COLUMN_CLEAN_SESSION, if (conOpts.isCleanSession) 1 else 0) //convert boolean to int and then put in values
        values.put(COLUMN_PASSWORD, password?.let { String(it) }) //convert char[] to String
        values.put(COLUMN_MESSAGE, if (lastWill != null) String(lastWill.payload) else null) // convert byte[] to string
        values.put(COLUMN_QOS, lastWill?.qos ?: 0)
        if (lastWill == null) {
            values.put(COLUMN_RETAINED, 0)
        } else {
            values.put(COLUMN_RETAINED, if (lastWill.isRetained) 1 else 0) //convert from boolean to int
        }
        return values
    }

    /**
     * Persist a Subscription to the database
     *
     * @param subscription the subscription to persist
     * @throws PersistenceException If storing the data fails
     */
    @Throws(PersistenceException::class)
    fun persistSubscription(subscription: Subscription): Long {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_CLIENT_HANDLE, subscription.clientHandle)
        values.put(SUBSCRIPTIONS_COLUMN_TOPIC, subscription.topic)
        values.put(SUBSCRIPTIONS_COLUMN_NOTIFY, if (subscription.isEnableNotifications) 1 else 0) //convert boolean to int and then put in values
        values.put(SUBSCRIPTIONS_COLUMN_QOS, subscription.qos)
        val newRowId = db.insert(TABLE_SUBSCRIPTIONS, null, values)
        db.close()
        return if (newRowId == -1L) {
            throw PersistenceException("Failed to persist subscription: $subscription")
        } else {
            subscription.persistenceId = newRowId
            newRowId
        }
    }

    /**
     * Deletes a subscription from the database
     *
     * @param subscription The subscription to delete from the database
     */
    fun deleteSubscription(subscription: Subscription) {
        Timber.d("Deleting Subscription: $subscription")
        val db = writableDatabase
        db.delete(TABLE_SUBSCRIPTIONS, BaseColumns._ID + "=?", arrayOf(subscription.persistenceId.toString()))
        db.close()
        //don't care if it failed, means it's not in the db therefore no need to delete
    }

    /**
     * Recreates connection objects based upon information stored in the database
     *
     * @param context Context for creating [Connection] objects
     * @return list of connections that have been restored
     * @throws PersistenceException if restoring connections fails, this is thrown
     */
    @Throws(PersistenceException::class)
    fun restoreConnections(context: Context?): List<Connection> {

        //columns to return
        val connectionColumns = arrayOf(
            COLUMN_CLIENT_HANDLE,
            COLUMN_HOST,
            COLUMN_port,
            COLUMN_client_ID,
            COLUMN_ssl,
            COLUMN_KEEP_ALIVE,
            COLUMN_CLEAN_SESSION,
            COLUMN_TIME_OUT,
            COLUMN_USER_NAME,
            COLUMN_PASSWORD,
            COLUMN_TOPIC,
            COLUMN_MESSAGE,
            COLUMN_RETAINED,
            COLUMN_QOS,
            BaseColumns._ID
        )

        // Columns to return for subscription
        val subscriptionColumns = arrayOf(
            COLUMN_CLIENT_HANDLE,
            SUBSCRIPTIONS_COLUMN_TOPIC,
            SUBSCRIPTIONS_COLUMN_NOTIFY,
            SUBSCRIPTIONS_COLUMN_QOS,
            BaseColumns._ID
        )
        val subscriptionWhereQuery = "$COLUMN_CLIENT_HANDLE=?"

        //how to sort the data being returned
        val sort = COLUMN_HOST
        val db = readableDatabase
        val cursor = db.query(TABLE_CONNECTIONS, connectionColumns, null, null, null, null, sort)
        val list = ArrayList<Connection>(cursor.count)
        var connection: Connection
        for (i in 0 until cursor.count) {
            if (!cursor.moveToNext()) { //move to the next item throw persistence exception, if it fails
                throw PersistenceException("Failed restoring connection - count: " + cursor.count + "loop iteration: " + i)
            }
            //get data from cursor
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            //basic client information
            val clientHandle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_HANDLE))
            val host = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_HOST))
            val clientID = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_client_ID))
            val port = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_port))

            //connect options strings
            val username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_NAME))
            val password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD))
            val topic = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TOPIC))
            val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))

            //connect options integers
            val qos = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QOS))
            val keepAlive = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_KEEP_ALIVE))
            val timeout = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TIME_OUT))

            //get all values that need converting and convert integers to booleans in line using "condition ? trueValue : falseValue"
            val cleanSession = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLEAN_SESSION)) == 1
            val retained = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RETAINED)) == 1
            val ssl = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ssl)) == 1

            //rebuild objects starting with the connect options
            val opts = MqttConnectOptions()
            opts.isCleanSession = cleanSession
            opts.keepAliveInterval = keepAlive
            opts.connectionTimeout = timeout
            opts.password = password?.toCharArray() ?: "".toCharArray()
            opts.userName = username
            if (topic != null) {
                opts.setWill(topic, message.toByteArray(), qos, retained)
            }

            //now create the connection object
            connection = createConnection(clientHandle, clientID, host, port, context!!, ssl)
            connection.addConnectionOptions(opts)
            connection.persistenceId = id

            // Now we recover all subscriptions for this connection
            val args = arrayOf(clientHandle)
            Timber.d("SUB: $connection")
            val subC = db.query(TABLE_SUBSCRIPTIONS, subscriptionColumns, subscriptionWhereQuery, args, null, null, sort)
            val subscriptions = ArrayList<Subscription>(subC.count)
            for (x in 0 until subC.count) {
                if (!subC.moveToNext()) { //move to the next item throw persistence exception, if it fails
                    throw PersistenceException("Failed restoring subscription - count: " + subC.count + "loop iteration: " + x)
                }
                val subId = subC.getLong(subC.getColumnIndexOrThrow(BaseColumns._ID))
                val subClientHandle = subC.getString(subC.getColumnIndexOrThrow(COLUMN_CLIENT_HANDLE))
                val subTopic = subC.getString(subC.getColumnIndexOrThrow(SUBSCRIPTIONS_COLUMN_TOPIC))
                val subNotify = subC.getInt(subC.getColumnIndexOrThrow(SUBSCRIPTIONS_COLUMN_NOTIFY)) == 1
                val subQos = subC.getInt(subC.getColumnIndexOrThrow(SUBSCRIPTIONS_COLUMN_QOS))
                val sub = Subscription(subTopic, subQos, subClientHandle, subNotify)
                sub.persistenceId = subId
                Timber.d("Restoring Subscription: $sub")
                subscriptions.add(sub)
            }
            subC.close()
            connection.setSubscriptions(subscriptions)

            list.add(connection)
        }
        cursor.close()
        db.close()
        return list
    }

    /**
     * Deletes a connection from the database
     *
     * @param connection The connection to delete from the database
     */
    fun deleteConnection(connection: Connection) {
        val db = writableDatabase
        db.delete(TABLE_CONNECTIONS, BaseColumns._ID + "=?", arrayOf(connection.persistenceId.toString()))
        db.close()
        //don't care if it failed, means it's not in the db therefore no need to delete
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "connections.db"
        private const val TABLE_CONNECTIONS = "connections"
        private const val COLUMN_CLIENT_HANDLE = "clientHandle"
        private const val COLUMN_HOST = "host"
        private const val COLUMN_client_ID = "clientID"
        private const val COLUMN_port = "port"
        private const val COLUMN_ssl = "ssl"

        //connection options
        private const val COLUMN_TIME_OUT = "timeout"
        private const val COLUMN_KEEP_ALIVE = "keepalive"
        private const val COLUMN_USER_NAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_CLEAN_SESSION = "cleanSession"
        private const val COLUMN_TOPIC = "topic"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_QOS = "qos"
        private const val COLUMN_RETAINED = "retained"

        private const val TABLE_SUBSCRIPTIONS = "subscriptions"

        private const val SUBSCRIPTIONS_COLUMN_TOPIC = "topic"
        private const val SUBSCRIPTIONS_COLUMN_QOS = "qos"
        private const val SUBSCRIPTIONS_COLUMN_NOTIFY = "notify"

        private const val TEXT_TYPE = " TEXT"
        private const val INT_TYPE = " INTEGER"

        private const val COMMA_SEP = ","

        private const val SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_CONNECTIONS + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY," +
                COLUMN_CLIENT_HANDLE + TEXT_TYPE + COMMA_SEP +
                COLUMN_HOST + TEXT_TYPE + COMMA_SEP +
                COLUMN_client_ID + TEXT_TYPE + COMMA_SEP +
                COLUMN_port + INT_TYPE + COMMA_SEP +
                COLUMN_ssl + INT_TYPE + COMMA_SEP +
                COLUMN_TIME_OUT + INT_TYPE + COMMA_SEP +
                COLUMN_KEEP_ALIVE + INT_TYPE + COMMA_SEP +
                COLUMN_USER_NAME + TEXT_TYPE + COMMA_SEP +
                COLUMN_PASSWORD + TEXT_TYPE + COMMA_SEP +
                COLUMN_CLEAN_SESSION + INT_TYPE + COMMA_SEP +
                COLUMN_TOPIC + TEXT_TYPE + COMMA_SEP +
                COLUMN_MESSAGE + TEXT_TYPE + COMMA_SEP +
                COLUMN_QOS + INT_TYPE + COMMA_SEP +
                COLUMN_RETAINED + " INTEGER);"
        private const val SQL_CREATE_SUBSCRIPTION_ENTRIES = "CREATE TABLE " + TABLE_SUBSCRIPTIONS + " (" +
                BaseColumns._ID + " INTEGER PRIMARY KEY," +
                COLUMN_CLIENT_HANDLE + TEXT_TYPE + COMMA_SEP +
                COLUMN_HOST + TEXT_TYPE + COMMA_SEP +
                SUBSCRIPTIONS_COLUMN_TOPIC + TEXT_TYPE + COMMA_SEP +
                SUBSCRIPTIONS_COLUMN_NOTIFY + INT_TYPE + COMMA_SEP +
                SUBSCRIPTIONS_COLUMN_QOS + INT_TYPE + ");"

        /**
         * Delete tables entry
         */
        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS $TABLE_CONNECTIONS"
        private const val SQL_DELETE_SUBSCRIPTION_ENTRIES = "DROP TABLE IF EXISTS $TABLE_SUBSCRIPTIONS"
    }
}
