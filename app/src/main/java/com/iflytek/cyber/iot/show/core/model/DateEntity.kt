package com.iflytek.cyber.iot.show.core.model

import com.zyyoona7.wheel.IWheelEntity
import java.text.SimpleDateFormat
import java.util.*

class DateEntity(var date: Date, var type: String) : IWheelEntity {

    override fun getWheelText(): String {
        return formatDate(date)
    }

    private fun formatDate(date: Date): String {
        val format = if (type == Alert.TYPE_NONE) {
            SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
        } else {
            SimpleDateFormat("MM月dd日", Locale.CHINA)
        }
        return format.format(date)
    }
}