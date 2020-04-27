package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Color
import android.net.Uri
import android.util.Log
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
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.AlbumItem
import com.iflytek.cyber.iot.show.core.model.SourceItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger

class AlbumSectionContentAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_1P0 = 0
        const val TYPE_1P6 = 1
        const val TYPE_0P75 = 2
    }

    private val albumList = mutableListOf<SourceItem>()

    var onItemClickListener: OnItemClickListener? = null

    var isDark = false

    var ratio = 1f

    fun setAlbumList(albumList: List<SourceItem>) {
        this.albumList.clear()
        this.albumList.addAll(albumList)
    }

    fun appendAlbumList(albumList: List<SourceItem>) {
        this.albumList.addAll(albumList)
    }

    fun getAlbumItem(position: Int) = if (position < albumList.size) albumList[position] else null

    override fun getItemViewType(position: Int): Int {
        return when {
            ratio < 1 -> TYPE_0P75
            ratio == 1f -> TYPE_1P0
            else -> TYPE_1P6
        }
    }

    override fun getItemCount(): Int {
        return albumList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = when (viewType) {
            TYPE_1P6 -> SectionContentHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_single_album_1p6,
                    parent,
                    false
                )
            )
            TYPE_1P0 -> SectionContentHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_single_album_1p0,
                    parent,
                    false
                )
            )
            else -> SectionContentHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_single_album_0p75,
                    parent,
                    false
                )
            )
        }
        holder.itemView.clickWithTrigger {
            onItemClickListener?.onItemClick(parent, it, holder.adapterPosition)
        }
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SectionContentHolder) {
            val sourceItem = albumList[position]

            if (isDark) {
                holder.titleTextView.setTextColor(Color.WHITE)
                holder.subTitleTextView.setTextColor(Color.WHITE)
            } else {
                holder.titleTextView.setTextColor(Color.BLACK)
                holder.subTitleTextView.setTextColor(Color.BLACK)
            }

            holder.indexTextView.text = (position + 1).toString()
            holder.titleTextView.text = sourceItem.title
            //holder.subTitleTextView.text = albumItem.subtitle
            holder.subTitleTextView.isVisible = false

            if (!sourceItem.cover.isNullOrEmpty()) {
                try {
                    Glide.with(holder.albumImageView)
                        .load(sourceItem.cover)
                        .placeholder(R.drawable.bg_default_template_app_2)
                        .error(R.drawable.bg_default_template_app_2)
                        .into(holder.albumImageView)
                } catch (t: Throwable) {
                    t.printStackTrace()
                    holder.albumImageView.setImageResource(R.drawable.bg_default_template_app_2)
                }
            } else {
                holder.albumImageView.setImageResource(R.drawable.bg_default_template_app_2)
            }
        }
    }
}

private class SectionContentHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val albumImageView = itemView.findViewById<com.makeramen.roundedimageview.RoundedImageView>(R.id.album_media_0)
    val indexTextView = itemView.findViewById<TextView>(R.id.index_media_0)
    val titleTextView = itemView.findViewById<TextView>(R.id.title_media_0)
    val subTitleTextView = itemView.findViewById<TextView>(R.id.subtitle_media_0)
}