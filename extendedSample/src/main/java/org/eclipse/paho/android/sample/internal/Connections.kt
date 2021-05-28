package org.eclipse.paho.android.sample.internal

import android.content.Context
import org.eclipse.paho.android.sample.activity.Connection
import java.util.HashMap
import timber.log.Timber


class Connections private constructor(context: Context) {

    var connections: HashMap<String, Connection> = HashMap()

    private var persistence: Persistence = Persistence(context)

    init {
        try {
            val connectionList = persistence.restoreConnections(context)
            for (connection in connectionList) {
                Timber.d("Connection was persisted.. ${connection.handle()}")
                connections[connection.handle()] = connection
            }
        } catch (e: PersistenceException) {
            Timber.e(e)
        }
    }

    fun getConnection(handle: String): Connection? {
        return connections[handle]
    }

    fun addConnection(connection: Connection) {
        connections[connection.handle()] = connection
        try {
            persistence.persistConnection(connection)
        } catch (e: PersistenceException) {
            Timber.e(e)
        }
    }

    fun removeConnection(connection: Connection) {
        connections.remove(connection.handle())
        persistence.deleteConnection(connection)
    }

    fun updateConnection(connection: Connection) {
        connections[connection.handle()] = connection
        persistence.updateConnection(connection)
    }

    companion object {
        private var instance: Connections? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): Connections {
            if (instance == null) {
                instance = Connections(context)
            }
            return instance!!
        }
    }
}
