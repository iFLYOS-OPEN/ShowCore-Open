package com.iflytek.cyber.iot.show.core.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.iflytek.cyber.evs.sdk.agent.Alarm
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.iot.show.core.*
import com.iflytek.cyber.iot.show.core.accessibility.TouchAccessibility
import com.iflytek.cyber.iot.show.core.api.AlarmApi
import com.iflytek.cyber.iot.show.core.api.BannerApi
import com.iflytek.cyber.iot.show.core.impl.alarm.EvsAlarm
import com.iflytek.cyber.iot.show.core.impl.audioplayer.EvsAudioPlayer
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import com.iflytek.cyber.iot.show.core.model.Alert
import com.iflytek.cyber.iot.show.core.model.Banner
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.utils.ConnectivityUtils
import com.iflytek.cyber.iot.show.core.utils.VoiceButtonUtils
import com.iflytek.cyber.iot.show.core.widget.BatteryView
import com.iflytek.cyber.iot.show.core.widget.FadeInPageTransformer
import com.iflytek.cyber.iot.show.core.widget.InterceptFrameLayout
import me.yokeyword.fragmentation.ISupportFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


class MainFragment2 : BaseFragment(), PageScrollable {
    companion object {
        private const val ACTION_PREFIX = "com.iflytek.cyber.iot.show.core.action"
        private const val ACTION_REQUEST_BANNERS = "$ACTION_PREFIX.REQUEST_BANNERS"
        private const val ACTION_REQUEST_SCROLL = "$ACTION_PREFIX.REQUEST_SCROLL"

        const val ACTION_UPDATE_MESSAGE_NUM = "$ACTION_PREFIX.UPDATE_MESSAGE_NUM" // 更新留言板未读数量
        const val ACTION_SHOW_WIFI_ERROR = "$ACTION_PREFIX.SHOW_WIFI_ERROR"
        const val ACTION_DISMISS_WIFI_ERROR = "$ACTION_PREFIX.DISMISS_WIFI_ERROR"
        const val ACTION_SHOW_MUTE = "$ACTION_PREFIX.SHOW_MUTE"
        const val ACTION_DISMISS_MUTE = "$ACTION_PREFIX.DISMISS_MUTE"
        const val ACTION_SHOW_MICROPHONE_OFF = "$ACTION_PREFIX.SHOW_MICROPHONE_OFF"
        const val ACTION_DISMISS_MICROPHONE_OFF = "$ACTION_PREFIX.DISMISS_MICROPHONE_OFF"
        const val ACTION_OPEN_WIFI = "$ACTION_PREFIX.OPEN_WIFI"
        const val ACTION_OPEN_AUTH = "$ACTION_PREFIX.OPEN_AUTH"

        const val EXTRA_NUM = "num"

        private const val REQUEST_BANNERS_CODE = 20001
        private const val REQUEST_SCROLL_CODE = 20002
        private const val REQUEST_ALARM_CODE = 20003

        private val REQUEST_BANNERS_DELAY = TimeUnit.MINUTES.toMillis(10)
        private val REQUEST_SCROLL_DELAY = TimeUnit.SECONDS.toMillis(20)

        fun newInstance(): MainFragment2 {
            return MainFragment2().apply {
                arguments = bundleOf()
            }
        }
    }

    private var viewPager: ViewPager2? = null
    private val bannerList = mutableListOf<Banner>()
    private var ivAlarm: ImageView? = null
    private var ivCover: LottieAnimationView? = null

    private var clock: TextView? = null
    private var adapter: PagerAdapter? = null

    private var networkCallback: Any? = null // 不声明 NetworkCallback 的类，否则 L 以下会找不到类

    private var isLowPower = false

    private var needRequestNewBanners = false
    private var needRequestNewScroll = false

