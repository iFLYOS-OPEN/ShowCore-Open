package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.task.SleepWorker
import com.iflytek.cyber.iot.show.core.task.SleepWorker.Companion.END_SLEEP_HOUR
import com.iflytek.cyber.iot.show.core.task.SleepWorker.Companion.END_SLEEP_MINUTE
import com.iflytek.cyber.iot.show.core.task.SleepWorker.Companion.START_SLEEP_HOUR
import com.iflytek.cyber.iot.show.core.task.SleepWorker.Companion.START_SLEEP_MINUTE
import com.zyyoona7.wheel.WheelView

class TimeSelectedFragment : BaseFragment() {

    private lateinit var startHourWheel: WheelView<String>
    private lateinit var startMinuteWheel: WheelView<String>
    private lateinit var endHourWheel: WheelView<String>
    private lateinit var endMinuteWheel: WheelView<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return LayoutInflater.from(context).inflate(R.layout.fragment_time_selected, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener { pop() }
        view.findViewById<View>(R.id.done).setOnClickListener {
            updateSleepTime()
        }

        startHourWheel = view.findViewById(R.id.start_hour_wheel)
        startMinuteWheel = view.findViewById(R.id.start_minute_wheel)
        endHourWheel = view.findViewById(R.id.en_hour_wheel)
        endMinuteWheel = view.findViewById(R.id.end_minute_wheel)

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        val startSleepHour = pref.getInt(START_SLEEP_HOUR, 22)
        val startSleepMinute = pref.getInt(START_SLEEP_MINUTE, 0)
        val endSleepHour = pref.getInt(END_SLEEP_HOUR, 7)
        val endSleepMinute = pref.getInt(END_SLEEP_MINUTE, 0)

        startHourWheel.data = getHourList()
        startMinuteWheel.data = getMinList()
        startHourWheel.setSelectedItemPosition(startSleepHour, true)
        startMinuteWheel.setSelectedItemPosition(startSleepMinute, true)

        endHourWheel.data = startHourWheel.data
        endMinuteWheel.data = startMinuteWheel.data
        endHourWheel.setSelectedItemPosition(endSleepHour, true)
        endMinuteWheel.setSelectedItemPosition(endSleepMinute, true)
    }

    private fun updateSleepTime() {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        pref.edit {
            putInt(START_SLEEP_HOUR, startHourWheel.selectedItemPosition)
            putInt(START_SLEEP_MINUTE, startMinuteWheel.selectedItemPosition)
            putInt(END_SLEEP_HOUR, endHourWheel.selectedItemPosition)
            putInt(END_SLEEP_MINUTE, endMinuteWheel.selectedItemPosition)
        }

        SleepWorker.get(startHourWheel.context).updateSleepTime()

        pop()
    }

    private fun getHourList(): ArrayList<String> {
        val hourList = ArrayList<String>()
        for (hour in 0..23) {
            hourList.add("$hour")
        }
        return hourList
    }

    private fun getMinList(): ArrayList<String> {
        val minList = ArrayList<String>()
        for (min in 0..59) {
            minList.add("$min")
        }
        return minList
    }
}