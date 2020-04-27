package com.iflytek.cyber.iot.show.core.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.MediaEntity

class RecommendMediaAdapter : RecyclerView.Adapter<MediaViewHolder>() {
    enum class ColsType {
        THREE_COLS, FIVE_COLS
    }

    enum class MediaType {
        AUDIO, VIDEO
    }

    interface OnItemClickListener {
        fun onItemClicked(view: View, position: Int)
    }

    private var colsType = ColsType.THREE_COLS
    private var mediaType = MediaType.AUDIO
    private var itemList: List<MediaEntity>? = null

    private var onItemClickListener: OnItemClickListener? = null

    fun setColsType(type: ColsType) {
        colsType = type
    }

    fun setMediaType(type: MediaType) {
        mediaType = type
    }

    fun setItems(items: List<MediaEntity>) {
        itemList = items
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    fun getItem(position: Int): MediaEntity? {
        return if (itemList == null) null else itemList!![position]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return when (mediaType) {
            MediaType.AUDIO -> {
                if (colsType == ColsType.THREE_COLS) {
                    MediaViewHolder(LayoutInflater.from(parent.context).inflate(
                            R.layout.item_recommend_music_three_cols, null))
                } else {
                    MediaViewHolder(LayoutInflater.from(parent.context).inflate(
                            R.layout.item_recommend_music_five_cols, null))
                }
            }
            else -> {
                MediaViewHolder(LayoutInflater.from(parent.context).inflate(
                            R.layout.item_recommend_video, null))
            }
        }
    }

    override fun getItemCount(): Int {
        return if (itemList == null) 0 else itemList!!.size
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.setItem(getItem(position)!!, position)
        holder.setOnClickListener(View.OnClickListener {
            onItemClickListener?.onItemClicked(it, position)
        })
    }
}