    private val batteryReceiver = object : SelfBroadcastReceiver(
        Intent.ACTION_BATTERY_CHANGED,
        Intent.ACTION_BATTERY_LOW,
        Intent.ACTION_BATTERY_OKAY,
        Intent.ACTION_POWER_CONNECTED,
        Intent.ACTION_POWER_DISCONNECTED
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    updateBattery(intent)
                }
                Intent.ACTION_BATTERY_LOW -> {

                }
                Intent.ACTION_BATTERY_OKAY -> {

                }
            }
        }
    }
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            if (state == ViewPager2.SCROLL_STATE_IDLE) {
                val position = viewPager?.currentItem

                // 无限滚动
                if (position == 0) {
                    viewPager?.setCurrentItem((adapter?.itemCount ?: 2) - 2, false)
                } else if (position == (adapter?.itemCount ?: 2) - 1) {
                    viewPager?.setCurrentItem(1, false)
                }
            }

            postNextScroll()
        }
    }
    private val timerReceiver = object : SelfBroadcastReceiver(
        ACTION_REQUEST_SCROLL,
        ACTION_REQUEST_BANNERS
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                ACTION_REQUEST_BANNERS -> {
                    requestBanners()
                }
                ACTION_REQUEST_SCROLL -> {
                    val state = viewPager?.scrollState
                    if (state == ViewPager2.SCROLL_STATE_IDLE && isSupportVisible) {
                        val animator = ValueAnimator.ofFloat(0f, 1f)
                        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
                            private var cacheValue = 0f
                            override fun onAnimationUpdate(animation: ValueAnimator) {
                                val value = animation.animatedValue as Float

                                viewPager?.let { viewPager ->
                                    if (viewPager.isFakeDragging && viewPager.isAttachedToWindow)
                                        viewPager.fakeDragBy(-viewPager.width * abs(value - cacheValue))
                                }

                                cacheValue = value
                            }
                        })
                        animator.duration = 2000
                        animator.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator?) {
                            }

                            override fun onAnimationEnd(animation: Animator?) {
                                viewPager?.endFakeDrag()
                                if (viewPager?.tag == animation) {
                                    viewPager?.tag = null
                                }
                            }

                            override fun onAnimationCancel(animation: Animator?) {
                                viewPager?.endFakeDrag()
                                if (viewPager?.tag == animation) {
                                    viewPager?.tag = null
                                }
                            }

                            override fun onAnimationStart(animation: Animator?) {
                                viewPager?.beginFakeDrag()
                            }

                        })
                        animator.start()

                        viewPager?.tag = animator
                    }

                    postNextScroll()
                }
            }
        }
    }
    private val statusUiReceiver = object : SelfBroadcastReceiver(
        ACTION_SHOW_MICROPHONE_OFF,
        ACTION_DISMISS_MICROPHONE_OFF,
        ACTION_SHOW_MUTE,
        ACTION_DISMISS_MUTE,
        ACTION_SHOW_WIFI_ERROR,
        ACTION_DISMISS_WIFI_ERROR,
        ACTION_UPDATE_MESSAGE_NUM,
        ACTION_OPEN_AUTH,
        ACTION_OPEN_WIFI
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                ACTION_SHOW_MICROPHONE_OFF -> {
                    view?.findViewById<View>(R.id.microphone)?.visibility = View.VISIBLE
                }
                ACTION_UPDATE_MESSAGE_NUM -> {
                    val num = intent.getIntExtra(EXTRA_NUM, -1)
                    if (num > 0) {
                        view?.findViewById<View>(R.id.message_container)?.visibility = View.VISIBLE
                        view?.findViewById<TextView>(R.id.message_num)?.let { textView ->
                            textView.text = num.toString()
                        }
                    } else {
                        view?.findViewById<View>(R.id.message_container)?.visibility = View.GONE
                    }
                }
                ACTION_SHOW_MUTE -> {
                    view?.findViewById<View>(R.id.speaker)?.visibility = View.VISIBLE
                }
                ACTION_SHOW_WIFI_ERROR -> {
                    view?.findViewById<View>(R.id.wifi_error)?.visibility = View.VISIBLE
                }
                ACTION_DISMISS_MUTE -> {
                    view?.findViewById<View>(R.id.speaker)?.visibility = View.GONE
                }
                ACTION_DISMISS_MICROPHONE_OFF -> {
                    view?.findViewById<View>(R.id.microphone)?.visibility = View.GONE
                }
                ACTION_DISMISS_WIFI_ERROR -> {
                    view?.findViewById<View>(R.id.wifi_error)?.visibility = View.GONE
                }
                ACTION_OPEN_WIFI -> {
                    val startMain = Intent(context, EvsLauncherActivity::class.java)
                    startMain.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startMain.action = EvsLauncherActivity.ACTION_OPEN_WLAN
                    startActivity(startMain)
                }
                ACTION_OPEN_AUTH -> {
                    val startMain = Intent(context, EvsLauncherActivity::class.java)
                    startMain.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startMain.action = EvsLauncherActivity.ACTION_OPEN_AUTH
                    startActivity(startMain)
                }
            }
        }
    }
    private val onConfigChangedListener = object : ConfigUtils.OnConfigChangedListener {
        override fun onConfigChanged(key: String, value: Any?) {
            when (key) {
                ConfigUtils.KEY_VOICE_WAKEUP_ENABLED -> {
                    view?.findViewById<View>(R.id.microphone)?.isVisible = value != true

                    val intent = Intent(context, EngineService::class.java)
                    intent.action = EngineService.ACTION_SET_WAKE_UP_ENABLED
                    intent.putExtra(EngineService.EXTRA_ENABLED, value == true)
                    context?.startService(intent)
                }
            }
        }
    }
    private val onVolumeChangedListener = object : EvsSpeaker.OnVolumeChangedListener {
        override fun onVolumeChanged(volume: Int, fromRemote: Boolean) {
            post {
                view?.findViewById<View>(R.id.speaker)?.isVisible = volume <= 0
            }
        }
    }
    private val connectStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            when (intent.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    // api 21 以上应使用 networkCallback
                    val connectivityManager =
                        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                    if (connectivityManager.activeNetworkInfo?.isConnected == true) {
                        ConnectivityUtils.checkIvsAvailable({
                            post {
                                view?.findViewById<View>(R.id.wifi_error)?.isVisible = false
                            }
                        }, { _, _ ->
                            post {
                                showNetworkError()
                            }
                        })
                    } else {
                        // 断开连接
                        showNetworkError()
                    }
                }
            }
        }
    }
    private val onAlarmUpdatedListener = object : Alarm.OnAlarmUpdatedListener {
        override fun onAlarmUpdated() {
            getAlerts()
        }
    }
    private val evsStatusReceiver = object : SelfBroadcastReceiver(
        EngineService.ACTION_EVS_CONNECTED,
        EngineService.ACTION_EVS_DISCONNECTED,
        EngineService.ACTION_SEND_REQUEST_FAILED
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                EngineService.ACTION_SEND_REQUEST_FAILED -> {
                    intent.getParcelableExtra<Result>(EngineService.EXTRA_RESULT)?.let { result ->
                        if (!result.isSuccessful) {
                            if (context != null && AuthDelegate.getAuthResponseFromPref(context!!) == null) {
                                showUnAuth()
                            } else if (result.code == Result.CODE_DISCONNECTED) {
                                ConnectivityUtils.checkNetworkAvailable({
                                    // 网络可用但 EVS 断连
                                    val disconnectNotification =
                                        Intent(context, FloatingService::class.java)
                                    disconnectNotification.action =
                                        FloatingService.ACTION_SHOW_NOTIFICATION
                                    disconnectNotification.putExtra(
                                        FloatingService.EXTRA_MESSAGE,
                                        getString(R.string.message_evs_disconnected)
                                    )
                                    disconnectNotification.putExtra(
                                        FloatingService.EXTRA_TAG,
                                        "network_error"
                                    )
                                    disconnectNotification.putExtra(
                                        FloatingService.EXTRA_ICON_RES,
                                        R.drawable.ic_default_error_white_40dp
                                    )
                                    disconnectNotification.putExtra(
                                        FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                        getString(R.string.i_got_it)
                                    )
                                    disconnectNotification.putExtra(
                                        FloatingService.EXTRA_KEEPING, true
                                    )
                                    context?.startService(disconnectNotification)
                                }, { _, _ ->
                                    // 网络不可用
                                    PromptManager.play(PromptManager.NETWORK_LOST)

                                    val networkErrorNotification =
                                        Intent(context, FloatingService::class.java)
                                    networkErrorNotification.action =
                                        FloatingService.ACTION_SHOW_NOTIFICATION
                                    networkErrorNotification.putExtra(
                                        FloatingService.EXTRA_MESSAGE, "网络连接异常，请重新设置"
                                    )
                                    networkErrorNotification.putExtra(
                                        FloatingService.EXTRA_TAG, "network_error"
                                    )
                                    networkErrorNotification.putExtra(
                                        FloatingService.EXTRA_POSITIVE_BUTTON_TEXT, "设置网络"
                                    )
                                    networkErrorNotification.putExtra(
                                        FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                                        ACTION_OPEN_WIFI
                                    )
                                    networkErrorNotification.putExtra(
                                        FloatingService.EXTRA_ICON_RES,
                                        R.drawable.ic_wifi_error_white_40dp
                                    )
                                    context?.startService(networkErrorNotification)
                                })
                            }
                        }
                    }
                }
                EngineService.ACTION_EVS_DISCONNECTED -> {
                    view?.findViewById<View>(R.id.wifi_error)?.isVisible = true
                }
                EngineService.ACTION_EVS_CONNECTED -> {
                    view?.findViewById<View>(R.id.wifi_error)?.isVisible = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        batteryReceiver.register(context)

        timerReceiver.register(context)

        statusUiReceiver.register(context)

        evsStatusReceiver.register(context)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val connectStateFilter = IntentFilter()
            @Suppress("DEPRECATION")
            connectStateFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            context?.registerReceiver(connectStateReceiver, connectStateFilter)
        } else {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network?) {
                    super.onAvailable(network)

                    ConnectivityUtils.checkIvsAvailable({
                        post {
                            view?.findViewById<View>(R.id.wifi_error)?.visibility = View.GONE
                        }
                    }, { _, _ ->
                        showNetworkError()
                    })
                }

                override fun onLost(network: Network?) {
                    super.onLost(network)

                    showNetworkError()
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)

            this.networkCallback = networkCallback
        }

        ConfigUtils.registerOnConfigChangedListener(onConfigChangedListener)

        EvsSpeaker.get(context).addOnVolumeChangedListener(onVolumeChangedListener)

        EvsAlarm.get(context).addOnAlarmUpdatedListener(onAlarmUpdatedListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = view.findViewById(R.id.background_pager)
        clock = view.findViewById(R.id.launcher_clock)
        ivCover = view.findViewById(R.id.iv_cover)
        view.findViewById<View>(R.id.cover_container)?.setOnClickListener {
            val playerInfo = ContentStorage.get().playerInfo
            if (playerInfo == null) {
//                launcher?.getService()?.getPlaybackController()
//                    ?.sendCommand(PlaybackController.Command.Resume)
                launcher?.getService()?.sendTextIn("我要听歌")
            } else {
                startPlayerInfo()
            }
        }

        val frameLayout = view.findViewById<InterceptFrameLayout>(R.id.main_content)
        frameLayout.onInterceptTouchListener = View.OnTouchListener { v, _ ->
            val context = v.context
            VoiceButtonUtils.lastTouchTime = System.currentTimeMillis()
//            SleepWorker.get(context).doTouchWork(context)
            false
        }

        val shadowColor = Color.parseColor("#19000000")
        val dy = resources.getDimensionPixelSize(R.dimen.dp_2).toFloat()
        val radius = resources.getDimensionPixelSize(R.dimen.dp_4).toFloat()
        clock?.setShadowLayer(radius, 0f, dy, shadowColor)

        viewPager?.let { viewPager ->
            val adapter = PagerAdapter(this)
            viewPager.setPageTransformer(FadeInPageTransformer())
            viewPager.adapter = adapter

            viewPager.registerOnPageChangeCallback(pageChangeCallback)

            this.adapter = adapter
        }

        view.findViewById<View>(R.id.skills).setOnClickListener {
            val context = it.context
            if (AuthDelegate.getAuthResponseFromPref(context) == null) {
                showUnAuth()
            } else if (ConnectivityUtils.isNetworkAvailable(context))
                start(SkillsFragment())
            else {
                PromptManager.play(PromptManager.NETWORK_LOST)

                val intent = Intent(context, FloatingService::class.java)
                intent.action = FloatingService.ACTION_SHOW_NOTIFICATION
                intent.putExtra(FloatingService.EXTRA_MESSAGE, "网络连接异常，请重新设置")
                intent.putExtra(FloatingService.EXTRA_TAG, "network_error")
                intent.putExtra(FloatingService.EXTRA_POSITIVE_BUTTON_TEXT, "设置网络")
                intent.putExtra(
                    FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                    ACTION_OPEN_WIFI
                )
                intent.putExtra(
                    FloatingService.EXTRA_ICON_RES,
                    R.drawable.ic_wifi_error_white_40dp
                )
                context?.startService(intent)
            }
        }

        view.findViewById<View>(R.id.microphone).setOnClickListener {
            start(MicrophoneFragment(), ISupportFragment.SINGLETOP)
        }
        view.findViewById<View>(R.id.speaker).setOnClickListener {
            start(VolumeFragment(), ISupportFragment.SINGLETOP)
        }
        view.findViewById<View>(R.id.wifi_error).setOnClickListener {
            start(WifiSettingsFragment(), ISupportFragment.SINGLETOP)
        }
        view.findViewById<View>(R.id.message_container).setOnClickListener {
            start(MessageBoardFragment(), ISupportFragment.SINGLETOP)
        }

        ivAlarm = view.findViewById(R.id.alarm)
        ivAlarm?.setOnClickListener {
            startAlarm()
        }

        view.findViewById<View>(R.id.found).setOnClickListener {
            val context = it.context
            if (AuthDelegate.getAuthResponseFromPref(context) == null) {
                showUnAuth()
            } else if (ConnectivityUtils.isNetworkAvailable(context))
                start(MediaFragment())
            else {
                PromptManager.play(PromptManager.NETWORK_LOST)

                val intent = Intent(context, FloatingService::class.java)
                intent.action = FloatingService.ACTION_SHOW_NOTIFICATION
                intent.putExtra(FloatingService.EXTRA_MESSAGE, "网络连接异常，请重新设置")
                intent.putExtra(FloatingService.EXTRA_TAG, "network_error")
                intent.putExtra(FloatingService.EXTRA_POSITIVE_BUTTON_TEXT, "设置网络")
                intent.putExtra(
                    FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                    ACTION_OPEN_WIFI
                )
                intent.putExtra(FloatingService.EXTRA_ICON_RES, R.drawable.ic_wifi_error_white_40dp)
                context?.startService(intent)
            }
        }

        post {
            updateCalendar(Calendar.getInstance())
        }

        val timerHandler = TimerHandler(this)
        timerHandler.sendEmptyMessageDelayed(0, 1000)

        getBannersFromPref()

        requestBanners()

        getAlerts()
    }

    private fun showUnAuth() {
        val disconnectNotification =
            Intent(context, FloatingService::class.java)
        disconnectNotification.action =
            FloatingService.ACTION_SHOW_NOTIFICATION
        disconnectNotification.putExtra(
            FloatingService.EXTRA_MESSAGE,
            getString(R.string.message_evs_auth_expired)
        )
        disconnectNotification.putExtra(
            FloatingService.EXTRA_TAG,
            "auth_error"
        )
        disconnectNotification.putExtra(
            FloatingService.EXTRA_ICON_RES,
            R.drawable.ic_default_error_white_40dp
        )
        disconnectNotification.putExtra(
            FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
            getString(R.string.re_auth)
        )
        disconnectNotification.putExtra(
            FloatingService.EXTRA_POSITIVE_BUTTON_ACTION, ACTION_OPEN_AUTH
        )
        disconnectNotification.putExtra(
            FloatingService.EXTRA_KEEPING, true
        )
        context?.startService(disconnectNotification)
    }

    fun setupCover() {
        if (context == null || ivCover == null) {
            return
        }

        if (EvsAudioPlayer.get(context).playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING) {
            ivCover?.playAnimation()
        } else {
            ivCover?.pauseAnimation()
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        val intent = Intent(context, FloatingService::class.java)
        intent.action = FloatingService.ACTION_SET_CONTROL_PANEL_ENABLED
        intent.putExtra(FloatingService.EXTRA_ENABLED, true)
        context?.startService(intent)

        setupCover()

        VoiceButtonUtils.requestRefresh()

        ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true).let {
            if (it) {
                view?.findViewById<View>(R.id.microphone)?.visibility = View.GONE
            } else {
                view?.findViewById<View>(R.id.microphone)?.visibility = View.VISIBLE
            }
        }
        if (EvsSpeaker.get(context).getCurrentVolume() == 0) {
            view?.findViewById<View>(R.id.speaker)?.visibility = View.VISIBLE
        } else {
            view?.findViewById<View>(R.id.speaker)?.visibility = View.GONE
        }

        ConnectivityUtils.checkIvsAvailable({
            post {
                view?.findViewById<View>(R.id.wifi_error)?.visibility = View.GONE
            }
        }, { _, _ ->
            post {
                view?.findViewById<View>(R.id.wifi_error)?.visibility = View.VISIBLE
            }
        })

        viewPager?.post {
            val current = viewPager?.currentItem ?: 0
            viewPager?.currentItem = current
        }

        if (needRequestNewBanners) {
            val intent = Intent(ACTION_REQUEST_BANNERS)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_BANNERS_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT
            )
            (context?.applicationContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                ?.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REQUEST_BANNERS_DELAY, pendingIntent
                )
        }
        if (needRequestNewScroll) {
            val scrollIntent = Intent(ACTION_REQUEST_SCROLL)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_SCROLL_CODE, scrollIntent, PendingIntent.FLAG_CANCEL_CURRENT
            )
            (context?.applicationContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                ?.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REQUEST_SCROLL_DELAY, pendingIntent
                )
        }

        TouchAccessibility.isMainFragment = true
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()

        if (viewPager?.isFakeDragging == true) {
            (viewPager?.tag as? Animator)?.cancel()

            viewPager?.endFakeDrag()
            viewPager?.currentItem = viewPager?.currentItem ?: 0
        }

        TouchAccessibility.isMainFragment = false
    }

    fun startAlarm() {
        startForResult(AlarmFragment(), REQUEST_ALARM_CODE)
    }

    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle) {
        super.onFragmentResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ALARM_CODE) {
            val shouldUpdate = data.getBoolean("shouldUpdateAlarm")
            if (shouldUpdate) {
                getAlerts()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
    }

    override fun onDestroy() {
        super.onDestroy()

        batteryReceiver.unregister(context)

        timerReceiver.unregister(context)

        statusUiReceiver.unregister(context)

        evsStatusReceiver.unregister(context)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            context?.unregisterReceiver(connectStateReceiver)
        } else {
            (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.let { connectivityManager ->
                    val networkCallback =
                        (this.networkCallback as? ConnectivityManager.NetworkCallback)
                            ?: return
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
        }

        ConfigUtils.unregisterOnConfigChangedListener(onConfigChangedListener)

        EvsSpeaker.get(context).removeOnVolumeChangedListener(onVolumeChangedListener)

        EvsAlarm.get(context).addOnAlarmUpdatedListener(onAlarmUpdatedListener)
    }

    override fun onBackPressedSupport(): Boolean {
        return true
    }

    private fun showNetworkError() {
        if (findFragment(WifiSettingsFragment::class.java) == null) {
            val intent = Intent(context, FloatingService::class.java)
            intent.action = FloatingService.ACTION_SHOW_NOTIFICATION
            intent.putExtra(FloatingService.EXTRA_MESSAGE, "网络连接异常，请重新设置")
            intent.putExtra(FloatingService.EXTRA_TAG, "network_error")
            intent.putExtra(FloatingService.EXTRA_POSITIVE_BUTTON_TEXT, "设置网络")
            intent.putExtra(FloatingService.EXTRA_POSITIVE_BUTTON_ACTION, ACTION_OPEN_WIFI)
            intent.putExtra(FloatingService.EXTRA_ICON_RES, R.drawable.ic_wifi_error_white_40dp)
            context?.startService(intent)
        }

        post {
            view?.findViewById<View>(R.id.wifi_error)?.visibility = View.VISIBLE
        }
    }

    private fun getBannersFromPref() {
        ConfigUtils.getString(ConfigUtils.KEY_BANNERS, null)?.let { json ->
            try {
                val jsonArray = JsonParser().parse(json).asJsonArray
                val gson = Gson()
                val list = mutableListOf<Banner>()
                for (jsonObject in jsonArray) {
                    val banner = gson.fromJson(jsonObject, Banner::class.java)
                    list.add(banner)
                }
                updatePagerUi(list)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } ?: run {
            updatePagerUi(
                listOf(
                    Banner(
                        UUID.randomUUID().toString(),
                        "file:///android_asset/wallpaper/wallpaper_a.jpg",
                        "",
                        resources.getStringArray(R.array.simple_guide_descriptions),
                        "",
                        ""
                    ),
                    Banner(
                        UUID.randomUUID().toString(),
                        "file:///android_asset/wallpaper/wallpaper_b.jpg",
                        "",
                        resources.getStringArray(R.array.simple_guide_descriptions),
                        "",
                        ""
                    ),
                    Banner(
                        UUID.randomUUID().toString(),
                        "file:///android_asset/wallpaper/wallpaper_c.jpg",
                        "",
                        resources.getStringArray(R.array.simple_guide_descriptions),
                        "",
                        ""
                    )
                )
            )
        }
    }

    private fun updatePagerUi(list: List<Banner>) {
        bannerList.clear()
        bannerList.addAll(list)
        adapter?.notifyDataSetChanged()

        if (bannerList.isNotEmpty()) {
            viewPager?.post {
                if (viewPager?.isFakeDragging == true)
                    viewPager?.endFakeDrag()
                viewPager?.setCurrentItem(1, false)
            }
        }

        postNextRequestBanners()

        postNextScroll()
    }

    private fun postNextRequestBanners() {
        val context = context
        if (context != null && isSupportVisible) {
            val intent = Intent(ACTION_REQUEST_BANNERS)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_BANNERS_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT
            )
            (context.applicationContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                ?.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REQUEST_BANNERS_DELAY, pendingIntent
                )
        } else {
            needRequestNewBanners = true
        }
    }

    private fun postNextScroll() {
        val context = context
        if (context != null && isSupportVisible) {
            val scrollIntent = Intent(ACTION_REQUEST_SCROLL)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_SCROLL_CODE, scrollIntent, PendingIntent.FLAG_CANCEL_CURRENT
            )
            (context.applicationContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                ?.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REQUEST_SCROLL_DELAY, pendingIntent
                )
        } else {
            needRequestNewScroll = true
        }
    }

    @Suppress("UNUSED_VARIABLE")
    private fun updateBattery(batteryStatus: Intent?) {
        (batteryStatus
            ?: context?.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            ))?.let { intent ->

            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            // How are we charging?
            val chargePlug: Int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC

            val isCharging: Boolean =
                (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
                    && chargePlug == BatteryManager.BATTERY_PLUGGED_USB

            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryLevel = level * 100 / scale

            if (isCharging) {
                view?.findViewById<View>(R.id.battery_container)?.visibility = View.GONE

                isLowPower = false
            } else {
                if (!isLowPower) {
                    if (level <= 20) {
                        val lowPowerNotification = Intent(context, FloatingService::class.java)
                        lowPowerNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
                        lowPowerNotification.putExtra(FloatingService.EXTRA_MESSAGE, "设备电量不足，请及时充电")
                        lowPowerNotification.putExtra(
                            FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                            "我知道了"
                        )
                        lowPowerNotification.putExtra(
                            FloatingService.EXTRA_ICON_RES,
                            R.drawable.ic_warning_white_40dp
                        )
                        context?.startService(lowPowerNotification)

                        isLowPower = true
                    }
                }
                view?.findViewById<View>(R.id.battery_container)?.visibility = View.VISIBLE
                view?.findViewById<BatteryView>(R.id.battery_view)?.let { batteryView ->
                    batteryView.level = batteryLevel
                }
                view?.findViewById<TextView>(R.id.tv_battery)?.let { tvBattery ->
                    tvBattery.text = getString(R.string.count_of_percent, batteryLevel)
                }
            }
        }
    }

    private fun requestBanners() {
        val context = context ?: return
        val api = CoreApplication.from(context).createApi(BannerApi::class.java)
        api?.getBanners()?.enqueue(object : Callback<List<Banner>> {
            override fun onFailure(call: Call<List<Banner>>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<List<Banner>>, response: Response<List<Banner>>) {
                if (response.isSuccessful) {
                    val body = response.body()

                    updatePagerUi(body ?: emptyList())

                    ConfigUtils.putString(ConfigUtils.KEY_BANNERS, Gson().toJson(bannerList))

                    needRequestNewBanners = false
                }
            }
        })
    }

    private fun updateCalendar(calendar: Calendar) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        clock?.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    override fun scrollToNext(): Boolean {
        viewPager?.let {
            if (it.currentItem < (adapter?.itemCount ?: 0) - 1) {
                it.setCurrentItem(it.currentItem + 1, true)
            }
            return true
        } ?: return false
    }

    override fun scrollToPrevious(): Boolean {
        viewPager?.let {
            if (it.currentItem > 0) {
                it.setCurrentItem(it.currentItem - 1, true)
            }
            return true
        } ?: return false
    }

    fun startPlayerInfo() {
        val topFragment = getTopFragment()
        if (topFragment is VideoFragment) {
            pop()
            extraTransaction().startDontHideSelf(PlayerInfoFragment2(), ISupportFragment.SINGLETOP)
        } else if (topFragment !is PlayerInfoFragment2) {
            extraTransaction().startDontHideSelf(PlayerInfoFragment2(), ISupportFragment.SINGLETOP)
        }
    }

    fun startVideoPlayer() {
        if (ContentStorage.get().playerInfo != null) {
            ContentStorage.get().savePlayInfo(null)
            ContentStorage.get().isMusicPlaying = false
            val intent = Intent(context, FloatingService::class.java).apply {
                action = FloatingService.ACTION_UPDATE_MUSIC
            }
            context?.startService(intent)
        }
        if (getTopFragment() is PlayerInfoFragment2) {
            popTo(PlayerInfoFragment2::class.java, true)
            start(VideoFragment(), ISupportFragment.SINGLETOP)
        } else {
            start(VideoFragment(), ISupportFragment.SINGLETOP)
        }
    }

    private fun getAlerts() {
        getAlarmApi()?.getAlerts()?.enqueue(object : Callback<ArrayList<Alert>> {
            override fun onFailure(call: Call<ArrayList<Alert>>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(
                call: Call<ArrayList<Alert>>,
                response: Response<ArrayList<Alert>>
            ) {
                if (response.isSuccessful) {
                    val items = response.body()
                    items?.let { ivAlarm?.isVisible = !it.isNullOrEmpty() }
                }
            }
        })
    }

    private inner class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int {
            return if (bannerList.isNotEmpty()) bannerList.size + 2 else 0
        }

        override fun getItemId(position: Int): Long {
            return when (position) {
                0 ->
                    "first${bannerList[0].id}".hashCode().toLong()
                itemCount - 1 ->
                    "end${bannerList[bannerList.size - 1].id}".hashCode().toLong()
                else ->
                    (bannerList[position - 1].id?.hashCode() ?: 0).toLong()
            }
        }

        override fun containsItem(itemId: Long): Boolean {
            bannerList.map {
                if (it.id?.hashCode()?.toLong() == itemId)
                    return true
            }
            return false
        }

        override fun createFragment(position: Int): Fragment {
            val fragment = BannerFragment()
            val arguments = Bundle()
            arguments.putParcelable(
                "banner", when (position) {
                    0 ->
                        bannerList[bannerList.size - 1]
                    itemCount - 1 ->
                        bannerList[0]
                    else ->
                        bannerList[position - 1]
                }
            )
            arguments.putInt("position", position)
            fragment.arguments = arguments
            return fragment
        }
    }

    private class TimerHandler internal constructor(
        mainFragment2: MainFragment2,
        private val reference: SoftReference<MainFragment2> =
            SoftReference(mainFragment2)
    ) : Handler() {

        override fun handleMessage(msg: Message) {
            val fragment = reference.get()
            if (fragment != null && !fragment.isDetached) {
                fragment.updateCalendar(Calendar.getInstance())
                sendEmptyMessageDelayed(0, 1000)
            }
        }
    }

    private fun getAlarmApi(): AlarmApi? {
        return if (launcher != null) {
            CoreApplication.from(launcher!!).createApi(AlarmApi::class.java)
        } else {
            null
        }
    }
}