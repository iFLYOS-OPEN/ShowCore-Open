package com.iflytek.cyber.iot.show.core.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.GroupItem
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.widget.RatioImageView

class AlbumAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_SQUARE = 0
        const val TYPE_RECTANGLE = 1
    }

    private var groupList: List<GroupItem>? = null

    var itemViewType = TYPE_SQUARE

    internal var onGroupItemClickListener: OnGroupItemClickListener? = null

    fun setGroupList(groupList: List<GroupItem>?) {
        this.groupList = groupList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (itemViewType == TYPE_SQUARE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
            AlbumHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_album_video, parent, false)
            AlbumVideoHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return if (groupList != null) {
            groupList!!.size
        } else {
            0
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AlbumHolder) {
            val context = holder.itemView.context
            val groupItem = groupList!![position]
            val resource = holder.itemView.resources

            val transformer = MultiTransformation(
                    CenterCrop(),
                    RoundedCornersTransformation(
                            resource.getDimensionPixelSize(R.dimen.dp_6), 0)
            )

            Glide.with(context)
                    .load(groupItem.image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.with(
                            DrawableCrossFadeFactory.Builder()
                                    .setCrossFadeEnabled(true).build()))
                    .transform(transformer)
                    .into(holder.ivAlbum)
            holder.albumTitle.text = groupItem.name

            holder.albumContent.setOnClickListener {
                onGroupItemClickListener?.onGroupItemClick(it, groupItem)
            }
        } else if (holder is AlbumVideoHolder) {
            val context = holder.itemView.context
            val groupItem = groupList!![position]
            val resource = holder.itemView.resources

            val transformer = MultiTransformation(
                    CenterCrop(),
                    RoundedCornersTransformation(
                            resource.getDimensionPixelSize(R.dimen.dp_6), 0)
            )

            holder.ivAlbum.setOriginalSize(1000, 574)
            holder.albumContent.setOriginalSize(1000, 574)

            Glide.with(context)
                    .load(groupItem.image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .transition(DrawableTransitionOptions.with(
                            DrawableCrossFadeFactory.Builder()
                                    .setCrossFadeEnabled(true).build()))
                    .transform(transformer)
                    .into(holder.ivAlbum)
            holder.albumTitle.text = groupItem.name

            holder.albumContent.setOnClickListener {
                onGroupItemClickListener?.onGroupItemClick(it, groupItem)
            }
        }
    }

    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAlbum = itemView.findViewById<ImageView>(R.id.iv_album)
        val albumTitle = itemView.findViewById<TextView>(R.id.album_title)
        val albumContent = itemView.findViewById<FrameLayout>(R.id.album_content)
    }

    class AlbumVideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAlbum = itemView.findViewById<RatioImageView>(R.id.iv_album)
        val albumTitle = itemView.findViewById<TextView>(R.id.album_title)
        val albumContent = itemView.findViewById<RatioImageView>(R.id.album_content)
    }

    interface OnGroupItemClickListener {
        fun onGroupItemClick(itemView: View, groupItem: GroupItem)
    }
}
