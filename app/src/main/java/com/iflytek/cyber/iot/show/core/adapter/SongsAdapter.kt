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
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.Song

class SongsAdapter(private val onItemClickListener: (song: Song) -> Unit)
    : RecyclerView.Adapter<SongsAdapter.SongHolder>() {

    var songList = ArrayList<Song>()

    private val playingTextColor = Color.parseColor("#1784e9")
    private val normalTextColor = Color.parseColor("#262626")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongHolder {
        return SongHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false))
    }

    override fun getItemCount(): Int {
        return songList.size
    }

    override fun onBindViewHolder(holder: SongHolder, position: Int) {
        val song = songList[position]
        holder.rank.text = (position + 1).toString()
        holder.title.text = song.metadata.title
        holder.artist.text = song.metadata.subtitle
        val playerInfo = ContentStorage.get().playerInfo
        val isPlaying = TextUtils.equals(playerInfo?.resourceId, song.stream.token)
        if (isPlaying) {
            holder.ivPlaying.visibility = View.VISIBLE
            holder.rank.visibility = View.GONE
            holder.title.setTextColor(playingTextColor)
            holder.onlyTitle.setTextColor(playingTextColor)
        } else {
            holder.ivPlaying.visibility = View.GONE
            holder.rank.visibility = View.VISIBLE
            holder.title.setTextColor(normalTextColor)
            holder.onlyTitle.setTextColor(normalTextColor)
        }
        holder.songContent.setOnClickListener {
            onItemClickListener.invoke(song)
        }
        holder.divider.isVisible = position != songList.size - 1

        if (song.metadata.subtitle.isNullOrEmpty()) {
            holder.textContent.isVisible = false
            holder.onlyTitle.isVisible = true
            holder.onlyTitle.text = song.metadata.title
        } else {
            holder.textContent.isVisible = true
            holder.onlyTitle.isVisible = false
        }
    }

    class SongHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songContent: FrameLayout = itemView.findViewById(R.id.song_content)
        var rank: TextView = itemView.findViewById(R.id.song_rank)
        var title: TextView = itemView.findViewById(R.id.song_title)
        var artist: TextView = itemView.findViewById(R.id.song_artist)
        val ivPlaying: ImageView = itemView.findViewById(R.id.iv_icon_playing)
        val divider = itemView.findViewById<View>(R.id.divider)
        val textContent = itemView.findViewById<LinearLayout>(R.id.text_content)
        val onlyTitle = itemView.findViewById<TextView>(R.id.tv_only_title)
    }
}