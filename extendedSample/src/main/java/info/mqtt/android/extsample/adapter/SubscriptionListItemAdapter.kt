package info.mqtt.android.extsample.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import info.mqtt.android.extsample.R
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import info.mqtt.android.extsample.internal.Connection
import info.mqtt.android.extsample.model.Subscription

class SubscriptionListItemAdapter(context: Context, private val connection: Connection) :
    ArrayAdapter<Subscription>(context, R.layout.subscription_list_item, connection.getSubscriptions().toMutableList()) {

    fun refresh() {
        this.clear()
        this.addAll(connection.getSubscriptions().toMutableList())
        super.notifyDataSetChanged()
    }

    @SuppressLint("ViewHolder", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.subscription_list_item, parent, false)
        val topicTextView = rowView.findViewById<TextView>(R.id.message_text)
        val topicDeleteButton = rowView.findViewById<ImageView>(R.id.topic_delete_image)
        val qosTextView = rowView.findViewById<TextView>(R.id.qos_label)
        topicTextView.text = connection.getSubscriptions()[position].topic
        qosTextView.text = "Qos: ${connection.getSubscriptions()[position].qos}(${connection.getSubscriptions()[position].qos.value})"
        val notifyTextView = rowView.findViewById<TextView>(R.id.show_notifications_label)
        val notifyString = context.getString(R.string.notify_text, connection.getSubscriptions()[position].isEnableNotifications.toString())
        notifyTextView.text = notifyString
        topicDeleteButton.setOnClickListener {
            connection.unsubscribe(connection.getSubscriptions()[position])
            refresh()
        }
        return rowView
    }

}
