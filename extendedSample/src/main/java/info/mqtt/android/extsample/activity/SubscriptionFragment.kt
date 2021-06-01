package info.mqtt.android.extsample.activity

import info.mqtt.android.extsample.internal.Connections.Companion.getInstance
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.components.SubscriptionListItemAdapter
import timber.log.Timber
import org.eclipse.paho.client.mqttv3.MqttException
import android.annotation.SuppressLint
import android.content.Context
import android.widget.AdapterView.OnItemSelectedListener
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import info.mqtt.android.extsample.model.Subscription
import java.util.ArrayList
import java.util.HashMap

class SubscriptionFragment : Fragment() {

    private var tempQosValue = 0
    private lateinit var subscriptions: ArrayList<Subscription>
    private var connection: Connection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        val connectionHandle = bundle!!.getString(ActivityConstants.CONNECTION_KEY)
        val connections: HashMap<String, Connection> = getInstance(requireActivity()).connections
        connection = connections[connectionHandle]
        subscriptions = connection!!.getSubscriptions()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_subscriptions, container, false)
        val subscribeButton = rootView.findViewById<Button>(R.id.subscribe_button)
        subscribeButton.setOnClickListener { showInputDialog() }
        val subscriptionListView = rootView.findViewById<ListView>(R.id.subscription_list_view)
        val adapter = SubscriptionListItemAdapter(requireContext(), subscriptions)
        adapter.addOnUnsubscribeListener(object : SubscriptionListItemAdapter.OnUnsubscribeListener {
            override fun onUnsubscribe(subscription: Subscription) {
                try {
                    connection!!.unsubscribe(subscription)
                    Timber.d("Unsubscribed from: $subscription")
                } catch (ex: MqttException) {
                    Timber.d("Failed to unsubscribe from $subscription ${ex.message}")
                }
            }
        })
        subscriptionListView.adapter = adapter
        return rootView
    }

    private fun showInputDialog() {
        val layoutInflater = requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        @SuppressLint("InflateParams")
        val promptView = layoutInflater.inflate(R.layout.subscription_dialog, null)
        val topicText = promptView.findViewById<EditText>(R.id.subscription_topic_edit_text)
        val qos = promptView.findViewById<Spinner>(R.id.subscription_qos_spinner)
        val adapter = ArrayAdapter.createFromResource(requireActivity(), R.array.qos_options, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        qos.adapter = adapter
        qos.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                tempQosValue = resources.getStringArray(R.array.qos_options)[position].toInt()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        val notifySwitch = promptView.findViewById<SwitchCompat>(R.id.show_notifications_switch)
        val alertDialogBuilder = AlertDialog.Builder(requireActivity())
        alertDialogBuilder.setView(promptView)
        alertDialogBuilder.setCancelable(true).setPositiveButton(R.string.subscribe_ok) { _, _ ->
            val topic = topicText.text.toString()
            val subscription = Subscription(topic, tempQosValue, connection!!.handle(), notifySwitch.isChecked)
            subscriptions.add(subscription)
            try {
                connection!!.addNewSubscription(subscription)
            } catch (ex: MqttException) {
                Timber.d(ex)
            }
            adapter.notifyDataSetChanged()
        }.setNegativeButton(R.string.subscribe_cancel) { dialog, _ -> dialog.cancel() }
        val alert = alertDialogBuilder.create()
        alert.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        alert.show()
    }
}