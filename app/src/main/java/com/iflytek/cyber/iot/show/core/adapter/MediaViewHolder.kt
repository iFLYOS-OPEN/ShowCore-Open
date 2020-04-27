package com.iflytek.cyber.iot.show.core.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.MediaEntity
import com.makeramen.roundedimageview.RoundedImageView

class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private var image: RoundedImageView? = itemView.findViewById(R.id.img_cover)
    private var order: TextView? = itemView.findViewById(R.id.txt_order)
    private var name: TextView? = itemView.findViewById(R.id.txt_name)
    private var posInt: Int? = null

    fun setItem(item: MediaEntity, pos: Int) {
        posInt = pos
        order?.text = (pos + 1).toString()
        name?.text = item.name

        if (item.image != null) {
            Glide.with(itemView.context)
//                    .asBitmap()
                    .load(item.image)
                    .error(R.drawable.default_media_placeholder)
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(image!!)
        }
    }

    fun setOnClickListener(listener: View.OnClickListener) {
        this.itemView.setOnClickListener(listener)
    }
}
