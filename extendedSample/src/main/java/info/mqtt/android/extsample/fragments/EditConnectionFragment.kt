package info.mqtt.android.extsample.fragments

import info.mqtt.android.extsample.internal.Connections.Companion.getInstance
import info.mqtt.android.extsample.model.ConnectionModel
import android.os.Bundle
import timber.log.Timber
import android.text.TextWatcher
import android.text.Editable
import android.widget.AdapterView.OnItemSelectedListener
import android.annotation.SuppressLint
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import info.mqtt.android.extsample.ActivityConstants
import info.mqtt.android.extsample.MainActivity
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.internal.Connection
import org.eclipse.paho.android.service.QoS
import java.lang.StringBuilder
import java.util.*

class EditConnectionFragment : Fragment() {
    private var clientId: EditText? = null
    private var serverHostname: EditText? = null
    private lateinit var serverPort: EditText
    private lateinit var cleanSession: SwitchCompat
    private var username: EditText? = null
    private var password: EditText? = null
    private var tlsServerKey: EditText? = null
    private var tlsClientKey: EditText? = null
    private var timeout: EditText? = null
    private var keepAlive: EditText? = null
    private var lwtTopic: EditText? = null
    private var lwtMessage: EditText? = null
    private lateinit var lwtQos: Spinner
    private lateinit var lwtRetain: SwitchCompat
    private lateinit var formModel: ConnectionModel
    private var newConnection = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_edit_connection, container, false)
        clientId = rootView.findViewById(R.id.client_id)
        serverHostname = rootView.findViewById(R.id.hostname)
        serverPort = rootView.findViewById(R.id.add_connection_port)
        serverPort.setText("")
        cleanSession = rootView.findViewById(R.id.clean_session_switch)
        username = rootView.findViewById(R.id.username)
        password = rootView.findViewById(R.id.password)
        tlsServerKey = rootView.findViewById(R.id.tls_server_key)
        tlsClientKey = rootView.findViewById(R.id.tls_client_key)
        timeout = rootView.findViewById(R.id.timeout)
        keepAlive = rootView.findViewById(R.id.keepalive)
        lwtTopic = rootView.findViewById(R.id.lwt_topic)
        lwtMessage = rootView.findViewById(R.id.lwt_message)
        lwtQos = rootView.findViewById(R.id.lwt_qos_spinner)
        lwtRetain = rootView.findViewById(R.id.retain_switch)
        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, QoS.values())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        lwtQos.adapter = adapter
        if (this.arguments != null && requireArguments().getString(ActivityConstants.CONNECTION_KEY) != null) {
            val connections: HashMap<String, Connection> = getInstance(requireActivity()).connections
            val connectionKey = requireArguments().getString(ActivityConstants.CONNECTION_KEY)
            val connection = connections[connectionKey]
            Timber.d("Editing an existing connection: ${connection!!.handle()}")
            newConnection = false
            formModel = ConnectionModel(connection)
            Timber.d("Form Model: $formModel")
            formModel.clientHandle = connection.handle()
        } else {
            formModel = ConnectionModel()
        }
        populateFromConnectionModel(formModel)
        setFormItemListeners()

        // Inflate the layout for this fragment
        return rootView
    }

    private fun setFormItemListeners() {
        clientId!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                formModel.clientId = s.toString()
            }
        })
        serverHostname!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                formModel.serverHostName = s.toString()
            }
        })
        serverPort.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString().isNotEmpty()) {
                    formModel.serverPort = s.toString().toInt()
                }
            }
        })
        cleanSession.setOnCheckedChangeListener { _, isChecked -> formModel.isCleanSession = isChecked }
        username!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString().trim { it <= ' ' } != "") {
                    formModel.username = s.toString()
                } else {
                    formModel.username = ActivityConstants.empty
                }
            }
        })
        password!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString().trim { it <= ' ' } != "") {
                    formModel.password = s.toString()
                } else {
                    formModel.password = ActivityConstants.empty
                }
            }
        })
        tlsServerKey!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                formModel.tlsServerKey = s.toString()
            }
        })
        tlsClientKey!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                formModel.tlsClientKey = s.toString()
            }
        })
        timeout!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString().isNotEmpty()) {
                    formModel.timeout = s.toString().toInt()
                }
            }
        })
        keepAlive!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (s.toString().isNotEmpty()) {
                    formModel.keepAlive = s.toString().toInt()
                }
            }
        })
        lwtTopic!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                formModel.lwtTopic = s.toString()
            }
        })
        lwtMessage!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                formModel.lwtMessage = s.toString()
            }
        })
        lwtQos.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
                formModel.lwtQos = QoS.values()[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        lwtRetain.setOnCheckedChangeListener { _, isChecked -> formModel.isLwtRetain = isChecked }
    }

    @SuppressLint("SetTextI18n")
    private fun populateFromConnectionModel(connectionModel: ConnectionModel) {
        clientId!!.setText(connectionModel.clientId)
        serverHostname!!.setText(connectionModel.serverHostName)
        serverPort.setText(connectionModel.serverPort.toString())
        cleanSession.isChecked = connectionModel.isCleanSession
        username!!.setText(connectionModel.username)
        password!!.setText(connectionModel.password)
        tlsServerKey!!.setText(connectionModel.tlsServerKey)
        tlsClientKey!!.setText(connectionModel.tlsClientKey)
        timeout!!.setText(connectionModel.timeout.toString())
        keepAlive!!.setText(connectionModel.keepAlive.toString())
        lwtTopic!!.setText(connectionModel.lwtTopic)
        lwtMessage!!.setText(connectionModel.lwtMessage)
        lwtQos.setSelection(connectionModel.lwtQos.value)
        lwtRetain.isChecked = connectionModel.isLwtRetain
    }

    private fun saveConnection() {
        Timber.d(formModel.toString())
        if (newConnection) {
            // Generate a new Client Handle
            val sb = StringBuilder(length)
            for (i in 0 until length) {
                sb.append(AB[random.nextInt(AB.length)])
            }
            val clientHandle = sb.toString() + '-' + formModel.serverHostName + '-' + formModel.clientId
            formModel.clientHandle = clientHandle
            (activity as MainActivity).persistAndConnect(formModel)
        } else {
            // Update an existing connection
            (activity as MainActivity).updateAndConnect(formModel)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_edit_connection, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_save_connection) {
            saveConnection()
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val random = Random()
        private const val length = 8
    }
}
