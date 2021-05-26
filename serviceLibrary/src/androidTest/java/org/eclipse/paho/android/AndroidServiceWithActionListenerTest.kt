package org.eclipse.paho.android

import android.test.ServiceTestCase
import org.eclipse.paho.android.service.MqttService
import kotlin.Throws
import java.lang.Exception
import android.content.Intent
import android.test.suitebuilder.annotation.Suppress
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import java.util.*
import java.util.concurrent.TimeUnit

class AndroidServiceWithActionListenerTest : ServiceTestCase<MqttService>(MqttService::class.java) {
    private val classCanonicalName = this.javaClass.canonicalName
    private var serverURI: String? = null
    private var mqttSSLServerURI: String? = null
    private var waitForCompletionTime = 0
    private var keyStorePwd = ""

    //since we know tokens do not work when an action listener isn't specified
    private var notifier = TestCaseNotifier()

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val intent = Intent()
        intent.setClassName("org.eclipse.paho.android.service", "MqttService")
        bindService(intent)
        val properties = TestProperties(this.context)
        serverURI = properties.serverURI
        mqttSSLServerURI = properties.serverSSLURI
        waitForCompletionTime = properties.waitForCompletionTime
        val clientKeyStore = properties.clientKeyStore
        keyStorePwd = properties.clientKeyStorePassword
    }

    @Throws(Throwable::class)
    fun testConnect() {
        val mqttClient: IMqttAsyncClient?
        mqttClient = MqttAndroidClient(mContext, serverURI!!, "testConnect")
        mqttClient.connect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        mqttClient.disconnect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        mqttClient.connect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        mqttClient.disconnect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
    }

    @Throws(Throwable::class)
    fun testRemoteConnect() {
        val methodName = "testRemoteConnect"
        val mqttClient: IMqttAsyncClient?
        mqttClient = MqttAndroidClient(mContext, serverURI!!, "testRemoteConnect")
        mqttClient.connect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        mqttClient.disconnect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
        mqttClient.setCallback(mqttV3Receiver)
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = false
        mqttClient.connect(mqttConnectOptions, null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        val topicNames = arrayOf("$methodName/Topic")
        val topicQos = intArrayOf(0)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        val payload = "Message payload $classCanonicalName.$methodName".toByteArray()
        mqttClient.publish(topicNames[0], payload, 1, false, null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        val ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, payload)
        if (!ok) {
            fail("Receive failed")
        }
        mqttClient.disconnect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
    }

    @Throws(Throwable::class)
    fun testLargeMessage() {
        notifier = TestCaseNotifier()
        val methodName = "testLargeMessage"
        var mqttClient: IMqttAsyncClient? = null
        try {
            mqttClient = MqttAndroidClient(mContext, serverURI!!, "testLargeMessage")
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null) //TODO do something about this?
            mqttClient.setCallback(mqttV3Receiver)
            mqttClient.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            val largeSize = 1000
            val topicNames = arrayOf("testLargeMessage" + "/Topic")
            val topicQos = intArrayOf(0)
            val message = ByteArray(largeSize)
            Arrays.fill(message, 's'.toByte())
            mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.unsubscribe(topicNames, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.publish(topicNames[0], message, 0, false, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            val ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive failed")
            }
        } catch (exception: Exception) {
            fail(
                "Failed to instantiate:" + methodName + " exception="
                        + exception
            )
        } finally {
            try {
                mqttClient!!.disconnect(null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime.toLong())
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    @Throws(Throwable::class)
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
            mqttPublisher[i] = MqttAndroidClient(mContext, serverURI!!, "MultiPub$i")
            mqttPublisher[i]!!.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
        } // for...
        val mqttV3Receiver = arrayOfNulls<MqttV3Receiver>(mqttSubscriber.size)
        for (i in mqttSubscriber.indices) {
            mqttSubscriber[i] = MqttAndroidClient(mContext, serverURI!!, "MultiSubscriber$i")
            mqttV3Receiver[i] = MqttV3Receiver(mqttSubscriber[i], null)
            mqttSubscriber[i]!!.setCallback(mqttV3Receiver[i])
            mqttSubscriber[i]!!.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            mqttSubscriber[i]!!.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
        } // for...
        for (iMessage in 0..1) {
            val payload = "Message $iMessage".toByteArray()
            for (aMqttPublisher in mqttPublisher) {
                aMqttPublisher!!.publish(topicNames[0], payload, 0, false, null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime.toLong())
            }
            TimeUnit.MILLISECONDS.sleep(8888)
            for (i in mqttSubscriber.indices) {
                for (aMqttPublisher in mqttPublisher) {
                    val ok = mqttV3Receiver[i]!!.validateReceipt(topicNames[0], 0, payload)
                    if (!ok) {
                        fail("Receive failed")
                    }
                }
            }
        }
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
    }

    @Throws(Throwable::class)
    private fun testNonDurableSubs() {
        val methodName = "testNonDurableSubs"
        notifier = TestCaseNotifier()
        var mqttClient: IMqttAsyncClient?
        val connectToken: IMqttToken
        mqttClient = MqttAndroidClient(mContext, serverURI!!, "testNonDurableSubs")
        var mqttV3Receiver = MqttV3Receiver(mqttClient, null)
        mqttClient.setCallback(mqttV3Receiver)
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
        var ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadNotRetained)
        if (!ok) {
            fail("Receive failed")
        }

        // Retained publications.
        // ----------------------
        val payloadRetained = "Message payload $classCanonicalName.$methodName retained".toByteArray()
        mqttClient.publish(topicNames[0], payloadRetained, 2, true, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadRetained)
        if (!ok) {
            fail("Receive failed")
        }

        // Check that unsubscribe and re subscribe resends the publication.
        mqttClient.unsubscribe(topicNames, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadRetained)
        if (!ok) {
            fail("Receive failed")
        }

        // Check that subscribe without unsubscribe receives the
        // publication.
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        ok = mqttV3Receiver.validateReceipt(topicNames[0], 2, payloadRetained)
        if (!ok) {
            fail("Receive failed")
        }

        // Disconnect, reconnect and check that the retained publication is
        // still delivered.
        mqttClient.disconnect(null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        mqttClient.close()
        mqttClient = MqttAndroidClient(mContext, serverURI!!, "testNonDurableSubs")
        mqttV3Receiver = MqttV3Receiver(mqttClient, null)
        mqttClient.setCallback(mqttV3Receiver)
        mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = true
        connectToken = mqttClient.connect(mqttConnectOptions, null, ActionListener(notifier))
        connectToken.waitForCompletion(1000)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        ok = mqttV3Receiver.validateReceipt(
            topicNames[0], 2,
            payloadRetained
        )
        if (!ok) {
            fail("Receive failed")
        }
        mqttClient.disconnect(null, ActionListener(notifier))
        notifier.waitForCompletion(1000)
        mqttClient.close()
    }

    @Suppress
    @Throws(Throwable::class)
    fun testQoSPreserved() {
        val mqttClient: IMqttAsyncClient
        val methodName = "testQoSPreserved"
        mqttClient = MqttAndroidClient(mContext, serverURI!!, "testQoSPreserved")
        val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
        mqttClient.setCallback(mqttV3Receiver)
        mqttClient.connect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        val topicNames = arrayOf("$methodName/Topic0", "$methodName/Topic1", "$methodName/Topic2")
        val topicQos = intArrayOf(0, 1, 2)
        mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
        for (i in topicNames.indices) {
            val message = ("Message payload " + classCanonicalName + "." + methodName + " " + topicNames[i]).toByteArray()
            for (iQos in 0..2) {
                mqttClient.publish(topicNames[i], message, iQos, false, null, null)
                notifier.waitForCompletion(waitForCompletionTime.toLong())
                val ok = mqttV3Receiver.validateReceipt(
                    topicNames[i],
                    Math.min(iQos, topicQos[i]), message
                )
                if (!ok) {
                    fail("Receive failed sub Qos=" + topicQos[i] + " PublishQos=" + iQos)
                }
            }
        }
        mqttClient.disconnect(null, ActionListener(notifier))
        notifier.waitForCompletion(waitForCompletionTime.toLong())
    }

    @Throws(Throwable::class)
    fun testHAConnect() {
        val methodName = "testHAConnect"
        var client: IMqttAsyncClient? = null
        try {
            try {
                val junk = "tcp://junk:123"
                client = MqttAndroidClient(mContext, junk, methodName)
                val urls = arrayOf("tcp://junk", serverURI)
                val options = MqttConnectOptions()
                options.serverURIs = urls
                Log.i(methodName, "HA connect")
                client.connect(options, null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime.toLong())
                Log.i(methodName, "HA disconnect")
                client.disconnect(null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime.toLong())
                Log.i(methodName, "HA success")
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        } finally {
            client?.close()
        }
    }

    @Throws(Throwable::class)
    fun testPubSub() {
        val methodName = "testPubSub"
        var mqttClient: IMqttAsyncClient? = null
        try {
            mqttClient = MqttAndroidClient(mContext, serverURI!!, methodName)
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            mqttClient.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            val topicNames = arrayOf("testPubSub" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testPubSub".toByteArray())
            val message = mqttMessage.payload
            mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.publish(topicNames[0], message, 0, false, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            TimeUnit.MILLISECONDS.sleep(3000)
            val ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive failed")
            }
        } catch (exception: Exception) {
            fail("Failed to instantiate:$methodName exception=$exception")
        } finally {
            try {
                mqttClient!!.disconnect(null, ActionListener(notifier))
                notifier.waitForCompletion(waitForCompletionTime.toLong())
                mqttClient.close()
            } catch (ignored: Exception) {
            }
        }
    }

    @Throws(Throwable::class)
    fun testRetainedMessage() {
        val methodName = "testRetainedMessage"
        val mqttClient: IMqttAsyncClient?
        val mqttClientRetained: IMqttAsyncClient?
        try {
            mqttClient = MqttAndroidClient(mContext, serverURI!!, methodName)
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            mqttClient.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            val topicNames = arrayOf("testRetainedMessage" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testPubSub".toByteArray())
            val message = mqttMessage.payload
            mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.publish(topicNames[0], message, 0, true, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            TimeUnit.MILLISECONDS.sleep(3000)
            var ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive failed")
            }
            Log.i(methodName, "First client received message successfully")
            mqttClient.disconnect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.close()
            mqttClientRetained = MqttAndroidClient(mContext, serverURI!!, "Retained")
            Log.i(methodName, "New MqttAndroidClient mqttClientRetained")
            val mqttV3ReceiverRetained = MqttV3Receiver(mqttClientRetained, null)
            mqttClientRetained.setCallback(mqttV3ReceiverRetained)
            Log.i(methodName, "Assigning callback...")
            mqttClientRetained.connect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            Log.i(methodName, "Connect to mqtt server")
            mqttClientRetained.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            Log.i(methodName, "subscribe " + topicNames[0] + " QoS is " + topicQos[0])
            TimeUnit.MILLISECONDS.sleep(3000)
            ok = mqttV3ReceiverRetained.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive retained message failed")
            }
            Log.i(methodName, "Second client received message successfully")
            mqttClientRetained.disconnect(null, ActionListener(notifier))
            notifier.waitForCompletion(waitForCompletionTime.toLong())
            mqttClientRetained.close()
        } catch (exception: Exception) {
            fail("Failed to instantiate:$methodName exception=$exception")
        }
    }

    /**
     * Tests that a client can be constructed and that it can connect to and
     * disconnect from the service via SSL
     *
     * @throws Exception
     */
    @Suppress
    @Throws(Exception::class)
    fun testSSLConnect() {
        var mqttClient: MqttAndroidClient? = null
        try {
            mqttClient = MqttAndroidClient(mContext, mqttSSLServerURI!!, "testSSLConnect")
            val options = MqttConnectOptions()
            options.socketFactory = mqttClient.getSSLSocketFactory(this.context.assets.open("test.bks"), keyStorePwd)
            var connectToken: IMqttToken = mqttClient.connect(options, this.context, ActionListener(notifier))
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            var disconnectToken: IMqttToken = mqttClient.disconnect(null, ActionListener(notifier))
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            connectToken = mqttClient.connect(options, this.context, ActionListener(notifier))
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            disconnectToken = mqttClient.disconnect(null, ActionListener(notifier))
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
        } catch (exception: Exception) {
            fail("Failed:testSSLConnect exception=$exception")
        } finally {
            mqttClient?.close()
        }
    }

    /**
     * An SSL connection with server cert authentication, simple pub/sub of an message
     */
    @Suppress
    @Throws(Exception::class)
    fun testSSLPubSub() {
        var mqttClient: MqttAndroidClient? = null
        val connectToken: IMqttToken?
        val disconnectToken: IMqttToken?
        val subToken: IMqttToken?
        val pubToken: IMqttDeliveryToken?
        try {
            mqttClient = MqttAndroidClient(mContext, mqttSSLServerURI!!, "testSSLPubSub")
            val options = MqttConnectOptions()
            options.socketFactory = mqttClient.getSSLSocketFactory(this.context.assets.open("test.bks"), keyStorePwd)
            val mqttV3Receiver = MqttV3Receiver(mqttClient, null)
            mqttClient.setCallback(mqttV3Receiver)
            connectToken = mqttClient.connect(options, this.context, ActionListener(notifier))
            connectToken.waitForCompletion(waitForCompletionTime.toLong())
            val topicNames = arrayOf("testSSLPubSub" + "/Topic")
            val topicQos = intArrayOf(0)
            val mqttMessage = MqttMessage("message for testSSLPubSub".toByteArray())
            val message = mqttMessage.payload
            subToken = mqttClient.subscribe(topicNames, topicQos, null, ActionListener(notifier))
            subToken.waitForCompletion(waitForCompletionTime.toLong())
            pubToken = mqttClient.publish(topicNames[0], message, 0, false, null, ActionListener(notifier))
            pubToken.waitForCompletion(waitForCompletionTime.toLong())
            TimeUnit.MILLISECONDS.sleep(6000)
            val ok = mqttV3Receiver.validateReceipt(topicNames[0], 0, message)
            if (!ok) {
                fail("Receive failed")
            }
        } catch (exception: Exception) {
            fail("Failed:testSSLPubSub exception=$exception")
        } finally {
            disconnectToken = mqttClient!!.disconnect(null, ActionListener(notifier))
            disconnectToken.waitForCompletion(waitForCompletionTime.toLong())
            mqttClient.close()
        }
    }

    private inner class ActionListener(val notifier: TestCaseNotifier) : IMqttActionListener {

        override fun onFailure(token: IMqttToken, exception: Throwable) {
            this.notifier.storeException(exception)
        }

        override fun onSuccess(token: IMqttToken) = Unit

    }
}