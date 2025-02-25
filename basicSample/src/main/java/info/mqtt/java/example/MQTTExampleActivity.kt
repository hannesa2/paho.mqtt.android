package info.mqtt.java.example

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.material.snackbar.Snackbar
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.QoS
import info.mqtt.java.example.databinding.ActivityScrollingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.*
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class MQTTExampleActivity : AppCompatActivity() {

    private lateinit var mqttAndroidClient: MqttAndroidClient
    private lateinit var mqttAndroidClient_new: MqttAndroidClient
    private lateinit var adapter: HistoryAdapter
    private lateinit var binding: ActivityScrollingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScrollingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { publishMessage() }
        val mLayoutManager: LayoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.layoutManager = mLayoutManager
        adapter = HistoryAdapter()
        binding.historyRecyclerView.adapter = adapter
        clientId += System.currentTimeMillis()
        mqttAndroidClient = MqttAndroidClient(applicationContext, serverUri, clientId)
        mqttAndroidClient_new = MqttAndroidClient(applicationContext, serverUri, clientId = "BasicSample_2_${System.currentTimeMillis()}")
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                if (reconnect) {
                    addToHistory("Reconnected: $serverURI mqtt1")
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic(mqttAndroidClient,"mqtt1")
                } else {
                    addToHistory("Connected: $serverURI mqtt1")
                }
            }

            override fun connectionLost(cause: Throwable?) {
                addToHistory("The Connection was lost. mqtt1")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                addToHistory("mqtt1 Incoming message: " + String(message.payload))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })

        mqttAndroidClient_new.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                if (reconnect) {
                    addToHistory("Reconnected: $serverURI mqtt2")
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic(mqttAndroidClient_new,"mqtt2")
                } else {
                    addToHistory("Connected: $serverURI mqtt2")
                }
            }

            override fun connectionLost(cause: Throwable?) {
                addToHistory("The Connection was lost. mqtt2")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                addToHistory("mqtt2 Incoming message: " + String(message.payload))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}
        })


        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false
        mqttConnectOptions.keepAliveInterval = 20
        mqttConnectOptions.userName = "cloudapp"
        mqttConnectOptions.password = "cloudapp".toCharArray()

        addToHistory("Mqtt1 - Connecting: $serverUri")
        mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                val disconnectedBufferOptions = DisconnectedBufferOptions()
                disconnectedBufferOptions.isBufferEnabled = true
                disconnectedBufferOptions.bufferSize = 100
                disconnectedBufferOptions.isPersistBuffer = false
                disconnectedBufferOptions.isDeleteOldestMessages = false
                mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                subscribeToTopic(mqttAndroidClient,"mqtt1")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                addToHistory("Mqtt1 - Failed to connect: $serverUri")
            }
        })
        addToHistory("Mqtt2 - Connecting: $serverUri")
        mqttAndroidClient_new.connect(mqttConnectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                val disconnectedBufferOptions = DisconnectedBufferOptions()
                disconnectedBufferOptions.isBufferEnabled = true
                disconnectedBufferOptions.bufferSize = 100
                disconnectedBufferOptions.isPersistBuffer = false
                disconnectedBufferOptions.isDeleteOldestMessages = false
                mqttAndroidClient_new.setBufferOpts(disconnectedBufferOptions)
                subscribeToTopic(mqttAndroidClient_new,"mqtt2")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                addToHistory("Mqtt2 - Failed to connect: $serverUri")
            }
        })
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        mqttAndroidClient.disconnect()
        super.onDestroy()
    }

    private fun addToHistory(mainText: String) {
        Timber.d(mainText)
        @SuppressLint("SimpleDateFormat")
        val timestamp = SimpleDateFormat("HH:mm.ss.SSS").format(Date(System.currentTimeMillis()))
        CoroutineScope(Dispatchers.Main).launch {
            adapter.add("$timestamp $mainText")
        }
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG).setAction("Action", null).show()
    }

    fun subscribeToTopic(client: MqttAndroidClient,tag: String) {
        client.subscribe(subscriptionTopic, QoS.AtMostOnce.value, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                addToHistory("${tag} Subscribed! $subscriptionTopic")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                addToHistory("${tag}  Failed to subscribe $exception")
            }
        })

        // THIS DOES NOT WORK!
        client.subscribe(subscriptionTopic, QoS.AtMostOnce.value) { topic, message ->
            Timber.d("${tag}  Message arrived $topic : ${String(message.payload)}")
            addToHistory("${tag}  Message arrived $message")
        }
    }

    private fun publishMessage() {
        val message = MqttMessage()
        message.payload = publishMessage.toByteArray()
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.publish(publishTopic, message)
            addToHistory("Message Published >$publishMessage<")
            if (!mqttAndroidClient.isConnected) {
                addToHistory(mqttAndroidClient.bufferedMessageCount.toString() + " messages in buffer.")
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_SHORT).setAction("Action", null).show()
        }
    }

    companion object {
        private const val serverUri = "tcp://be-test.mqtt.ubiecloud.com:1883"
        private const val subscriptionTopic = "exampleAndroidTopic"
        private const val publishTopic = "exampleAndroidPublishTopic"
        private const val publishMessage = "Hello World"
        private var clientId = "BasicSample"
    }
}
