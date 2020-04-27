package com.iflytek.cyber.iot.show.core.adapter

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Result
import com.iflytek.cyber.iot.show.core.utils.HighLightUtils
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.widget.RatioImageView

class VideoSearchResultAdapter(val onItemClick: (Result) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var results = ArrayList<Result>()
    private var isFinished = false
    private var showLoading = true
    private var keyword = ""

    fun setResults(keyword: String, results: List<Result>) {
        this.keyword = keyword
        this.results.clear()
        this.results.addAll(results)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.item_album_video -> AlbumHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_album_video, parent, false)
            )
            R.layout.item_loading -> LoadHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
            )
            else -> AlbumHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_album_video, parent, false)
            )
        }
    }

    fun showLoading(show: Boolean) {
        this.showLoading = show
    }

    fun loadingFinish(isFinished: Boolean) {
        this.isFinished = isFinished
        if (results.size > 0) {
            notifyItemChanged(results.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (showLoading) {
            if (position + 1 == itemCount) {
                R.layout.item_loading
            } else {
                R.layout.item_album_video
            }
        } else {
            R.layout.item_album_video
        }
    }

    override fun getItemCount(): Int {
        if (showLoading) {
            var itemCount = 1
            if (results.size > 0) {
                itemCount += results.size
            } else return 0
            return itemCount
        } else {
            return results.size
        }
    }

    private fun getKeyList(keyword: String): ArrayList<String> {
        val keys = ArrayList<String>()
        keyword.forEach {
            keys.add(it.toString())
        }
        return keys
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == R.layout.item_album_video) {
            holder as AlbumHolder
            val context = holder.itemView.context
            val item = results[position]
            val resource = holder.itemView.resources

            val transformer = MultiTransformation(
                CenterCrop(),
                RoundedCornersTransformation(
                    resource.getDimensionPixelSize(R.dimen.dp_6), 0
                )
            )

            holder.ivAlbum.setOriginalSize(1000, 574)
            holder.albumContent.setOriginalSize(1000, 574)

            Glide.with(context)
                .load(item.cover)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .transition(
                    DrawableTransitionOptions.with(
                        DrawableCrossFadeFactory.Builder()
                            .setCrossFadeEnabled(true).build()
                    )
                )
                .error(R.drawable.placeholder_music_album)
                .placeholder(R.drawable.placeholder_music_album)
                .transform(transformer)
                .into(holder.ivAlbum)

            if (keyword.isNotEmpty()) {
                var titleRes = StringBuffer("")
                titleRes = HighLightUtils.highLightText(item.title, getKeyList(keyword), titleRes)
                holder.albumTitle.text = Html.fromHtml(titleRes.toString())
            } else {
                holder.albumTitle.text = item.title
            }

            holder.albumContent.setOnClickListener {
                onItemClick.invoke(item)
            }
        } else if (getItemViewType(position) == R.layout.item_loading) {
            holder as LoadHolder
            if (isFinished) {
                holder.tvLoadingText.text = "已经到底了"
            } else {
                holder.tvLoadingText.text = "正在加载..."
            }
        }
    }

    inner class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAlbum = itemView.findViewById<RatioImageView>(R.id.iv_album)
        val albumTitle = itemView.findViewById<TextView>(R.id.album_title)
        val albumContent = itemView.findViewById<RatioImageView>(R.id.album_content)
    }

    inner class LoadHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLoadingText = itemView.findViewById<TextView>(R.id.tv_loading_text)
    }
}