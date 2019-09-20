package com.iflytek.cyber.iot.show.core.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.swipe.SwipeLayout
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Alert

class AlarmAdapter(private val onItemClickListener: (Alert) -> Unit,
                   private val onDeleteAlarmListener: (Alert, Int) -> Unit) : RecyclerView.Adapter<AlarmAdapter.AlarmHolder>() {

    val alerts = ArrayList<Alert>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
        return AlarmHolder(view)
    }

    override fun getItemCount(): Int {
        return alerts.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AlarmHolder, position: Int) {
        val alert = alerts[position]
        holder.alarm.text = alert.time
        val description = if (alert.content.isNullOrEmpty()) {
            alert.description
        } else {
            alert.description + ", " + alert.content
        }
        holder.alarmDesc.text = description
        holder.item.setOnClickListener {
            onItemClickListener.invoke(alert)
        }
        holder.delete.setOnClickListener {
            onDeleteAlarmListener.invoke(alert, position)
        }
        holder.swipeLayout.showMode = SwipeLayout.ShowMode.PullOut
    }

    class AlarmHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val alarm = itemView.findViewById<TextView>(R.id.tv_alarm)
        val alarmDesc = itemView.findViewById<TextView>(R.id.tv_alarm_desc)
        val item = itemView.findViewById<LinearLayout>(R.id.item)
        val delete = itemView.findViewById<ImageView>(R.id.delete)
        val swipeLayout = itemView.findViewById<SwipeLayout>(R.id.swipe_layout)
    }
}