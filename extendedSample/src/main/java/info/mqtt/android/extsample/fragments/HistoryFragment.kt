package info.mqtt.android.extsample.fragments

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
import info.mqtt.android.extsample.adapter.HistoryListItemAdapter
import info.mqtt.android.extsample.internal.Connection
import timber.log.Timber

class HistoryFragment : Fragment() {

    private var historyListItemAdapter: HistoryListItemAdapter? = null
    private lateinit var connection: Connection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val connections = Connections.getInstance(requireActivity()).connections
        connection = connections[requireArguments().getString(ActivityConstants.CONNECTION_KEY)]!!
        setHasOptionsMenu(true)
        Timber.d("CONNECTION_KEY=${requireArguments().getString(ActivityConstants.CONNECTION_KEY)} '${connection.id}'")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_connection_history, container, false)
        historyListItemAdapter = HistoryListItemAdapter(requireContext(), connection.history)
        val historyListView = rootView.findViewById<ListView>(R.id.history_list_view)
        historyListView.adapter = historyListItemAdapter
        val clearButton = rootView.findViewById<Button>(R.id.history_clear_button)
        clearButton.setOnClickListener {
            connection.history.clear()
            historyListItemAdapter!!.notifyDataSetChanged()
        }

        return rootView
    }

}