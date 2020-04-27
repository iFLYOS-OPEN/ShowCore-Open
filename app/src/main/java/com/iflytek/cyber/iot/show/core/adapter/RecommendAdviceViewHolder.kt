package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.drakeet.multitype.ItemViewBinder
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.DeskRecommend
import com.iflytek.cyber.iot.show.core.model.DeskRecommendItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.OnMultiTypeItemClickListener
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger

class RecommendAdviceViewHolder : ItemViewBinder<DeskRecommend, RecyclerView.ViewHolder>() {
    var onItemClickListener: OnMultiTypeItemClickListener? = null
    var onCardRefreshListener: OnItemClickListener? = null

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: DeskRecommend) {
        if (holder is ItemViewHolder) {
            holder.setItem(item)
        }
    }

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): RecyclerView.ViewHolder {
        val holder = ItemViewHolder(inflater.inflate(R.layout.item_recommend_advice, parent, false))
        holder.onItemClickListener = object :
            OnItemClickListener {
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun onItemClick(parentInside: ViewGroup, itemViewInside: View, position: Int) {
                onItemClickListener?.onItemClick(
                    parent,
                    holder.itemView,
                    holder.adapterPosition,
                    position
                )
            }
        }
        holder.refresh?.clickWithTrigger {
            onCardRefreshListener?.onItemClick(parent, holder.itemView, holder.adapterPosition)
        }
        return holder
    }

    private class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivBackground: ImageView? = itemView.findViewById(R.id.background)
        val container: View? = itemView.findViewById(R.id.container)
        val tvTitle: TextView? = itemView.findViewById(R.id.title)
        val recyclerView: RecyclerView? = itemView.findViewById(R.id.recycler_view)
        val refresh: ImageView? = itemView.findViewById(R.id.refresh)
        private val adapter = AdviceAdapter()
        private val innerOnItemCLickListener = object :
            OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                onItemClickListener?.let { listener ->
                    val item = adapter.items[position]
                    listener.onItemClick(parent, itemView, position)
                }
            }
        }
        var onItemClickListener: OnItemClickListener? = null

        init {
            recyclerView?.adapter = adapter
            adapter.onItemClickListener = innerOnItemCLickListener
        }

        fun setItem(item: DeskRecommend) {
            item.background?.let { background ->
                if (background.isEmpty()) {
                    container?.background?.setColorFilter(
                        itemView.resources.getColor(R.color.campus_blue),
                        PorterDuff.Mode.SRC_IN
                    ) ?: run {
                        val drawable =
                            itemView.resources.getDrawable(R.drawable.bg_white_round_16dp)
                        drawable.setColorFilter(
                            itemView.resources.getColor(R.color.campus_blue),
                            PorterDuff.Mode.SRC_IN
                        )
                        container?.background = drawable
                    }
                } else {
                    var isColor = false
                    try {
                        val color = Color.parseColor(background)
                        container?.background?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                            ?: run {
                                val drawable =
                                    itemView.resources.getDrawable(R.drawable.bg_white_round_16dp)
                                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                                container?.background = drawable
                            }
                        isColor = true
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                    if (!isColor) {
                        ivBackground?.let { imageView ->
                            Glide.with(ivBackground)
                                .load(background)
                                .into(imageView)
                        }
                    } else {

                    }
                }
            }
            tvTitle?.text = item.title
            try {
                val color = Color.parseColor(item.titleColor)
                tvTitle?.setTextColor(color)
                refresh?.setColorFilter(color)
            } catch (t: Throwable) {

            }

            adapter.items.apply {
                clear()
                item.items?.let { items ->
                    addAll(items)
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private class AdviceAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        val items = mutableListOf<DeskRecommendItem>()
        var onItemClickListener: OnItemClickListener? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = AdviceViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_text_bubble,
                    parent,
                    false
                )
            )
            holder.clickableView?.setOnClickListener {
                onItemClickListener?.onItemClick(parent, holder.itemView, holder.adapterPosition)
            }
            return holder
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is AdviceViewHolder) {
                holder.tvTitle?.text = items[position].title

                try {
                    val color = Color.parseColor(items[position].titleColor)
                    holder.tvTitle?.setTextColor(color)
                } catch (t: Throwable) {

                }

                try {
                    val color = Color.parseColor(items[position].background)
                    holder.clickableView?.background?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                } catch (t: Throwable) {

                }
            }
        }

    }

    private class AdviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView? = itemView.findViewById(R.id.title)
        val clickableView: View? = itemView.findViewById(R.id.clickable_view)
    }
}