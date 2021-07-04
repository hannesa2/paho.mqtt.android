package info.mqtt.android.service.storage

import org.eclipse.paho.client.mqttv3.MqttMessage


interface MessageStore {
    /**
     * Store a message and return an identifier for it
     *
     * @param clientHandle identifier for the client
     * @param message      message to be stored
     * @return a unique identifier for it
     */
    fun storeArrived(clientHandle: String?, topic: String?, message: MqttMessage?): String?

    /**
     * Discard a message - called when we are certain that an arrived message
     * has reached the application.
     *
     * @param clientHandle identifier for the client
     * @param id           id of message to be discarded
     */
    fun discardArrived(clientHandle: String?, id: String?): Boolean

    /**
     * Get all the stored messages, usually for a specific client
     *
     * @param clientHandle identifier for the client - if null, then messages for all
     * clients are returned
     */
    fun getAllArrivedMessages(clientHandle: String): Iterator<StoredMessage>

    /**
     * Discard stored messages, usually for a specific client
     *
     * @param clientHandle identifier for the client - if null, then messages for all
     * clients are discarded
     */
    fun clearArrivedMessages(clientHandle: String)
    fun close()

}
