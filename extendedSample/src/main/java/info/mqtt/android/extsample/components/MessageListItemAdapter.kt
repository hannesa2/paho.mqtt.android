package info.mqtt.android.extsample.components

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.model.ReceivedMessage
import java.text.SimpleDateFormat

class MessageListItemAdapter(context: Context, private val messages: List<ReceivedMessage>) :
    ArrayAdapter<ReceivedMessage>(context, R.layout.message_list_item, messages) {

    @SuppressLint("ViewHolder", "SimpleDateFormat")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.message_list_item, parent, false)
        val topicTextView = rowView.findViewById<TextView>(R.id.message_topic_text)
        val messageTextView = rowView.findViewById<TextView>(R.id.message_text)
        val dateTextView = rowView.findViewById<TextView>(R.id.message_date_text)
        messageTextView.text = String(messages[position].message.payload)
        topicTextView.text = context.getString(R.string.topic_fmt, messages[position].topic)
        val dateTimeFormatter = SimpleDateFormat("HH:mm:ss.sss")
        val shortDateStamp = dateTimeFormatter.format(messages[position].timestamp)
        dateTextView.text = shortDateStamp
        return rowView
    }
}
