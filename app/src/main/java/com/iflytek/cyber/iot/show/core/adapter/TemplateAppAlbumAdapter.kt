package com.iflytek.cyber.iot.show.core.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.AlbumItem
import com.iflytek.cyber.iot.show.core.model.MediaItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger
import kotlin.math.min

class TemplateAppAlbumAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_1P0 = 0
        const val TYPE_1P6 = 1
        const val TYPE_0P75 = 2
    }

    private val albumList = mutableListOf<AlbumItem>()

    var onAlbumMoreClickListener: OnAlbumMoreClickListener? = null

    var onSubItemClickListener: OnSubItemClickListener? = null

    var ratio = 1f

    var isDark = false

    fun setAlbumList(albumList: List<AlbumItem>) {
        this.albumList.clear()
        this.albumList.addAll(albumList)
    }

    fun appendAlbumList(albumList: List<AlbumItem>) {
        this.albumList.addAll(albumList)
    }

    fun getAlbumItem(position: Int) = if (position < albumList.size) albumList[position] else null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = when (viewType) {
            TYPE_1P6 -> AlbumItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_album_1p6,
                    parent,
                    false
                )
            )
            TYPE_1P0 -> AlbumItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_album_1p0,
                    parent,
                    false
                )
            )
            else -> AlbumItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_template_app_album_0p75,
                    parent,
                    false
                )
            )
        }
        holder.childList.mapIndexed { index, mediaGroup ->
            mediaGroup.ivAlbumMedia?.clickWithTrigger {
                onSubItemClickListener?.onSubItemClick(
                    parent,
                    holder.itemView,
                    holder.adapterPosition,
                    index
                )
            }
        }
        holder.seeMore?.clickWithTrigger {
            onAlbumMoreClickListener?.onAlbumMoreClick(
                it,
                albumList[holder.adapterPosition],
                holder.adapterPosition
            )
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
        return albumList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AlbumItemViewHolder) {
            val childSize = holder.fullChildSize()
            var indexSize = 0
            for (i in 0 until position) {
                indexSize += min(childSize, albumList[i].result?.size ?: 0)
            }
            holder.setupAlbum(albumList[position], isDark, indexSize)
        }
    }

    private class AlbumItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAlbumTitle: TextView? = itemView.findViewById(R.id.album_title)
        val seeMore: View? = itemView.findViewById(R.id.album_more)
        val tvMore: TextView? = itemView.findViewById(R.id.tv_more)
        val ivMore: ImageView? = itemView.findViewById(R.id.iv_more)

        val childList = listOf(
            MediaGroup(
                itemView,
                R.id.album_media_0,
                R.id.title_media_0,
                R.id.subtitle_media_0,
                R.id.index_media_0
            ),
            MediaGroup(
                itemView,
                R.id.album_media_1,
                R.id.title_media_1,
                R.id.subtitle_media_1,
                R.id.index_media_1
            ),
            MediaGroup(
                itemView,
                R.id.album_media_2,
                R.id.title_media_2,
                R.id.subtitle_media_2,
                R.id.index_media_2
            ),
            MediaGroup(
                itemView,
                R.id.album_media_3,
                R.id.title_media_3,
                R.id.subtitle_media_3,
                R.id.index_media_3
            ),
            MediaGroup(
                itemView,
                R.id.album_media_4,
                R.id.title_media_4,
                R.id.subtitle_media_4,
                R.id.index_media_4
            )
        )

        fun setupAlbum(albumItem: AlbumItem, isDark: Boolean, indexFirst: Int) {
            val childSize = fullChildSize()

            tvAlbumTitle?.text = albumItem.album
            seeMore?.isVisible =
                !albumItem.result.isNullOrEmpty() && albumItem.result.size > childSize

            if (isDark) {
                seeMore?.setBackgroundResource(R.drawable.bg_round_border_50white_28dp)
                tvAlbumTitle?.setTextColor(Color.WHITE)
                tvMore?.setTextColor(Color.WHITE)
                ivMore?.imageTintList = ColorStateList.valueOf(Color.WHITE)
            } else {
                seeMore?.setBackgroundResource(R.drawable.bg_round_border_grey_28dp)
                tvAlbumTitle?.setTextColor(Color.BLACK)
                tvMore?.setTextColor(Color.BLACK)
                ivMore?.imageTintList = ColorStateList.valueOf(Color.BLACK)
            }

            for (i in 0 until childSize) {
                if (i < albumItem.result?.size ?: 0) {
                    albumItem.result?.get(i)?.let { mediaItem ->
                        childList[i].setupMediaItem(
                            mediaItem,
                            isDark,
                            (indexFirst + i + 1).toString()
                        )
                        childList[i].setVisible(true)
                    } ?: run {
                        childList[i].setVisible(false)
                    }
                } else {
                    childList[i].setVisible(false)
                }
            }
        }

        fun fullChildSize() = if (childList[3].exists()) 5 else 3
    }

    private class MediaGroup(
        itemView: View,
        albumId: Int,
        titleId: Int,
        subtitleId: Int,
        indexId: Int
    ) {
        val ivAlbumMedia: ImageView? = itemView.findViewById(albumId)
        val tvAlbumTitle: TextView? = itemView.findViewById(titleId)
        val tvAlbumSubtitle: TextView? = itemView.findViewById(subtitleId)
        val tvAlbumIndex: TextView? = itemView.findViewById(indexId)

        fun exists() = ivAlbumMedia != null

        fun setVisible(isVisible: Boolean) {
            ivAlbumMedia?.isVisible = isVisible
            tvAlbumTitle?.isVisible = isVisible
            tvAlbumSubtitle?.isVisible = isVisible
            tvAlbumIndex?.isVisible = isVisible
        }

        fun setupMediaItem(mediaItem: MediaItem, isDark: Boolean, indexValue: String) {
            tvAlbumTitle?.text = mediaItem.title
            tvAlbumSubtitle?.text = mediaItem.subtitle
            if (mediaItem.subtitle.isNullOrEmpty()) {
                tvAlbumSubtitle?.height = 0
            } else {
                if (tvAlbumSubtitle?.height == 0) {
                    tvAlbumSubtitle.layoutParams = ConstraintLayout.LayoutParams(
                        0,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        startToStart = tvAlbumTitle?.id ?: 0
                        topToBottom = tvAlbumTitle?.id ?: 0
                        endToEnd = tvAlbumTitle?.id ?: 0
                    }
                }
            }
            tvAlbumIndex?.text = indexValue

            if (isDark) {
                tvAlbumTitle?.setTextColor(Color.WHITE)
                tvAlbumSubtitle?.setTextColor(Color.WHITE)
            } else {
                tvAlbumTitle?.setTextColor(Color.BLACK)
                tvAlbumSubtitle?.setTextColor(Color.BLACK)
            }

            ivAlbumMedia?.let { imageView ->
                if (!mediaItem.cover.isNullOrEmpty()) {
                    try {
                        Uri.parse(mediaItem.cover)?.let { uri ->
                            Glide.with(imageView)
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
                                            imageView.resources.getDimensionPixelSize(
                                                R.dimen.dp_6
                                            ), 0
                                        )
                                    )
                                )
                                .into(imageView)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        imageView.setImageResource(R.drawable.bg_default_template_app_2)
                    }
                } else {
                    imageView.setImageResource(R.drawable.bg_default_template_app_2)
                }
            }
        }
    }

    interface OnAlbumMoreClickListener {
        fun onAlbumMoreClick(view: View, albumItem: AlbumItem, position: Int)
    }

    interface OnSubItemClickListener {
        fun onSubItemClick(parent: ViewGroup, view: View, position: Int, subPosition: Int)
    }
}