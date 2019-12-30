package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.BrightnessUtils
import com.iflytek.cyber.iot.show.core.utils.ScreenOffTimeoutUtils
import com.iflytek.cyber.iot.show.core.widget.BoxedHorizontal
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min


class ScreenBrightnessFragment : BaseFragment(), View.OnClickListener, PageScrollable {

    private var brightnessSlider: BoxedHorizontal? = null

    private lateinit var oneMinSleep: TextView
    private lateinit var twoMinSleep: TextView
    private lateinit var fiveMinSleep: TextView
    private lateinit var tenMinSleep: TextView
    private lateinit var neverSleep: TextView
    private lateinit var tvSleepTips: TextView
    private lateinit var tvSleepEnableTips: TextView

    private var scrollView: ScrollView? = null
    private var contentContainer: LinearLayout? = null

    private var selectedViewList = ArrayList<TextView>()

    private var backCount = 0

    private val brightnessObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            val context = context ?: return
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

        val ONE_SLEEP_TIME: Long = TimeUnit.MINUTES.toMillis(1)
        val TWO_SLEEP_TIME: Long = TimeUnit.MINUTES.toMillis(2)
        val FIVE_SLEEP_TIME: Long = TimeUnit.MINUTES.toMillis(5)
        val TEN_SLEEP_TIME: Long = TimeUnit.MINUTES.toMillis(10)
        val DEFAULT_SLEEP_TIME: Long = TEN_SLEEP_TIME
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

        scrollView = view.findViewById(R.id.scroll_view)
        contentContainer = view.findViewById(R.id.content_container)

        oneMinSleep = view.findViewById(R.id.one_min_sleep)
        oneMinSleep.setOnClickListener(this)
        twoMinSleep = view.findViewById(R.id.two_min_sleep)
        twoMinSleep.setOnClickListener(this)
        fiveMinSleep = view.findViewById(R.id.five_min_sleep)
        fiveMinSleep.setOnClickListener(this)
        tenMinSleep = view.findViewById(R.id.ten_min_sleep)
        tenMinSleep.setOnClickListener(this)
        neverSleep = view.findViewById(R.id.never_sleep)
        neverSleep.setOnClickListener(this)
        selectedViewList.add(oneMinSleep)
        selectedViewList.add(twoMinSleep)
        selectedViewList.add(fiveMinSleep)
        selectedViewList.add(tenMinSleep)
        selectedViewList.add(neverSleep)

        tvSleepTips = view.findViewById(R.id.tv_sleep_tips)
        tvSleepEnableTips = view.findViewById(R.id.tv_sleep_enable_tips)

        view.findViewById<View>(R.id.back).setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

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
    }

    private fun updatePref() {
        val context = context ?: return
        val sleepTime = ScreenOffTimeoutUtils.getTimeout(context)
        when {
            sleepTime == ScreenOffTimeoutUtils.ONE_MIN_TIMEOUT -> {
                tvSleepTips.text =
                    launcher?.getString(R.string.sleep_tips, oneMinSleep.text.toString())
                oneMinSleep.isSelected = true
                updateSelectState(oneMinSleep)
            }
            sleepTime == ScreenOffTimeoutUtils.TWO_MIN_TIMEOUT -> {
                tvSleepTips.text =
                    launcher?.getString(R.string.sleep_tips, twoMinSleep.text.toString())
                twoMinSleep.isSelected = true
                updateSelectState(twoMinSleep)
            }
            sleepTime == ScreenOffTimeoutUtils.FIVE_MIN_TIMEOUT -> {
                tvSleepTips.text =
                    launcher?.getString(R.string.sleep_tips, fiveMinSleep.text.toString())
                fiveMinSleep.isSelected = true
                updateSelectState(fiveMinSleep)
            }
            sleepTime == ScreenOffTimeoutUtils.TEN_MIN_TIMEOUT -> {
                tvSleepTips.text =
                    launcher?.getString(R.string.sleep_tips, tenMinSleep.text.toString())
                tenMinSleep.isSelected = true
                updateSelectState(tenMinSleep)
            }
            sleepTime == ScreenOffTimeoutUtils.NEVER_TIMEOUT -> {
                tvSleepTips.text = null
//                switchScreenTimeoutContent.isVisible = false
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
        when (v?.id) {
            R.id.ten_min_sleep -> {
                ScreenOffTimeoutUtils.setTimeout(v.context, ScreenOffTimeoutUtils.TEN_MIN_TIMEOUT)
                updatePref()
            }
            R.id.five_min_sleep -> {
                ScreenOffTimeoutUtils.setTimeout(v.context, ScreenOffTimeoutUtils.FIVE_MIN_TIMEOUT)
                updatePref()
            }
            R.id.two_min_sleep -> {
                ScreenOffTimeoutUtils.setTimeout(v.context, ScreenOffTimeoutUtils.TWO_MIN_TIMEOUT)
                updatePref()
            }
            R.id.one_min_sleep -> {
                ScreenOffTimeoutUtils.setTimeout(v.context, ScreenOffTimeoutUtils.ONE_MIN_TIMEOUT)
                updatePref()
            }
            R.id.never_sleep -> {
                ScreenOffTimeoutUtils.setTimeout(v.context, ScreenOffTimeoutUtils.NEVER_TIMEOUT)
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

    override fun onSupportVisible() {
        super.onSupportVisible()

        updatePref()
    }

    override fun scrollToNext(): Boolean {
        scrollView?.let { scrollView ->
            val pageHeight = scrollView.height
            val scrollY = scrollView.scrollY
            val contentHeight = contentContainer?.height ?: 0
            if (scrollY == contentHeight - pageHeight) {
                return false
            }
            val target = min(contentHeight - pageHeight, scrollY + pageHeight)
            smoothScrollTo(target)
            return true
        } ?: run {
            return false
        }
    }

    override fun scrollToPrevious(): Boolean {
        scrollView?.let { scrollView ->
            val pageHeight = scrollView.height
            val scrollY = scrollView.scrollY
            if (scrollY == 0) {
                return false
            }
            val target = max(0, scrollY - pageHeight)
            smoothScrollTo(target)
            return true
        } ?: run {
            return false
        }
    }

    private fun smoothScrollTo(scrollY: Int) {
        scrollView?.isSmoothScrollingEnabled = true
        scrollView?.smoothScrollTo(0, scrollY)
    }
}