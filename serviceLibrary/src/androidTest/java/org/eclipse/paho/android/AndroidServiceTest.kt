package org.eclipse.paho.android

import android.test.AndroidTestCase
import android.test.suitebuilder.annotation.Suppress
import kotlin.Throws
import java.lang.Exception
import timber.log.Timber
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import java.util.*
import java.util.concurrent.TimeUnit

class AndroidServiceTest : AndroidTestCase() {
    private val classCanonicalName = this.javaClass.canonicalName
    private var mqttServerURI: String? = null
    private var mqttSSLServerURI: String? = null
    private var waitForCompletionTime = 0
    private lateinit var keyStorePwd: String

    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        val properties = TestProperties(this.context)
        mqttServerURI = properties.serverURI
        mqttSSLServerURI = properties.serverSSLURI
        waitForCompletionTime = properties.waitForCompletionTime
        keyStorePwd = properties.clientKeyStorePassword
        Timber.d(properties.serverSSLURI)
    }

    /**
     * Tests that a client can be constructed and that it can connect to and
     * disconnect from the service
     */
    fun testConnect() {
        try {
            MqttAndroidClient(mContext, mqttServerURI!!, "testConnect").use { mqttclient ->
                var connectToken: IMqttToken
                var disconnectToken: IMqttToken
                connectToken = mqttclient.connect(null, null)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                disconnectToken = mqttclient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                connectToken = mqttclient.connect(null, null)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                disconnectToken = mqttclient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            }
        } catch (exception: Exception) {
            fail("Failed: $mqttServerURI exception=$exception")
        }
    }

    /**
     * Tests that a client connection with cleanSession=False recieves the session Present Flag in
     * a subsequent connection.
     *
     *
     * 1. Connect with CleanSession=True to ensure that state is cleared.
     * 2. Connect with CleanSession=False and ensure that sessionPresent is false.
     * 3. Connect with CleanSession=False and ensure that sessionPresent is true.
     */
    fun testCleanSession() {
        try {
            MqttAndroidClient(mContext, mqttServerURI!!, "testConnectWithCleanSession").use { mqttClient ->
                var connectToken: IMqttToken
                var disconnectToken: IMqttToken
                val options1 = MqttConnectOptions()
                options1.isCleanSession = true
                val connectCallback1 = MqttConnectCallback()
                connectToken = mqttClient.connect(options1, null, connectCallback1)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                val connectedToken1 = connectCallback1.asyncActionToken
                assertFalse(connectedToken1!!.sessionPresent)
                disconnectToken = mqttClient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                val options2 = MqttConnectOptions()
                options2.isCleanSession = false
                val connectCallback2 = MqttConnectCallback()
                connectToken = mqttClient.connect(options2, null, connectCallback2)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                val connectedToken2 = connectCallback2.asyncActionToken
                assertFalse(connectedToken2!!.sessionPresent)
                disconnectToken = mqttClient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                val connectCallback3 = MqttConnectCallback()
                connectToken = mqttClient.connect(options2, null, connectCallback3)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                val connectedToken3 = connectCallback3.asyncActionToken
                assertTrue(connectedToken3!!.sessionPresent)
                disconnectToken = mqttClient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            }
        } catch (exception: Exception) {
            fail("Failed: $mqttServerURI exception=$exception")
        }
    }

    /**
     * Tests isConnected() returns false after a disconnect() call.
     */
    @Throws(Exception::class)
    fun testIsConnected() {
        MqttAndroidClient(mContext, mqttServerURI!!, "testConnect").use { mqttClient ->
            val connectToken: IMqttToken
            val disconnectToken: IMqttToken
            assertFalse(mqttClient.isConnected)
            connectToken = mqttClient.connect(null, null)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            assertTrue(mqttClient.isConnected)
            disconnectToken = mqttClient.disconnect(null, null)
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            assertFalse(mqttClient.isConnected)
        }
    }

    /**
     * Test connection using a remote host name for the local host.
     */
    fun testRemoteConnect() {
        val methodName = "testRemoteConnect"
        try {
            MqttAndroidClient(mContext, mqttServerURI!!, "testRemoteConnect").use { mqttClient ->
                var connectToken: IMqttToken
                val subToken: IMqttToken
                val pubToken: IMqttDeliveryToken
                var disconnectToken: IMqttToken
                connectToken = mqttClient.connect(null, null)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                disconnectToken = mqttClient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
                mqttClient.setCallback(mqttV3Receiver)
                val mqttConnectOptions = MqttConnectOptions()
                mqttConnectOptions.isCleanSession = false
                connectToken = mqttClient.connect(mqttConnectOptions, null, null)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                val topicNames = arrayOf("$methodName/Topic")
                val topicQos = intArrayOf(0)
                subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
                subToken.waitForCompletion(waitForCompletionTime.toLong())
                val payload = "Message payload $classCanonicalName.$methodName"
                    .toByteArray()
                pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null)
                pubToken.waitForCompletion(waitForCompletionTime.toLong())
                val ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload)
                if (!ok) {
                    fail("Receive failed")
                }
                disconnectToken = mqttClient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            }
        } catch (exception: Exception) {
            fail("Failed: $mqttServerURI exception=$exception")
        }
    }

    /**
     * Test client pubSub using very large messages
     */
    fun testLargeMessage() {
        val methodName = "testLargeMessage"
        var mqttClient: IMqttAsyncClient? = null
        try {
            mqttClient = MqttAndroidClient(
                mContext, mqttServerURI!!,
                "testLargeMessage"
            )
            val connectToken: IMqttToken
            var subToken: IMqttToken
            val unsubToken: IMqttToken
            val pubToken: IMqttDeliveryToken
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null) //TODO do something about this?
            mqttClient.setCallback(mqttV3Receiver)
            connectToken = mqttClient.connect(null, null)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            val largeSize = 1000
            val topicNames = arrayOf("testLargeMessage" + "/Topic")
            val topicQos = intArrayOf(0)
            val message = ByteArray(largeSize)
            Arrays.fill(message, 's'.toByte())
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            unsubToken = mqttClient.unsubscribe(topicNames, null, null)
            unsubToken.waitForCompletion(waitForCompletionTime.toLong())
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, null)
            pubToken.waitForCompletion(waitForCompletionTime.toLong())
            val ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive failed")
            }
        } catch (exception: Exception) {
            fail("Failed to instantiate:$methodName exception=$exception")
        } finally {
            try {
                val disconnectToken: IMqttToken
                disconnectToken = mqttClient!!.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Multiple publishers and subscribers.
     */
    @Suppress
    fun testMultipleClients() {
        val publishers = 2
        val subscribers = 5
        val methodName = "testMultipleClients"
        val mqttPublisher = arrayOfNulls<IMqttAsyncClient>(publishers)
        val mqttSubscriber = arrayOfNulls<IMqttAsyncClient>(subscribers)
        var connectToken: IMqttToken
        var subToken: IMqttToken
        var pubToken: IMqttDeliveryToken
        var disconnectToken: IMqttToken
        try {
            val topicNames = arrayOf("$methodName/Topic")
            val topicQos = intArrayOf(0)
            for (i in mqttPublisher.indices) {
                mqttPublisher[i] = MqttAndroidClient(mContext, mqttServerURI!!, "MultiPub$i")
                connectToken = mqttPublisher[i]!!.connect(null, null)
                Log.i(methodName, "publisher connecting url " + mqttServerURI + "MultiPub" + i)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
            } // for...
            val mqttV3Receiver = arrayOfNulls<MqttV3Receiver>(mqttSubscriber.size)
            for (i in mqttSubscriber.indices) {
                mqttSubscriber[i] = MqttAndroidClient(mContext, mqttServerURI!!, "MultiSubscriber$i")
                mqttV3Receiver[i] = MqttV3Receiver(mqttSubscriber[i], null)
                mqttSubscriber[i]!!.setCallback(mqttV3Receiver[i])
                Log.i(methodName, "Assigning callback...")
                connectToken = mqttSubscriber[i]!!.connect(null, null)
                Log.i(methodName, "subscriber connecting url " + mqttServerURI + "MultiSubscriber" + i)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                subToken = mqttSubscriber[i]!!.subscribe(topicNames, topicQos, null, null)
                Log.i(methodName, "subscribe " + topicNames[0] + " QoS is " + topicQos[0])
                subToken.waitForCompletion(waitForCompletionTime.toLong())
            }
            for (iMessage in 0..1) {
                val payload = "Message $iMessage".toByteArray()
                for (aMqttPublisher in mqttPublisher) {
                    pubToken = aMqttPublisher!!.publish(topicNames[0], payload, 0, false, null, null)
                    Log.i(methodName, "publish to " + topicNames[0] + " payload is " + Arrays.toString(payload))
                    pubToken.waitForCompletion(waitForCompletionTime.toLong())
                }
                TimeUnit.MILLISECONDS.sleep(9999)
                for (i in mqttSubscriber.indices) {
                    for (aMqttPublisher in mqttPublisher) {
                        Log.i(methodName, "validate time = " + Date().toString())
                        val ok = mqttV3Receiver[i]!!.validateReceipt(topicNames[0], 0, payload)
                        if (!ok) {
                            fail("Receive failed")
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            fail("Failed to instantiate:$methodName exception=$exception")
        } finally {
            try {
                for (aMqttPublisher in mqttPublisher) {
                    disconnectToken = aMqttPublisher!!.disconnect(null, null)
                    disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                    aMqttPublisher.close()
                }
                for (aMqttSubscriber in mqttSubscriber) {
                    disconnectToken = aMqttSubscriber!!.disconnect(null, null)
                    disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                    aMqttSubscriber.close()
                }
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Test that QOS values are preserved between MQTT publishers and
     * subscribers.
     */
    @Suppress
    fun testQoSPreserved() {
        var mqttClient: IMqttAsyncClient? = null
        val connectToken: IMqttToken
        val subToken: IMqttToken
        var pubToken: IMqttDeliveryToken
        val disconnectToken: IMqttToken
        val methodName = "testQoSPreserved"
        try {
            mqttClient = MqttAndroidClient(mContext, mqttServerURI!!, "testQoSPreserved")
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            connectToken = mqttClient.connect(null, null)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            val topicNames = arrayOf("$methodName/Topic0", "$methodName/Topic1", "$methodName/Topic2")
            val topicQos = intArrayOf(0, 1, 2)
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            for (i in topicNames.indices) {
                val message = ("Message payload " + classCanonicalName + "." + methodName + " " + topicNames[i]).toByteArray()
                for (iQos in 0..2) {
                    pubToken = mqttClient.publish(topicNames[i], message, iQos, false, null, null)
                    pubToken.waitForCompletion(waitForCompletionTime.toLong())
                    val ok = mqttV3Receiver.validateReceipt(topicNames[i], Math.min(iQos, topicQos[i]), message)
                    if (!ok) {
                        fail("Receive failed sub Qos=" + topicQos[i] + " PublishQos=" + iQos)
                    }
                }
            }
        } catch (exception: Exception) {
            fail("Failed:$methodName exception=$exception")
        } finally {
            try {
                disconnectToken = mqttClient!!.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Test non durable subscriptions.
     */
    private fun testNonDurableSubs() {
        val methodName = "testNonDurableSubs"
        var mqttClient: IMqttAsyncClient? = null
        var connectToken: IMqttToken
        var subToken: IMqttToken
        val unsubToken: IMqttToken
        var pubToken: IMqttDeliveryToken
        var disconnectToken: IMqttToken
        try {
            mqttClient = MqttAndroidClient(mContext, mqttServerURI!!, "testNonDurableSubs")
            var mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            var mqttConnectOptions = MqttConnectOptions()
            // Clean session true is the default and implies non durable
            // subscriptions.
            mqttConnectOptions.isCleanSession = true
            connectToken = mqttClient.connect(mqttConnectOptions, null, null)
            connectToken.waitForCompletion(10000)
            val topicNames = arrayOf("$methodName/Topic")
            val topicQos = intArrayOf(2)
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(10000)
            val payloadNotRetained = "Message payload $classCanonicalName.$methodName not retained".toByteArray()
            pubToken = mqttClient.publish(topicNames[0], payloadNotRetained, 2, false, null, null)
            pubToken.waitForCompletion(100000)
            var ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadNotRetained)
            if (!ok) {
                fail("Receive failed")
            }

            // Retained publications.
            // ----------------------
            val payloadRetained = "Message payload $classCanonicalName.$methodName retained".toByteArray()
            pubToken = mqttClient.publish(topicNames[0], payloadRetained, 2, true, null, null)
            pubToken.waitForCompletion(10000)
            ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadRetained)
            if (!ok) {
                fail("Receive failed")
            }

            // Check that unsubscribe and re subscribe resends the publication.
            unsubToken = mqttClient.unsubscribe(topicNames, null, null)
            unsubToken.waitForCompletion(10000)
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(10000)
            ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadRetained)
            if (!ok) {
                fail("Receive failed")
            }

            // Check that subscribe without unsubscribe receives the
            // publication.
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(10000)
            ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadRetained)
            if (!ok) {
                fail("Receive failed")
            }

            // Disconnect, reconnect and check that the retained publication is
            // still delivered.
            disconnectToken = mqttClient.disconnect(null, null)
            disconnectToken.waitForCompletion(10000)
            mqttClient.close()
            mqttClient = MqttAndroidClient(mContext, mqttServerURI!!, "testNonDurableSubs")
            mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isCleanSession = true
            connectToken = mqttClient.connect(mqttConnectOptions, null, null)
            connectToken.waitForCompletion(1000)
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(1000)
            ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadRetained)
            if (!ok) {
                fail("Receive failed")
            }
        } catch (exception: Exception) {
            fail("Failed:$methodName exception=$exception")
        } finally {
            try {
                disconnectToken = mqttClient!!.disconnect(null, null)
                disconnectToken.waitForCompletion(1000)
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    /**
     * Test the behaviour of the cleanStart flag, used to clean up before
     * re-connecting.
     */
    fun testCleanStart() {
        var mqttClient: IMqttAsyncClient? = null
        var connectToken: IMqttToken
        var subToken: IMqttToken
        var pubToken: IMqttDeliveryToken
        var disconnectToken: IMqttToken
        val methodName = "testCleanStart"
        try {
            mqttClient = MqttAndroidClient(mContext, mqttServerURI!!, "testCleanStart")
            var mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            var mqttConnectOptions = MqttConnectOptions()
            // Clean start: true - The broker cleans up all client state,
            // including subscriptions, when the client is disconnected.
            // Clean start: false - The broker remembers all client state,
            // including subscriptions, when the client is disconnected.
            // Matching publications will get queued in the broker whilst the
            // client is disconnected.
            // For Mqtt V3 cleanSession=false, implies new subscriptions are
            // durable.
            mqttConnectOptions.isCleanSession = false
            connectToken = mqttClient.connect(mqttConnectOptions, null, null)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            val topicNames = arrayOf("$methodName/Topic")
            val topicQos = intArrayOf(0)
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            var payload = "Message payload $classCanonicalName.$methodName First".toByteArray()
            pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null)
            pubToken.waitForCompletion(waitForCompletionTime.toLong())
            var ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload)
            if (!ok) {
                fail("Receive failed")
            }

            // Disconnect and reconnect to make sure the subscription and all
            // queued messages are cleared.
            disconnectToken = mqttClient.disconnect(null, null)
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.close()

            // Send a message from another client, to our durable subscription.
            mqttClient = MqttAndroidClient(mContext, mqttServerURI!!, "testCleanStart" + "Other")
            mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isCleanSession = true
            connectToken = mqttClient.connect(mqttConnectOptions, null, null)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())

            // Receive the publication so that we can be sure the first client
            // has also received it.
            // Otherwise the first client may reconnect with its clean session
            // before the message has arrived.
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            payload = "Message payload $classCanonicalName.$methodName Other client".toByteArray()
            pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null)
            pubToken.waitForCompletion(waitForCompletionTime.toLong())
            ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload)
            if (!ok) {
                fail("Receive failed")
            }
            disconnectToken = mqttClient.disconnect(null, null)
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.close()

            // Reconnect and check we have no messages.
            mqttClient = MqttAndroidClient(mContext, mqttServerURI!!, "testCleanStart")
            mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isCleanSession = true
            connectToken = mqttClient.connect(mqttConnectOptions, null, null)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            var receivedMessage = mqttV3Receiver.receiveNext(100)
            if (receivedMessage != null) {
                fail("Receive messaqe:" + String(receivedMessage.message.payload))
            }

            // Also check that subscription is cancelled.
            payload = "Message payload $classCanonicalName.$methodName Cancelled Subscription".toByteArray()
            pubToken = mqttClient.publish(topicNames[0], payload, 1, false, null, null)
            pubToken.waitForCompletion(waitForCompletionTime.toLong())
            receivedMessage = mqttV3Receiver.receiveNext(100)
            if (receivedMessage != null) {
                fail("Receive messaqe:" + String(receivedMessage.message.payload))
            }
        } catch (exception: Exception) {
            fail("Failed:$methodName exception=$exception")
        } finally {
            try {
                disconnectToken = mqttClient!!.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    fun testPubSub() {
        val methodName = "testPubSub"
        var mqttClient: IMqttAsyncClient? = null
        try {
            mqttClient = MqttAndroidClient(mContext, mqttServerURI!!, methodName)
            val connectToken: IMqttToken
            val subToken: IMqttToken
            val pubToken: IMqttDeliveryToken
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            connectToken = mqttClient.connect(null, null)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            val topicNames = arrayOf("testPubSub" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testPubSub".toByteArray())
            val message = mqttMessage.payload
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, null)
            pubToken.waitForCompletion(waitForCompletionTime.toLong())
            TimeUnit.MILLISECONDS.sleep(3000)
            val ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive failed")
            }
        } catch (exception: Exception) {
            fail("Failed to instantiate:$methodName exception=$exception")
        } finally {
            try {
                val disconnectToken: IMqttToken
                disconnectToken = mqttClient!!.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    //	/** Oringally commented out from the fv test version
    //	 * Tests that invalid clientIds cannot connect.
    //	 *
    //	 * @throws Exception
    //	 */
    //	@Test
    //	public void testBadClientId() throws Exception {
    //		final String methodName = Utility.getMethodName();
    //		logger.entering(classCanonicalName, methodName);
    //
    //		// Client ids with length errors are now trapped by the client
    //		// implementation.
    //		// String[] clientIds = new
    //		// String[]{"","Minus-ClientId","123456789012345678901234"};
    //		String[] clientIds = new String[] { "Minus-ClientId" };
    //		IMqttAsyncClient mqttClient = null;
    //		IMqttToken connectToken ;
    //		IMqttToken disconnectToken;
    //
    //		for (String clientId : clientIds) {
    //			try {
    //				mqttClient = new MqttAndroidClient(mContext, serverURI, "testConnect");
    //						clientId);
    //
    //				try {
    //					connectToken = mqttClient.connect(null, null);
    //					connectToken.waitForCompletion(1000);
    //					connectToken.reset();
    //
    //					disconnectToken = mqttClient.disconnect(null, null);
    //					disconnectToken.waitForCompletion(1000);
    //					disconnectToken.reset();
    //
    //					fail("We shouldn't have been able to connect!");
    //				} catch (MqttException exception) {
    //					// This is the expected exception.
    //					logger.fine("We expect an exception because we used an invalid client id");
    //					// logger.log(Level.SEVERE, "caught exception:", exception);
    //				}
    //			} catch (Exception exception) {
    //				logger.fine("Failed:" + methodName + " exception="
    //						+ exception.getClass().getName() + "."
    //						+ exception.getMessage());
    //				logger.exiting(classCanonicalName, methodName,
    //						new Object[] { exception });
    //				throw exception;
    //			}
    //		}
    //
    //		logger.exiting(classCanonicalName, methodName);
    //	}
    @Throws(Exception::class)
    fun testHAConnect() {
        val methodName = "testHAConnect"
        var client: IMqttAsyncClient? = null
        try {
            try {
                val junk = "tcp://junk:123"
                client = MqttAndroidClient(mContext, junk, methodName)
                val urls = arrayOf("tcp://junk", mqttServerURI)
                val options = MqttConnectOptions()
                options.serverURIs = urls
                Log.i(methodName, "HA connect")
                val connectToken = client.connect(options)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                Log.i(methodName, "HA disconnect")
                val disconnectToken = client.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                Log.i(methodName, "HA success")
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        } finally {
            client?.close()
        }
    }

    fun testRetainedMessage() {
        val methodName = "testRetainedMessage"
        val mqttClient: IMqttAsyncClient
        val mqttClientRetained: IMqttAsyncClient
        var disconnectToken: IMqttToken
        try {
            mqttClient = MqttAndroidClient(mContext, mqttServerURI!!, methodName)
            var connectToken: IMqttToken
            var subToken: IMqttToken
            val pubToken: IMqttDeliveryToken
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            connectToken = mqttClient.connect(null, null)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            val topicNames = arrayOf("testRetainedMessage" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testPubSub".toByteArray())
            val message = mqttMessage.payload
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            pubToken = mqttClient.publish(topicNames[0], message, 0, true, null, null)
            pubToken.waitForCompletion(waitForCompletionTime.toLong())
            TimeUnit.MILLISECONDS.sleep(3000)
            var ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive failed")
            }
            Log.i(methodName, "First client received message successfully")
            disconnectToken = mqttClient.disconnect(null, null)
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.close()
            mqttClientRetained = MqttAndroidClient(mContext, mqttServerURI!!, "Retained")
            Log.i(methodName, "New MqttAndroidClient mqttClientRetained")
            val mqttV3ReceiverRetained = MqttV3Receiver(mqttClientRetained, null)
            mqttClientRetained.setCallback(mqttV3ReceiverRetained)
            Log.i(methodName, "Assigning callback...")
            connectToken = mqttClientRetained.connect(null, null)
            connectToken.waitForCompletion()
            Log.i(methodName, "Connect to mqtt server")
            subToken = mqttClientRetained.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion()
            Log.i(methodName, "subscribe " + topicNames[0] + " QoS is " + topicQos[0])
            TimeUnit.MILLISECONDS.sleep(3000)
            ok = mqttV3ReceiverRetained.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive retained message failed")
            }
            Log.i(methodName, "Second client received message successfully")
            disconnectToken = mqttClientRetained.disconnect(null, null)
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            mqttClientRetained.close()
        } catch (exception: Exception) {
            fail("Failed to instantiate:$methodName exception=$exception")
        }
    }

    /**
     * Tests that a client can be constructed and that it can connect to and
     * disconnect from the service via SSL
     */
    @Suppress
    fun testSSLConnect() {
        try {
            MqttAndroidClient(mContext, mqttSSLServerURI!!, "testSSLConnect").use { mqttClient ->
                val options = MqttConnectOptions()
                options.socketFactory = mqttClient.getSSLSocketFactory(this.context.assets.open("test.bks"), keyStorePwd)
                var connectToken: IMqttToken
                var disconnectToken: IMqttToken
                connectToken = mqttClient.connect(options)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                disconnectToken = mqttClient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
                connectToken = mqttClient.connect(options)
                connectToken.waitForCompletion(waitForCompletionTime.toLong())
                disconnectToken = mqttClient.disconnect(null, null)
                disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            }
        } catch (exception: Exception) {
            fail("Failed:testSSLConnect exception=$exception")
        }
    }

    /**
     * An SSL connection with server cert authentication, simple pub/sub of an message
     */
    @Suppress
    @Throws(Exception::class)
    fun testSSLPubSub() {
        var mqttClient: MqttAndroidClient? = null
        val connectToken: IMqttToken
        val disconnectToken: IMqttToken
        val subToken: IMqttToken
        val pubToken: IMqttDeliveryToken
        try {
            mqttClient = MqttAndroidClient(mContext, mqttSSLServerURI!!, "testSSLPubSub")
            val options = MqttConnectOptions()
            options.socketFactory = mqttClient.getSSLSocketFactory(this.context.assets.open("test.bks"), keyStorePwd)
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            connectToken = mqttClient.connect(options)
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            val topicNames = arrayOf("testSSLPubSub" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testSSLPubSub".toByteArray())
            val message = mqttMessage.payload
            subToken = mqttClient.subscribe(topicNames, topicQos, null, null)
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, null)
            pubToken.waitForCompletion(waitForCompletionTime.toLong())
            TimeUnit.MILLISECONDS.sleep(6000)
            val ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive failed")
            }
        } catch (exception: Exception) {
            fail("Failed:testSSLPubSub exception=$exception")
        } finally {
            disconnectToken = mqttClient!!.disconnect(null, null)
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            if (mqttClient != null) {
                mqttClient.close()
            }
        }
    }

    private inner class MqttConnectCallback : IMqttActionListener {
        var asyncActionToken: IMqttToken? = null
            private set

        override fun onSuccess(asyncActionToken: IMqttToken) {
            this.asyncActionToken = asyncActionToken
        }

        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {}
    }

    companion object {
        private const val TAG = "AndroidServiceTest"
    }
}