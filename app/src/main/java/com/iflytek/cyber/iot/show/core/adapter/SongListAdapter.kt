package com.iflytek.cyber.iot.show.core.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.Song
import com.iflytek.cyber.iot.show.core.model.SongItem

class SongListAdapter(private val onItemClickListener: (SongItem) -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var isFinished = false

    var items = ArrayList<SongItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_song -> SongHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_song_list, parent, false))
            R.layout.item_load_more -> LoadingHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_load_more, parent, false))
            else -> SongHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_song_list, parent, false))
        }
    }

    fun loadingFinish(isFinished: Boolean) {
        this.isFinished = isFinished
        if (items.size > 0) {
            notifyItemChanged(items.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position + 1 == itemCount) {
            R.layout.item_load_more
        } else {
            R.layout.item_song_list
        }
    }

    override fun getItemCount(): Int {
        var itemCount = 1
        if (items.size > 0) {
            itemCount += items.size
        } else return 0
        return itemCount
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == R.layout.item_song_list) {
            holder as SongHolder
            val item = items[position]
            holder.rank.text = (position + 1).toString()
            holder.title.text = item.name
            holder.artist.text = item.artist
            holder.artist.isVisible = !(item.artist.isNullOrEmpty())
            val currentPlayingMusic = ContentStorage.get().playerInfo
            val isPlaying = TextUtils.equals(currentPlayingMusic?.resourceId, item.id)
            if (isPlaying) {
                holder.ivPlaying.visibility = View.VISIBLE
                holder.rank.visibility = View.GONE
            } else {
                holder.ivPlaying.visibility = View.GONE
                holder.rank.visibility = View.VISIBLE
            }
            holder.songContent.setOnClickListener {
                if (item.available) {
                    onItemClickListener.invoke(item)
                }
            }
        } else if (getItemViewType(position) == R.layout.item_load_more) {
            holder as LoadingHolder
            holder.showLoading(isFinished)
        }
    }

    class SongHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songContent: FrameLayout = itemView.findViewById(R.id.song_content)
        var rank: TextView = itemView.findViewById(R.id.song_rank)
        var title: TextView = itemView.findViewById(R.id.song_title)
        var artist: TextView = itemView.findViewById(R.id.song_artist)
        val ivPlaying: ImageView = itemView.findViewById(R.id.iv_icon_playing)
    }

    class LoadingHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val progressBar = itemView.findViewById<ProgressBar>(R.id.progressBar)
        private val finishText: TextView = itemView.findViewById(R.id.finish_text)

        fun showLoading(isFinished: Boolean) {
            if (isFinished) {
                progressBar.visibility = View.GONE
                finishText.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.VISIBLE
                finishText.visibility = View.GONE
            }
        }
    }
}