package com.iflytek.cyber.iot.show.core.utils

import android.annotation.SuppressLint
import com.iflytek.cyber.iot.show.core.model.DateEntity
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class DateUtils {

    companion object {
        fun stringToDate(strDate: String): Date {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINESE)
            return sdf.parse(strDate)
        }

        fun stringToDate(pattern: String, strDate: String): Date {
            val sdf = SimpleDateFormat(pattern, Locale.CHINESE)
            return sdf.parse(strDate)
        }

        fun dateToStr(pattern: String, date: Date): String  {
            val sdf = SimpleDateFormat(pattern, Locale.CHINESE)
            return sdf.format(date)
        }

        fun getHourList(): ArrayList<String> {
            val hourList = ArrayList<String>()
            for (hour in 0..23) {
                hourList.add("${hour}时")
            }
            return hourList
        }

        fun getMinList(): ArrayList<String> {
            val minList = ArrayList<String>()
            for (min in 0..59) {
                minList.add("${min}分")
            }
            return minList
        }

        fun getMonthList(): ArrayList<String> {
            val monthList = ArrayList<String>()
            for (month in 1..31) {
                monthList.add("${month}日")
            }
            return monthList
        }

        @SuppressLint("SimpleDateFormat")
        fun getDateList(start: String, end: String, type: String): ArrayList<DateEntity> {
            val dates = ArrayList<DateEntity>()
            val df1 = SimpleDateFormat("yyyy-MM-dd")

            var date1: Date? = null
            var date2: Date? = null

            try {
                date1 = df1.parse(start)
                date2 = df1.parse(end)
            } catch (e: ParseException) {
                e.printStackTrace()
            }

            val cal1 = Calendar.getInstance()
            cal1.time = date1

            val cal2 = Calendar.getInstance()
            cal2.time = date2

            while (!cal1.after(cal2)) {
                dates.add(DateEntity(cal1.time, type))
                cal1.add(Calendar.DATE, 1)
            }
            return dates
        }
    }
}