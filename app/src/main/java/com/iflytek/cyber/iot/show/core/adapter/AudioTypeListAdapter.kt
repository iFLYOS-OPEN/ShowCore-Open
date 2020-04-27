package com.iflytek.cyber.iot.show.core.adapter

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Result
import com.iflytek.cyber.iot.show.core.utils.HighLightUtils

class AudioTypeListAdapter(
    val onItemClickListener: (Result) -> Unit,
    val onMoreClickListener: (ArrayList<Result>) -> Unit
) :
    RecyclerView.Adapter<AudioTypeListAdapter.AudioTypeListHolder>() {

    private var items = ArrayList<Result>()
    private var keyword: String = ""

    fun setItems(items: ArrayList<Result>, keyword: String) {
        this.keyword = keyword
        this.items.clear()
        this.items.addAll(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioTypeListHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audio_result, parent, false)
        return AudioTypeListHolder(view)
    }

    override fun getItemCount(): Int {
        return this.items.size
    }

    private fun getKeyList(keyword: String): ArrayList<String> {
        val keys = ArrayList<String>()
        keyword.forEach {
            keys.add(it.toString())
        }
        return keys
    }

    override fun onBindViewHolder(holder: AudioTypeListHolder, position: Int) {
        val item = this.items[position]
        var titleRes = StringBuffer("")
        titleRes = HighLightUtils.highLightText(item.title, getKeyList(keyword), titleRes)
        holder.title.text = Html.fromHtml(titleRes.toString())
        if (item.author.isNullOrEmpty()) {
            holder.desc.isVisible = false
            holder.title.isVisible = false
            holder.onlyTitle.isVisible = true
            holder.onlyTitle.text = holder.title.text
        } else {
            holder.desc.isVisible = true
            holder.title.isVisible = true
            holder.onlyTitle.isVisible = false
            var descRes = StringBuffer("")
            descRes = HighLightUtils.highLightText(item.author, getKeyList(keyword), descRes)
            holder.desc.text = Html.fromHtml(descRes.toString())
        }
        //holder.moreContent.isVisible = position == items.size - 1
        if (position == items.size - 1) {
            holder.moreContent.isVisible = items.size >= 10
            holder.bottomLine.isVisible = holder.moreContent.isVisible
        } else {
            holder.bottomLine.isVisible = true
            holder.moreContent.isVisible = false
        }
        holder.moreContent.setOnClickListener {
            onMoreClickListener.invoke(items)
        }
        holder.item.setOnClickListener {
            onItemClickListener.invoke(item)
        }
    }

    inner class AudioTypeListHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.title)
        val desc = itemView.findViewById<TextView>(R.id.desc)
        val moreContent = itemView.findViewById<LinearLayout>(R.id.more_content)
        val item = itemView.findViewById<LinearLayout>(R.id.item)
        val onlyTitle = itemView.findViewById<TextView>(R.id.tv_only_title)
        val bottomLine = itemView.findViewById<View>(R.id.bottom_line)
    }
}