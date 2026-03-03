package info.mqtt.android.extsample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import info.mqtt.android.extsample.ActivityConstants
import info.mqtt.android.extsample.adapter.MessageListItemAdapter
import info.mqtt.android.extsample.databinding.FragmentConnectionHistoryBinding
import info.mqtt.android.extsample.internal.Connection
import info.mqtt.android.extsample.internal.Connections
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
        Timber.d("CONNECTION_KEY=${requireArguments().getString(ActivityConstants.CONNECTION_KEY)} '${connection.id}'")
        
        messageListAdapter = MessageListItemAdapter(requireContext(), connection.messages.value)
        binding.historyListView.adapter = messageListAdapter
        
        viewLifecycleOwner.lifecycleScope.launch {
            connection.messages.collectLatest {
                messageListAdapter.notifyDataSetChanged()
            }
        }

        binding.historyClearButton.setOnClickListener {
            connection.messageList.clear()
            messageListAdapter.notifyDataSetChanged()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
