package info.mqtt.android.extsample.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import info.mqtt.android.extsample.adapter.PingListAdapter
import info.mqtt.android.extsample.ActivityConstants
import info.mqtt.android.extsample.repository.PingViewModel
import info.mqtt.android.extsample.databinding.ContentPingBinding
import info.mqtt.android.extsample.internal.Connection
import info.mqtt.android.extsample.internal.Connections
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
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ContentPingBinding.inflate(inflater, container, false)

        val adapter = PingListAdapter(requireContext())
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(requireContext())

        pingViewModel = ViewModelProvider(this).get(PingViewModel::class.java)

        pingViewModel.listLiveData.observe(requireActivity()) { pingEntities ->
            pingEntities?.let { adapter.setWords(it) }
        }

        connection.history.observe(this.viewLifecycleOwner) {
            adapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
