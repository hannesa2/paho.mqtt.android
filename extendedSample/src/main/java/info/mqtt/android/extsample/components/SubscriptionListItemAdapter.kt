package info.mqtt.android.extsample.components

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ArrayAdapter
import info.mqtt.android.extsample.R
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import info.mqtt.android.extsample.model.Subscription
import java.util.ArrayList

class SubscriptionListItemAdapter(context: Context, private val topics: MutableList<Subscription>) :
    ArrayAdapter<Subscription?>(context, R.layout.subscription_list_item, topics.toList()) {

    private val unsubscribeListeners = ArrayList<OnUnsubscribeListener>()

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.subscription_list_item, parent, false)
        val topicTextView = rowView.findViewById<TextView>(R.id.message_text)
        val topicDeleteButton = rowView.findViewById<ImageView>(R.id.topic_delete_image)
        val qosTextView = rowView.findViewById<TextView>(R.id.qos_label)
        topicTextView.text = topics[position].topic
        val qosString = context.getString(R.string.qos_text, topics[position].qos)
        qosTextView.text = qosString
        val notifyTextView = rowView.findViewById<TextView>(R.id.show_notifications_label)
        val notifyString = context
            .getString(
                R.string.notify_text, if (topics[position].isEnableNotifications) context.getString(R.string.enabled) else context
                    .getString(R.string.disabled)
            )
        notifyTextView.text = notifyString
        topicDeleteButton.setOnClickListener {
            unsubscribeListeners.forEach {
                it.onUnsubscribe(topics[position])
            }
            topics.removeAt(position)
            notifyDataSetChanged()
        }
        return rowView
    }

    fun addOnUnsubscribeListener(listener: OnUnsubscribeListener) {
        unsubscribeListeners.add(listener)
    }

    interface OnUnsubscribeListener {
        fun onUnsubscribe(subscription: Subscription)
    }
}
