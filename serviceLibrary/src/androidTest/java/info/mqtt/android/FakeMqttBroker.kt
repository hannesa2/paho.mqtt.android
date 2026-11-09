package info.mqtt.android

import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * A tiny MQTT 3.1.1 broker for tests, running on the device's localhost.
 *
 * It does just enough for Paho to connect, subscribe and ping, and lets a test put the client in
 * states a real broker can't produce on demand:
 * - [TestBroker.Mode.REFUSE]: close every new socket right away, so Paho keeps failing and waits between
 *   auto-reconnect attempts (state DISCONNECTED).
 * - [TestBroker.Mode.HANG]: read the CONNECT and never answer, so Paho stays stuck mid-connect (CONNECTING).
 * - [dropAll]: cut every live connection, like a network loss.
 *
 * Like a real broker, a CONNECT with a client id that already has a live session closes the
 * older one (a "takeover"). That's how two clients with the same id fight.
 */
class FakeMqttBroker : TestBroker {

    @Volatile
    override var mode = TestBroker.Mode.ACCEPT

    private val server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
    override val uri: String = "tcp://127.0.0.1:${server.localPort}"

    private class Session(val clientId: String, val socket: Socket)

    private val sockets = CopyOnWriteArrayList<Socket>()

    /** CONNECTs received in [TestBroker.Mode.HANG] and not answered yet. */
    private val hanging = ConcurrentHashMap<Socket, Pair<String, OutputStream>>()
    private val live = ConcurrentHashMap<String, Session>()

    /** Every CONNECT packet received, per client id, with the time it arrived. */
    private val connectTimes = ConcurrentHashMap<String, CopyOnWriteArrayList<Long>>()

    /** Every CONNACK sent (a successful connect), per client id. */
    private val connackTimes = ConcurrentHashMap<String, CopyOnWriteArrayList<Long>>()

    private val takeovers = ConcurrentHashMap<String, AtomicInteger>()
    private val cleanDisconnects = ConcurrentHashMap<String, AtomicInteger>()

    /** Sockets closed right away in [TestBroker.Mode.REFUSE]: each one is a failed reconnect attempt. */
    private val refused = AtomicInteger()

    @Volatile
    private var running = true

    init {
        thread(name = "FakeMqttBroker-accept", isDaemon = true) {
            while (running) {
                val socket = try {
                    server.accept()
                } catch (e: IOException) {
                    break
                }
                if (mode == TestBroker.Mode.REFUSE) {
                    refused.incrementAndGet()
                    runCatching { socket.close() }
                    continue
                }
                sockets += socket
                thread(name = "FakeMqttBroker-conn", isDaemon = true) { serve(socket) }
            }
        }
    }

    override fun refusedConnections(): Int = refused.get()

    /**
     * Answers every CONNECT that is still hanging, like a slow network finally delivering the
     * CONNACK. A client that was dropped but never stopped gets connected here.
     */
    override fun releaseHanging() {
        hanging.entries.toList().forEach { (socket, pending) ->
            hanging.remove(socket)
            if (!socket.isClosed) runCatching { accept(pending.first, socket, pending.second) }
        }
    }

    /** CONNECTs still waiting for an answer whose socket the client hasn't closed. */
    override fun hangingConnects(): Int = hanging.keys.count { !it.isClosed }
    override fun connectAttempts(clientId: String): Int = connectTimes[clientId]?.size ?: 0
    override fun connectAttemptsSince(clientId: String, time: Long): Int = connectTimes[clientId]?.count { it >= time } ?: 0
    override fun connacksSince(clientId: String, time: Long): Int = connackTimes[clientId]?.count { it >= time } ?: 0
    override fun isLive(clientId: String): Boolean = live.containsKey(clientId)
    override fun takeovers(clientId: String): Int = takeovers[clientId]?.get() ?: 0
    override fun cleanDisconnects(clientId: String): Int = cleanDisconnects[clientId]?.get() ?: 0

    /** Sockets that are still open on the broker side (live sessions plus hanging connects). */
    fun openSockets(): Int = sockets.count { !it.isClosed }

    /** Cuts every connection, like the network going away. */
    override fun dropAll() {
        sockets.forEach { runCatching { it.close() } }
        sockets.clear()
        live.clear()
        hanging.clear()
    }

    override fun stop() {
        running = false
        runCatching { server.close() }
        dropAll()
    }

