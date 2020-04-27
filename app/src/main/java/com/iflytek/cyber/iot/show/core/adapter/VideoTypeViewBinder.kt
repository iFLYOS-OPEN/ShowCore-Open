package com.iflytek.cyber.iot.show.core.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Result
import com.iflytek.cyber.iot.show.core.model.SearchResult

class VideoTypeViewBinder(
    val onItemClick: (Result) -> Unit,
    val onMoreClick: (ArrayList<Result>) -> Unit
) : ItemViewBinder<SearchResult, VideoTypeViewBinder.VideoTypeHolder>() {

    private var keyword = ""

    fun setKeyword(keyword: String) {
        this.keyword = keyword
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): VideoTypeHolder {
        val view = inflater.inflate(R.layout.item_category, parent, false)
        return VideoTypeHolder(view)
    }

    override fun onBindViewHolder(holder: VideoTypeHolder, item: SearchResult) {
        holder.title.text = "视频"
        holder.more.text = "查看更多"
        val newItems = if (item.results != null && item.results.size > 4) {
            item.results.subList(0, 4)
        } else {
            item.results
        }
        holder.moreContent.isVisible = newItems?.size ?: 0 >= 4
        holder.setList(newItems)
        holder.moreContent.setOnClickListener {
            if (item.results != null) {
                onMoreClick.invoke(item.results)
            }
        }
    }

    inner class VideoTypeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.category_title)
        val albumList = itemView.findViewById<RecyclerView>(R.id.album_list)
        val more = itemView.findViewById<TextView>(R.id.more)
        val moreContent = itemView.findViewById<LinearLayout>(R.id.more_content)

        val adapter = VideoSearchResultAdapter {
            onItemClick.invoke(it)
        }

        fun setList(results: List<Result>?) {
            if (results != null) {
                adapter.showLoading(false)
                adapter.setResults(keyword, results)
                albumList.adapter = adapter
            }
        }
    }
}