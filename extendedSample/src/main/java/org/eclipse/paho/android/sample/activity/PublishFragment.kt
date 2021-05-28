package org.eclipse.paho.android.sample.activity

import android.os.Bundle
import org.eclipse.paho.android.sample.internal.Connections
import timber.log.Timber
import android.view.LayoutInflater
import android.view.ViewGroup
import org.eclipse.paho.android.sample.R
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import android.text.TextWatcher
import android.text.Editable
import android.view.View
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.fragment.app.Fragment

class PublishFragment : Fragment() {

    private var connection: Connection? = null
    private var selectedQos = 0
    private var retainValue = false
    private var topic = "/test"
    private var message = "Hello world"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connections = Connections.getInstance(requireActivity()).connections
        connection = connections[requireArguments().getString(ActivityConstants.CONNECTION_KEY)]
        Timber.d("FRAGMENT CONNECTION: ${requireArguments().getString(ActivityConstants.CONNECTION_KEY)}")
        Timber.d("NAME:${connection!!.id}")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_publish, container, false)
        val topicText = rootView.findViewById<EditText>(R.id.topic)
        val messageText = rootView.findViewById<EditText>(R.id.message)
        val qos = rootView.findViewById<Spinner>(R.id.qos_spinner)
        val retain = rootView.findViewById<Switch>(R.id.retain_switch)
        topicText.setText(topic)
        topicText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable) {
                topic = s.toString()
            }
        })
        messageText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable) {
                message = s.toString()
            }
        })
        qos.onItemSelectedListener = object : OnItemSelectedListener {

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedQos = resources.getStringArray(R.array.qos_options)[position].toInt()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        retain.setOnCheckedChangeListener { _, isChecked -> retainValue = isChecked }
        val adapter = ArrayAdapter
            .createFromResource(requireActivity(), R.array.qos_options, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        qos.adapter = adapter
        val publishButton = rootView.findViewById<Button>(R.id.publish_button)
        publishButton.setOnClickListener {
            Timber.d("Publishing: [topic: $topic, message: $message, QoS: $selectedQos, Retain: $retainValue]")
            (requireActivity() as MainActivity).publish(connection, topic, message, selectedQos, retainValue)
        }
        return rootView
    }
}