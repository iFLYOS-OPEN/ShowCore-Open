package com.iflytek.cyber.iot.show.core.fragment

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.task.SleepWorker.Companion.END_SLEEP_HOUR
import com.iflytek.cyber.iot.show.core.task.SleepWorker.Companion.END_SLEEP_MINUTE
import com.iflytek.cyber.iot.show.core.task.SleepWorker.Companion.START_SLEEP_HOUR
import com.iflytek.cyber.iot.show.core.task.SleepWorker.Companion.START_SLEEP_MINUTE
import com.iflytek.cyber.iot.show.core.utils.BrightnessUtils
import com.iflytek.cyber.iot.show.core.widget.BoxedHorizontal
import java.util.*
import kotlin.collections.ArrayList


class ScreenBrightnessFragment : BaseFragment(), View.OnClickListener {

    private var brightnessSlider: BoxedHorizontal? = null

    private lateinit var tenMinSleep: TextView
    private lateinit var twentyMinSleep: TextView
    private lateinit var halfHourSleep: TextView
    private lateinit var hourSleep: TextView
    private lateinit var neverSleep: TextView
    private lateinit var tvSleepTips: TextView
    private lateinit var tvSleepEnableTips: TextView
    private lateinit var applicableTime: TextView
    private lateinit var switchScreenTimeoutContent: FrameLayout

    private var selectedViewList = ArrayList<TextView>()

