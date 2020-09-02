package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.CollectionSong
import com.iflytek.cyber.iot.show.core.model.ContentStorage

class CollectionSingleAdapter(private val onItemClickListener: (song: CollectionSong) -> Unit) :
    RecyclerView.Adapter<CollectionSingleAdapter.CollectionSingleHolder>() {

    var items = ArrayList<CollectionSong>()

    private val playingTextColor = Color.parseColor("#1784e9")
    private val normalTextColor = Color.parseColor("#262626")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionSingleHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collection_single, parent, false)
        return CollectionSingleHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: CollectionSingleHolder, position: Int) {
        val song = items[position]
        holder.rank.text = (position + 1).toString()
        holder.title.text = song.name
        holder.artist.text = song.artist

        setupItemPlaying(holder, song)

        holder.songContent.setOnClickListener {
            onItemClickListener.invoke(song)
        }
        holder.divider.isVisible = position != items.size - 1

        if (song.artist.isNullOrEmpty()) {
            holder.textContent.isVisible = false
            holder.onlyTitle.isVisible = true
            holder.onlyTitle.text = song.name
        } else {
            holder.textContent.isVisible = true
            holder.onlyTitle.isVisible = false
        }
    }

    private fun setupItemPlaying(holder: CollectionSingleHolder, song: CollectionSong) {
        val playerInfo = ContentStorage.get().playerInfo
        val isPlaying = TextUtils.equals(playerInfo?.resourceId, song.id)
        if (isPlaying) {
            holder.ivPlaying.visibility = View.VISIBLE
            holder.rank.visibility = View.GONE
            holder.title.setTextColor(playingTextColor)
            holder.onlyTitle.setTextColor(playingTextColor)
            if (ContentStorage.get().isMusicPlaying) {
                holder.ivPlaying.pauseAnimation()
                holder.ivPlaying.playAnimation()
            } else {
                holder.ivPlaying.pauseAnimation()
            }
        } else {
            holder.ivPlaying.visibility = View.GONE
            holder.ivPlaying.pauseAnimation()
            holder.rank.visibility = View.VISIBLE
            holder.title.setTextColor(normalTextColor)
            holder.onlyTitle.setTextColor(normalTextColor)
        }
    }

    override fun onViewAttachedToWindow(holder: CollectionSingleHolder) {
        super.onViewAttachedToWindow(holder)
        val song = items[holder.adapterPosition]
        setupItemPlaying(holder, song)
    }

    inner class CollectionSingleHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songContent: FrameLayout = itemView.findViewById(R.id.song_content)
        var rank: TextView = itemView.findViewById(R.id.song_rank)
        var title: TextView = itemView.findViewById(R.id.song_title)
        var artist: TextView = itemView.findViewById(R.id.song_artist)
        val ivPlaying: LottieAnimationView = itemView.findViewById(R.id.iv_icon_playing)
        val divider = itemView.findViewById<View>(R.id.divider)
        val textContent = itemView.findViewById<LinearLayout>(R.id.text_content)
        val onlyTitle = itemView.findViewById<TextView>(R.id.tv_only_title)
    }
}