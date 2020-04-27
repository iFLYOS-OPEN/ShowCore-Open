package com.iflytek.cyber.iot.show.core.utils

import android.view.View
import android.view.ViewGroup

interface OnItemClickListener {
    fun onItemClick(parent: ViewGroup, itemView: View, position: Int)
}

interface OnMultiTypeItemClickListener {
    fun onItemClick(parent: ViewGroup, itemView: View, position: Int, subPosition: Int)
}