    private val brightnessObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            val context = context ?: return
            val contentResolver = context.contentResolver
            if (BRIGHTNESS_MODE_URI == uri) {
                val mode = BrightnessUtils.getBrightnessMode(context)

                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    brightnessSlider?.let { slider ->
                        slider.isEnable = true
                    }
                } else {
                    val currentBrightness = BrightnessUtils.getBrightness(context)
                    brightnessSlider?.let { slider ->
                        if (!slider.isPressed) {
                            slider.isEnable = true
                            slider.value = currentBrightness
                        }
                    }
                    view?.findViewById<LottieAnimationView>(R.id.brightness_icon)?.let { icon ->
                        icon.progress = currentBrightness / 100f
                    }
                }
            } else if (BRIGHTNESS_URI == uri) {
                val currentBrightness = BrightnessUtils.getBrightness(context)
                brightnessSlider?.let { slider ->
                    if (!slider.isPressed) {
                        slider.isEnable = true
                        slider.value = currentBrightness
                    }
                }
                view?.findViewById<LottieAnimationView>(R.id.brightness_icon)?.let { icon ->
                    icon.progress = currentBrightness / 100f
                }
            }
        }
    }

    companion object {
        private val BRIGHTNESS_MODE_URI =
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE)
        private val BRIGHTNESS_URI = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)

        const val DEFAULT_SLEEP_TIME: Long = 10 * 60 * 1000
        const val TWENTY_SLEEP_TIME: Long = 20 * 60 * 1000
        const val HALF_HOUR_SLEEP_TIME: Long = 30 * 60 * 1000
        const val HOUR_SLEEP_TIME: Long = 60 * 60 * 1000
        const val NEVER_SLEEP_TIME: Long = -1

        const val KEY_SLEEP_TIME = "sleep_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            context?.contentResolver?.let { cr ->
                cr.registerContentObserver(BRIGHTNESS_MODE_URI, false, brightnessObserver)
                cr.registerContentObserver(BRIGHTNESS_URI, false, brightnessObserver)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_screen_brightness, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tenMinSleep = view.findViewById(R.id.ten_min_sleep)
        tenMinSleep.setOnClickListener(this)
        twentyMinSleep = view.findViewById(R.id.twenty_min_sleep)
        twentyMinSleep.setOnClickListener(this)
        halfHourSleep = view.findViewById(R.id.half_hour_sleep)
        halfHourSleep.setOnClickListener(this)
        hourSleep = view.findViewById(R.id.hour_sleep)
        hourSleep.setOnClickListener(this)
        neverSleep = view.findViewById(R.id.never_sleep)
        neverSleep.setOnClickListener(this)
        selectedViewList.add(tenMinSleep)
        selectedViewList.add(twentyMinSleep)
        selectedViewList.add(halfHourSleep)
        selectedViewList.add(hourSleep)
        selectedViewList.add(neverSleep)

        tvSleepTips = view.findViewById(R.id.tv_sleep_tips)
        tvSleepEnableTips = view.findViewById(R.id.tv_sleep_enable_tips)
        applicableTime = view.findViewById(R.id.applicable_time_value)

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        view.findViewById<View>(R.id.switch_screen_timeout_content).setOnClickListener {
            start(TimeSelectedFragment())
        }

        switchScreenTimeoutContent = view.findViewById(R.id.switch_screen_timeout_content)

        brightnessSlider = view.findViewById(R.id.brightness_slider)

        brightnessSlider?.setOnBoxedPointsChangeListener(object :
            BoxedHorizontal.OnValuesChangeListener {
            override fun onPointsChanged(
                boxedPoints: BoxedHorizontal,
                points: Int,
                fromTouch: Boolean
            ) {
                if (BrightnessUtils.hasPermission(context) && !fromTouch) {
                    BrightnessUtils.setBrightness(context, points)

                    view.findViewById<LottieAnimationView>(R.id.brightness_icon)?.let { icon ->
                        icon.progress = points / 100f
                    }
                }
            }

            override fun onStartTrackingTouch(boxedPoints: BoxedHorizontal) {

            }

            override fun onStopTrackingTouch(boxedPoints: BoxedHorizontal) {

            }

        })

        post {
            updateBrightness()
        }

        updatePref()
    }

    private fun updatePref() {
        if (launcher == null) {
            return
        }
        val pref = PreferenceManager.getDefaultSharedPreferences(launcher)
        val sleepTime = pref.getLong(KEY_SLEEP_TIME, DEFAULT_SLEEP_TIME)
        when {
            sleepTime == DEFAULT_SLEEP_TIME -> {
                switchScreenTimeoutContent.isVisible = true
                tvSleepTips.text =
                    launcher?.getString(R.string.sleep_tips, tenMinSleep.text.toString())
                tenMinSleep.isSelected = true
                updateSelectState(tenMinSleep)
            }
            sleepTime == TWENTY_SLEEP_TIME -> {
                switchScreenTimeoutContent.isVisible = true
                tvSleepTips.text =
                    launcher?.getString(R.string.sleep_tips, twentyMinSleep.text.toString())
                twentyMinSleep.isSelected = true
                updateSelectState(twentyMinSleep)
            }
            sleepTime == HALF_HOUR_SLEEP_TIME -> {
                switchScreenTimeoutContent.isVisible = true
                tvSleepTips.text =
                    launcher?.getString(R.string.sleep_tips, halfHourSleep.text.toString())
                halfHourSleep.isSelected = true
                updateSelectState(halfHourSleep)
            }
            sleepTime == HOUR_SLEEP_TIME -> {
                switchScreenTimeoutContent.isVisible = true
                tvSleepTips.text =
                    launcher?.getString(R.string.sleep_tips, hourSleep.text.toString())
                hourSleep.isSelected = true
                updateSelectState(hourSleep)
            }
            sleepTime < 0 -> {
                tvSleepTips.text = null
                switchScreenTimeoutContent.isVisible = false
                neverSleep.isSelected = true
                updateSelectState(neverSleep)
            }
        }
    }

    private fun updateSelectState(selected: TextView) {
        selectedViewList.forEach {
            if (selected != it) {
                it.isSelected = false
            }
        }
    }

    override fun onClick(v: View?) {
        if (!isServiceEnabled()) {
            showEnableDialog()
            return
        }
        when (v?.id) {
            R.id.ten_min_sleep -> {
                val pref = PreferenceManager.getDefaultSharedPreferences(launcher)
                pref.edit { putLong(KEY_SLEEP_TIME, DEFAULT_SLEEP_TIME) }
                updatePref()
            }
            R.id.twenty_min_sleep -> {
                val pref = PreferenceManager.getDefaultSharedPreferences(launcher)
                pref.edit { putLong(KEY_SLEEP_TIME, TWENTY_SLEEP_TIME) }
                updatePref()
            }
            R.id.half_hour_sleep -> {
                val pref = PreferenceManager.getDefaultSharedPreferences(launcher)
                pref.edit { putLong(KEY_SLEEP_TIME, HALF_HOUR_SLEEP_TIME) }
                updatePref()
            }
            R.id.hour_sleep -> {
                val pref = PreferenceManager.getDefaultSharedPreferences(launcher)
                pref.edit { putLong(KEY_SLEEP_TIME, HOUR_SLEEP_TIME) }
                updatePref()
            }
            R.id.never_sleep -> {
                val pref = PreferenceManager.getDefaultSharedPreferences(launcher)
                pref.edit { putLong(KEY_SLEEP_TIME, NEVER_SLEEP_TIME) }
                updatePref()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        context?.contentResolver?.unregisterContentObserver(brightnessObserver)
    }

    private fun updateBrightness() {
        context?.let { context ->
            val currentBrightness = BrightnessUtils.getBrightness(context)
            val mode = BrightnessUtils.getBrightnessMode(context)

            brightnessSlider?.let { slider ->
                slider.isEnable = mode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                slider.value = currentBrightness
            }
            view?.findViewById<LottieAnimationView>(R.id.brightness_icon)?.let { icon ->
                icon.progress = currentBrightness / 100f
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSleepTime() {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val startSleepHour = pref.getInt(START_SLEEP_HOUR, 22)
        val startSleepMinute = pref.getInt(START_SLEEP_MINUTE, 0)
        val endSleepHour = pref.getInt(END_SLEEP_HOUR, 7)
        val endSleepMinute = pref.getInt(END_SLEEP_MINUTE, 0)

        val startHourMin =
            String.format(Locale.getDefault(), "%02d:%02d", startSleepHour, startSleepMinute)
        val endHourMin =
            String.format(Locale.getDefault(), "%02d:%02d", endSleepHour, endSleepMinute)

        applicableTime.text = "$startHourMin-$endHourMin"
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        updateSleepTime()

        if (!isServiceEnabled()) {
            tvSleepEnableTips.text = launcher?.getString(R.string.sleep_enable_tips)
            showEnableDialog()
        } else {
            tvSleepEnableTips.text = launcher?.getString(R.string.auto_lock)
        }
    }

    private fun showEnableDialog() {
        if (launcher == null) return

        AlertDialog.Builder(launcher!!)
            .setMessage(R.string.sleep_enable_desc)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.go_to_setting) { dialog, which ->
                openAccessibility()
            }
            .show()
    }

    private fun openAccessibility() {
        try {
            val accessibleIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(accessibleIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isServiceEnabled(): Boolean {
        val accessibilityManager = launcher?.getSystemService<AccessibilityManager>()
            ?: return false
        val accessibilityServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in accessibilityServices) {
            if (info.id == launcher?.packageName + "/.accessibility.TouchAccessibility") {
                return true
            }
        }
        return false
    }
}