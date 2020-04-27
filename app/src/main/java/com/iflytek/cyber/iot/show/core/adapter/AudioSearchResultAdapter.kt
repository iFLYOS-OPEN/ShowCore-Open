package com.iflytek.cyber.iot.show.core.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Result

class AudioSearchResultAdapter(
    val results: ArrayList<Result>,
    val onItemClick: (Result) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isFinished = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_audio_search_result -> AudioSearchResultHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_audio_search_result, parent, false)
            )
            R.layout.item_loading -> LoadHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
            )
            else -> AudioSearchResultHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_audio_search_result, parent, false)
            )
        }
    }

    fun loadingFinish(isFinished: Boolean) {
        this.isFinished = isFinished
        if (results.size > 0) {
            notifyItemChanged(results.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position + 1 == itemCount) {
            R.layout.item_loading
        } else {
            R.layout.item_audio_search_result
        }
    }

    override fun getItemCount(): Int {
        var itemCount = 1
        if (results.size > 0) {
            itemCount += results.size
        } else return 0
        return itemCount
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == R.layout.item_audio_search_result) {
            holder as AudioSearchResultHolder
            val result = results[position]
            holder.title.text = result.title
            holder.desc.text = result.author
            holder.itemView.setOnClickListener {
                onItemClick.invoke(result)
            }
            if (result.author.isNullOrEmpty()) {
                holder.title.isVisible = false
                holder.desc.isVisible = false
                holder.onlyTitle.isVisible = true
                holder.onlyTitle.text = result.title
            } else {
                holder.title.isVisible = true
                holder.desc.isVisible = true
                holder.onlyTitle.isVisible = false
            }
        } else if (getItemViewType(position) == R.layout.item_loading) {
            holder as LoadHolder
            if (isFinished) {
                holder.tvLoadingText.text = "已经到底了"
            } else {
                holder.tvLoadingText.text = "正在加载..."
            }
        }
    }

    inner class AudioSearchResultHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.title)
        val desc = itemView.findViewById<TextView>(R.id.desc)
        val onlyTitle = itemView.findViewById<TextView>(R.id.tv_only_title)
    }

    inner class LoadHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLoadingText = itemView.findViewById<TextView>(R.id.tv_loading_text)
    }
}