package info.mqtt.android

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import info.hannes.timber.DebugFormatTree
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.MqttService
import info.mqtt.android.service.MqttServiceBinder
import info.mqtt.android.service.ping.AlarmPingSender
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * A client that is dropped while it isn't connected must stop for good.
 *
 * Before the fix, disconnect() skipped a client that wasn't connected, the service forgot it
 * anyway, and close() then closed an empty connection the service created on the spot. The real
 * Paho client kept auto-reconnecting with the same client id and fought any new client with that
 * id ("takeover" loop).
 *
 * Every test drops the client exactly the way EventHub does (see [letGo]) after putting it in a
 * precise state with a [TestBroker]. [OrphanPahoClientFakeBrokerTest] runs them against a local
 * fake broker inside the test process, so they need no network.
 */
abstract class OrphanPahoClientTestBase {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @get:Rule
    val nameRule = TestName()

    protected lateinit var broker: TestBroker
    private lateinit var service: MqttService
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val clients = mutableListOf<MqttAndroidClient>()

    abstract fun createBroker(): TestBroker

    @Before
    fun setUp() {
        if (Timber.treeCount == 0) Timber.plant(DebugFormatTree())
        broker = createBroker()
        val binder = serviceRule.bindService(Intent(context, MqttService::class.java))
        service = (binder as MqttServiceBinder).service
    }

    @After
    fun tearDown() {
        clients.forEach { letGo(it) }
        broker.stop()
    }

    // ── States ──

    @Test
    fun droppedWhileWaitingToReconnect_neverConnectsAgain() {
        val id = clientId()
        val client = newClient(id)
        connect(client)
        loseConnectionAndKeepRefusing(client)

        letGo(client)
        val since = settleThenAccept()

        Thread.sleep(OBSERVE_MS)
        assertEquals("dropped client sent CONNECT again", 0, broker.connectAttemptsSince(id, since))
        assertEquals("dropped client got connected again", 0, broker.connacksSince(id, since))
        assertFalse(broker.isLive(id))
        assertPingSenderReleased(id)
    }

    @Test
    fun droppedWhileConnecting_abortsAndNeverConnectsAgain() {
        val id = clientId()
        val client = newClient(id)
        connect(client)
        loseConnectionAndHangNextConnect(client, id)

        letGo(client)
        val since = settleThenAccept()
        assertEquals("the hanging connect was not aborted", 0, broker.hangingConnects())
        broker.releaseHanging() // a slow network finally answers

        Thread.sleep(OBSERVE_MS)
        assertEquals("dropped client sent CONNECT again", 0, broker.connectAttemptsSince(id, since))
        assertEquals("dropped client got connected again", 0, broker.connacksSince(id, since))
        assertFalse(broker.isLive(id))
        assertPingSenderReleased(id)
    }

    @Test
    fun droppedWhileConnected_disconnectsCleanlyAndNeverConnectsAgain() {
        val id = clientId()
        val client = newClient(id)
        connect(client)
        val since = System.currentTimeMillis()

        letGo(client)

        assertTrue("no DISCONNECT packet", waitUntil(5_000) { broker.cleanDisconnects(id) == 1 })
        Thread.sleep(OBSERVE_MS)
        assertEquals("dropped client sent CONNECT again", 0, broker.connectAttemptsSince(id, since))
        assertFalse(broker.isLive(id))
        assertPingSenderReleased(id)
    }

    @Test
    fun droppedWhileConnected_thenNetworkLost_neverConnectsAgain() {
        val id = clientId()
        val client = newClient(id)
        connect(client)

        letGo(client)
        broker.dropAll() // a late network loss must not wake the dropped client up
        val since = settleThenAccept()

        Thread.sleep(OBSERVE_MS)
        assertEquals("dropped client sent CONNECT again", 0, broker.connectAttemptsSince(id, since))
    }

    @Test
    fun drop_doesNotChangeTheAppsConnectOptions() {
        // The app owns the options object and may reuse it for its next connection.
        val id = clientId()
        val client = newClient(id)
        val options = options()
        connect(client, options)
        loseConnectionAndKeepRefusing(client)

        letGo(client)
        settleThenAccept()

        assertTrue("auto-reconnect was switched off on the app's options", options.isAutomaticReconnect)
    }

