package info.mqtt.android.extsample.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import info.mqtt.android.extsample.R
import info.mqtt.android.extsample.model.ReceivedMessage
import org.eclipse.paho.android.service.QoS
import java.text.SimpleDateFormat

class MessageListItemAdapter(context: Context, private val messages: List<ReceivedMessage>) :
    ArrayAdapter<ReceivedMessage>(context, R.layout.message_list_item, messages) {

    @SuppressLint("ViewHolder", "SimpleDateFormat", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.message_list_item, parent, false)
        val topicTextView = rowView.findViewById<TextView>(R.id.message_topic_text)
        val infoTextView = rowView.findViewById<TextView>(R.id.message_info)
        val idTextView = rowView.findViewById<TextView>(R.id.message_id)
        val messageTextView = rowView.findViewById<TextView>(R.id.message_text)
        val dateTextView = rowView.findViewById<TextView>(R.id.message_date_text)
        messageTextView.text = String(messages[position].message.payload)
        topicTextView.text = "${context.getString(R.string.topic_fmt)} ${messages[position].topic}"
        infoTextView.text = "qos=${QoS.valueOf(messages[position].message.qos)}(${messages[position].message.qos}) " +
                "isDuplicate=${messages[position].message.isDuplicate} retained=${messages[position].message.isRetained}"
        val dateTimeFormatter = SimpleDateFormat("HH:mm:ss.sss")
        val shortDateStamp = dateTimeFormatter.format(messages[position].timestamp)
        dateTextView.text = shortDateStamp
        idTextView.text = "ID=${messages[position].message.id}"

        infoTextView.visibility = View.VISIBLE
        idTextView.visibility = View.VISIBLE
        return rowView
    }
}
