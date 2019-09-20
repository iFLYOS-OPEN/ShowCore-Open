package com.iflytek.cyber.iot.show.core.utils

import android.view.View
import android.view.ViewGroup

interface OnItemClickListener {
    fun onItemClick(parent: ViewGroup, itemView: View, position: Int)
}