    // ── A new client with the same id (what the app does next) ──

    @Test
    fun newClientAfterDropWhileWaiting_isNotKickedOff() {
        val id = clientId()
        val old = newClient(id)
        connect(old)
        loseConnectionAndKeepRefusing(old)
        letGo(old)
        settleThenAccept()

        assertNewClientStaysAlone(id)
    }

    @Test
    fun newClientAfterDropWhileConnecting_isNotKickedOff() {
        val id = clientId()
        val old = newClient(id)
        connect(old)
        loseConnectionAndHangNextConnect(old, id)
        letGo(old)
        settleThenAccept()

        // A slow network answers the old client's connect after the new client is up.
        assertNewClientStaysAlone(id) { broker.releaseHanging() }
    }

    @Test
    fun newClientAfterDroppedTwice_isNotKickedOff() {
        // EventHub can call letGo again from finishClosing(); dropping twice must be harmless.
        val id = clientId()
        val old = newClient(id)
        connect(old)
        loseConnectionAndKeepRefusing(old)
        letGo(old)
        letGo(old)
        settleThenAccept()

        assertNewClientStaysAlone(id)
    }

    @Test
    fun newClientRightAfterDrop_withoutSettling_isNotKickedOff() {
        // The token refresh case: EventHub's reconnectWith() makes the new client right after the drop.
        val id = clientId()
        val old = newClient(id)
        connect(old)
        loseConnectionAndKeepRefusing(old)
        letGo(old)
        broker.mode = TestBroker.Mode.ACCEPT

        assertNewClientStaysAlone(id)
    }

    @Test
    fun newClientRightAfterDropWhileConnecting_withoutSettling_connectsAndIsNotKickedOff() {
        // Token refresh while the old client is stuck mid-connect: the new client is made right away.
        // Both share Paho's persistence folder, so the old one must be fully closed by then.
        val id = clientId()
        val old = newClient(id)
        connect(old)
        loseConnectionAndHangNextConnect(old, id)
        letGo(old)
        broker.mode = TestBroker.Mode.ACCEPT

        assertNewClientStaysAlone(id) { broker.releaseHanging() }
    }

    @Test
    fun newClientRightAfterDropWhileConnected_withoutSettling_connectsAndIsNotKickedOff() {
        // Token refresh while connected, the most common case.
        val id = clientId()
        val old = newClient(id)
        connect(old)
        letGo(old)

        assertNewClientStaysAlone(id)
    }

    // ── The service side ──

    @Test
    fun closeAfterDrop_doesNotLeaveAnEmptyConnectionInTheService() {
        val id = clientId()
        val client = newClient(id)
        connect(client)
        loseConnectionAndKeepRefusing(client)

        letGo(client)

        assertFalse(
            "close() left a connection for $id in the service",
            service.connections.keys.any { it.contains(id) }
        )
    }

    @Test
    fun isConnectedAndClose_rightAfterDisconnect_doNotThrow() {
        // The service forgets the handle as soon as disconnect() runs, but the client keeps its copy
        // until the disconnect answer arrives. Calls in between used to throw "Invalid ClientHandle".
        val id = clientId()
        val client = newClient(id)
        connect(client)

        client.disconnect(0) // don't wait for the answer
        val isConnected = runCatching { client.isConnected }
        val close = runCatching { client.close() }

        assertTrue("isConnected threw: ${isConnected.exceptionOrNull()}", isConnected.isSuccess)
        assertFalse("isConnected after disconnect", isConnected.getOrThrow())
        assertTrue("close threw: ${close.exceptionOrNull()}", close.isSuccess)
        assertTrue("no DISCONNECT packet", waitUntil(5_000) { broker.cleanDisconnects(id) == 1 })
    }

    // ── Helpers ──

    /** Same calls, same order, same timeouts as EventHub's Connection.letGo(). */
    private fun letGo(client: MqttAndroidClient) {
        runCatching { client.disconnect(0).waitForCompletion(2_000) }
        runCatching { client.close() }
        runCatching { client.unregisterResources() }
    }

