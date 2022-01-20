package info.mqtt.android

import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.*
import java.util.concurrent.TimeUnit

class MqttReceiver(mqttClient: IMqttAsyncClient) : MqttCallback {
    private var isReportConnectionLoss = true
    private var connected = false
    private var clientId: String
    private val receivedMessages = Collections.synchronizedList(ArrayList<ReceivedMessage>())

    init {
        connected = true
        clientId = mqttClient.clientId
    }

    @Throws(InterruptedException::class)
    fun receiveNext(waitMilliseconds: Long): ReceivedMessage? {
        val methodName = "receiveNext"
        var receivedMessage: ReceivedMessage? = null
        if (receivedMessages.isEmpty()) {
            TimeUnit.MILLISECONDS.sleep(waitMilliseconds)
        }
        Log.i(methodName, "receiveNext time is " + Date().toString())
        Log.i(methodName, "receivedMessages = $receivedMessages")
        if (!receivedMessages.isEmpty()) {
            Log.i(methodName, "MqttV3Receiver receive message")
            receivedMessage = receivedMessages.removeAt(0)
        }
        return receivedMessage
    }

    @Throws(InterruptedException::class)
    fun validateReceipt(sendTopic: String, expectedQos: Int, sentBytes: ByteArray?): ValidateResult {
        val waitMilliseconds: Long = 10000
        val receivedMessage = receiveNext(waitMilliseconds)
            ?: return ValidateResult(false, "No message received in waitMilliseconds=$waitMilliseconds")

        return if (sendTopic != receivedMessage.topic) {
            ValidateResult(false, " Received invalid topic sent=" + sendTopic + " received topic=" + receivedMessage.topic)
        } else if (expectedQos != receivedMessage.message.qos) {
            ValidateResult(false, "expectedQos=" + expectedQos + " != Received Qos=" + receivedMessage.message.qos)
        } else if (!Arrays.equals(sentBytes, receivedMessage.message.payload)) {
            ValidateResult(
                false,
                "Sent    :${String(sentBytes!!)}\n" +
                        "Received:${String(receivedMessage.message.payload)}\n\n" +
                        "Received invalid payload !\n"
            )
        } else
            ValidateResult(true, "")
    }

