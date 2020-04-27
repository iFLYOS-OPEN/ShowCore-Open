package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.MediaItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger

class TemplateAppMediaAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_1P0 = 0
        const val TYPE_1P6 = 1
        const val TYPE_0P75 = 2
    }

    private val mediaList = mutableListOf<MediaItem>()

    var onItemClickListener: OnItemClickListener? = null

    var ratio = 1f

    var isDark = false

    fun setMediaList(mediaList: List<MediaItem>) {
        clear()
        this.mediaList.addAll(mediaList)
    }

    fun appendMediaList(mediaList: List<MediaItem>) {
        this.mediaList.addAll(mediaList)
    }

    fun getMediaItem(position: Int) = if (position < mediaList.size) mediaList[position] else null

    fun clear() {
        mediaList.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = when (viewType) {
            TYPE_1P6 -> MediaItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_media_1p6,
                    parent,
                    false
                )
            )
            TYPE_1P0 -> MediaItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_media_1p0,
                    parent,
                    false
                )
            )
            else -> MediaItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_media_0p75,
                    parent,
                    false
                )
            )
        }
        holder.clickableView.clickWithTrigger {
            onItemClickListener?.onItemClick(parent, it, holder.adapterPosition)
        }
        return holder
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            ratio < 1 -> TYPE_0P75
            ratio == 1f -> TYPE_1P0
            else -> TYPE_1P6
        }
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MediaItemViewHolder) {
            holder.tvIndex.text = (position + 1).toString()

            val mediaItem = mediaList[position]

            if (!mediaItem.cover.isNullOrEmpty()) {
                try {
                    Uri.parse(mediaItem.cover)?.let { uri ->
                        Glide.with(holder.ivImage)
                            .load(uri)
                            .placeholder(R.drawable.bg_default_template_app_2)
                            .error(R.drawable.bg_default_template_app_2)
                            .transition(
                                DrawableTransitionOptions.with(
                                    DrawableCrossFadeFactory.Builder()
                                        .setCrossFadeEnabled(true).build()
                                )
                            )
                            .transform(
                                MultiTransformation(
                                    CenterCrop(), RoundedCornersTransformation(
                                        holder.itemView.resources.getDimensionPixelSize(
                                            R.dimen.dp_6
                                        ), 0
                                    )
                                )
                            )
                            .into(holder.ivImage)
                    } ?: run {
                        holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
                }
            } else {
                holder.ivImage.setImageResource(R.drawable.bg_default_template_app_2)
            }
            holder.tvFirstLine.text = mediaItem.title
            holder.tvSecondLine.text = mediaItem.subtitle
            holder.tvSecondLine.isVisible = !mediaItem.subtitle.isNullOrEmpty()

            if (isDark) {
                holder.tvFirstLine.setTextColor(Color.WHITE)
                holder.tvSecondLine.setTextColor(Color.WHITE)
            } else {
                holder.tvFirstLine.setTextColor(Color.BLACK)
                holder.tvSecondLine.setTextColor(Color.BLACK)
            }
        }
    }


    private class MediaItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clickableView: View = itemView.findViewById(R.id.clickable_view)
        val tvIndex: TextView = itemView.findViewById(R.id.tv_index)
        val ivImage: ImageView = itemView.findViewById(R.id.image)
        val tvFirstLine: TextView = itemView.findViewById(R.id.first_line)
        val tvSecondLine: TextView = itemView.findViewById(R.id.second_line)
    }

}