    /** Network loss, then every reconnect attempt fails: Paho waits between attempts. */
    private fun loseConnectionAndKeepRefusing(client: MqttAndroidClient) {
        broker.mode = TestBroker.Mode.REFUSE
        val refusedBefore = broker.refusedConnections()
        broker.dropAll()
        assertTrue("client didn't notice the lost connection", waitUntil(10_000) { !client.isConnectedSafe() })
        assertTrue("Paho didn't try to reconnect", waitUntil(10_000) { broker.refusedConnections() > refusedBefore })
    }

    /** Network loss, then the next reconnect attempt hangs: Paho is stuck mid-connect. */
    private fun loseConnectionAndHangNextConnect(client: MqttAndroidClient, id: String) {
        broker.mode = TestBroker.Mode.HANG
        val attemptsBefore = broker.connectAttempts(id)
        broker.dropAll()
        assertTrue("client didn't notice the lost connection", waitUntil(10_000) { !client.isConnectedSafe() })
        assertTrue("Paho didn't start a reconnect", waitUntil(10_000) { broker.connectAttempts(id) > attemptsBefore })
    }

    /**
     * Gives the drop time to finish, then lets connections through again. Anything this client id
     * does from now on is counted against it.
     */
    private fun settleThenAccept(): Long {
        Thread.sleep(SETTLE_MS)
        val since = System.currentTimeMillis()
        broker.mode = TestBroker.Mode.ACCEPT
        return since
    }

    /** A fresh client with the same id connects and must stay connected, with nobody fighting it. */
    private fun assertNewClientStaysAlone(id: String, afterConnect: () -> Unit = {}) {
        val completes = AtomicInteger()
        val losses = AtomicInteger()
        val fresh = newClient(id)
        fresh.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                completes.incrementAndGet()
            }

            override fun connectionLost(cause: Throwable?) {
                losses.incrementAndGet()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) = Unit
            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })
        connect(fresh)
        afterConnect()

        Thread.sleep(OBSERVE_MS)
        assertEquals("another client with the same id took over", 0, broker.takeovers(id))
        assertEquals("the new client lost its connection", 0, losses.get())
        assertEquals("the new client got someone else's connect events", 1, completes.get())
        assertTrue(fresh.isConnectedSafe())
    }

    private fun assertPingSenderReleased(id: String) {
        assertFalse("ClientComms kept after the drop", AlarmPingSender.clientCommsMap.containsKey(id))
    }

    // Unique per test, so tests never see each other's clients.
    private fun clientId() = "orphan-${nameRule.methodName.take(16)}-${UUID.randomUUID().toString().take(8)}"

    private fun newClient(id: String) = MqttAndroidClient(context, broker.uri, id).also { clients += it }

    private fun connect(client: MqttAndroidClient, options: MqttConnectOptions = options()) {
        client.connect(options).waitForCompletion(15_000)
        assertTrue("initial connect failed", waitUntil(10_000) { client.isConnectedSafe() })
    }

    // Like EventHub: auto-reconnect on. Short delays so a leftover client shows up fast.
    private fun options() = MqttConnectOptions().apply {
        isAutomaticReconnect = true
        maxReconnectDelay = 2_000
        connectionTimeout = 10
        keepAliveInterval = 60
        isCleanSession = true
    }

    private fun MqttAndroidClient.isConnectedSafe() = runCatching { isConnected }.getOrDefault(false)

    protected fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (condition()) return true
            Thread.sleep(50)
        }
        return condition()
    }

    private companion object {
        /** Time for a drop to finish shutting the client down. */
        const val SETTLE_MS = 1_000L

        /**
         * How long to watch for a leftover client. With maxReconnectDelay = 2s, a leftover client
         * that is waiting reconnects well within this, and a released hanging connect is instant.
         */
        const val OBSERVE_MS = 9_000L
    }
}

/** Local fake broker: exact control, no network needed. */
@RunWith(AndroidJUnit4::class)
class OrphanPahoClientFakeBrokerTest : OrphanPahoClientTestBase() {
    override fun createBroker(): TestBroker = FakeMqttBroker()
}
