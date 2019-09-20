package com.iflytek.cyber.iot.show.core.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.GroupItem

class MusicAdapter(private val items: List<Pair<String?, List<GroupItem>>>,
                   private val onItemClickListener: (GroupItem) -> Unit,
                   private val onMoreClickListener: (String?) -> Unit)
    : RecyclerView.Adapter<MusicAdapter.MusicHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return MusicHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MusicHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.first
        val newItems =  if (item.second.size > 4) {
            item.second.subList(0, 4)
        } else {
            item.second
        }
        holder.setAlbumList(newItems, object : AlbumAdapter.OnGroupItemClickListener {
            override fun onGroupItemClick(itemView: View, groupItem: GroupItem) {
                onItemClickListener.invoke(groupItem)
            }
        })
        holder.moreContent.setOnClickListener {
            onMoreClickListener.invoke(item.first)
        }
        holder.more.text = "全部${item.first}"
    }

    class MusicHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title = itemView.findViewById<TextView>(R.id.category_title)
        val desc = itemView.findViewById<TextView>(R.id.category_desc)
        val albumList = itemView.findViewById<RecyclerView>(R.id.album_list)
        val more = itemView.findViewById<TextView>(R.id.more)
        val moreContent = itemView.findViewById<LinearLayout>(R.id.more_content)

        val adapter = AlbumAdapter()

        fun setAlbumList(albumList: List<GroupItem>, onGroupItemClickListener: AlbumAdapter.OnGroupItemClickListener) {
            adapter.setGroupList(albumList)
            adapter.onGroupItemClickListener = onGroupItemClickListener
            this.albumList.adapter = adapter
        }
    }
}