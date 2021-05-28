package info.mqtt.android.extsample.activity

import info.mqtt.android.extsample.components.MessageListItemAdapter
import java.util.ArrayList
import android.os.Bundle
import info.mqtt.android.extsample.internal.Connections
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Button
import androidx.fragment.app.Fragment
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.model.ReceivedMessage
import timber.log.Timber

class HistoryFragment : Fragment() {

    private var messageListAdapter: MessageListItemAdapter? = null
    private var messages: ArrayList<ReceivedMessage>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connections = Connections.getInstance(requireActivity()).connections
        val connection = connections[requireArguments().getString(ActivityConstants.CONNECTION_KEY)]
        setHasOptionsMenu(true)
        Timber.d("History Fragment: ${connection?.id}")
        setHasOptionsMenu(true)
        messages = connection?.messages
        connection?.addReceivedMessageListner { message ->
            Timber.d("GOT A MESSAGE in history ${String(message.message.payload)}")
            Timber.d("M: ${messages?.size}")
            messageListAdapter!!.notifyDataSetChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_connection_history, container, false)
        messageListAdapter = MessageListItemAdapter(requireContext(), messages ?: arrayListOf())
        val messageHistoryListView = rootView.findViewById<ListView>(R.id.history_list_view)
        messageHistoryListView.adapter = messageListAdapter
        val clearButton = rootView.findViewById<Button>(R.id.history_clear_button)
        clearButton.setOnClickListener {
            messages!!.clear()
            messageListAdapter!!.notifyDataSetChanged()
        }

        return rootView
    }

}