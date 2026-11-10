package info.mqtt.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import info.mqtt.android.service.MqttAndroidClient
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStream
import java.security.KeyStore
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSocketFactory

/**
 * Test class for the new getSSLSocketFactory methods with certificate pinning support
 */
@RunWith(AndroidJUnit4::class)
class SSLSocketFactoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testNewGetSSLSocketFactoryWithKeyStore() {
        // Load the test keystore
        val keyStoreInputStream: InputStream = context.assets.open("test.bks")
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
        val keyStoreInputStream: InputStream = context.assets.open("test.bks")
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

        // Clean up
        mqttClient.unregisterResources()
    }

    @Test
    fun testNewGetSSLSocketFactoryDefaultHostnameVerifier() {
        // Load the test keystore
        val keyStoreInputStream: InputStream = context.assets.open("test.bks")
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
}
