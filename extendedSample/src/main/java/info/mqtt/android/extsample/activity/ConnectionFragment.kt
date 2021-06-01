package info.mqtt.android.extsample.activity

import info.mqtt.android.extsample.internal.Connections.Companion.getInstance
import android.os.Bundle
import android.view.*
import info.mqtt.android.extsample.R
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTabHost
import java.util.HashMap

class ConnectionFragment : Fragment() {
    private var connection: Connection? = null
    private lateinit var tabHost: FragmentTabHost
    private var connectSwitch: SwitchCompat? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connections: HashMap<String, Connection> = getInstance(requireActivity()).connections
        connection = connections[requireArguments().getString(ActivityConstants.CONNECTION_KEY)]
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_connection, container, false)
        val bundle = Bundle()
        bundle.putString(ActivityConstants.CONNECTION_KEY, connection!!.handle())

        // Initialise the tab-host
        tabHost = rootView.findViewById(android.R.id.tabhost)
        tabHost.setup(requireActivity(), childFragmentManager, android.R.id.tabcontent)
        // Add a tab to the tabHost
        tabHost.addTab(tabHost.newTabSpec("History").setIndicator("History"), HistoryFragment::class.java, bundle)
        tabHost.addTab(tabHost.newTabSpec("Publish").setIndicator("Publish"), PublishFragment::class.java, bundle)
        tabHost.addTab(tabHost.newTabSpec("Subscribe").setIndicator("Subscribe"), SubscriptionFragment::class.java, bundle)
        return rootView
    }

    private fun changeConnectedState(state: Boolean) {
        tabHost.tabWidget.getChildTabViewAt(1).isEnabled = state
        tabHost.tabWidget.getChildTabViewAt(2).isEnabled = state
        connectSwitch?.isChecked = state
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_connection, menu)
        connectSwitch = menu.findItem(R.id.connect_switch).actionView.findViewById(R.id.switchForActionBar)
        connectSwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                connection?.let { (requireActivity() as MainActivity).connect(it) }
                changeConnectedState(true)
            } else {
                connection?.let { (requireActivity() as MainActivity).disconnect(it) }
                changeConnectedState(false)
            }
        }
        changeConnectedState(connection!!.isConnected)
        super.onCreateOptionsMenu(menu, inflater)
    }

}
