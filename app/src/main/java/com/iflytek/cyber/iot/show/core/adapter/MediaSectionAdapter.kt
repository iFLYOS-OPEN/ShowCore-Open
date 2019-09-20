package com.iflytek.cyber.iot.show.core.adapter

import android.annotation.SuppressLint
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Group
import com.iflytek.cyber.iot.show.core.model.GroupItem

class MediaSectionAdapter(private val name: String,
                          private val items: List<Group>,
                          private val onItemClickListener: (GroupItem) -> Unit,
                          private val onMoreClickListener: (Int) -> Unit) : RecyclerView.Adapter<MediaSectionAdapter.MediaSectionHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaSectionHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return MediaSectionHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MediaSectionHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.more.text = "全部${item.name}"
        holder.desc.text = item.descriptions[(Math.random() * item.descriptions.size - 1).toInt()]
        val newItems = if (item.items != null && item.items.size > 4) {
            item.items.subList(0, 4)
        } else {
            item.items
        }
        holder.setAlbumList(name, newItems, object : AlbumAdapter.OnGroupItemClickListener {
            override fun onGroupItemClick(itemView: View, groupItem: GroupItem) {
                onItemClickListener.invoke(groupItem)
            }
        })
        holder.moreContent.setOnClickListener {
            onMoreClickListener.invoke(position)
        }
    }

    class MediaSectionHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.category_title)
        val desc = itemView.findViewById<TextView>(R.id.category_desc)
        val albumList = itemView.findViewById<RecyclerView>(R.id.album_list)
        val more = itemView.findViewById<TextView>(R.id.more)
        val moreContent = itemView.findViewById<LinearLayout>(R.id.more_content)

        val adapter = AlbumAdapter()

        fun setAlbumList(name: String, albumList: List<GroupItem>?, onGroupItemClickListener: AlbumAdapter.OnGroupItemClickListener) {
            if (TextUtils.equals(name, "视频")) {
                adapter.itemViewType = AlbumAdapter.TYPE_RECTANGLE
            } else {
                adapter.itemViewType = AlbumAdapter.TYPE_SQUARE
            }
            adapter.setGroupList(albumList)
            adapter.onGroupItemClickListener = onGroupItemClickListener
            this.albumList.adapter = adapter
        }
    }
}