    /**
     * Validate receipt of a batch of messages sent to a topic by a number of
     * publishers The message payloads are expected to have the format**
     * "Batch Message payload :<batch>:<publisher>:<messageNumber>:<any additional payload>"
     *
     *
     * We want to detect excess messages, so we don't just handle a certain
     * number. Instead we wait for a timeout period, and exit if no message is
     * received in that period.** The timeout period can make this test long
     * running, so we attempt to dynamically adjust, allowing 10 seconds for the
     * first message and then averaging the time taken to receive messages and
     * applying some fudge factors.
     */
    @Throws(InterruptedException::class)
    fun validateReceipt(
        sendTopics: MutableList<String?>, expectedQosList: MutableList<Int>,
        expectedBatchNumber: Int, nPublishers: Int, sentBytes: MutableList<ByteArray?>,
        expectOrdered: Boolean
    ): Boolean {
        val expectedMessageNumbers = IntArray(nPublishers)
        for (i in 0 until nPublishers) {
            expectedMessageNumbers[i] = 0
        }
        var waitMilliseconds: Long = 10000

        // track time taken to receive messages
        var totWait: Long = 0
        var messageNo = 0
        while (true) {
            val startWait = System.currentTimeMillis()
            val receivedMessage = receiveNext(waitMilliseconds) ?: break
            messageNo++
            totWait += System.currentTimeMillis() - startWait

            // Calculate new wait time based on experience, but not allowing it
            // to get too small
            waitMilliseconds = Math.max(totWait / messageNo, 500)
            val payload = receivedMessage.message.payload
            val payloadString = String(payload)
            if (!payloadString.startsWith("Batch Message payload :")) {
                report("Received invalid payload\nReceived:$payloadString")
                report("Payload did not start with {" + "Batch Message payload :" + "}")
                return false
            }
            val payloadParts = payloadString.split(":").toTypedArray()
            if (payloadParts.size != 5) {
                report("Received invalid payload\nReceived:$payloadString")
                report("Payload was not of expected format")
                return false
            }
            try {
                val batchNumber = payloadParts[1].toInt()
                if (batchNumber != expectedBatchNumber) {
                    report("Received invalid payload\nReceived:$payloadString")
                    report("batchnumber$batchNumber was not the expected value $expectedBatchNumber")
                    return false
                }
            } catch (e: NumberFormatException) {
                report("Received invalid payload\nReceived:$payloadString")
                report("batchnumber was not a numeric value")
                return false
            }
            var publisher: Int
            try {
                publisher = payloadParts[2].toInt()
                if (publisher < 0 || publisher >= nPublishers) {
                    report("Received invalid payload\nReceived:$payloadString")
                    report("publisher " + publisher + " was not in the range 0 - " + (nPublishers - 1))
                    return false
                }
            } catch (e: NumberFormatException) {
                report("Received invalid payload\nReceived:$payloadString")
                report("publisher was not a numeric value")
                return false
            }
            if (expectOrdered) {
                try {
                    val messageNumber = payloadParts[3].toInt()
                    if (messageNumber == expectedMessageNumbers[publisher]) {
                        expectedMessageNumbers[publisher] += 1
                    } else {
                        report("Received invalid payload\nReceived:$payloadString")
                        report("messageNumber " + messageNumber + " was received out of sequence - expected value was " + expectedMessageNumbers[publisher])
                        return false
                    }
                } catch (e: NumberFormatException) {
                    report("Received invalid payload\nReceived:$payloadString")
                    report("messageNumber was not a numeric value")
                    return false
                }
            }
            var location = 0
            while (location < sentBytes.size) {
                if (Arrays.equals(payload, sentBytes[location])) {
                    break
                }
                location++
            }
            var sendTopic: String?
            var expectedQos: Int
            if (location < sentBytes.size) {
                sentBytes.removeAt(location)
                sendTopic = sendTopics.removeAt(location)
                expectedQos = expectedQosList.removeAt(location)
            } else {
                report("Received invalid payload\nReceived:$payloadString")
                for (expectedPayload in sentBytes) {
                    report("\texpected message :" + String(expectedPayload!!))
                }
                return false
            }
            if (sendTopic != receivedMessage.topic) {
                report(" Received invalid topic sent=" + sendTopic + " received topic=" + receivedMessage.topic)
                return false
            }
            if (expectedQos != receivedMessage.message.qos) {
                report("expectedQos=" + expectedQos + " != Received Qos=" + receivedMessage.message.qos)
                return false
            }
        }
        if (sentBytes.isNotEmpty()) {
            for (missedPayload in sentBytes) {
                report("Did not receive message ${String(missedPayload!!)}".trimIndent())
            }
            return false
        }
        return true
    }

    @Synchronized
    @Throws(InterruptedException::class)
    fun waitForConnectionLost(waitMilliseconds: Long): Boolean {
        if (connected) {
            Thread.sleep(waitMilliseconds)
        }
        return connected
    }

    override fun connectionLost(cause: Throwable?) {
        if (isReportConnectionLoss) {
            report("ConnectionLost: clientId=$clientId cause=$cause")
        }
        synchronized(this) {
            connected = false
        }
    }

    override fun deliveryComplete(arg0: IMqttDeliveryToken) = Unit

    override fun messageArrived(topic: String, message: MqttMessage) {
        val methodName = "messageArrived"
        Log.i(methodName, "messageArrived $topic = $message clientId = $clientId")
        Log.i(methodName, "messageArrived " + Date().toString())
        receivedMessages.add(ReceivedMessage(topic, message))
        Log.i(methodName, "receivedMessages = $receivedMessages")

        // notify();
    }

    private fun report(text: String?) {
        Log.e(this.javaClass.canonicalName, text!!)
    }

    data class ReceivedMessage(var topic: String, var message: MqttMessage)
    data class ValidateResult(val ok: Boolean, var message: String)
}