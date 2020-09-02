package com.iflytek.cyber.iot.show.core.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.LatestRecord
import com.iflytek.cyber.iot.show.core.widget.CircleCheckBox
import com.makeramen.roundedimageview.RoundedImageView

class LatestListAdapter(
    val onItemClick: (LatestRecord, Int) -> Unit,
    val onMoreClick: (View, LatestRecord) -> Unit,
    val onItemSelected: (Int) -> Unit
) : RecyclerView.Adapter<LatestListAdapter.LatestListHolder>() {

    private val playingTextColor = Color.parseColor("#1784e9")
    private val normalTextColor = Color.parseColor("#262626")

    private val items = ArrayList<LatestRecord>()
    private val selectIds = ArrayList<Long>()

    private var showCover = false
    private var isSelected = false
    private var isSelectAll = false

    fun setItems(items: ArrayList<LatestRecord>, clear: Boolean = true) {
        if (clear) {
            this.items.clear()
        }
        this.items.addAll(items)
    }

    fun getItems(): ArrayList<LatestRecord> {
        return items
    }

    fun getItem(position: Int): LatestRecord? {
        return if (items.size == 0) {
            null
        } else {
            items[position]
        }
    }

    fun isShowCover(showCover: Boolean) {
        this.showCover = showCover
    }

    fun getSelectIds(): ArrayList<Long> {
        return selectIds
    }

    fun selectAll() {
        if (isSelectAll) {
            selectIds.clear()
            items.forEach {
                it.isSelected = false
            }
            isSelectAll = false
        } else {
            items.forEach {
                it.isSelected = true
                if (!selectIds.contains(it.id)) {
                    selectIds.add(it.id)
                }
            }
            isSelectAll = true
        }
        notifyDataSetChanged()
    }

    fun isSelectAll(): Boolean {
        return isSelectAll
    }

    fun isSelected(): Boolean {
        return isSelected
    }

    fun switchSelectedState() {
        isSelected = !isSelected
        if (!isSelected) {
            items.forEach {
                it.isSelected = false
            }
            selectIds.clear()
        }
        notifyDataSetChanged()
    }

    fun updateItems(ids: ArrayList<Long>) {
        val removeList = arrayListOf<LatestRecord>()
        items.forEach {
            if (ids.contains(it.id)) {
                removeList.add(it)
            }
        }
        items.removeAll(removeList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LatestListHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_latest_media, parent, false)
        val holder = LatestListHolder(view)
        holder.itemView.setOnClickListener {
            val item = items[holder.adapterPosition]
            if (isSelected) {
                if (holder.selector.isChecked) {
                    selectIds.remove(item.id)
                    item.isSelected = false
                    holder.selector.isChecked = false
                } else {
                    selectIds.add(item.id)
                    item.isSelected = true
                    holder.selector.isChecked = true
                }
                onItemSelected.invoke(selectIds.size)
                isSelectAll = selectIds.size == itemCount
            } else {
                onItemClick.invoke(item, holder.adapterPosition)
            }
        }
        holder.moreImageView.setOnClickListener {
            val item = items[holder.adapterPosition]
            onMoreClick.invoke(it, item)
        }
        return holder
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: LatestListHolder, position: Int) {
        val item = items[position]
        holder.index.text = (position + 1).toString()
        holder.titleTextView.text = item.musicName
        holder.artistTextView.text = item.musicArtist
        //holder.countTextView.text = "${item.count}次"
        if (showCover) {
            holder.picView.isVisible = true
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .into(holder.coverImageView)
        } else {
            holder.picView.isVisible = false
        }
        setupItemPlaying(holder, item)
        holder.selector.isChecked = item.isSelected
    }

    private fun setupItemPlaying(holder: LatestListHolder, item: LatestRecord) {
        val playerInfo = ContentStorage.get().playerInfo
        val isPlaying = TextUtils.equals(playerInfo?.resourceId, item.musicId)
        if (isSelected) {
            holder.index.isVisible = false
            holder.selector.isVisible = true
            holder.moreImageView.isVisible = false
            //holder.countTextView.isVisible = false
            holder.playingView.isVisible = false
        } else {
            //holder.index.isVisible = true
            holder.selector.isVisible = false
            holder.moreImageView.isVisible = true
            //holder.countTextView.isVisible = true
            if (isPlaying) {
                holder.playingView.visibility = View.VISIBLE
                holder.index.visibility = View.GONE
                holder.titleTextView.setTextColor(playingTextColor)
                if (ContentStorage.get().isMusicPlaying) {
                    holder.playingView.pauseAnimation()
                    holder.playingView.playAnimation()
                } else {
                    holder.playingView.pauseAnimation()
                }
            } else {
                holder.playingView.visibility = View.GONE
                holder.playingView.pauseAnimation()
                holder.index.visibility = View.VISIBLE
                holder.titleTextView.setTextColor(normalTextColor)
            }
        }
    }

    override fun onViewAttachedToWindow(holder: LatestListHolder) {
        super.onViewAttachedToWindow(holder)
        val item = items[holder.adapterPosition]
        setupItemPlaying(holder, item)
    }

    inner class LatestListHolder(itemVView: View) : RecyclerView.ViewHolder(itemVView) {
        val index = itemView.findViewById<TextView>(R.id.song_rank)
        val selector = itemView.findViewById<CircleCheckBox>(R.id.song_selected)
        val playingView = itemView.findViewById<LottieAnimationView>(R.id.iv_icon_playing)
        val coverImageView = itemView.findViewById<RoundedImageView>(R.id.iv_cover)
        val titleTextView = itemView.findViewById<TextView>(R.id.song_title)
        val artistTextView = itemView.findViewById<TextView>(R.id.song_artist)
        //val countTextView = itemView.findViewById<TextView>(R.id.tv_play_count)
        val moreImageView = itemView.findViewById<ImageView>(R.id.more)
        val picView = itemView.findViewById<FrameLayout>(R.id.pic_content)

        init {
            selector.isClickable = false
        }
    }
}