    private fun serve(socket: Socket) {
        var clientId: String? = null
        try {
            val input = DataInputStream(socket.getInputStream())
            val output = socket.getOutputStream()
            while (true) {
                val header = input.readUnsignedByte()
                val body = ByteArray(readRemainingLength(input))
                input.readFully(body)
                when (header shr 4) {
                    CONNECT -> {
                        val id = parseClientId(body)
                        clientId = id
                        connectTimes.getOrPut(id) { CopyOnWriteArrayList() } += System.currentTimeMillis()
                        if (mode == TestBroker.Mode.HANG) {
                            hanging[socket] = id to output // no answer: Paho stays CONNECTING
                            continue
                        }
                        accept(id, socket, output)
                    }
                    SUBSCRIBE -> {
                        val topics = countSubscribeTopics(body)
                        val ack = ByteArray(4 + topics)
                        ack[0] = 0x90.toByte()
                        ack[1] = (2 + topics).toByte()
                        ack[2] = body[0]
                        ack[3] = body[1]
                        send(output, ack)
                    }
                    UNSUBSCRIBE -> send(output, byteArrayOf(0xB0.toByte(), 0x02, body[0], body[1]))
                    PUBLISH -> {
                        val qos = (header shr 1) and 0x03
                        if (qos > 0) {
                            val topicLength = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF)
                            val idIndex = 2 + topicLength
                            send(output, byteArrayOf(0x40, 0x02, body[idIndex], body[idIndex + 1]))
                        }
                    }
                    PINGREQ -> send(output, byteArrayOf(0xD0.toByte(), 0x00))
                    DISCONNECT -> {
                        clientId?.let { cleanDisconnects.getOrPut(it) { AtomicInteger() }.incrementAndGet() }
                        break
                    }
                }
            }
        } catch (e: EOFException) {
            // client closed the socket
        } catch (e: IOException) {
            // socket closed by dropAll() or a takeover
        } finally {
            hanging.remove(socket)
            live.entries.removeIf { it.value.socket === socket }
            runCatching { socket.close() }
        }
    }

    private fun accept(id: String, socket: Socket, output: OutputStream) {
        live.put(id, Session(id, socket))?.let { older ->
            // Same client id already connected: the broker drops the older one.
            takeovers.getOrPut(id) { AtomicInteger() }.incrementAndGet()
            runCatching { older.socket.close() }
        }
        connackTimes.getOrPut(id) { CopyOnWriteArrayList() } += System.currentTimeMillis()
        send(output, byteArrayOf(0x20, 0x02, 0x00, 0x00))
    }

    private fun send(output: OutputStream, bytes: ByteArray) {
        synchronized(output) {
            output.write(bytes)
            output.flush()
        }
    }

    private fun readRemainingLength(input: DataInputStream): Int {
        var multiplier = 1
        var value = 0
        while (true) {
            val digit = input.readUnsignedByte()
            value += (digit and 0x7F) * multiplier
            if (digit and 0x80 == 0) return value
            multiplier *= 128
        }
    }

    // CONNECT variable header: protocol name (2-byte length + text), level (1), flags (1),
    // keep alive (2). The payload starts with the client id (2-byte length + text).
    private fun parseClientId(body: ByteArray): String {
        val nameLength = ((body[0].toInt() and 0xFF) shl 8) or (body[1].toInt() and 0xFF)
        val idStart = 2 + nameLength + 1 + 1 + 2
        val idLength = ((body[idStart].toInt() and 0xFF) shl 8) or (body[idStart + 1].toInt() and 0xFF)
        return String(body, idStart + 2, idLength, Charsets.UTF_8)
    }

    // SUBSCRIBE: packet id (2), then (topic length (2) + topic + qos (1)) repeated.
    private fun countSubscribeTopics(body: ByteArray): Int {
        var index = 2
        var count = 0
        while (index < body.size) {
            val length = ((body[index].toInt() and 0xFF) shl 8) or (body[index + 1].toInt() and 0xFF)
            index += 2 + length + 1
            count++
        }
        return count
    }

    private companion object {
        const val CONNECT = 1
        const val PUBLISH = 3
        const val SUBSCRIBE = 8
        const val UNSUBSCRIBE = 10
        const val PINGREQ = 12
        const val DISCONNECT = 14
    }
}
