package info.mqtt.android.extsample.activity

import android.annotation.SuppressLint
import info.mqtt.android.extsample.internal.Connections.Companion.getInstance
import android.os.Bundle
import android.view.*
import info.mqtt.android.extsample.R
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTabHost
import java.util.HashMap
import android.widget.TextView

import android.view.LayoutInflater
import info.mqtt.android.extsample.utils.connect


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
        tabHost = rootView.findViewById(R.id.tabhost)
        tabHost.setup(requireActivity(), childFragmentManager, R.id.tabcontent)
        // Add a tab to the tabHost
        tabHost.addTab(
            tabHost.newTabSpec("Messages").setIndicator(getTabIndicator("Messages", R.id.tab_id_message)),
            MessagesFragment::class.java,
            bundle
        )
        tabHost.addTab(
            tabHost.newTabSpec("Publish").setIndicator(getTabIndicator("Publish", R.id.tab_id_publish)),
            PublishFragment::class.java,
            bundle
        )
        tabHost.addTab(
            tabHost.newTabSpec("Subscribe").setIndicator(getTabIndicator("Subscribe", R.id.tab_id_subscribe)), SubscriptionFragment::class.java,
            bundle
        )
        return rootView
    }

    @SuppressLint("InflateParams")
    private fun getTabIndicator(title: String, viewId: Int): View {
        val view: View = LayoutInflater.from(context).inflate(R.layout.tab_layout, null)
        val tv = view.findViewById(R.id.text_view) as TextView
        tv.text = title
        tv.id = viewId
        tv.tag = title
        return view
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
                connection?.connect(requireActivity())
                changeConnectedState(true)
            } else {
                connection?.client?.disconnect()
                changeConnectedState(false)
            }
        }
        changeConnectedState(connection!!.isConnected)
        super.onCreateOptionsMenu(menu, inflater)
    }

}
