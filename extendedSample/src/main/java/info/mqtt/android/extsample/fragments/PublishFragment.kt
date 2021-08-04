package info.mqtt.android.extsample.fragments

import android.os.Bundle
import info.mqtt.android.extsample.internal.Connections
import timber.log.Timber
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import info.mqtt.android.extsample.ActivityConstants
import info.mqtt.android.extsample.MainActivity
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.internal.Connection
import org.eclipse.paho.android.service.QoS

class PublishFragment : Fragment() {

    private var connection: Connection? = null
    private var selectedQos = QoS.AtMostOnce
    private var retainValue = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connections = Connections.getInstance(requireActivity()).connections
        connection = connections[requireArguments().getString(ActivityConstants.CONNECTION_KEY)]
        Timber.d("CONNECTION_KEY=${requireArguments().getString(ActivityConstants.CONNECTION_KEY)} '${connection!!.id}'")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_publish, container, false)
        val topicText = rootView.findViewById<EditText>(R.id.topic)
        val messageText = rootView.findViewById<EditText>(R.id.message)
        val qos = rootView.findViewById<Spinner>(R.id.qos_spinner)
        val retain = rootView.findViewById<SwitchCompat>(R.id.retain_switch)
        topicText.setText(DEFAULT_TOTPIC)

        qos.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedQos = QoS.values()[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        retain.setOnCheckedChangeListener { _, isChecked -> retainValue = isChecked }
        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, QoS.values())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        qos.adapter = adapter
        val publishButton = rootView.findViewById<Button>(R.id.publish_button)
        publishButton.setOnClickListener {
            Timber.d("Publishing: [topic: ${topicText.text}, message: ${messageText.text}, QoS: $selectedQos, Retain: $retainValue]")
            connection?.let { it1 ->
                (requireActivity() as MainActivity).publish(it1, topicText.text.toString(), messageText.text.toString(), selectedQos, retainValue)
            } ?: run {
                Toast.makeText(requireContext(), "Offline !", Toast.LENGTH_SHORT).show()
            }
        }
        return rootView
    }

    companion object {
        private const val DEFAULT_TOTPIC = "/test"
    }
}