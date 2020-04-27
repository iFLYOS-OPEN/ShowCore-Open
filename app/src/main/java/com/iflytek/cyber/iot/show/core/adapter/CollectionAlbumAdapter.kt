package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.CollectionSong
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation


class CollectionAlbumAdapter(private val onItemClickListener: (song: CollectionSong) -> Unit) :
    RecyclerView.Adapter<CollectionAlbumAdapter.CollectionAlbumHolder>() {

    var items = ArrayList<CollectionSong>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionAlbumHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_collection_album, parent, false)
        return CollectionAlbumHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: CollectionAlbumHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.artist.text = item.artist

        val transformer: MultiTransformation<Bitmap> = MultiTransformation(
            CenterCrop(),
            RoundedCornersTransformation(
                holder.itemView.context.resources.getDimensionPixelSize(R.dimen.dp_6), 0
            )
        )

        Glide.with(holder.itemView.context)
            .load(item.musicImg)
            .transform(transformer)
            .into(holder.albumImage)

        holder.songContent.setOnClickListener {
            onItemClickListener.invoke(item)
        }
    }

    inner class CollectionAlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songContent: FrameLayout = itemView.findViewById(R.id.song_content)
        var title: TextView = itemView.findViewById(R.id.album_title)
        var artist: TextView = itemView.findViewById(R.id.album_desc)
        val divider = itemView.findViewById<View>(R.id.divider)
        val albumImage = itemView.findViewById<ImageView>(R.id.album_image)
    }
}