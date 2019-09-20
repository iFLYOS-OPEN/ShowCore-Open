package com.iflytek.cyber.iot.show.core.utils

import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.utils.OnLoadMoreListener

inline fun RecyclerView.onLoadMore(crossinline action: () -> Unit) {
    this.addOnScrollListener(OnLoadMoreListener(object : OnLoadMoreListener.Callback {
        override fun onLoadMore() {
            action.invoke()
        }
    }))
}