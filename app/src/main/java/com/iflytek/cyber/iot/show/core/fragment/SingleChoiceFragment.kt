package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.widget.CircleCheckBox
import com.iflytek.cyber.iot.show.core.widget.DividerItemDecoration

class SingleChoiceFragment : BaseFragment() {
    private val titles = mutableListOf<String>()
    private val summaries = mutableListOf<String>()
    private var selectedItem = 0

    private var title: String? = null
    private var message: String? = null

    private var popWhenSelected: Boolean = false

    private val adapter = ListAdapter()

    private var backCount = 0

    companion object {
        fun newInstance(
            title: String,
            message: String?,
            itemTitles: Array<String>,
            itemSummaries: Array<String>?,
            selectedItem: Int = 0,
            popWhenSelected: Boolean = false
        ): SingleChoiceFragment {
            val fragment = SingleChoiceFragment()
            fragment.titles.addAll(itemTitles)
            itemSummaries?.let {
                fragment.summaries.addAll(itemSummaries)
            }
            fragment.selectedItem = selectedItem
            fragment.title = title
            fragment.message = message
            fragment.popWhenSelected = popWhenSelected
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_single_choice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            setResult()
            pop()
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        val itemDecoration = DividerItemDecoration.Builder(view.context)
            .setDividerColor(resources.getColor(R.color.divider_line))
            .setPaddingLeft(resources.getDimensionPixelSize(R.dimen.dp_40))
            .setPaddingRight(resources.getDimensionPixelSize(R.dimen.dp_40))
            .build()
        recyclerView.addItemDecoration(itemDecoration)

        val messageView = view.findViewById<TextView>(R.id.message)
        messageView.text = message
        messageView.isVisible = !message.isNullOrEmpty()

        val titleView = view.findViewById<TextView>(R.id.title)
        titleView.text = title
    }

    private inner class ListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val holder = ItemViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_single_choice,
                    parent,
                    false
                )
            )
            holder.itemView.setOnClickListener {
                selectedItem = holder.adapterPosition
                adapter.notifyItemRangeChanged(0, itemCount)
                if (popWhenSelected) {
                    setResult()
                    pop()
                }
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
                if (summaries.size > position) {
                    holder.summaryView.isVisible = true
                    holder.summaryView.text = summaries[position]
                } else {
                    holder.summaryView.isVisible = false
                }
                holder.checkBox.isVisible = position == selectedItem
            }
        }
    }

    override fun onBackPressedSupport(): Boolean {
        setResult()
        return super.onBackPressedSupport()
    }

    private fun setResult() {
        val bundle = Bundle()
        bundle.putInt("selected_item", selectedItem)
        setFragmentResult(0, bundle)
    }

    private class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleView: TextView = itemView.findViewById(R.id.title)
        val summaryView: TextView = itemView.findViewById(R.id.message)
        val checkBox: CircleCheckBox = itemView.findViewById(R.id.check_box)
    }
}