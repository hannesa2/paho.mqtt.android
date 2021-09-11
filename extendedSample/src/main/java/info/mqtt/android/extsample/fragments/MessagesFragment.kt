package info.mqtt.android.extsample.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import info.mqtt.android.extsample.ActivityConstants
import info.mqtt.android.extsample.adapter.MessageListItemAdapter
import info.mqtt.android.extsample.databinding.FragmentConnectionHistoryBinding
import info.mqtt.android.extsample.internal.Connection
import info.mqtt.android.extsample.internal.Connections
import info.mqtt.android.extsample.internal.IReceivedMessageListener
import info.mqtt.android.extsample.model.ReceivedMessage
import timber.log.Timber

class MessagesFragment : Fragment() {

    private lateinit var messageListAdapter: MessageListItemAdapter
    private lateinit var connection: Connection

    private var _binding: FragmentConnectionHistoryBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConnectionHistoryBinding.inflate(inflater, container, false)
        return binding.root
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
        binding.historyListView.adapter = messageListAdapter
        binding.historyClearButton.setOnClickListener {
            Handler(Looper.getMainLooper()).run {
                connection.messages.clear()
                messageListAdapter.notifyDataSetChanged()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
