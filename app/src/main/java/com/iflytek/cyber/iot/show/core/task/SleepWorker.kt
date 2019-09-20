package com.iflytek.cyber.iot.show.core.task

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.fragment.BaseFragment
import com.iflytek.cyber.iot.show.core.fragment.MainFragment2
import com.iflytek.cyber.iot.show.core.fragment.ScreenBrightnessFragment.Companion.DEFAULT_SLEEP_TIME
import com.iflytek.cyber.iot.show.core.fragment.ScreenBrightnessFragment.Companion.KEY_SLEEP_TIME
import com.iflytek.cyber.iot.show.core.utils.DateUtils
import me.yokeyword.fragmentation.ISupportFragment
import me.yokeyword.fragmentation.SupportFragment
import java.util.*
import kotlin.math.min

class SleepWorker private constructor(context: Context) {

    private var handler = Handler()

    private var intent: Intent? = null
    private var hideIntent: Intent? = null

    var topFragment: ISupportFragment? = null

    private var pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    //work running
    private var isRunning = false
    private var startSleepDate = 0L
    private var endSleepDate = 0L

    companion object {

        const val START_SLEEP_HOUR = "start_sleep_hour"
        const val START_SLEEP_MINUTE = "start_sleep_minute"
        const val END_SLEEP_HOUR = "end_sleep_hour"
        const val END_SLEEP_MINUTE = "end_sleep_minute"

        private var worker: SleepWorker? = null

        fun get(context: Context): SleepWorker {
            val current = worker
            return if (current == null) {
                val newWorker = SleepWorker(context)
                worker = newWorker
                newWorker
            } else
                current
        }
    }

    init {
        updateSleepTime()
    }

    fun setCurrentTopFragment(topFragment: ISupportFragment?) {
        this.topFragment = topFragment
    }

    fun updateSleepTime() {
        val calendar = Calendar.getInstance()

        val startSleepHour = pref.getInt(START_SLEEP_HOUR, 22)
        val startSleepMinute = pref.getInt(START_SLEEP_MINUTE, 0)
        val endSleepHour = pref.getInt(END_SLEEP_HOUR, 7)
        val endSleepMinute = pref.getInt(END_SLEEP_MINUTE, 0)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        var day = calendar.get(Calendar.DAY_OF_MONTH)

        val startHourMin = String.format(Locale.getDefault(), "%02d:%02d", startSleepHour, startSleepMinute)
        val realMonth = String.format(Locale.getDefault(), "%02d", month)
        var realDay = String.format(Locale.getDefault(), "%02d", day)

        val startDate = DateUtils.stringToDate("yyyy-MM-dd HH:mm",
                "$year-$realMonth-$realDay $startHourMin")
        startSleepDate = startDate.time

        if (endSleepHour < startSleepHour) {
            day += 1
        }

        val endHourMin = String.format(Locale.getDefault(), "%02d:%02d", endSleepHour, endSleepMinute)

        realDay = String.format(Locale.getDefault(), "%02d", day)

        val endDate = DateUtils.stringToDate("yyyy-MM-dd HH:mm",
                "$year-$realMonth-$realDay $endHourMin")

        endSleepDate = endDate.time
    }

    private fun updateNextDaySleepTime() {
        val calendar = Calendar.getInstance()
        calendar.time = Date(startSleepDate) //根据上一次睡眠时间计算下一次的睡眠时间

        val startSleepHour = pref.getInt(START_SLEEP_HOUR, 22)
        val startSleepMinute = pref.getInt(START_SLEEP_MINUTE, 0)
        val endSleepHour = pref.getInt(END_SLEEP_HOUR, 7)
        val endSleepMinute = pref.getInt(END_SLEEP_MINUTE, 0)

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        var day = calendar.get(Calendar.DAY_OF_MONTH) + 1

        val startHourMin = String.format(Locale.getDefault(), "%02d:%02d", startSleepHour, startSleepMinute)
        val realMonth = String.format(Locale.getDefault(), "%02d", month)
        var realDay = String.format(Locale.getDefault(), "%02d", day)

        val startDate = DateUtils.stringToDate("yyyy-MM-dd HH:mm",
                "$year-$realMonth-$realDay $startHourMin")
        startSleepDate = startDate.time

        if (endSleepHour < startSleepHour) {
            day += 1
        }

        realDay = String.format(Locale.getDefault(), "%02d", day)

        val endHourMin = String.format(Locale.getDefault(), "%02d:%02d", endSleepHour, endSleepMinute)

        val endDate = DateUtils.stringToDate("yyyy-MM-dd HH:mm",
                "$year-$realMonth-$realDay $endHourMin")

        endSleepDate = endDate.time
    }

    //doing touch work, should not start sleep
    fun doTouchWork(context: Context) {
        if (!isServiceEnabled(context)) {
            return
        }

        if (startSleepDate == 0L) {
            updateSleepTime()
        }

        val currentTime = System.currentTimeMillis()

        if (currentTime > startSleepDate && currentTime > endSleepDate) {
            updateNextDaySleepTime()
        }

        if (currentTime in startSleepDate until endSleepDate) {
            isRunning = false
            handler.removeCallbacksAndMessages(null)
        }
    }

    fun startSleep(context: Context) {
        if (!isServiceEnabled(context)) {
            return
        }

        if (startSleepDate == 0L) {
            updateSleepTime()
        }

        val currentTime = System.currentTimeMillis()

        if (currentTime > startSleepDate && currentTime > endSleepDate) {
            updateNextDaySleepTime()
        }

        if (currentTime in startSleepDate until endSleepDate) {
            if (!isRunning) {
                val sleepTime = pref.getLong(KEY_SLEEP_TIME, DEFAULT_SLEEP_TIME)
                if (sleepTime < 0) { //never sleep
                    return
                }

                handler.postDelayed(sleepTime) {
                    showSleepView(context)
                    isRunning = false
                }
            }

            isRunning = true
        }
    }

    fun hideSleepView(context: Context) {
        handler.removeCallbacksAndMessages(null)
        if (hideIntent == null) {
            hideIntent = Intent(context, FloatingService::class.java).apply {
                action = FloatingService.ACTION_HIDE_SLEEP
            }
        }
        context.startService(hideIntent)
        isRunning = false
    }

    fun showSleepView(context: Context) {
        if (intent == null) {
            intent = Intent(context, FloatingService::class.java).apply {
                action = FloatingService.ACTION_SHOW_SLEEP
            }
        }
        context.startService(intent)
    }

    private fun isServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService<AccessibilityManager>() ?: return false
        val accessibilityServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in accessibilityServices) {
            if (info.id == context.packageName + "/.accessibility.TouchAccessibility") {
                return true
            }
        }
        return false
    }
}