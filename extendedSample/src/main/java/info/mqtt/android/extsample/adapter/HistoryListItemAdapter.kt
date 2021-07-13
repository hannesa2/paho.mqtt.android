package info.mqtt.android.extsample.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import info.mqtt.android.extsample.R

class HistoryListItemAdapter(context: Context, private val history: List<String>) :
    ArrayAdapter<String>(context, R.layout.message_list_item, history) {

    @SuppressLint("ViewHolder", "SimpleDateFormat")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.message_list_item, parent, false)
        val topicTextView = rowView.findViewById<TextView>(R.id.message_topic_text)
        val messageTextView = rowView.findViewById<TextView>(R.id.message_text)
        val dateTextView = rowView.findViewById<TextView>(R.id.message_date_text)
        messageTextView.text = history[position]
        topicTextView.text = history[position]
        val shortDateStamp = history[position].takeLast(12)
        dateTextView.text = shortDateStamp
        return rowView
    }
}
