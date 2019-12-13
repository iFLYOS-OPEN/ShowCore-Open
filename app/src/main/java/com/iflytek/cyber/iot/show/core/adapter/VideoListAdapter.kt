package com.iflytek.cyber.iot.show.core.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.Song
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation

class VideoListAdapter(
    private val videoList: ArrayList<Song>,
    private val onItemClickListener: (song: Song) -> Unit
) : RecyclerView.Adapter<VideoListAdapter.VideoListHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoListHolder(view)
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    override fun onBindViewHolder(holder: VideoListHolder, position: Int) {
        val song = videoList[position]
        holder.desc.text = song.metadata.title
        if (song.metadata.art != null && song.metadata.art?.sources?.isNullOrEmpty() == false) {
            holder.cover.isVisible = true
            holder.onlyDesc.isVisible = false
            val resource = holder.itemView.resources
            Glide.with(holder.cover)
                .load(song.metadata.art?.sources?.get(0)?.url)
                .apply(
                    RequestOptions()
                        .centerCrop()
                        .transform(
                            RoundedCornersTransformation(
                                resource.getDimensionPixelSize(R.dimen.dp_4), 0
                            )
                        )
                )
                .into(holder.cover)
        } else {
            holder.desc.isVisible = false
            holder.cover.isVisible = false
            holder.onlyDesc.isVisible = true
            holder.onlyDesc.text = song.metadata.title
        }
        val video = ContentStorage.get().video
        if (TextUtils.equals(video?.resourceId, song.stream.token)) {
            holder.desc.setTextColor(
                ContextCompat.getColor(
                    holder.desc.context,
                    R.color.setup_primary
                )
            )
        } else {
            holder.desc.setTextColor(
                ContextCompat.getColor(
                    holder.desc.context,
                    R.color.semi_black
                )
            )
        }
        holder.itemView.setOnClickListener {
            onItemClickListener.invoke(song)
        }
    }

    class VideoListHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cover = itemView.findViewById<ImageView>(R.id.iv_video_cover)
        val desc = itemView.findViewById<TextView>(R.id.tv_video_desc)
        val onlyDesc = itemView.findViewById<TextView>(R.id.tv_only_desc)
    }
}