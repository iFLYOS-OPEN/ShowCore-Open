package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Tags

class TagAdapter(val items: ArrayList<Tags>, val onItemClick: (Tags, Int) -> Unit) : RecyclerView.Adapter<TagAdapter.TagHolder>() {

    private var selectorColor = Color.parseColor("#262626")
    private var defaultColor = Color.parseColor("#9E9FA7")
    private var indicatorColor = Color.parseColor("#5280ff")

    private var currentPosition = 0

    fun setItemColor(selectColor: Int, defaultColor: Int, indicatorColor: Int) {
        this.selectorColor = selectColor
        this.defaultColor = defaultColor
        this.indicatorColor = indicatorColor
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        val holder = TagHolder(view)
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            updateIndicator(position)
            onItemClick.invoke(items[position], position)
        }
        return holder
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun updateIndicator(position: Int) {
        currentPosition = position
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: TagHolder, position: Int) {
        val item = items[position]
        holder.tagTextView.text = item.name
        holder.indicatorView.setColorFilter(indicatorColor, android.graphics.PorterDuff.Mode.SRC_IN)
        if (currentPosition == position) {
            holder.indicatorView.isInvisible = false
            holder.tagTextView.setTextColor(selectorColor)
        } else {
            holder.indicatorView.isInvisible = true
            holder.tagTextView.setTextColor(defaultColor)
        }
        /*holder.itemView.setOnClickListener {
            updateIndicator(position)
            onItemClick.invoke(items[position], position)
        }*/
    }

    inner class TagHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val indicatorView = itemView.findViewById<ImageView>(R.id.indicator_view)
        val tagTextView = itemView.findViewById<TextView>(R.id.name_text)
    }
}