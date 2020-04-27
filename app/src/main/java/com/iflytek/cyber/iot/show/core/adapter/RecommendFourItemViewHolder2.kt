package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.drakeet.multitype.ItemViewBinder
import com.iflytek.cyber.evs.sdk.utils.AppUtil
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.DeskRecommend
import com.iflytek.cyber.iot.show.core.model.DeskRecommendItem
import com.iflytek.cyber.iot.show.core.utils.OnItemClickListener
import com.iflytek.cyber.iot.show.core.utils.OnMultiTypeItemClickListener
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.iflytek.cyber.iot.show.core.utils.clickWithTrigger

class RecommendFourItemViewHolder2 : ItemViewBinder<DeskRecommend, RecyclerView.ViewHolder>() {
    var onItemClickListener: OnMultiTypeItemClickListener? = null
    var onCardRefreshListener: OnItemClickListener? = null
    var onOpenWebPageListener: OnOpenWebPageListener? = null

    interface OnOpenWebPageListener {
        fun onOpenWebPage(url: String)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: DeskRecommend) {
        if (holder is ItemViewHolder) {
            holder.setItem(item)
        }
    }

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup
    ): RecyclerView.ViewHolder {
        val holder =
            ItemViewHolder(inflater.inflate(R.layout.item_recommend_four_item_2, parent, false))
        holder.childList.mapIndexed { index, itemGroup ->
            itemGroup.clickableView?.setOnClickListener {
                onItemClickListener?.onItemClick(
                    parent,
                    holder.itemView,
                    holder.adapterPosition,
                    index
                )
            }
        }
        holder.refresh?.clickWithTrigger {
            onCardRefreshListener?.onItemClick(parent, it, holder.adapterPosition)
        }
        holder.openMap?.setOnClickListener {
            onOpenWebPageListener?.onOpenWebPage("https://voice.baidu.com/act/newpneumonia/newpneumonia/from=osari_pc_3")
        }
        return holder
    }

    private class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivBackground: ImageView? = itemView.findViewById(R.id.background)
        val container: View? = itemView.findViewById(R.id.container)
        val tvTitle: TextView? = itemView.findViewById(R.id.title)
        val refresh: ImageView? = itemView.findViewById(R.id.refresh)
        val openMap: TextView? = itemView.findViewById(R.id.tv_open_map)
        val childList = arrayOf(
            ItemGroup(itemView, R.id.iv_image_0, R.id.tv_title_0, R.id.tv_subtitle_0, R.id.item_0),
            ItemGroup(itemView, R.id.iv_image_1, R.id.tv_title_1, R.id.tv_subtitle_1, R.id.item_1),
            ItemGroup(itemView, R.id.iv_image_2, R.id.tv_title_2, R.id.tv_subtitle_2, R.id.item_2),
            ItemGroup(itemView, R.id.iv_image_3, R.id.tv_title_3, R.id.tv_subtitle_3, R.id.item_3)
        )

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
                        val transformer: MultiTransformation<Bitmap> = MultiTransformation(
                            CenterCrop(),
                            RoundedCornersTransformation(
                                itemView.context.resources.getDimensionPixelSize(R.dimen.dp_16), 0
                            )
                        )
                        ivBackground?.isVisible = true
                        ivBackground?.let { imageView ->
                            Glide.with(ivBackground)
                                .load(background)
                                .transform(transformer)
                                .into(imageView)
                        }
                    } else {
                        ivBackground?.isVisible = false
                    }
                }
            }
            tvTitle?.text = item.title
            refresh?.isVisible = item.title.equals("疫情专区") ||
                    item.title.equals("你可以说") ||
                    item.title.equals("在线教育") ||
                    item.title.equals("推荐应用")
            openMap?.isVisible = item.title.equals("疫情专区")
            try {
                val color = Color.parseColor(item.titleColor)
                refresh?.setColorFilter(color)
                tvTitle?.setTextColor(color)
            } catch (t: Throwable) {
            }

            val newItems = if (!item.items.isNullOrEmpty()) {
                filterItems(item.items)
            } else {
                null
            }
            childList.mapIndexed { index, itemGroup ->
                if (!newItems.isNullOrEmpty() && index < newItems.size) {
                    itemGroup.isVisible = true
                    itemGroup.setItem(newItems[index])
                } else {
                    itemGroup.isVisible = false
                }
            }
        }

        private fun filterItems(items: ArrayList<DeskRecommendItem>): ArrayList<DeskRecommendItem> {
            val newItems = ArrayList<DeskRecommendItem>()
            items.forEach {
                if (!it.shouldHide) {
                    newItems.add(it)
                }
            }
            return newItems
        }
    }

    private class ItemGroup(
        itemView: View,
        imageId: Int,
        titleId: Int,
        subtitleId: Int,
        clickableViewId: Int
    ) {
        val tvTitle: TextView? = itemView.findViewById(titleId)
        val tvSubtitle: TextView? = itemView.findViewById(subtitleId)
        val ivImage: ImageView? = itemView.findViewById(imageId)
        val clickableView: View? = itemView.findViewById(clickableViewId)

        var isVisible: Boolean = true
            set(value) {
                field = value

                clickableView?.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
            }

        fun setItem(item: DeskRecommendItem) {
            clickableView?.isVisible = !item.shouldHide

            tvTitle?.text = item.title
            try {
                val color = Color.parseColor(item.titleColor)
                tvTitle?.setTextColor(color)
            } catch (t: Throwable) {

            }
            tvSubtitle?.text = item.subtitle
            try {
                val color = Color.parseColor(item.subtitleColor)
                tvSubtitle?.setTextColor(color)
            } catch (t: Throwable) {
            }

            ivImage?.let { imageView ->
                Glide.with(imageView)
                    .load(item.cover)
                    .transition(
                        DrawableTransitionOptions.with(
                            DrawableCrossFadeFactory.Builder()
                                .setCrossFadeEnabled(true).build()
                        )
                    )
                    .placeholder(R.drawable.bg_grey_round_10dp)
                    .optionalTransform(
                        MultiTransformation(
                            CenterCrop(),
                            MultiTransformation(
                                CenterCrop(), RoundedCornersTransformation(
                                    imageView.resources.getDimensionPixelSize(
                                        R.dimen.dp_10
                                    ), 0
                                )
                            )
                        )
                    )
                    .into(imageView)
            }
        }
    }
}