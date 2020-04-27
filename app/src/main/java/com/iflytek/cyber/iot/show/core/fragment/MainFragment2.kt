package com.iflytek.cyber.iot.show.core.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.alibaba.fastjson.JSON
import com.drakeet.multitype.MultiTypeAdapter
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.iflytek.cyber.evs.sdk.agent.Alarm
import com.iflytek.cyber.evs.sdk.agent.AppAction
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.evs.sdk.utils.AppUtil
import com.iflytek.cyber.iot.show.core.*
import com.iflytek.cyber.iot.show.core.accessibility.TouchAccessibility
import com.iflytek.cyber.iot.show.core.adapter.*
import com.iflytek.cyber.iot.show.core.api.*
import com.iflytek.cyber.iot.show.core.impl.alarm.EvsAlarm
import com.iflytek.cyber.iot.show.core.impl.appaction.EvsAppAction
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import com.iflytek.cyber.iot.show.core.launcher.TemplateAppData
import com.iflytek.cyber.iot.show.core.model.*
import com.iflytek.cyber.iot.show.core.utils.*
import com.iflytek.cyber.iot.show.core.widget.BatteryView
import com.iflytek.cyber.iot.show.core.widget.InterceptFrameLayout
import com.iflytek.cyber.iot.show.core.widget.InterceptRecyclerView
import com.kk.taurus.playerbase.utils.NetworkUtils
import me.yokeyword.fragmentation.ISupportFragment
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.ref.SoftReference
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit


