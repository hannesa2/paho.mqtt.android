package info.mqtt.android.extsample.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.mqtt.android.extsample.R
import info.mqtt.android.service.room.entity.PingEntity
import java.text.SimpleDateFormat
import java.util.Date

class PingListAdapter internal constructor(context: Context) : RecyclerView.Adapter<PingListAdapter.WordViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var pingEntities = emptyList<PingEntity>()

    inner class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stateItemView: TextView = itemView.findViewById(R.id.textState)
        val messageItemView: TextView = itemView.findViewById(R.id.textView)
        val timeView: TextView = itemView.findViewById(R.id.textTime)
        val idView: TextView = itemView.findViewById(R.id.textId)
        val uriView: TextView = itemView.findViewById(R.id.textUri)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_ping, parent, false)
        return WordViewHolder(itemView)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val current = pingEntities[position]
        holder.messageItemView.text = current.message
        holder.stateItemView.text = if (current.success) "ok" else "failure"
        holder.timeView.text = SimpleDateFormat("HH:mm.ss.SSS").format(Date(current.timestamp))
        holder.uriView.text = current.serverURI
        holder.idView.text = current.clientId
    }

    internal fun setWords(pingEntityList: List<PingEntity>) {
        this.pingEntities = pingEntityList
        notifyDataSetChanged()
    }

    override fun getItemCount() = pingEntities.size
}
