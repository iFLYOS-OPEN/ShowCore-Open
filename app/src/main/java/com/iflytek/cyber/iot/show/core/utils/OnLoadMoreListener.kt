package com.iflytek.cyber.iot.show.core.utils

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class OnLoadMoreListener(val callback: Callback) : RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val visibleItemCount = recyclerView.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()

        if (totalItemCount > 1 && totalItemCount - visibleItemCount <= firstVisibleItem + VISIBLE_THRESHOLD) {
            callback.onLoadMore()
        }
    }

    companion object {
        private const val VISIBLE_THRESHOLD = 5
    }

    interface Callback {
        fun onLoadMore()
    }
}