class MainFragment2 : BaseFragment(), PageScrollable {
    companion object {
        private const val ACTION_PREFIX = "com.iflytek.cyber.iot.show.core.action"
        private const val ACTION_REQUEST_BANNERS = "$ACTION_PREFIX.REQUEST_BANNERS"
        private const val ACTION_REQUEST_SCROLL = "$ACTION_PREFIX.REQUEST_SCROLL"

        const val RECOMMEND_CARD_MODE = "card_mode"

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

    private var timeTextView: AppCompatTextView? = null
    private var alarmImageView: ImageView? = null
    private val bannerList = mutableListOf<Banner>()
    private var ivAlarm: ImageView? = null

    private var networkCallback: Any? = null // 不声明 NetworkCallback 的类，否则 L 以下会找不到类

    private var isLowPower = false
    private var isWifiError = false
    private var shouldShowLoading = true

    private var needRequestNewBanners = false
    private var needRequestNewScroll = false

    private lateinit var templateList: InterceptRecyclerView

    private lateinit var multiTypeAdapter: MultiTypeAdapter

    private lateinit var mainViewBinder: MainViewBinder
    private val recommendFourItemViewBinder = RecommendFourItemViewHolder()
    private val recommendFourItemViewBinder2 = RecommendFourItemViewHolder2()
    private val recommendFiveItemViewBinder = RecommendFiveItemViewHolder()
    private val recommendSevenItemViewBinder = RecommendSevenItemViewHolder()
    private val recommendEightItemViewBinder = RecommendEightItemViewHolder()
    private val recommendEightItemViewBinder2 = RecommendEightItemViewHolder2()
    private val recommendAdviceViewBinder = RecommendAdviceViewHolder()
    private val recommendSixItemViewHolder = RecommendSixItemViewHolder()
    private lateinit var recommendSettingsViewBinder: RecommendSettingsViewBinder

    private var deskData = DeskData(null, null)

    private var bannerScrollHandler = Handler() //TODO:这并不好，后面再优化
    private var requestBannerHandler = Handler()

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

    private val bannerScrollRunnable = Runnable {
        mainViewBinder.requestScroll(templateList)
        postNextScroll()
    }

    private var requestBannerRunnable = Runnable {
        requestBanners()
        postNextRequestBanners()
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
                    mainViewBinder.requestScroll(templateList)
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
                    isWifiError = true
                    recommendSettingsViewBinder.switchCanClickable = false
                    if (multiTypeAdapter.items.isNotEmpty()) {
                        multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                    }
                }
                ACTION_DISMISS_MUTE -> {
                    view?.findViewById<View>(R.id.speaker)?.visibility = View.GONE
                }
                ACTION_DISMISS_MICROPHONE_OFF -> {
                    view?.findViewById<View>(R.id.microphone)?.visibility = View.GONE
                }
                ACTION_DISMISS_WIFI_ERROR -> {
                    isWifiError = false
                    view?.findViewById<View>(R.id.wifi_error)?.visibility = View.GONE
                    recommendSettingsViewBinder.switchCanClickable = true
                    if (multiTypeAdapter.items.isNotEmpty()) {
                        multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                    }
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
        override fun onReceive(context: Context, intent: Intent) = @Suppress("DEPRECATION")
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                // api 21 以上应使用 networkCallback
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                if (connectivityManager.activeNetworkInfo?.isConnected == true) {
                    ConnectivityUtils.checkIvsAvailable({
                        post {
                            isWifiError = false
                            view?.findViewById<View>(R.id.wifi_error)?.isVisible = false
                            recommendSettingsViewBinder.switchCanClickable = true
                            if (multiTypeAdapter.items.isNotEmpty()) {
                                multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                            }
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
            else -> {
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
                                    PromptManager.play(PromptManager.CONNECTING_PLEASE_WAIT)
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
                    isWifiError = true
                    view?.findViewById<View>(R.id.wifi_error)?.isVisible = true
                    recommendSettingsViewBinder.switchCanClickable = false
                    if (multiTypeAdapter.items.isNotEmpty()) {
                        multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                    }
                }
                EngineService.ACTION_EVS_CONNECTED -> {
                    isWifiError = false
                    view?.findViewById<View>(R.id.wifi_error)?.isVisible = false
                    recommendSettingsViewBinder.switchCanClickable = true
                    if (multiTypeAdapter.items.isNotEmpty()) {
                        multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        batteryReceiver.register(context)

        //timerReceiver.register(context)

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

                    view?.post {
                        if (deskData.loadingTemplate != null) {
                            getDeskRecommend()
                        }

                        if (multiTypeAdapter.items.size > 1) {
                            multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                        }
                    }

                    ConnectivityUtils.checkIvsAvailable({
                        post {
                            isWifiError = false
                            view?.findViewById<View>(R.id.wifi_error)?.visibility = View.GONE
                            recommendSettingsViewBinder.switchCanClickable = true
                            if (multiTypeAdapter.items.isNotEmpty()) {
                                multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                            }
                        }
                    }, { _, _ ->
                        showNetworkError()
                    })
                }

                override fun onLost(network: Network?) {
                    super.onLost(network)

                    view?.post {
                        if (multiTypeAdapter.items.size > 1) {
                            multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                        }
                    }

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

        EventBus.getDefault().register(this)
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

        timeTextView = view.findViewById(R.id.time_text)

        val frameLayout = view.findViewById<InterceptFrameLayout>(R.id.main_content)
        frameLayout.onInterceptTouchListener = View.OnTouchListener { v, _ ->
            val context = v.context
            VoiceButtonUtils.lastTouchTime = System.currentTimeMillis()
//            SleepWorker.get(context).doTouchWork(context)
            false
        }

        view.findViewById<View>(R.id.apps)?.setOnClickListener {
            start(LauncherFragment2(), ISupportFragment.SINGLETOP)
        }

        view.findViewById<View>(R.id.microphone).setOnClickListener {
            start(MicrophoneFragment(), ISupportFragment.SINGLETOP)
        }
        view.findViewById<View>(R.id.speaker).setOnClickListener {
            start(VolumeFragment(), ISupportFragment.SINGLETOP)
        }

        view.findViewById<View>(R.id.message_container).setOnClickListener {
            start(MessageBoardFragment(), ISupportFragment.SINGLETOP)
        }

        ivAlarm = view.findViewById(R.id.alarm)
        ivAlarm?.setOnClickListener {
            startAlarm()
        }

        view.findViewById<View>(R.id.wifi_error).setOnClickListener {
            start(WifiSettingsFragment(), ISupportFragment.SINGLETOP)
        }

        alarmImageView = view.findViewById(R.id.alarm)
        alarmImageView?.setOnClickListener {
            startAlarm()
        }

        post {
            updateCalendar(Calendar.getInstance())
        }

        val timerHandler = TimerHandler(this)
        timerHandler.sendEmptyMessageDelayed(0, 1000)

        templateList = view.findViewById(R.id.template_list)

        mainViewBinder = MainViewBinder()
        mainViewBinder.setRecyclerView(templateList)
        mainViewBinder.setMainViewItemClickListener(object :
            MainViewBinder.MainViewItemClickListener {
            override fun onIqiyiFrameClick() {
                val context = context ?: return
                val packageManager = context.packageManager
                val appInfo = AppUtil.getAppInfo(context, "com.qiyi.video.speaker")
                if (appInfo == null) {
                    Toast.makeText(
                        context,
                        getString(R.string.app_action_app_not_found),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    packageManager?.getLaunchIntentForPackage("com.qiyi.video.speaker")
                        ?.let { intent ->
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                }
            }

            override fun onFavFrameClick() {
                if (context != null && !NetworkUtils.isNetConnected(context)) {
                    PromptManager.play(PromptManager.NETWORK_LOST)
                    return
                }
                start(CollectionFragment())
            }

            override fun onMusicFrameClick() {
                if (context != null && !NetworkUtils.isNetConnected(context)) {
                    PromptManager.play(PromptManager.NETWORK_LOST)
                    return
                }

                val playerInfo = ContentStorage.get().playerInfo
                if (playerInfo == null) {
                    Thread { launcher?.getService()?.sendTextIn("我要听歌") }.start()
                } else {
                    startPlayerInfo()
                }
            }

            override fun onBannerItemClick(banner: Banner) {
                val intent = Intent(context, EngineService::class.java)
                intent.action = EngineService.ACTION_SEND_TEXT_IN
                intent.putExtra(EngineService.EXTRA_QUERY, banner.content)
                context?.startService(intent)
            }
        })

        recommendSettingsViewBinder = RecommendSettingsViewBinder { isChecked ->
            if (isChecked) {
                getRecommend(DeskApi.MODEL_CHILD, "已将儿童内容优先展示")
            } else {
                getRecommend(DeskApi.MODEL_ADULT, "已取消将儿童内容优先展示")
            }
        }

        multiTypeAdapter = MultiTypeAdapter()
        multiTypeAdapter.register(MainTemplate::class.java).to(
            mainViewBinder,
            MainLoadingBinder(),
            recommendSettingsViewBinder
        ).withKotlinClassLinker { _, item ->
            when (item.type) {
                1 -> MainViewBinder::class
                1000 -> MainLoadingBinder::class
                1001 -> RecommendSettingsViewBinder::class
                else -> MainViewBinder::class
            }
        }
        val onMultiTypeItemClickListener = object : OnMultiTypeItemClickListener {
            override fun onItemClick(
                parent: ViewGroup,
                itemView: View,
                position: Int,
                subPosition: Int
            ) {
                if (context != null && !NetworkUtils.isNetConnected(context) || isWifiError) {
                    PromptManager.play(PromptManager.NETWORK_LOST)
                    Toast.makeText(context, "网络连接异常，请重新设置", Toast.LENGTH_SHORT).show()
                    return
                }

                (getInitialItemList()[position] as? DeskRecommend)?.let { deskRecommend ->
                    if (deskRecommend.items != null) {
                        val items = filterItems(deskRecommend.items)
                        executeItem(deskRecommend, items[subPosition])
                    }
                }
            }
        }
        val onMoreClickListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                if (context != null && !NetworkUtils.isNetConnected(context) || isWifiError) {
                    PromptManager.play(PromptManager.NETWORK_LOST)
                    Toast.makeText(context, "网络连接异常，请重新设置", Toast.LENGTH_SHORT).show()
                    return
                }
                (getInitialItemList()[position] as? DeskRecommend)?.let { deskRecommend ->
                    deskRecommend.more?.let { deskRecommendMore ->
                        executeMore(deskRecommendMore)
                    }
                }
            }
        }
        val onCardRefreshListener = object : OnItemClickListener {
            override fun onItemClick(parent: ViewGroup, itemView: View, position: Int) {
                if ((context != null && !NetworkUtils.isNetConnected(context)) || isWifiError) {
                    PromptManager.play(PromptManager.NETWORK_LOST)
                    Toast.makeText(context, "网络连接异常，请重新设置", Toast.LENGTH_SHORT).show()
                    return
                }
                if (position < 0) {
                    return
                }
                (getInitialItemList()[position] as? DeskRecommend)?.let { deskRecommend ->
                    if (deskRecommend.id != null) {
                        refreshCard(deskRecommend.id)
                    }
                }
            }
        }

        recommendFourItemViewBinder2.onOpenWebPageListener =
            object : RecommendFourItemViewHolder2.OnOpenWebPageListener {
                override fun onOpenWebPage(url: String) {
                    val fragment = WebViewFragment()
                    fragment.arguments = bundleOf(Pair("url", url))
                    start(fragment)
                }
            }

        recommendAdviceViewBinder.onItemClickListener = onMultiTypeItemClickListener
        recommendAdviceViewBinder.onCardRefreshListener = onCardRefreshListener
        recommendFourItemViewBinder.onItemClickListener = onMultiTypeItemClickListener
        recommendFourItemViewBinder.onMoreClickListener = onMoreClickListener
        recommendFourItemViewBinder.onCardRefreshListener = onCardRefreshListener
        recommendFourItemViewBinder2.onItemClickListener = onMultiTypeItemClickListener
        recommendFourItemViewBinder2.onCardRefreshListener = onCardRefreshListener
        recommendFiveItemViewBinder.onItemClickListener = onMultiTypeItemClickListener
        recommendFiveItemViewBinder.onMoreClickListener = onMoreClickListener
        recommendFiveItemViewBinder.onCardRefreshListener = onCardRefreshListener
        recommendSevenItemViewBinder.onItemClickListener = onMultiTypeItemClickListener
        recommendSevenItemViewBinder.onMoreClickListener = onMoreClickListener
        recommendSevenItemViewBinder.onCardRefreshListener = onCardRefreshListener
        recommendEightItemViewBinder.onItemClickListener = onMultiTypeItemClickListener
        recommendEightItemViewBinder.onMoreClickListener = onMoreClickListener
        recommendEightItemViewBinder.onCardRefreshListener = onCardRefreshListener
        recommendEightItemViewBinder2.onItemClickListener = onMultiTypeItemClickListener
        recommendEightItemViewBinder2.onMoreClickListener = onMoreClickListener
        recommendEightItemViewBinder2.onCardRefreshListener = onCardRefreshListener
        recommendSixItemViewHolder.onItemClickListener = onMultiTypeItemClickListener
        recommendSixItemViewHolder.onMoreClickListener = onMoreClickListener
        recommendSixItemViewHolder.onCardRefreshListener = onCardRefreshListener
        multiTypeAdapter.register(DeskRecommend::class.java).to(
            recommendFourItemViewBinder,
            recommendFourItemViewBinder2,
            recommendFiveItemViewBinder,
            recommendSevenItemViewBinder,
            recommendEightItemViewBinder,
            recommendEightItemViewBinder2,
            recommendAdviceViewBinder,
            recommendSixItemViewHolder
        ).withKotlinClassLinker { _, item ->
            when (item.type) {
                1 -> RecommendFourItemViewHolder::class
                2 -> RecommendEightItemViewHolder::class
                3 -> RecommendAdviceViewHolder::class
                4 -> RecommendEightItemViewHolder2::class
                5 -> RecommendFiveItemViewHolder::class
                6 -> RecommendFourItemViewHolder2::class
                7 -> RecommendSevenItemViewHolder::class
                8 -> RecommendSixItemViewHolder::class
                else -> RecommendFourItemViewHolder::class
            }
        }
        templateList.adapter = multiTypeAdapter

        getBannersFromPref()

        requestBanners()

        getAlerts()

        getDeskRecommend()
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
        mainViewBinder.updateMusicCard(templateList)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onViewPagerScrollAction(scrollAction: MainViewBinder.ScrollAction) {
        if (scrollAction.action == 0) {
            postNextScroll()
        } else if (scrollAction.action == 1) {
            postNextRequestBanners()
        } else if (scrollAction.action == 2) {
            setupCover()
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

            val intent = Intent(context, EngineService::class.java)
            intent.action = EngineService.ACTION_SET_WAKE_UP_ENABLED
            intent.putExtra(EngineService.EXTRA_ENABLED, it)
            context?.startService(intent)
        }
        if (EvsSpeaker.get(context).getCurrentVolume() == 0) {
            view?.findViewById<View>(R.id.speaker)?.visibility = View.VISIBLE
        } else {
            view?.findViewById<View>(R.id.speaker)?.visibility = View.GONE
        }

        ConnectivityUtils.checkIvsAvailable({
            post {
                isWifiError = false
                view?.findViewById<View>(R.id.wifi_error)?.visibility = View.GONE
                recommendSettingsViewBinder.switchCanClickable = true
                if (multiTypeAdapter.items.isNotEmpty()) {
                    multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                }
            }
        }, { _, _ ->
            post {
                isWifiError = true
                view?.findViewById<View>(R.id.wifi_error)?.visibility = View.VISIBLE
                recommendSettingsViewBinder.switchCanClickable = false
                if (multiTypeAdapter.items.isNotEmpty()) {
                    multiTypeAdapter.notifyItemChanged(multiTypeAdapter.items.size - 1)
                }
            }
        })

        if (needRequestNewBanners) {
            /*val intent = Intent(ACTION_REQUEST_BANNERS)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_BANNERS_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT
            )
            (context?.applicationContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                ?.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REQUEST_BANNERS_DELAY, pendingIntent
                )*/
            requestBannerHandler.removeCallbacksAndMessages(null)
            requestBannerHandler.postDelayed(requestBannerRunnable, REQUEST_BANNERS_DELAY)
        }
        if (needRequestNewScroll) {
            /*val scrollIntent = Intent(ACTION_REQUEST_SCROLL)
            val pendingIntent = PendingIntent.getBroadcast(
                context, REQUEST_SCROLL_CODE, scrollIntent, PendingIntent.FLAG_CANCEL_CURRENT
            )

            (context?.applicationContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                ?.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REQUEST_SCROLL_DELAY, pendingIntent
                )*/
            bannerScrollHandler.removeCallbacksAndMessages(null)
            bannerScrollHandler.postDelayed(bannerScrollRunnable, REQUEST_SCROLL_DELAY)
        }

        TouchAccessibility.isMainFragment = true
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()

        requestBannerHandler.removeCallbacksAndMessages(null)
        bannerScrollHandler.removeCallbacksAndMessages(null)
        needRequestNewBanners = true
        needRequestNewScroll = true

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

    override fun onDestroy() {
        super.onDestroy()

        EventBus.getDefault().unregister(this)

        batteryReceiver.unregister(context)

        //timerReceiver.unregister(context)

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
                updatePagerUi()
                setupTemplate(MainTemplate(1, list))
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } ?: run {
            val list = listOf(
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
            setupTemplate(MainTemplate(1, list))
            updatePagerUi()
        }
    }

    private fun updatePagerUi() {
        postNextRequestBanners()
        postNextScroll()
    }

    private fun postNextRequestBanners() {
        val context = context
        if (context != null && isSupportVisible) {
            /*val intent = Intent(ACTION_REQUEST_BANNERS)
            val pendingIntent = ContextWrapper.getBroadcastAsUser(
                context, REQUEST_SCROLL_CODE, intent, PendingIntent.FLAG_CANCEL_CURRENT,
                "CURRENT"
            )
            (context.applicationContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                ?.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + REQUEST_BANNERS_DELAY, pendingIntent
                )*/
            requestBannerHandler.removeCallbacksAndMessages(null)
            requestBannerHandler.postDelayed(requestBannerRunnable, REQUEST_BANNERS_DELAY)
        } else {
            requestBannerHandler.removeCallbacksAndMessages(null)
            needRequestNewBanners = true
        }
    }

    private fun postNextScroll() {
        val context = context
        if (context != null && isSupportVisible) {
            /* val scrollIntent = Intent(ACTION_REQUEST_SCROLL)
             val pendingIntent = ContextWrapper.getBroadcastAsUser(
                 context, REQUEST_SCROLL_CODE, scrollIntent, PendingIntent.FLAG_CANCEL_CURRENT,
                 "CURRENT"
             )
             (context.applicationContext?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
                 ?.set(
                     AlarmManager.RTC_WAKEUP,
                     System.currentTimeMillis() + REQUEST_SCROLL_DELAY, pendingIntent
                 )*/
            bannerScrollHandler.removeCallbacksAndMessages(null)
            bannerScrollHandler.postDelayed(bannerScrollRunnable, REQUEST_SCROLL_DELAY)
        } else {
            bannerScrollHandler.removeCallbacksAndMessages(null)
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

                        view?.findViewById<BatteryView>(R.id.battery_view)?.let { batteryView ->
                            if (isAdded && context != null) {
                                batteryView.contentColor =
                                    ContextCompat.getColor(context!!, R.color.tablet_red)
                            }
                        }
                        isLowPower = true
                    } else {
                        view?.findViewById<BatteryView>(R.id.battery_view)?.let { batteryView ->
                            if (isAdded && context != null) {
                                batteryView.contentColor =
                                    ContextCompat.getColor(context!!, R.color.tablet_grey_500)
                            }
                        }
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

    private fun setupTemplate(main: MainTemplate) {
        deskData.mainTemplate = main
        if (shouldShowLoading) {
            deskData.loadingTemplate = MainTemplate(1000, emptyList())
        }
        deskData.settingTemplate = MainTemplate(1001, emptyList())
        multiTypeAdapter.items = getInitialItemList()
        multiTypeAdapter.notifyDataSetChanged()
        shouldShowLoading = false
    }

    private fun requestBanners() {
        val context = context ?: return
        val api = CoreApplication.from(context).createApi(BannerApi::class.java)
        api?.loadBanners()?.enqueue(object : Callback<List<Banner>> {
            override fun onFailure(call: Call<List<Banner>>, t: Throwable) {
                t.printStackTrace()
            }

            override fun onResponse(call: Call<List<Banner>>, response: Response<List<Banner>>) {
                if (response.isSuccessful) {
                    val body = response.body()

                    setupTemplate(MainTemplate(1, body ?: emptyList()))
                    updatePagerUi()

                    ConfigUtils.putString(ConfigUtils.KEY_BANNERS, Gson().toJson(body))

                    needRequestNewBanners = false
                }
            }
        })
    }

    private fun updateCalendar(calendar: Calendar) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        //clock?.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        timeTextView?.text = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    override fun scrollToNext(): Boolean {
        mainViewBinder.getViewPager2(templateList)?.let {
            if (it.currentItem < mainViewBinder.getBannerCount(templateList) - 1) {
                it.setCurrentItem(it.currentItem + 1, true)
            }
            return true
        } ?: return false
    }

    override fun scrollToPrevious(): Boolean {
        mainViewBinder.getViewPager2(templateList)?.let {
            if (it.currentItem > 0) {
                it.setCurrentItem(it.currentItem - 1, true)
            }
            return true
        } ?: return false
    }

    fun startPlayerInfo() {
        val topFragment = getTopFragment()
        if (topFragment is VideoFragment) {
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
                    //items?.let { ivAlarm?.isVisible = !it.isNullOrEmpty() }
                    items?.let { alarmImageView?.isVisible = !it.isNullOrEmpty() }
                }
            }
        })
    }

    private fun refreshCard(cardId: Int) {
        getDeskApi()?.refreshCard(cardId)?.enqueue(object : Callback<DeskRecommend> {
            override fun onFailure(call: Call<DeskRecommend>, t: Throwable) {
                t.printStackTrace()
                if (t is UnknownHostException || t is ConnectException ||
                    t is SocketTimeoutException
                ) {
                    isWifiError = true
                    val wifiError = view?.findViewById<View>(R.id.wifi_error)
                    if (wifiError?.isVisible == false) {
                        wifiError.isVisible = true
                    }
                }
            }

            override fun onResponse(call: Call<DeskRecommend>, response: Response<DeskRecommend>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        deskData.deskRecommends?.forEachIndexed { _, deskRecommend ->
                            deskRecommend.takeIf { deskRecommend.id == cardId }?.let {
                                deskRecommend.background = body.background
                                deskRecommend.more = body.more
                                deskRecommend.title = body.title
                                deskRecommend.titleColor = body.titleColor
                                deskRecommend.type = body.type
                                deskRecommend.items?.forEachIndexed { index, item ->
                                    item.background = body.items?.get(index)?.background
                                    item.cover = body.items?.get(index)?.cover
                                    item.metadata = body.items?.get(index)?.metadata
                                    item.subtitle = body.items?.get(index)?.subtitle
                                    item.subtitleColor = body.items?.get(index)?.subtitleColor
                                    item.title = body.items?.get(index)?.title
                                    item.titleColor = body.items?.get(index)?.titleColor
                                    item.type = body.items?.get(index)?.type
                                }
                            }
                        }
                        multiTypeAdapter.items = getInitialItemList()
                        multiTypeAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun executeMore(more: DeskRecommendMore) {
        when (more.type) {
            1 -> {
                val text = more.metadata?.getString("text")
                if (!text.isNullOrEmpty()) {
                    val textIn = Intent(context, EngineService::class.java)
                    textIn.action = EngineService.ACTION_SEND_TEXT_IN
                    textIn.putExtra(EngineService.EXTRA_QUERY, text)
                    context?.startService(textIn)
                }
            }
            2 -> {
                val albumId = more.metadata?.get("item_id")
                (albumId as? Number)?.let { id ->
                    try {
                        val albumIdString = id.toInt().toString()
                        if (albumIdString.isNotEmpty())
                            start(SongListFragment.instance(albumIdString, "", null))
                    } catch (t: NumberFormatException) {

                    }
                } ?: run {
                    val albumIdString = albumId?.toString()
                    if (!albumIdString.isNullOrEmpty())
                        start(SongListFragment.instance(albumIdString, "", null))
                }
            }
            3 -> {
                val mediaId = more.metadata?.getString("media_id")
                val itemId = more.metadata?.getIntValue("item_id")
                playMusic(itemId, mediaId)
            }
            4 -> {
                val templateApp =
                    JSON.parseObject(more.metadata.toString(), TemplateApp::class.java)
                templateApp?.let {
                    when (templateApp.template) {
                        TemplateApp.TEMPLATE_TEMPLATE_1 -> {
                            start(TemplateApp1Fragment.newInstance(templateApp))
                        }
                        TemplateApp.TEMPLATE_TEMPLATE_2 -> {
                            start(TemplateApp2Fragment.newInstance(templateApp))
                        }
                        TemplateApp.TEMPLATE_XMLR -> {
                            start(TemplateAppXmlyFragment.newInstance(templateApp))
                        }
                        else -> {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    private fun executeItem(parentItem: DeskRecommend, item: DeskRecommendItem) {
        when (item.type) {
            1 -> {
                val text = item.metadata?.getString("text")
                if (!text.isNullOrEmpty()) {
                    val textIn = Intent(context, EngineService::class.java)
                    textIn.action = EngineService.ACTION_SEND_TEXT_IN
                    textIn.putExtra(EngineService.EXTRA_QUERY, text)
                    textIn.putExtra(EngineService.EXTRA_WITH_TTS, parentItem.type == 3)
                    context?.startService(textIn)
                }
            }
            2 -> {
                val albumId = item.metadata?.get("item_id")
                (albumId as? Number)?.let { id ->
                    try {
                        val albumIdString = id.toInt().toString()
                        if (albumIdString.isNotEmpty())
                            start(SongListFragment.instance(albumIdString, "", null))
                    } catch (t: NumberFormatException) {

                    }
                } ?: run {
                    val albumIdString = albumId?.toString()
                    if (!albumIdString.isNullOrEmpty())
                        start(SongListFragment.instance(albumIdString, "", null))
                }
            }
            3 -> {
                val mediaId = item.metadata?.getString("media_id")
                val itemId = item.metadata?.getIntValue("item_id")
                playMusic(itemId, mediaId)
            }
            4 -> {
                val templateApp =
                    JSON.parseObject(item.metadata.toString(), TemplateApp::class.java)
                when (templateApp.type) {
                    TemplateAppData.TYPE_TEMPLATE -> {
                        setTemplate(templateApp)
                    }
                    TemplateAppData.TYPE_H5_APP -> {
                        val webViewFragment = WebViewFragment().apply {
                            arguments = bundleOf(Pair(WebViewFragment.EXTRA_URL, templateApp.url))
                        }
                        start(webViewFragment)
                    }
                    TemplateAppData.TYPE_SKILL -> {
                        val textIn = Intent(context, EngineService::class.java)
                        textIn.action = EngineService.ACTION_SEND_TEXT_IN
                        textIn.putExtra(EngineService.EXTRA_QUERY, templateApp.textIn)
                        context?.startService(textIn)
                    }
                    else -> {
                    }
                }
            }
            5 -> {
                item.metadata?.let {
                    EvsAppAction.get(context)
                        .executeAction(it) { isSuccess, errorLevel, succeedActionId ->
                            if (!isSuccess) {
                                when (errorLevel) {
                                    AppAction.FAILURE_LEVEL_ACTION_UNSUPPORTED -> {
                                        Toast.makeText(
                                            context,
                                            getString(R.string.app_action_unsupported),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    AppAction.FAILURE_LEVEL_APP_NOT_FOUND -> {
                                        Toast.makeText(
                                            context,
                                            getString(R.string.app_action_app_not_found),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    AppAction.FAILURE_LEVEL_INTERNAL_ERROR -> {
                                        Toast.makeText(
                                            context,
                                            getString(R.string.app_action_internal_error),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                }
            }
        }
    }

    private fun setTemplate(templateApp: TemplateApp) {
        when (templateApp.template) {
            TemplateApp.TEMPLATE_TEMPLATE_1 -> {
                start(TemplateApp1Fragment.newInstance(templateApp))
            }
            TemplateApp.TEMPLATE_TEMPLATE_2 -> {
                start(TemplateApp2Fragment.newInstance(templateApp))
            }
            TemplateApp.TEMPLATE_XMLR -> {
                start(TemplateAppXmlyFragment.newInstance(templateApp))
            }
            TemplateApp.TEMPLATE_TEMPLATE_3 -> {
                playTemplate3(templateApp)
            }
            else -> {
                // ignore
            }
        }
    }

    private fun playTemplate3(templateApp: TemplateApp) {
        val json = com.alibaba.fastjson.JSONObject()
        json["appName"] = templateApp.name
        val requestBody = RequestBody.create(
            MediaType.parse("application/json"),
            json.toString()
        )
        getAppApi()?.playTemplate3(requestBody)?.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (!isAdded || context == null) {
                    return
                }

                if (response.isSuccessful) {
                    Toast.makeText(context, "开始播放", Toast.LENGTH_SHORT).show()
                } else {
                    response.errorBody()?.let { errorBody ->
                        val errorString = errorBody.string()

                        val errorJson = JSONObject(errorString)

                        if (errorJson.has("message")) {
                            Toast.makeText(
                                context,
                                errorJson.optString("message"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show()
                        }
                        errorBody.close()
                    } ?: run {
                        Toast.makeText(context, "播放失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun getPackageName(item: DeskRecommendItem): String? {
        try {
            if (item.metadata?.containsKey("package_name") == false) {
                return null
            }
            return item.metadata?.getString("package_name")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun filterItems(items: ArrayList<DeskRecommendItem>?): ArrayList<DeskRecommendItem> {
        val newItems = ArrayList<DeskRecommendItem>()
        items?.forEach {
            if (!it.shouldHide) {
                newItems.add(it)
            }
        }
        return newItems
    }

    /**
     * 在线教育板块展示的应用如果本地都没装则不展示在线教育板块
     */
    private fun filterRecommends(deskRecommends: List<DeskRecommend>): List<DeskRecommend> {
        val recommends = mutableListOf<DeskRecommend>()
        deskRecommends.forEach { deskRecommend ->
            deskRecommend.items?.forEach { item ->
                if (item.type == 5) {
                    val packageName = getPackageName(item)
                    if (!packageName.isNullOrEmpty()) {
                        val appInfo = AppUtil.getAppInfo(launcher!!, packageName)
                        item.shouldHide = appInfo == null
                    } else {
                        item.shouldHide = false
                    }
                } else {
                    item.shouldHide = false
                }
            }
            val newItems = filterItems(deskRecommend.items)
            if (newItems.size > 0) {
                recommends.add(deskRecommend)
            }
        }
        return recommends
    }

    private fun getInitialItemList(): MutableList<Any> {
        val deskData = deskData
        val mainTemplate = deskData.mainTemplate
        val loadingTemplate = deskData.loadingTemplate
        val deskRecommends = deskData.deskRecommends
        val settingTemplate = deskData.settingTemplate
        val list = mutableListOf<Any>()
        if (mainTemplate != null) list.add(mainTemplate)
        if (loadingTemplate != null) list.add(loadingTemplate)
        if (deskRecommends != null) {
            val recommends = filterRecommends(deskRecommends)
            list.addAll(recommends)
        }
        if (settingTemplate != null) list.add(settingTemplate)
        return list
    }

    private fun playMusic(audioId: Int?, itemId: String?) {
        if (audioId == null) {
            return
        }
        val body = MusicBody(audioId, itemId, null)
        getMediaApi()?.playMusic(body)?.enqueue(object : Callback<String> {
            override fun onFailure(call: Call<String>, t: Throwable) {
                t.printStackTrace()

                if (t is UnknownHostException || t is ConnectException) {
                    val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                    intent.putExtra(
                        EngineService.EXTRA_RESULT,
                        Result(Result.CODE_DISCONNECTED, null)
                    )
                    context?.sendBroadcast(intent)
                }
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "开始播放", Toast.LENGTH_SHORT).show()
                } else {
                }
            }
        })
    }

    private fun getDeskRecommend() {
        val mode = ConfigUtils.getInt(RECOMMEND_CARD_MODE, DeskApi.MODEL_ADULT)
        getRecommend(mode)
    }

    private fun getRecommend(mode: Int, toast: String? = null) {
        if (multiTypeAdapter.items.isNotEmpty()) {
            recommendSettingsViewBinder.canSwitchClickable(
                templateList,
                multiTypeAdapter.items.size - 1,
                true
            )
        }
        getDeskApi()?.getRecommend(mode)
            ?.enqueue(object : Callback<List<DeskRecommend>> {
                override fun onFailure(call: Call<List<DeskRecommend>>, t: Throwable) {
                    t.printStackTrace()
                    if (multiTypeAdapter.items.isNotEmpty()) {
                        recommendSettingsViewBinder.canSwitchClickable(
                            templateList,
                            multiTypeAdapter.items.size - 1,
                            false
                        )
                    }
                }

                override fun onResponse(
                    call: Call<List<DeskRecommend>>,
                    response: Response<List<DeskRecommend>>
                ) {
                    if (multiTypeAdapter.items.isNotEmpty()) {
                        recommendSettingsViewBinder.canSwitchClickable(
                            templateList,
                            multiTypeAdapter.items.size - 1,
                            false
                        )
                    }

                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            ConfigUtils.putInt(RECOMMEND_CARD_MODE, mode)
                            if (!toast.isNullOrEmpty() && isAdded && context != null) {
                                Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
                            }
                            deskData.loadingTemplate = null
                            deskData.deskRecommends = body
                            multiTypeAdapter.items = getInitialItemList()
                            multiTypeAdapter.notifyDataSetChanged()
                        }
                    } else {

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

    private fun getDeskApi(): DeskApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(DeskApi::class.java)
    }

    private fun getMediaApi(): MediaApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(MediaApi::class.java)
    }

    private fun getAppApi(): AppApi? {
        val context = context ?: return null
        return CoreApplication.from(context).createApi(AppApi::class.java)
    }

    class DeskData(
        var mainTemplate: MainTemplate?,
        var deskRecommends: List<DeskRecommend>?,
        var loadingTemplate: MainTemplate? = null,
        var settingTemplate: MainTemplate? = null
    )
}