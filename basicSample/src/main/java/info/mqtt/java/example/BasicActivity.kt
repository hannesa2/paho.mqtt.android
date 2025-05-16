package info.mqtt.java.example

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
import androidx.core.net.toUri

class BasicActivity : AppCompatActivity() {

    private lateinit var mqttAndroidClient: MqttAndroidClient
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var binding: ActivityScrollingBinding
    private var hasNotificationPermissionGranted = false
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasNotificationPermissionGranted = isGranted
        if (!isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= 33) {
                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                        showNotificationPermissionRationale()
                    } else {
                        showSettingDialog()
                    }
                }
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "notification permission granted", Snackbar.LENGTH_LONG).setAction("Action", null)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScrollingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.toolbar)

        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermissionGranted = true
        }

        binding.fab.setOnClickListener { publishMessage() }
        val mLayoutManager: LayoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.layoutManager = mLayoutManager
        historyAdapter = HistoryAdapter()
        binding.historyRecyclerView.adapter = historyAdapter
        mqttAndroidClient = MqttAndroidClient(applicationContext, SERVER_URI, clientId)
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                if (reconnect) {
                    addToHistory("Reconnected: $serverURI")
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic()
                } else {
                    addToHistory("Connected: $serverURI")
                }
            }

            override fun connectionLost(cause: Throwable?) {
                addToHistory("The Connection was lost.")
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                addToHistory("Incoming message: " + String(message.payload))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) = Unit
        })
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false
        addToHistory("Connecting: $SERVER_URI")
        mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                val disconnectedBufferOptions = DisconnectedBufferOptions().apply {
                    isBufferEnabled = true
                    bufferSize = 100
                    isPersistBuffer = false
                    isDeleteOldestMessages = false
                }
                mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                subscribeToTopic()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                addToHistory("Failed to connect: $SERVER_URI")
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
            historyAdapter.add("$timestamp $mainText")
        }
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG).setAction("Action", null).show()
    }

    fun subscribeToTopic() {
        mqttAndroidClient.subscribe(SUBSCRIPTION_TOPIC, QoS.AtMostOnce.value, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken) {
                addToHistory("Subscribed! $SUBSCRIPTION_TOPIC")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                addToHistory("Failed to subscribe $exception")
            }
        })

        // THIS DOES NOT WORK!
        mqttAndroidClient.subscribe(SUBSCRIPTION_TOPIC, QoS.AtMostOnce.value) { topic, message ->
            Timber.d("Message arrived $topic : ${String(message.payload)}")
            addToHistory("Message arrived $message")
        }
    }

    private fun publishMessage() {
        val message = MqttMessage()
        message.payload = PUBLISH_MESSAGE.toByteArray()
        if (mqttAndroidClient.isConnected) {
            mqttAndroidClient.publish(PUBLISH_TOPIC, message)
            addToHistory("Message Published >$PUBLISH_MESSAGE<")
            if (!mqttAndroidClient.isConnected) {
                addToHistory(mqttAndroidClient.bufferedMessageCount.toString() + " messages in buffer.")
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_SHORT).setAction("Action", null).show()
        }
    }

    private fun showSettingDialog() {
        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Material3)
            .setTitle("Notification Permission")
            .setMessage("Notification permission is required, Please allow notification permission from setting").setPositiveButton("Ok") { _, _ ->
                val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showNotificationPermissionRationale() {
        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Material3)
            .setTitle("Alert")
            .setMessage("Notification permission is required, to show notification").setPositiveButton("Ok") { _, _ ->
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val SERVER_URI = "tcp://broker.hivemq.com:1883"
        private const val SUBSCRIPTION_TOPIC = "exampleAndroidTopic"
        private const val PUBLISH_TOPIC = "exampleAndroidPublishTopic"
        private const val PUBLISH_MESSAGE = "Hello World"
        private var clientId = "BasicSample" + System.currentTimeMillis()
    }
}
