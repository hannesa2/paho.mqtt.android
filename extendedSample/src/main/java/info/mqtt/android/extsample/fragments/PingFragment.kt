package info.mqtt.android.extsample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import info.mqtt.android.extsample.adapter.PingListAdapter
import info.mqtt.android.extsample.ActivityConstants
import info.mqtt.android.extsample.repository.PingViewModel
import info.mqtt.android.extsample.databinding.ContentPingBinding
import info.mqtt.android.extsample.internal.Connection
import info.mqtt.android.extsample.internal.Connections
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class PingFragment : Fragment() {

    private lateinit var connection: Connection
    private lateinit var pingViewModel: PingViewModel

    private var _binding: ContentPingBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connections = Connections.getInstance(requireActivity()).connections
        connection = connections[requireArguments().getString(ActivityConstants.CONNECTION_KEY)]!!
        Timber.d("CONNECTION_KEY=${requireArguments().getString(ActivityConstants.CONNECTION_KEY)} '${connection.id}'")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentPingBinding.inflate(inflater, container, false)

        val adapter = PingListAdapter(requireContext())
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())

        pingViewModel = ViewModelProvider(this).get(PingViewModel::class.java)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pingViewModel.listStateFlow.collectLatest { pingEntities ->
                    adapter.setWords(pingEntities)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                connection.history.collectLatest {
                    adapter.notifyDataSetChanged()
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
