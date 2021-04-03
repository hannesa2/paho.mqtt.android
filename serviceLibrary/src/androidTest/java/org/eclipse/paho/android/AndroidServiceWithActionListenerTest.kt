package org.eclipse.paho.android

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.android.service.MqttService
import org.eclipse.paho.android.service.MqttServiceBinder
import org.eclipse.paho.client.mqttv3.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.any
import org.junit.*
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class AndroidServiceWithActionListenerTest {

    private val classCanonicalName = this.javaClass.canonicalName
    private var serverURI: String? = null
    private var mqttSSLServerURI: String? = null
    private var waitForCompletionTime = 0L
    private var keyStorePwd = ""

    //since we know tokens do not work when an action listener isn't specified
    private var notifier = TestCaseNotifier()

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Before
    @Throws(TimeoutException::class)
    fun setUp() {
        val properties = TestProperties(InstrumentationRegistry.getInstrumentation().targetContext)
        serverURI = properties.serverURI
        mqttSSLServerURI = properties.serverSSLURI
        waitForCompletionTime = properties.waitForCompletionTime
        keyStorePwd = properties.clientKeyStorePassword

        // Create the service Intent.
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            MqttService::class.java
        ).apply {
            // putExtra(SEED_KEY, 42L)
        }

        // Bind the service and grab a reference to the binder.
        val binder: IBinder = serviceRule.bindService(serviceIntent)

        // Get the reference to the service, to call public methods on the binder directly.
        val service: MqttService = (binder as MqttServiceBinder).service

        // Verify that the service is working correctly.
        assertThat(service.isTraceEnabled, `is`(any(Boolean::class.java)))
    }

    @Test
    fun testConnect() {
        val mqttClient: IMqttAsyncClient?
        mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "testConnect")
        mqttClient.connect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        mqttClient.connect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
    }

    @Test
    fun testRemoteConnect() {
        val methodName = "testRemoteConnect"
        val mqttClient: IMqttAsyncClient?
        mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "testRemoteConnect")
        mqttClient.connect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        val mqttV3Receiver = MqttReceiver(mqttClient)
        mqttClient.setCallback(mqttV3Receiver)
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = false
        mqttClient.connect(mqttConnectOptions, null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        val topicNames = arrayOf("$methodName/Topic")
        val topicQos = intArrayOf(0)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        val payload = "Message payload $classCanonicalName.$methodName".toByteArray()
        mqttClient.publish(topicNames[0], payload, 1, false, null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        val validateResult = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload)
        if (!validateResult.ok) {
            Assert.fail(validateResult.message)
        }
        mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
    }

    @Test
    fun testLargeMessage() {
        notifier = TestCaseNotifier()
        val methodName = "testLargeMessage"
        var mqttClient: IMqttAsyncClient? = null
        try {
            mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "testLargeMessage")
            val mqttReceiver = MqttReceiver(mqttClient)
            mqttClient.setCallback(mqttReceiver)
            mqttClient.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            val largeSize = 1000
            val topicNames = arrayOf("testLargeMessage" + "/Topic")
            val topicQos = intArrayOf(0)
            val message = ByteArray(largeSize)
            Arrays.fill(message, 's'.code.toByte())
            mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            mqttClient.unsubscribe(topicNames, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            mqttClient.publish(topicNames[0], message, 0, false, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            val validateResult = mqttReceiver.validateReceipt(topicNames[0], 0, message)
            if (!validateResult.ok) {
                Assert.fail(validateResult.message)
            }
        } catch (exception: Exception) {
            Assert.fail("Failed to instantiate:$methodName exception=$exception")
        } finally {
            try {
                mqttClient!!.disconnect(null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime)
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    @Test
    fun testMultipleClients() {
        val publishers = 2
        val subscribers = 5
        val methodName = "testMultipleClients"
        val mqttPublisher = arrayOfNulls<IMqttAsyncClient>(publishers)
        val mqttSubscriber = arrayOfNulls<IMqttAsyncClient>(subscribers)
        var disconnectToken: IMqttToken
        val topicNames = arrayOf("$methodName/Topic")
        val topicQos = intArrayOf(0)
        for (i in mqttPublisher.indices) {
            mqttPublisher[i] = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "MultiPub$i")
            mqttPublisher[i]!!.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
        }
        val mqttReceiver = arrayOfNulls<MqttReceiver>(mqttSubscriber.size)
        for (i in mqttSubscriber.indices) {
            mqttSubscriber[i] = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "MultiSubscriber$i")
            mqttReceiver[i] = MqttReceiver(mqttSubscriber[i]!!)
            mqttSubscriber[i]!!.setCallback(mqttReceiver[i]!!)
            mqttSubscriber[i]!!.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            mqttSubscriber[i]!!.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
        } // for...
        for (iMessage in 0..1) {
            val payload = "Message $iMessage".toByteArray()
            for (aMqttPublisher in mqttPublisher) {
                aMqttPublisher!!.publish(topicNames[0], payload, 0, false, null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime)
            }
            TimeUnit.MILLISECONDS.sleep(8888)
            for (i in mqttSubscriber.indices) {
                for (aMqttPublisher in mqttPublisher) {
                    val validateResult = mqttReceiver[i]!!.validateReceipt(topicNames[0], 0, payload)
                    if (!validateResult.ok) {
                        Assert.fail(validateResult.message)
                    }
                }
            }
        }
        for (aMqttPublisher in mqttPublisher) {
            disconnectToken = aMqttPublisher!!.disconnect(null, null)
            disconnectToken.waitForCompletion(waitForCompletionTime)
            aMqttPublisher.close()
        }
        for (aMqttSubscriber in mqttSubscriber) {
            disconnectToken = aMqttSubscriber!!.disconnect(null, null)
            disconnectToken.waitForCompletion(waitForCompletionTime)
            aMqttSubscriber.close()
        }
    }

    @Test
    @Ignore("Received invalid payload")
    fun testNonDurableSubs() {
        val methodName = "testNonDurableSubs"
        notifier = TestCaseNotifier()
        var mqttClient: IMqttAsyncClient?
        val connectToken: IMqttToken
        mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "testNonDurableSubs")
        var mqttReceiver = MqttReceiver(mqttClient)
        mqttClient.setCallback(mqttReceiver)
        var mqttConnectOptions = MqttConnectOptions()
        // Clean session true is the default and implies non durable subscriptions.
        mqttConnectOptions.isCleanSession = true
        mqttClient.connect(mqttConnectOptions, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        val topicNames = arrayOf("$methodName/Topic")
        val topicQos = intArrayOf(2)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        val payloadNotRetained = "Message payload $classCanonicalName.$methodName not retained".toByteArray()
        mqttClient.publish(topicNames[0], payloadNotRetained, 2, false, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        var validateResult = mqttReceiver.validateReceipt(topicNames[0], 2, payloadNotRetained)
        if (!validateResult.ok) {
            Assert.fail(validateResult.message)
        }

        // Retained publications.
        // ----------------------
        val payloadRetained = "Message payload $classCanonicalName.$methodName retained".toByteArray()
        mqttClient.publish(topicNames[0], payloadRetained, 2, true, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        validateResult = mqttReceiver.validateReceipt(topicNames[0], 2, payloadRetained)
        if (!validateResult.ok) {
            Assert.fail(validateResult.message)
        }

        // Check that unsubscribe and re subscribe resends the publication.
        mqttClient.unsubscribe(topicNames, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        validateResult = mqttReceiver.validateReceipt(topicNames[0], 2, payloadRetained)
        if (!validateResult.ok) {
            Assert.fail(validateResult.message)
        }

        // Check that subscribe without unsubscribe receives the
        // publication.
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        validateResult = mqttReceiver.validateReceipt(topicNames[0], 2, payloadRetained)
        if (!validateResult.ok) {
            Assert.fail(validateResult.message)
        }

        // Disconnect, reconnect and check that the retained publication is still delivered.
        mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        mqttClient.close()
        mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "testNonDurableSubs")
        mqttReceiver = MqttReceiver(mqttClient)
        mqttClient.setCallback(mqttReceiver)
        mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = true
        connectToken = mqttClient.connect(mqttConnectOptions, null, ActionListener(notifier))
        connectToken.waitForCompletion(1000)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        validateResult = mqttReceiver.validateReceipt(topicNames[0], 2, payloadRetained)
        if (!validateResult.ok) {
            Assert.fail(validateResult.message)
        }
        mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        mqttClient.close()
    }

    @Suppress
    @Test
    fun testQoSPreserved() {
        val mqttClient: IMqttAsyncClient
        val methodName = "testQoSPreserved"
        mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "testQoSPreserved")
        val mqttReceiver = MqttReceiver(mqttClient)
        mqttClient.setCallback(mqttReceiver)
        mqttClient.connect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        val topicNames = arrayOf("$methodName/Topic0", "$methodName/Topic1", "$methodName/Topic2")
        val topicQos = intArrayOf(0, 1, 2)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
        for (i in topicNames.indices) {
            val message = ("Message payload " + classCanonicalName + "." + methodName + " " + topicNames[i]).toByteArray()
            for (iQos in 0..2) {
                mqttClient.publish(topicNames[i], message, iQos, false, null, null)
                notifier.waitForCompletion(waitForCompletionTime)
                val validateResult = mqttReceiver.validateReceipt(topicNames[i], min(iQos, topicQos[i]), message)
                if (!validateResult.ok) {
                    Assert.fail("Receive failed sub Qos=" + topicQos[i] + " PublishQos=" + iQos)
                }
            }
        }
        mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime)
    }

    @Test
    fun testHAConnect() {
        val methodName = "testHAConnect"
        var client: IMqttAsyncClient? = null
        try {
            try {
                val junk = "tcp://junk:123"
                client = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, junk, methodName)
                val urls = arrayOf("tcp://junk", serverURI)
                val options = MqttConnectOptions()
                options.serverURIs = urls
                Log.i(methodName, "HA connect")
                client.connect(options, null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime)
                Log.i(methodName, "HA disconnect")
                client.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime)
                Log.i(methodName, "HA success")
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        } finally {
            client?.close()
        }
    }

    @Test
    fun testPubSub() {
        val methodName = "testPubSub"
        var mqttClient: IMqttAsyncClient? = null
        try {
            mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, methodName)
            val mqttReceiver = MqttReceiver(mqttClient)
            mqttClient.setCallback(mqttReceiver)
            mqttClient.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            val topicNames = arrayOf("testPubSub" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testPubSub".toByteArray())
            val message = mqttMessage.payload
            mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            mqttClient.publish(topicNames[0], message, 0, false, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            TimeUnit.MILLISECONDS.sleep(3000)
            val validateResult = mqttReceiver.validateReceipt(topicNames[0], 0, message)
            if (!validateResult.ok) {
                Assert.fail(validateResult.message)
            }
        } catch (exception: Exception) {
            Assert.fail(
                "Failed to instantiate:" + methodName + " exception="
                        + exception
            )
        } finally {
            try {
                mqttClient!!.disconnect(null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime)
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    @Test
    fun testRetainedMessage() {
        val methodName = "testRetainedMessage"
        val mqttClient: IMqttAsyncClient?
        val mqttClientRetained: IMqttAsyncClient?
        try {
            mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, methodName)
            val mqttV3Receiver = MqttReceiver(mqttClient)
            mqttClient.setCallback(mqttV3Receiver)
            mqttClient.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            val topicNames = arrayOf("testRetainedMessage" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testPubSub".toByteArray())
            val message = mqttMessage.payload
            mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            mqttClient.publish(topicNames[0], message, 0, true, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            TimeUnit.MILLISECONDS.sleep(3000)
            var validateResult = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!validateResult.ok) {
                Assert.fail(validateResult.message)
            }
            Log.i(methodName, "First client received message successfully")
            val token = mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
            token.waitForCompletion(waitForCompletionTime)
            notifier.waitForCompletion(waitForCompletionTime)
            mqttClient.close()
            mqttClientRetained = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, serverURI!!, "Retained")
            Log.i(methodName, "New MqttAndroidClient mqttClientRetained")
            val mqttReceiver = MqttReceiver(mqttClientRetained)
            mqttClientRetained.setCallback(mqttReceiver)
            Log.i(methodName, "Assigning callback...")
            mqttClientRetained.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            Log.i(methodName, "Connect to mqtt server")
            mqttClientRetained.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            Log.i(methodName, "subscribe " + topicNames[0] + " QoS is " + topicQos[0])
            TimeUnit.MILLISECONDS.sleep(3000)
            validateResult = mqttReceiver.validateReceipt(topicNames[0], 0, message)
            if (!validateResult.ok) {
                Assert.fail(validateResult.message)
            }
            Log.i(methodName, "Second client received message successfully")
            mqttClientRetained.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime)
            mqttClientRetained.close()
        } catch (exception: Exception) {
            Assert.fail("Failed to instantiate:$methodName exception=$exception")
        }
    }

    /**
     * Tests that a client can be constructed and that it can connect to and disconnect from the service via SSL
     */
    @Ignore("Trust anchor for certification path not found")
    @Test
    fun testSSLConnect() {
        var mqttClient: MqttAndroidClient? = null
        try {
            mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, mqttSSLServerURI!!, "testSSLConnect")
            val options = MqttConnectOptions()
            options.socketFactory =
                mqttClient.getSSLSocketFactory(InstrumentationRegistry.getInstrumentation().targetContext.assets.open("test.bks"), keyStorePwd)
            var connectToken = mqttClient.connect(options, InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
            connectToken.waitForCompletion(waitForCompletionTime)
            var disconnectToken: IMqttToken =
                mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
            disconnectToken.waitForCompletion(waitForCompletionTime)
            connectToken = mqttClient.connect(options, InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
            connectToken.waitForCompletion(waitForCompletionTime)
            disconnectToken = mqttClient.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
            disconnectToken.waitForCompletion(waitForCompletionTime)
        } catch (exception: Exception) {
            Assert.fail("Failed:testSSLConnect exception=$exception")
        } finally {
            mqttClient?.close()
        }
    }

    /**
     * An SSL connection with server cert authentication, simple pub/sub of an message
     */
    @Ignore("Trust anchor for certification path not found")
    @Test
    fun testSSLPubSub() {
        var mqttClient: MqttAndroidClient? = null
        val connectToken: IMqttToken?
        val disconnectToken: IMqttToken?
        val subToken: IMqttToken?
        val pubToken: IMqttDeliveryToken?
        try {
            mqttClient = MqttAndroidClient(InstrumentationRegistry.getInstrumentation().targetContext, mqttSSLServerURI!!, "testSSLPubSub")
            val options = MqttConnectOptions()
            options.socketFactory =
                mqttClient.getSSLSocketFactory(InstrumentationRegistry.getInstrumentation().targetContext.assets.open("test.bks"), keyStorePwd)
            val mqttReceiver = MqttReceiver(mqttClient)
            mqttClient.setCallback(mqttReceiver)
            connectToken = mqttClient.connect(options, InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
            connectToken.waitForCompletion(waitForCompletionTime)
            val topicNames = arrayOf("testSSLPubSub" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testSSLPubSub".toByteArray())
            val message = mqttMessage.payload
            subToken = mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            subToken.waitForCompletion(waitForCompletionTime)
            pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, ActionListener(notifier))
            pubToken.waitForCompletion(waitForCompletionTime)
            TimeUnit.MILLISECONDS.sleep(6000)
            val validateResult = mqttReceiver.validateReceipt(topicNames[0], 0, message)
            if (!validateResult.ok) {
                Assert.fail(validateResult.message)
            }
        } catch (exception: Exception) {
            Assert.fail("Failed:testSSLPubSub exception=$exception")
        } finally {
            disconnectToken = mqttClient!!.disconnect(InstrumentationRegistry.getInstrumentation().targetContext, ActionListener(notifier))
            disconnectToken.waitForCompletion(waitForCompletionTime)
            mqttClient.close()
        }
    }

    private inner class ActionListener(notifier: TestCaseNotifier) : IMqttActionListener {

        init {
            notifier.start = 0
        }

        override fun onFailure(token: IMqttToken, exception: Throwable) {
            Assert.fail("onFailure $exception")
        }

        override fun onSuccess(token: IMqttToken) {
            Log.d(TAG, "onSuccess")
        }
    }

    companion object {
        private val classCanonicalName = this::class.java.canonicalName
        private val TAG = classCanonicalName
    }
}