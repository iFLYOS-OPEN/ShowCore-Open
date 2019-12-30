package com.iflytek.cyber.iot.show.core.launcher

import android.graphics.drawable.Drawable

data class AppData(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val iconUrl: String? = null,
    val appType: Int = TYPE_PARTY
) {
    companion object {
        const val TYPE_PARTY = 1
        const val TYPE_INTERNAL = 2
        const val TYPE_TEMPLATE = 3
    }
}