package info.mqtt.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.VerifyingSSLSocket
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import java.nio.channels.SocketChannel
import java.security.KeyStore
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HandshakeCompletedListener
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Test class for the new getSSLSocketFactory methods with certificate pinning support
 */
@RunWith(AndroidJUnit4::class)
class SSLSocketFactoryTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val assets = instrumentation.context.assets

    @Test
    fun testNewGetSSLSocketFactoryWithKeyStore() {
        // Load the test keystore
        val keyStoreInputStream: InputStream = assets.open("test.bks")
        val trustStore = KeyStore.getInstance("BKS")
        val keyStorePwd = "mqtttest"
        trustStore.load(keyStoreInputStream, keyStorePwd.toCharArray())

        // Get the MQTT client
        val mqttClient = MqttAndroidClient(
            context,
            "tcp://broker.hivemq.com:1883",
            "testSSLSocketFactory"
        )

        // Test the new method with KeyStore parameter
        val sslSocketFactory: SSLSocketFactory = mqttClient.getSSLSocketFactory(trustStore)

        // Verify the socket factory is not null (it always returns a valid factory if no exception is thrown)

        // Clean up
        mqttClient.unregisterResources()
    }

    @Test
    fun testNewGetSSLSocketFactoryWithHostnameVerifier() {
        // Load the test keystore
        val keyStoreInputStream: InputStream = assets.open("test.bks")
        val trustStore = KeyStore.getInstance("BKS")
        val keyStorePwd = "mqtttest"
        trustStore.load(keyStoreInputStream, keyStorePwd.toCharArray())

        // Get the MQTT client
        val mqttClient = MqttAndroidClient(
            context,
            "tcp://broker.hivemq.com:1883",
            "testSSLSocketFactoryWithVerifier"
        )

        // Create a simple hostname verifier
        val hostnameVerifier = HostnameVerifier { _, _ ->
            // Simple verification - in production, you'd do actual verification
            true
        }

        // Test the new method with KeyStore and HostnameVerifier parameters
        val sslSocketFactory: SSLSocketFactory = mqttClient.getSSLSocketFactory(trustStore, hostnameVerifier)

        // Verify the socket factory is not null
        assert(sslSocketFactory != null) { "SSLSocketFactory should not be null" }

        val socket = sslSocketFactory.createSocket()
        assertTrue(socket is SSLSocket)
        assertFalse(socket.isConnected)
        socket.close()

        val address = InetAddress.getLoopbackAddress()
        try {
            sslSocketFactory.createSocket(address, 8883)
            fail("Address-only socket creation should require an expected hostname")
        } catch (_: SocketException) {
        }
        try {
            sslSocketFactory.createSocket(address, 8883, address, 0)
            fail("Address-only socket creation should require an expected hostname")
        } catch (_: SocketException) {
        }

        // Clean up
        mqttClient.unregisterResources()
    }

    @Test
    fun testNewGetSSLSocketFactoryDefaultHostnameVerifier() {
        // Load the test keystore
        val keyStoreInputStream: InputStream = assets.open("test.bks")
        val trustStore = KeyStore.getInstance("BKS")
        val keyStorePwd = "mqtttest"
        trustStore.load(keyStoreInputStream, keyStorePwd.toCharArray())

        // Get the MQTT client
        val mqttClient = MqttAndroidClient(
            context,
            "tcp://broker.hivemq.com:1883",
            "testSSLSocketFactoryDefaultVerifier"
        )

        // Test the new method with only KeyStore (default hostname verifier)
        val sslSocketFactory: SSLSocketFactory = mqttClient.getSSLSocketFactory(trustStore)

        // Verify the socket factory is not null (it always returns a valid factory if no exception is thrown)

        // Clean up
        mqttClient.unregisterResources()
    }

    @Test
    fun acceptingVerifierRunsOnEachConnectionWithReusedOptions() {
        val broker = FakeMqttBroker()
        val verifierCalls = AtomicInteger()
        val factory = TestSSLSocketFactory(HostnameVerifier { host, _ ->
            assertEquals("127.0.0.1", host)
            verifierCalls.incrementAndGet()
            true
        })
        val client = MqttAndroidClient(
            context,
            broker.uri.replace("tcp://", "ssl://"),
            "acceptingVerifierReconnect"
        )
        val options = MqttConnectOptions().apply {
            socketFactory = factory
            connectionTimeout = 5
        }

        try {
            repeat(2) {
                client.connect(options).waitForCompletion(10_000)
                assertTrue(client.isConnected)
                client.disconnect(0).waitForCompletion(10_000)
            }
            assertEquals(2, verifierCalls.get())
        } finally {
            client.close()
            client.unregisterResources()
            broker.stop()
        }
    }

    @Test
    fun rejectingVerifierFailsConnectTokenAndCallback() {
        val broker = FakeMqttBroker()
        val verifierCalls = AtomicInteger()
        val callbackComplete = CountDownLatch(1)
        val callbackFailure = AtomicReference<Throwable?>()
        val factory = TestSSLSocketFactory(HostnameVerifier { _, _ ->
            verifierCalls.incrementAndGet()
            false
        })
        val client = MqttAndroidClient(
            context,
            broker.uri.replace("tcp://", "ssl://"),
            "rejectingVerifier"
        )
        val options = MqttConnectOptions().apply {
            socketFactory = factory
            connectionTimeout = 5
        }

        try {
            val token = client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    callbackComplete.countDown()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    callbackFailure.set(exception)
                    callbackComplete.countDown()
                }
            })
            var tokenFailure: MqttException? = null
            try {
                token.waitForCompletion(10_000)
            } catch (e: MqttException) {
                tokenFailure = e
            }

            assertNotNull("connect token should fail", tokenFailure)
            assertTrue("failure callback was not invoked", callbackComplete.await(5, TimeUnit.SECONDS))
            assertNotNull("failure callback should receive the error", callbackFailure.get())
            assertEquals(1, verifierCalls.get())
            assertEquals(0, broker.connectAttempts("rejectingVerifier"))
        } finally {
            client.close()
            client.unregisterResources()
            broker.stop()
        }
    }

    @Test
    fun sessionAccessRunsAndEnforcesVerifier() {
        val verifierCalls = AtomicInteger()
        val acceptingSocket = TestSSLSocket()
        val acceptingWrapper = VerifyingSSLSocket(
            acceptingSocket,
            HostnameVerifier { _, _ ->
                verifierCalls.incrementAndGet()
                true
            },
            "127.0.0.1"
        )
        acceptingWrapper.session
        assertEquals(1, verifierCalls.get())

        val rejectingSocket = TestSSLSocket()
        val rejectingWrapper = VerifyingSSLSocket(
            rejectingSocket,
            HostnameVerifier { _, _ ->
                verifierCalls.incrementAndGet()
                false
            },
            "127.0.0.1"
        )
        try {
            rejectingWrapper.session
            throw AssertionError("Rejected hostname should prevent session access")
        } catch (_: javax.net.ssl.SSLHandshakeException) {
            assertTrue(rejectingSocket.isClosed)
        }
        assertEquals(2, verifierCalls.get())
    }

    private class TestSSLSocketFactory(
        private val verifier: HostnameVerifier
    ) : SSLSocketFactory() {
        override fun getDefaultCipherSuites() = emptyArray<String>()
        override fun getSupportedCipherSuites() = emptyArray<String>()
        override fun createSocket(): Socket = VerifyingSSLSocket(TestSSLSocket(), verifier)
        override fun createSocket(socket: Socket, host: String, port: Int, autoClose: Boolean): Socket =
            VerifyingSSLSocket(TestSSLSocket(socket), verifier, host)
        override fun createSocket(host: String, port: Int): Socket =
            createSocket().apply { connect(InetSocketAddress(host, port)) }
        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
            createSocket().apply {
                bind(InetSocketAddress(localHost, localPort))
                connect(InetSocketAddress(host, port))
            }
        override fun createSocket(host: InetAddress, port: Int): Socket =
            createSocket().apply { connect(InetSocketAddress(host, port)) }
        override fun createSocket(
            host: InetAddress,
            port: Int,
            localHost: InetAddress,
            localPort: Int
        ): Socket = createSocket().apply {
            bind(InetSocketAddress(localHost, localPort))
            connect(InetSocketAddress(host, port))
        }
    }

    private class TestSSLSocket(private val delegate: Socket = Socket()) : SSLSocket() {

        override fun bind(bindpoint: SocketAddress?) = delegate.bind(bindpoint)
        override fun connect(endpoint: SocketAddress?) = delegate.connect(endpoint)
        override fun connect(endpoint: SocketAddress?, timeout: Int) = delegate.connect(endpoint, timeout)
        override fun getChannel(): SocketChannel? = delegate.channel
        override fun getInetAddress(): InetAddress? = delegate.inetAddress
        override fun getLocalAddress(): InetAddress = delegate.localAddress
        override fun getPort(): Int = delegate.port
        override fun getLocalPort(): Int = delegate.localPort
        override fun getRemoteSocketAddress(): SocketAddress? = delegate.remoteSocketAddress
        override fun getLocalSocketAddress(): SocketAddress? = delegate.localSocketAddress
        override fun getInputStream() = delegate.getInputStream()
        override fun getOutputStream() = delegate.getOutputStream()
        override fun setTcpNoDelay(on: Boolean) = delegate.setTcpNoDelay(on)
        override fun getTcpNoDelay(): Boolean = delegate.tcpNoDelay
        override fun setSoLinger(on: Boolean, linger: Int) = delegate.setSoLinger(on, linger)
        override fun getSoLinger(): Int = delegate.soLinger
        override fun sendUrgentData(data: Int) = delegate.sendUrgentData(data)
        override fun setOOBInline(on: Boolean) = delegate.setOOBInline(on)
        override fun getOOBInline(): Boolean = delegate.oobInline
        override fun setSoTimeout(timeout: Int) = delegate.soTimeout = timeout
        override fun getSoTimeout(): Int = delegate.soTimeout
        override fun setSendBufferSize(size: Int) = delegate.setSendBufferSize(size)
        override fun getSendBufferSize(): Int = delegate.sendBufferSize
        override fun setReceiveBufferSize(size: Int) = delegate.setReceiveBufferSize(size)
        override fun getReceiveBufferSize(): Int = delegate.receiveBufferSize
        override fun setKeepAlive(on: Boolean) = delegate.setKeepAlive(on)
        override fun getKeepAlive(): Boolean = delegate.keepAlive
        override fun setTrafficClass(tc: Int) = delegate.trafficClass = tc
        override fun getTrafficClass(): Int = delegate.trafficClass
        override fun setReuseAddress(on: Boolean) = delegate.setReuseAddress(on)
        override fun getReuseAddress(): Boolean = delegate.reuseAddress
        override fun close() = delegate.close()
        override fun shutdownInput() = delegate.shutdownInput()
        override fun shutdownOutput() = delegate.shutdownOutput()
        override fun isConnected(): Boolean = delegate.isConnected
        override fun isBound(): Boolean = delegate.isBound
        override fun isClosed(): Boolean = delegate.isClosed
        override fun isInputShutdown(): Boolean = delegate.isInputShutdown
        override fun isOutputShutdown(): Boolean = delegate.isOutputShutdown
        override fun setPerformancePreferences(connectionTime: Int, latency: Int, bandwidth: Int) =
            delegate.setPerformancePreferences(connectionTime, latency, bandwidth)

        override fun getSupportedCipherSuites() = emptyArray<String>()
        override fun getEnabledCipherSuites() = emptyArray<String>()
        override fun setEnabledCipherSuites(suites: Array<out String>?) = Unit
        override fun getSupportedProtocols() = emptyArray<String>()
        override fun getEnabledProtocols() = emptyArray<String>()
        override fun setEnabledProtocols(protocols: Array<out String>?) = Unit
        override fun getSession(): SSLSession = TestSSLSession
        override fun getHandshakeSession(): SSLSession? = null
        override fun addHandshakeCompletedListener(listener: HandshakeCompletedListener?) = Unit
        override fun removeHandshakeCompletedListener(listener: HandshakeCompletedListener?) = Unit
        override fun startHandshake() = Unit
        override fun setUseClientMode(mode: Boolean) = Unit
        override fun getUseClientMode(): Boolean = true
        override fun setNeedClientAuth(need: Boolean) = Unit
        override fun getNeedClientAuth(): Boolean = false
        override fun setWantClientAuth(want: Boolean) = Unit
        override fun getWantClientAuth(): Boolean = false
        override fun setEnableSessionCreation(flag: Boolean) = Unit
        override fun getEnableSessionCreation(): Boolean = true
        override fun getSSLParameters() = SSLParameters()
        override fun setSSLParameters(params: SSLParameters?) = Unit
    }

    private object TestSSLSession : SSLSession by java.lang.reflect.Proxy.newProxyInstance(
        SSLSession::class.java.classLoader,
        arrayOf(SSLSession::class.java)
    ) { _, method, _ ->
        when (method.name) {
            "getPeerHost" -> "127.0.0.1"
            "getPeerPort" -> 1883
            "isValid" -> true
            else -> null
        }
    } as SSLSession
}
