package com.iflytek.cyber.iot.show.core.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.drakeet.multitype.ItemViewBinder
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Result
import com.iflytek.cyber.iot.show.core.model.SearchResult


class AudioTypeViewBinder(
    val onItemClick: (Result) -> Unit,
    val onMoreClick: (ArrayList<Result>) -> Unit
) :
    ItemViewBinder<SearchResult, AudioTypeViewBinder.AudioTypeHolder>() {

    private var keyword: String = ""

    fun setKeyword(keyword: String) {
        this.keyword = keyword
    }

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): AudioTypeHolder {
        val view = inflater.inflate(R.layout.item_search_audio, parent, false)
        return AudioTypeHolder(view)
    }

    override fun onBindViewHolder(holder: AudioTypeHolder, item: SearchResult) {
        if (item.results != null) {
            holder.setupList(item.results)
        }
    }

    inner class AudioTypeHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val audioList = itemView.findViewById<RecyclerView>(R.id.audio_list)

        val adapter = AudioTypeListAdapter({
            onItemClick.invoke(it)
        }, {
            onMoreClick.invoke(it)
        })

        fun setupList(results: ArrayList<Result>) {
            adapter.setItems(results, keyword)
            audioList.adapter = adapter
        }
    }
}