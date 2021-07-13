package info.mqtt.android.extsample.fragments

import info.mqtt.android.extsample.adapter.MessageListItemAdapter
import android.os.Bundle
import info.mqtt.android.extsample.internal.Connections
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Button
import androidx.fragment.app.Fragment
import info.mqtt.android.extsample.ActivityConstants
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.internal.Connection
import info.mqtt.android.extsample.internal.IReceivedMessageListener
import info.mqtt.android.extsample.model.ReceivedMessage
import timber.log.Timber

class MessagesFragment : Fragment() {

    private var messageListAdapter: MessageListItemAdapter? = null
    private lateinit var connection: Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connections = Connections.getInstance(requireActivity()).connections
        connection = connections[requireArguments().getString(ActivityConstants.CONNECTION_KEY)]!!
        setHasOptionsMenu(true)
        Timber.d("CONNECTION_KEY=${requireArguments().getString(ActivityConstants.CONNECTION_KEY)} '${connection.id}'")
        setHasOptionsMenu(true)
        connection.addReceivedMessageListener(object : IReceivedMessageListener {
            override var identifer: String = MessagesFragment::class.java.simpleName

            override fun onMessageReceived(message: ReceivedMessage?) {
                Timber.d("Message in history ${String(message?.message?.payload!!)} ${connection.messages.size}")
                messageListAdapter!!.notifyDataSetChanged()
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_connection_history, container, false)
        messageListAdapter = MessageListItemAdapter(requireContext(), connection.messages)
        val messageHistoryListView = rootView.findViewById<ListView>(R.id.history_list_view)
        messageHistoryListView.adapter = messageListAdapter
        val clearButton = rootView.findViewById<Button>(R.id.history_clear_button)
        clearButton.setOnClickListener {
            connection.messages.clear()
            messageListAdapter!!.notifyDataSetChanged()
        }

        return rootView
    }

}