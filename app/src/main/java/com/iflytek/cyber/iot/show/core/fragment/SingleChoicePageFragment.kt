package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.widget.CircleCheckBox

class SingleChoicePageFragment : Fragment() {
    companion object {
        fun newInstance(
            titles: Array<String>,
            icons: Array<String>?,
            selectedItem: Int,
            tag: Any? = null
        ): SingleChoicePageFragment {
            val fragment = SingleChoicePageFragment()
            fragment.titles.addAll(titles)
            icons?.let {
                fragment.icons.addAll(it)
            }
            fragment.selectedItem = selectedItem
            fragment.tag = tag
            return fragment
        }
    }

    private var recyclerView: RecyclerView? = null
    val titles = mutableListOf<String>()
    val icons = mutableListOf<String>()
    var selectedItem = 0
        private set
    private val adapter = ListAdapter()
    var onItemSelectedListener: OnItemSelectedListener? = null
    var tag: Any? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_single_choice_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView?.adapter = adapter
        recyclerView?.itemAnimator = DefaultItemAnimator()
    }

    fun updateData(titles: Array<String>, icons: Array<String>?, selectedItem: Int) {
        this.titles.clear()
        this.titles.addAll(titles)
        this.icons.clear()
        icons?.let {
            this.icons.addAll(icons)
        }
        this.selectedItem = selectedItem
        adapter.notifyDataSetChanged()
    }

    private inner class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = ItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_single_choice_with_icon,
                    parent,
                    false
                )
            )
            holder.itemView.setOnClickListener {
                selectedItem = holder.adapterPosition
                adapter.notifyItemRangeChanged(0, itemCount)
                onItemSelectedListener?.onItemSelect(this@SingleChoicePageFragment, selectedItem)
            }
            holder.checkBox.isClickable = false
            return holder
        }

        override fun getItemCount(): Int {
            return titles.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is ItemViewHolder) {
                holder.titleView.text = titles[position]
                holder.summaryView.isVisible = false
                holder.checkBox.isVisible = position == selectedItem

                if (icons.size > position) {
                    icons[position].let {
                        if (it.isEmpty()) {
                            holder.iconView.isVisible = false
                        } else {
                            holder.iconView.isVisible = true
                            Glide.with(holder.iconView)
                                .asDrawable()
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .load(it)
                                .into(holder.iconView)
                        }
                    }
                } else {
                    holder.iconView.isVisible = false
                }
            }
        }
    }

    private class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.icon)
        val titleView: TextView = itemView.findViewById(R.id.title)
        val summaryView: TextView = itemView.findViewById(R.id.message)
        val checkBox: CircleCheckBox = itemView.findViewById(R.id.check_box)
    }

    interface OnItemSelectedListener {
        fun onItemSelect(fragment: SingleChoicePageFragment, position: Int)
    }
}