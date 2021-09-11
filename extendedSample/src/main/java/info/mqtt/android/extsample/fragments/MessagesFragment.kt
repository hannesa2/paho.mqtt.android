package info.mqtt.android.extsample.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import androidx.fragment.app.Fragment
import info.mqtt.android.extsample.ActivityConstants
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.adapter.MessageListItemAdapter
import info.mqtt.android.extsample.internal.Connection
import info.mqtt.android.extsample.internal.Connections
import info.mqtt.android.extsample.internal.IReceivedMessageListener
import info.mqtt.android.extsample.model.ReceivedMessage
import timber.log.Timber

class MessagesFragment : Fragment() {

    private lateinit var messageListAdapter: MessageListItemAdapter
    private lateinit var connection: Connection

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_connection_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val connections = Connections.getInstance(requireActivity()).connections
        connection = connections[requireArguments().getString(ActivityConstants.CONNECTION_KEY)]!!
        setHasOptionsMenu(true)
        Timber.d("CONNECTION_KEY=${requireArguments().getString(ActivityConstants.CONNECTION_KEY)} '${connection.id}'")
        setHasOptionsMenu(true)
        connection.addReceivedMessageListener(object : IReceivedMessageListener {
            override var identifer: String = MessagesFragment::class.java.simpleName

            override fun onMessageReceived(message: ReceivedMessage?) {
                Timber.d("Message in history ${String(message?.message?.payload!!)} ${connection.messages.size} ${Thread.currentThread().name}")
                messageListAdapter.messages = listOf(*connection.messages.toTypedArray())
                messageListAdapter.notifyDataSetChanged()
            }
        })

        val tempList = listOf(*connection.messages.toTypedArray())
        messageListAdapter = MessageListItemAdapter(requireContext(), tempList)
        val messageHistoryListView = view.findViewById<ListView>(R.id.history_list_view)
        messageHistoryListView.adapter = messageListAdapter
        val clearButton = view.findViewById<Button>(R.id.history_clear_button)
        clearButton.setOnClickListener {
            Handler(Looper.getMainLooper()).run {
                connection.messages.clear()
                messageListAdapter.notifyDataSetChanged()
            }
        }

    }

}
