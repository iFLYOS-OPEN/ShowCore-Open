package com.iflytek.cyber.iot.show.core.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SpacesItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    var left = -1
    var right = -1
    var bottom = -1
    var top = -1

    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        val left = if (left != -1) left else space / 2
        val right = if (right != -1) right else space / 2
        val bottom = if (bottom != -1) bottom else space / 2
        val top = if (top != -1) top else space / 2

        // Add top margin only for the first item to avoid double space between result
        val layoutManager = parent.layoutManager as GridLayoutManager
        val spanCount = layoutManager.spanCount
        val position = parent.getChildLayoutPosition(view)
        val childCount = parent.adapter?.itemCount ?: 0

        outRect.top = top
        outRect.left = left
        outRect.right = right
        outRect.bottom = bottom
        if (position < spanCount) {
            // 第一横排
            outRect.top = 0
        }
        if (position >= childCount - 3) {
            // 最后横排
            outRect.bottom = 0
        }
    }
}