package info.mqtt.android

/**
 * What the orphan-client tests need from a broker: put the client in a precise network state, and
 * count what each client id does. Implemented by [FakeMqttBroker] (local, exact timing); a proxy in
 * front of a real broker can implement it too.
 */
interface TestBroker {

    enum class Mode {
        /** Let connections through. */
        ACCEPT,

        /** Close every new socket right away: Paho fails and waits between reconnect attempts. */
        REFUSE,

        /** Hold the CONNECT and never answer: Paho stays stuck mid-connect (CONNECTING). */
        HANG,
    }

    var mode: Mode
    val uri: String

    fun refusedConnections(): Int
    fun connectAttempts(clientId: String): Int
    fun connectAttemptsSince(clientId: String, time: Long): Int
    fun connacksSince(clientId: String, time: Long): Int
    fun isLive(clientId: String): Boolean

    /** A client id connected while another connection with that id was still live. */
    fun takeovers(clientId: String): Int
    fun cleanDisconnects(clientId: String): Int

    /** CONNECTs still held in [Mode.HANG] whose socket the client hasn't closed. */
    fun hangingConnects(): Int

    /** Lets every held CONNECT through, like a slow network finally answering. */
    fun releaseHanging()

    /** Cuts every connection, like the network going away. */
    fun dropAll()
    fun stop()
}
