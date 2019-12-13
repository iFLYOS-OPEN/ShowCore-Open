package com.iflytek.cyber.iot.show.core

import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.*
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityManager
import android.widget.*
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.google.gson.JsonParser
import com.iflytek.cyber.evs.sdk.RequestCallback
import com.iflytek.cyber.evs.sdk.agent.Alarm
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.agent.PlaybackController
import com.iflytek.cyber.evs.sdk.agent.Template
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.iot.show.core.EvsLauncherActivity.Companion.ACTION_START_PLAYER
import com.iflytek.cyber.iot.show.core.accessibility.TouchAccessibility
import com.iflytek.cyber.iot.show.core.impl.audioplayer.EvsAudioPlayer
import com.iflytek.cyber.iot.show.core.impl.playback.EvsPlaybackController
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.impl.screen.EvsScreen
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import com.iflytek.cyber.iot.show.core.impl.template.EvsTemplate
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.PlayerInfoPayload
import com.iflytek.cyber.iot.show.core.template.BodyTemplateView1
import com.iflytek.cyber.iot.show.core.template.BodyTemplateView3
import com.iflytek.cyber.iot.show.core.template.TemplateViewBuilder
import com.iflytek.cyber.iot.show.core.utils.*
import com.iflytek.cyber.iot.show.core.widget.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.SoftReference
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.*

class FloatingService : Service() {
    private var recognizeView: View? = null
    private var notificationView: View? = null
    private var sideIndicatorView: View? = null
    private var slidePanel: View? = null
    private var musicPanel: RelativeLayout? = null
    private var ivPrevious: ImageView? = null
    private var ivNext: ImageView? = null
    private var playPause: ImageView? = null
    private var musicTitle: TextView? = null
    private var musicCover: ImageView? = null
    private var tvQuit: TextView? = null
    private var templateContainer: FrameLayout? = null
    private var alarmView: View? = null
    private var volumeAnimator: Animator? = null
    private var animatingVolumeTo = 0f
    private var backAnimationView: LottieAnimationView? = null
    private var backgroundRecognizeView: View? = null
    private var voiceButtonView: View? = null
    private var isControlPanelEnabled = false
    private var isBackgroundRecognize = false
    private var currentTemplateId: String? = null
    private var microphoneAnimator: Animator? = null

    private var isShowRecognize = false
        set(value) {
            field = value

            EvsSpeaker.get(baseContext).isVisualFocusGain = isVisualFocusGain
            EvsSpeaker.get(baseContext).refreshNativeAudioFocus(baseContext)
        }
    private var isShowTemplate = false
        set(value) {
            field = value

            EvsSpeaker.get(baseContext).isVisualFocusGain = isVisualFocusGain
            EvsSpeaker.get(baseContext).refreshNativeAudioFocus(baseContext)
        }
    private val isVisualFocusGain: Boolean
        get() {
            return isShowRecognize || isShowTemplate
        }

    private var isPanelOpened = false

    private var timerHandler: TimerHandler? = null

    private var isAlarmPlaying = false

    @Suppress("DEPRECATION")
    private val connectionReceiver = object : SelfBroadcastReceiver(
        ConnectivityManager.CONNECTIVITY_ACTION
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val network = intent.getParcelableExtra<NetworkInfo>(
                        ConnectivityManager.EXTRA_NETWORK_INFO
                    )
                    val detailed = network.detailedState

                    slidePanel?.let { panel ->
                        val tvSsid: TextView = panel.findViewById(R.id.tv_ssid)
                        if (detailed == NetworkInfo.DetailedState.CONNECTED) {
                            WifiUtils.getConnectedSsid(baseContext)?.let { ssid ->
                                if (ssid.isNotEmpty()) {
                                    tvSsid.text = ssid
                                } else {
                                    tvSsid.setText(R.string.wlan)
                                }
                            } ?: run {
                                tvSsid.setText(R.string.wlan)
                            }
                        } else {
                            tvSsid.setText(R.string.wlan)
                        }
                    }
                }
            }
        }
    }
    private var networkCallback: Any? = null // 不声明 NetworkCallback 的类，否则 L 以下会找不到类
    private val hideVoiceButtonHandler = HideVoiceButtonHandler(this)
    private val clearCardHandler = ClearCardHandler(this)
    private val clearNotificationHandler = ClearNotificationHandler(this)
    private val clearSideIndicatorHandler = ClearSideIndicatorHandler(this)
    private val panelCloseCountHandler = PanelCloseCountHandler(this)
    private val brightnessObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean, uri: Uri) {
            if (BRIGHTNESS_MODE_URI == uri) {
                val currentBrightness = BrightnessUtils.getBrightness(baseContext)
                val mode = BrightnessUtils.getBrightnessMode(baseContext)

                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    slidePanel?.findViewById<BoxedHorizontal>(R.id.brightness_slider)
                        ?.let { slider ->
                            slider.isEnable = true
                        }
                } else {
                    slidePanel?.findViewById<BoxedHorizontal>(R.id.brightness_slider)
                        ?.let { slider ->
                            if (!slider.isPressed) {
                                slider.isEnable = true
                                slider.value = currentBrightness
                            }
                        }
                    slidePanel?.findViewById<LottieAnimationView>(R.id.brightness_icon)
                        ?.let { icon ->
                            icon.progress = currentBrightness / 100f
                        }
                }
            } else if (BRIGHTNESS_URI == uri) {
                val currentBrightness = BrightnessUtils.getBrightness(baseContext)

                slidePanel?.findViewById<BoxedHorizontal>(R.id.brightness_slider)?.let { slider ->
                    if (!slider.isPressed) {
                        slider.isEnable = true
                        slider.value = currentBrightness
                    }
                }
                slidePanel?.findViewById<LottieAnimationView>(R.id.brightness_icon)?.let { icon ->
                    icon.progress = currentBrightness / 100f
                }
            }
        }
    }
    private val volumeChangeReceiver =
        object : SelfBroadcastReceiver(VOLUME_CHANGED_ACTION) {
            override fun onReceiveAction(action: String, intent: Intent) {
                when (action) {
                    VOLUME_CHANGED_ACTION -> {
                        val type = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
                        val volume = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1)
                        if (volume == -1)
                            return
                        if (type == AudioManager.STREAM_MUSIC) {
//                            EvsSpeaker.get(baseContext).updateCurrentVolume()
                            slidePanel?.findViewById<BoxedHorizontal>(R.id.volume_slider)
                                ?.let { slider ->

                                    if (!slider.isPressed) {
                                        updateVolume()
                                    }
                                }
                        }
                    }
                }
            }
        }
    private val alarmStateReceiver = object : SelfBroadcastReceiver(
        EngineService.ACTION_ALARM_STATE_CHANGED
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                EngineService.ACTION_ALARM_STATE_CHANGED -> {
                    val alarmId = intent.getStringExtra(EngineService.EXTRA_ALARM_ID)

                    when (intent.getStringExtra(EngineService.EXTRA_ALARM_STATE)
                        ?.let { Alarm.AlarmState.valueOf(it) }) {
                        Alarm.AlarmState.Stopped -> {
                            isAlarmPlaying = false
                            templateContainer?.getChildAt(0)?.let { templateView ->
                                clearCardHandler.startCount(templateView, ALARM_TEMPLATE_TIMEOUT)
                            }

                            clearAlarmActiveView()
                        }
                        Alarm.AlarmState.Started -> {
                            isAlarmPlaying = true

                            renderAlarmActiveView()
                        }
                    }
                }
            }
        }
    }
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
    private val volumeChangedListener = object : EvsSpeaker.OnVolumeChangedListener {
        override fun onVolumeChanged(volume: Int, fromRemote: Boolean) {
            if (fromRemote) {
                showSideIndicator(R.raw.animation_volume_level, volume)
            }
        }
    }
    private val brightnessChangedCallback = object : EvsScreen.BrightnessChangedCallback {
        override fun onBrightnessChanged(brightness: Int) {
            if (!isPanelOpened) {
                showSideIndicator(R.raw.animation_brightness_level, brightness)
            }
        }
    }
    private val configChangedListener = object : ConfigUtils.OnConfigChangedListener {
        override fun onConfigChanged(key: String, value: Any?) {
            when (key) {
                ConfigUtils.KEY_VOICE_WAKEUP_ENABLED -> {
                    slidePanel?.let { panel ->
                        if (value == true) {
                            enableMicrophoneAnimation()
                        } else {
                            disableMicrophoneAnimation()
                        }
                    }
                }
            }
        }
    }
    private val wifiStateReceiver = object : SelfBroadcastReceiver(
        WifiManager.WIFI_STATE_CHANGED_ACTION
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiManager.WIFI_STATE_DISABLED) {
                        disableWifiIcon()
                    } else if (state == WifiManager.WIFI_STATE_ENABLED) {
                        enableWifiIcon()
                    }
                }
            }
        }
    }
    private val audioPlayerStateListener = object : AudioPlayer.MediaStateChangedListener {

        override fun onStarted(player: AudioPlayer, type: String, resourceId: String) {
            when (type) {
                AudioPlayer.TYPE_TTS -> {
                    val currentTemplateId = currentTemplateId ?: return
                    val requestId = RequestIdMap.findRequestByTts(resourceId)
                    val templateRequest =
                        RequestIdMap.findRequestByTemplate(currentTemplateId)
                    if (requestId != templateRequest)
                        return
                    (templateContainer?.getChildAt(0) as? BodyTemplateView1)?.startBodyAnimation()
                }
                AudioPlayer.TYPE_RING -> {
                    isAlarmPlaying = true
                }
                AudioPlayer.TYPE_PLAYBACK -> {
                    updateMusicPanel(ContentStorage.get().playerInfo)
                }
            }
        }

        override fun onResumed(player: AudioPlayer, type: String, resourceId: String) {
            if (type == AudioPlayer.TYPE_PLAYBACK) {
                updateMusicPanel(ContentStorage.get().playerInfo)
            }
        }

        override fun onPaused(player: AudioPlayer, type: String, resourceId: String) {
            when (type) {
                AudioPlayer.TYPE_TTS -> {
                    val currentTemplateId = currentTemplateId ?: return
                    val requestId = RequestIdMap.findRequestByTts(resourceId)
                    val templateRequest =
                        RequestIdMap.findRequestByTemplate(currentTemplateId)
                    if (requestId != templateRequest)
                        return
                    templateContainer?.getChildAt(0)?.let { templateView ->
                        (templateView as? BodyTemplateView1)?.stopBodyAnimation()
                        clearCardHandler.startCount(templateView, STATIC_TEMPLATE_TIMEOUT)
                    }
                }
                AudioPlayer.TYPE_RING -> {
                    val currentTemplateId = currentTemplateId ?: return
                    val requestId = RequestIdMap.findRequestByTts(resourceId)
                    val templateRequest =
                        RequestIdMap.findRequestByTemplate(currentTemplateId)
                    if (requestId != templateRequest)
                        return
                    isAlarmPlaying = false
                    templateContainer?.getChildAt(0)?.let { templateView ->
                        clearCardHandler.startCount(templateView, ALARM_TEMPLATE_TIMEOUT)
                    }
                }
                AudioPlayer.TYPE_PLAYBACK -> {
                    updateMusicPanel(ContentStorage.get().playerInfo)
                }
            }
        }

        override fun onStopped(player: AudioPlayer, type: String, resourceId: String) {
            when (type) {
                AudioPlayer.TYPE_TTS -> {
                    val currentTemplateId = currentTemplateId ?: return
                    val requestId = RequestIdMap.findRequestByTts(resourceId)
                    val templateRequest =
                        RequestIdMap.findRequestByTemplate(currentTemplateId)
                    if (requestId != templateRequest)
                        return
                    templateContainer?.getChildAt(0)?.let { templateView ->
                        (templateView as? BodyTemplateView1)?.stopBodyAnimation()
                        clearCardHandler.startCount(templateView, STATIC_TEMPLATE_TIMEOUT)
                    }
                }
                AudioPlayer.TYPE_RING -> {
                    val currentTemplateId = currentTemplateId ?: return
                    val requestId = RequestIdMap.findRequestByTts(resourceId)
                    val templateRequest =
                        RequestIdMap.findRequestByTemplate(currentTemplateId)
                    if (requestId != templateRequest)
                        return
                    isAlarmPlaying = false
                    templateContainer?.getChildAt(0)?.let { templateView ->
                        clearCardHandler.startCount(templateView, ALARM_TEMPLATE_TIMEOUT)
                    }
                }
                AudioPlayer.TYPE_PLAYBACK -> {
                    updateMusicPanel(null)
                }
            }
        }

        override fun onCompleted(player: AudioPlayer, type: String, resourceId: String) {
            when (type) {
                AudioPlayer.TYPE_TTS -> {
                    val currentTemplateId = currentTemplateId ?: return
                    val requestId = RequestIdMap.findRequestByTts(resourceId)
                    val templateRequest =
                        RequestIdMap.findRequestByTemplate(currentTemplateId)
                    if (requestId != templateRequest)
                        return
                    templateContainer?.getChildAt(0)?.let { templateView ->
                        (templateView as? BodyTemplateView1)?.stopBodyAnimation()
                        clearCardHandler.startCount(templateView, STATIC_TEMPLATE_TIMEOUT)
                    }
                }
                AudioPlayer.TYPE_RING -> {

                    val currentTemplateId = currentTemplateId ?: return
                    val requestId = RequestIdMap.findRequestByTts(resourceId)
                    val templateRequest =
                        RequestIdMap.findRequestByTemplate(currentTemplateId)
                    if (requestId != templateRequest)
                        return
                    isAlarmPlaying = false
                    templateContainer?.getChildAt(0)?.let { templateView ->
                        clearCardHandler.startCount(templateView, ALARM_TEMPLATE_TIMEOUT)
                    }
                }
                AudioPlayer.TYPE_PLAYBACK -> {
                }
            }
        }

        override fun onPositionUpdated(
            player: AudioPlayer,
            type: String,
            resourceId: String,
            position: Long
        ) {
            if (type == AudioPlayer.TYPE_TTS)
                (templateContainer?.getChildAt(0) as? BodyTemplateView1)?.let {
                    if (it.isBodyAnimating()) {
                        it.updateBodyPosition(position)
                    }
                }
        }

        override fun onError(
            player: AudioPlayer,
            type: String,
            resourceId: String,
            errorCode: String
        ) {
            ContentStorage.get().isMusicPlaying = false
        }

    }

    companion object {
        private const val ACTION_PREFIX = "com.iflytek.cyber.iot.show.core"
        const val ACTION_SHOW_RECOGNIZE = "$ACTION_PREFIX.SHOW_RECOGNIZE"
        const val ACTION_DISMISS_RECOGNIZE = "$ACTION_PREFIX.DISMISS_RECOGNIZE"
        const val ACTION_INTERMEDIATE_TEXT = "$ACTION_PREFIX.INTERMEDIATE_TEXT"
        const val ACTION_UPDATE_VOLUME = "$ACTION_PREFIX.UPDATE_VOLUME"
        const val ACTION_INIT_OVERLAY = "$ACTION_PREFIX.INIT_CONTROL_PANEL"
        const val ACTION_INIT_CONFIG = "$ACTION_PREFIX.INIT_CONFIG"
        const val ACTION_RENDER_TEMPLATE = "$ACTION_PREFIX.RENDER_TEMPLATE"
        const val ACTION_CLEAR_TEMPLATE = "$ACTION_PREFIX.CLEAR_TEMPLATE"
        const val ACTION_SET_CONTROL_PANEL_ENABLED = "$ACTION_PREFIX.SET_CONTROL_PANEL_ENABLED"
        const val ACTION_UPDATE_MUSIC = "$ACTION_PREFIX.UPDATE_MUSIC"
        const val ACTION_UPDATE_MUSIC_CONTROL_BUTTON = "$ACTION_PREFIX.UPDATE_MUSIC_CONTROL_BUTTON"
        const val ACTION_SHOW_NOTIFICATION = "$ACTION_PREFIX.SHOW_NOTIFICATION"
        const val ACTION_DISMISS_NOTIFICATION = "$ACTION_PREFIX.DISMISS_NOTIFICATION"
        const val ACTION_SET_BACKGROUND_RECOGNIZE = "$ACTION_PREFIX.SET_BACKGROUND_RECOGNIZE"

        const val EXTRA_TEXT = "text"
        const val EXTRA_ENABLED = "enabled"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_PAYLOAD = "payload"
        const val EXTRA_EXPECT_REPLY = "expect_reply"

        const val EXTRA_MESSAGE = "message"
        const val EXTRA_TAG = "tag"
        const val EXTRA_TYPE = "type"
        const val EXTRA_ICON_RES = "icon_res"
        const val EXTRA_POSITIVE_BUTTON_TEXT = "positive_button_text"
        const val EXTRA_POSITIVE_BUTTON_ACTION = "positive_button_action"
        const val EXTRA_NEGATIVE_BUTTON_TEXT = "negative_button_text"
        const val EXTRA_NEGATIVE_BUTTON_ACTION = "negative_button_text"
        const val EXTRA_KEEPING = "keeping"

        private val BRIGHTNESS_MODE_URI =
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE)
        private val BRIGHTNESS_URI = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)

        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val STREAM_MUTE_CHANGED_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val EXTRA_VOLUME_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"

        private const val TAG = "FloatingService"

        private const val VOLUME_0 = 0f
        private const val VOLUME_1 = 0.34444f
        private const val VOLUME_2 = 0.66667f
        private const val VOLUME_3 = 1f

        private const val STATIC_TEMPLATE_TIMEOUT = 20 * 1000L
        private const val ALARM_TEMPLATE_TIMEOUT = 20 * 1000L
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        try {
            val cr = contentResolver
            cr.registerContentObserver(BRIGHTNESS_MODE_URI, false, brightnessObserver)
            cr.registerContentObserver(BRIGHTNESS_URI, false, brightnessObserver)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        volumeChangeReceiver.register(this)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            connectionReceiver.register(this)
        } else {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network?) {
                    super.onAvailable(network)

                    slidePanel?.post {
                        slidePanel?.let { panel ->
                            val tvSsid: TextView = panel.findViewById(R.id.tv_ssid)
                            WifiUtils.getConnectedSsid(baseContext)?.let { ssid ->
                                if (ssid.isNotEmpty()) {
                                    tvSsid.text = ssid
                                } else {
                                    tvSsid.setText(R.string.wlan)
                                }
                            } ?: run {
                                tvSsid.setText(R.string.wlan)
                            }
                        }
                    }
                }

                override fun onLinkPropertiesChanged(
                    network: Network?,
                    linkProperties: LinkProperties?
                ) {
                    super.onLinkPropertiesChanged(network, linkProperties)

                    slidePanel?.post {
                        slidePanel?.let { panel ->
                            val tvSsid: TextView = panel.findViewById(R.id.tv_ssid)
                            WifiUtils.getConnectedSsid(baseContext)?.let { ssid ->
                                if (ssid.isNotEmpty()) {
                                    tvSsid.text = ssid
                                } else {
                                    tvSsid.setText(R.string.wlan)
                                }
                            } ?: run {
                                tvSsid.setText(R.string.wlan)
                            }
                        }
                    }
                }

                override fun onLost(network: Network?) {
                    super.onLost(network)
                    slidePanel?.post {
                        slidePanel?.let { panel ->
                            val tvSsid: TextView = panel.findViewById(R.id.tv_ssid)
                            tvSsid.setText(R.string.wlan)
                        }
                    }
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)

            this.networkCallback = networkCallback
        }

        startTimer()

        alarmStateReceiver.register(this)
        wifiStateReceiver.register(this)

        batteryReceiver.register(this)

        EvsSpeaker.get(this).addOnVolumeChangedListener(volumeChangedListener)

        EvsScreen.get(this).registerBrightnessChangedCallback(brightnessChangedCallback)

        ConfigUtils.registerOnConfigChangedListener(configChangedListener)

        ConfigUtils.registerOnConfigChangedListener(VoiceButtonUtils)
        VoiceButtonUtils.onVoiceButtonVisibleCallback =
            object : VoiceButtonUtils.OnVoiceButtonVisibleCallback {
                override fun onShow() {
                    showVoiceButton()
                }

                override fun onDisappear() {
                    hideVoiceButton()
                }

                override fun onScreenTouched() {
                    strongVoiceButton()

                    if (isServiceEnabled())
                        hideVoiceButtonHandler.postScrollEvent()
                }
            }

        EvsAudioPlayer.get(this).addListener(audioPlayerStateListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INIT_OVERLAY -> {
                if (slidePanel == null)
                    initOverlay()
            }
            ACTION_INIT_CONFIG -> {
                // 初始化 WiFi 名称
                val tvSsid: TextView? = slidePanel?.findViewById(R.id.tv_ssid)

                WifiUtils.getConnectedSsid(baseContext)?.let { ssid ->
                    if (ssid.isNotEmpty())
                        tvSsid?.text = ssid
                    else
                        tvSsid?.setText(R.string.wlan)
                } ?: run {
                    tvSsid?.setText(R.string.wlan)
                }

                // 初始化语音唤醒状态
                val isMicrophoneEnabled =
                    ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
                if (isMicrophoneEnabled) {
                    enableMicrophoneAnimation()
                } else {
                    disableMicrophoneAnimation()
                }
            }
            ACTION_SHOW_RECOGNIZE -> {
                val isExpectReply = intent.getBooleanExtra(EXTRA_EXPECT_REPLY, false)
                if (isPanelOpened) {
                    dismissPanel()
                }
                if (isAlarmPlaying) {
                    clearCurrentTemplate()
                    clearAlarmActiveView()
                }
                if (!isBackgroundRecognize)
                    showRecognizeView()
            }
            ACTION_DISMISS_RECOGNIZE -> {
                if (!isBackgroundRecognize)
                    dismissRecognizeView()
            }
            ACTION_INTERMEDIATE_TEXT -> {
                updateIntermediateText(intent.getStringExtra(EXTRA_TEXT))
            }
            ACTION_UPDATE_VOLUME -> {
                updateRecordVolume(intent.getFloatExtra(EXTRA_VOLUME, 0f).toDouble())
            }
            ACTION_RENDER_TEMPLATE -> {
                val payload = intent.getStringExtra(EXTRA_PAYLOAD)
                renderTemplate(payload)
            }
            ACTION_CLEAR_TEMPLATE -> {
                clearCurrentTemplate()
            }
            ACTION_SET_CONTROL_PANEL_ENABLED -> {
                isControlPanelEnabled = intent.getBooleanExtra(EXTRA_ENABLED, false)
                dismissPanel()
            }
            ACTION_UPDATE_MUSIC -> {
                updateMusicPanel(ContentStorage.get().playerInfo)
            }
            ACTION_UPDATE_MUSIC_CONTROL_BUTTON -> {
                val firstPlayId = intent.getStringExtra("firstPlayId")
                val endPlayId = intent.getStringExtra("endPlayId")
                updateMusicControlButton(firstPlayId, endPlayId)
            }
            ACTION_SHOW_NOTIFICATION -> {
                val message = intent.getStringExtra(EXTRA_MESSAGE)
                val tag = intent.getStringExtra(EXTRA_TAG)
                val iconRes = intent.getIntExtra(EXTRA_ICON_RES, -1)
                val positiveButtonText = intent.getStringExtra(EXTRA_POSITIVE_BUTTON_TEXT)
                val positiveButtonAction = intent.getStringExtra(EXTRA_POSITIVE_BUTTON_ACTION)
                val negativeButtonText = intent.getStringExtra(EXTRA_NEGATIVE_BUTTON_TEXT)
                val negativeButtonAction = intent.getStringExtra(EXTRA_NEGATIVE_BUTTON_ACTION)
                val keeping = intent.getBooleanExtra(EXTRA_KEEPING, false)

                showNotification(
                    if (iconRes != -1)
                        ContextCompat.getDrawable(this, iconRes)
                    else
                        null,
                    message,
                    if (positiveButtonText.isNullOrEmpty())
                        null
                    else
                        Pair(positiveButtonText, View.OnClickListener {
                            dismissNotification()
                            positiveButtonAction?.let {
                                val broadcast = Intent(it)
                                intent.extras?.let { extras ->
                                    broadcast.putExtras(extras)
                                }
                                sendBroadcast(broadcast)
                            }
                        }),
                    if (negativeButtonText.isNullOrEmpty())
                        null
                    else
                        Pair(negativeButtonText, View.OnClickListener {
                            dismissNotification()
                            negativeButtonAction?.let {
                                val broadcast = Intent(it)
                                intent.extras?.let { extras ->
                                    broadcast.putExtras(extras)
                                }
                                sendBroadcast(broadcast)
                            }
                        }),
                    keeping,
                    tag
                )
            }
            ACTION_DISMISS_NOTIFICATION -> {
                val tag = intent.getStringExtra(EXTRA_TAG)
                if (tag.isNullOrEmpty()) {
                    dismissNotification()
                } else {
                    val currentTag = notificationView?.findViewById<TextView>(
                        R.id.notification_message
                    )?.tag.toString()
                    if (currentTag == tag) {
                        dismissNotification()
                    }
                }
            }
            ACTION_SET_BACKGROUND_RECOGNIZE -> {
                isBackgroundRecognize = intent.getBooleanExtra(EXTRA_ENABLED, false)

                if (isBackgroundRecognize) {
                    dismissRecognizeView()

                    showBackgroundRecognizeView()
                } else {
                    dismissBackgroundRecognizeView()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()

        timerHandler?.removeCallbacksAndMessages(null)
        contentResolver.unregisterContentObserver(brightnessObserver)

        volumeChangeReceiver.unregister(this)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            connectionReceiver.unregister(this)
        } else {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.let { connectivityManager ->
                    val networkCallback =
                        (this.networkCallback as? ConnectivityManager.NetworkCallback)
                            ?: return
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
        }
        alarmStateReceiver.unregister(this)
        wifiStateReceiver.unregister(this)

        batteryReceiver.unregister(this)

        EvsSpeaker.get(this).removeOnVolumeChangedListener(volumeChangedListener)

        EvsScreen.get(this).unregisterBrightnessChangedCallback(brightnessChangedCallback)

        EvsAudioPlayer.get(this).removeListener(audioPlayerStateListener)

        ConfigUtils.unregisterOnConfigChangedListener(configChangedListener)

        ConfigUtils.unregisterOnConfigChangedListener(VoiceButtonUtils)
    }

    private fun showSideIndicator(animatorRes: Int, value: Int) {
        sideIndicatorView?.post {
            sideIndicatorView?.let { sideIndicatorView ->
                var hideFirst = false
                if (sideIndicatorView.visibility == View.VISIBLE) {
                    if ((sideIndicatorView.tag as? Int) != animatorRes
                        || sideIndicatorView.findViewById<BoxedVertical>(
                            R.id.indicator_slide_bar
                        ).value != value
                    ) {
                        hideFirst = true
                        hideSideIndicator(Runnable { showSideIndicator(animatorRes, value) })
                    }
                }
                if (!hideFirst) {
                    sideIndicatorView.tag = animatorRes

                    sideIndicatorView.visibility = View.VISIBLE
                    sideIndicatorView.translationX = sideIndicatorView.width.toFloat()
                    sideIndicatorView.animate()
                        .translationX(0f)
                        .setDuration(500)
                        .start()

                    val boxedVertical: BoxedVertical =
                        sideIndicatorView.findViewById(R.id.indicator_slide_bar)
                    val animationView: LottieAnimationView =
                        sideIndicatorView.findViewById(R.id.indicator_icon)

                    boxedVertical.setValue(value)
                    animationView.setAnimation(animatorRes)

                    if (animatorRes == R.raw.animation_volume_level) {
                        val target = when (value) {
                            0 -> {
                                0f
                            }
                            in 1..33 -> {
                                0.33f
                            }
                            in 34..67 -> {
                                0.67f
                            }
                            else -> {
                                1f
                            }
                        }
                        animationView.progress = 0f
                        animationView.postDelayed({
                            animationView.progress = target
                        }, 300)
                    } else {
                        animationView.progress = 0f
                        animationView.postDelayed({
                            animationView.progress = 1f * value / 100
                        }, 300)
                    }

                    clearSideIndicatorHandler.startCount()
                }
            }
        }
    }

    private fun hideSideIndicator(endRunnable: Runnable? = null) {
        sideIndicatorView?.let { sideIndicatorView ->
            sideIndicatorView.animate()
                .translationX(sideIndicatorView.width.toFloat())
                .setDuration(500)
                .withEndAction {
                    sideIndicatorView.visibility = View.GONE
                    endRunnable?.run()
                }
                .start()
        }
    }

    private fun clearCurrentTemplate() {
        TouchAccessibility.isBodyTemplate = false

        isShowTemplate = false

        EvsTemplate.get().isOtherTemplateFocused = false

        EvsTemplate.get().templateType = null

        currentTemplateId = null

        val clearTemplate = Intent(this, EngineService::class.java)
        clearTemplate.action = EngineService.ACTION_CLEAR_TEMPLATE_FOCUS
        startService(clearTemplate)

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        templateContainer?.let { templateContainer ->
            templateContainer.animate()
                .alpha(0f)
                .translationY(templateContainer.height.toFloat())
                .setDuration(250)
                .withEndAction {
                    templateContainer.isVisible = false

                    templateContainer.removeAllViews()

                    val containerLayoutParams =
                        templateContainer.layoutParams as WindowManager.LayoutParams

                    containerLayoutParams.width = 0
                    containerLayoutParams.height = 0

                    windowManager.updateViewLayout(templateContainer, containerLayoutParams)
                }
                .start()
        }
    }

    private fun renderTemplate(payload: String) {
        EvsTemplate.get().isOtherTemplateFocused = true

        clearCardHandler.clearCount()

        val json = JsonParser().parse(payload).asJsonObject
        val templateId = json.get("template_id").asString
        val type = json.get("type").asString

        if (type == Template.TYPE_BODY_1 || type == Template.TYPE_BODY_2) {
            TouchAccessibility.isBodyTemplate = true
        }

        EvsTemplate.get().templateType = type

        currentTemplateId = templateId

        TemplateViewBuilder.build(baseContext, payload, View.OnClickListener {
            val stopAudioPlayer = Intent(this, EngineService::class.java)
            stopAudioPlayer.action = EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
            stopAudioPlayer.putExtra(EngineService.EXTRA_PLAYER_TYPE, AudioPlayer.TYPE_TTS)
            startService(stopAudioPlayer)

            val stopAlarm = Intent(this, EngineService::class.java)
            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
            startService(stopAlarm)

            clearCardHandler.clearCount()

            // close template
            clearCurrentTemplate()
        })?.let { childView ->
            if (childView is BodyTemplateView3) {
                alarmView?.findViewById<View>(R.id.alarm_placeholder)?.visibility = View.GONE
            }

            isShowTemplate = true

            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            templateContainer?.let { templateContainer ->
                templateContainer.removeAllViews()

                templateContainer.addView(
                    childView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                val point = Point()
                windowManager.defaultDisplay.getSize(point)
                templateContainer.isVisible = true
                templateContainer.translationY = if (isAlarmPlaying) 0f else point.y.toFloat()
                templateContainer.alpha = 0f

                templateContainer.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .start()

                val containerLayoutParams =
                    templateContainer.layoutParams as WindowManager.LayoutParams

                containerLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                containerLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT

                windowManager.updateViewLayout(templateContainer, containerLayoutParams)
            }
        }
    }

    private fun renderAlarmActiveView() {
        alarmView?.let { view ->
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutParams = view.layoutParams as WindowManager.LayoutParams

            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT

            if (templateContainer?.getChildAt(0) is BodyTemplateView3) {
                view.findViewById<View>(R.id.alarm_placeholder).visibility = View.GONE
            } else {
                view.findViewById<View>(R.id.alarm_placeholder).visibility = View.VISIBLE
            }

            view.findViewById<TextView>(R.id.close_alarm_message)?.let { tvMessage ->
                val array = resources.getStringArray(R.array.alert_tips)
                tvMessage.text = getString(
                    R.string.you_can_say,
                    array[((array.size - 1) * Math.random()).roundToInt()]
                )
            }

            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start()

            windowManager.updateViewLayout(view, layoutParams)
        }
    }

    private fun clearAlarmActiveView() {
        alarmView?.let { view ->
            view.animate()
                .alpha(0f)
                .translationY(view.height.toFloat())
                .setDuration(250)
                .withEndAction {
                    val windowManager =
                        getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val layoutParams =
                        view.layoutParams as WindowManager.LayoutParams

                    layoutParams.width = 0
                    layoutParams.height = 0

                    windowManager.updateViewLayout(view, layoutParams)
                }
                .start()
        }
    }

    private fun hasPermission(): Boolean {
        return OverlayUtils.hasPermission(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverlay() {
        if (hasPermission()) {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // Template UI
            run {
                val view = InterceptFrameLayout(baseContext)

                val layoutParams = WindowManager.LayoutParams()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags =
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

                // 初始化时置空即可，显示 Template 才填入 childView
                layoutParams.width = 0
                layoutParams.height = 0

                view.onInterceptTouchListener = View.OnTouchListener { _, _ ->
                    VoiceButtonUtils.lastTouchTime = System.currentTimeMillis()
                    templateContainer?.getChildAt(0)?.let { templateView ->
                        //                        val stopAudioPlayer = Intent(this, EngineService::class.java)
//                        stopAudioPlayer.action = EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
//                        stopAudioPlayer.putExtra(
//                            EngineService.EXTRA_PLAYER_TYPE,
//                            AudioPlayer.TYPE_TTS
//                        )
//                        startService(stopAudioPlayer)

                        if (templateView is BodyTemplateView1) {
                            templateView.stopBodyAnimation()
                        }

                        val stopAlarm = Intent(this, EngineService::class.java)
                        stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                        startService(stopAlarm)

                        if (isAlarmPlaying) {
                            clearCardHandler.startCount(templateView, ALARM_TEMPLATE_TIMEOUT)
                        } else {
                            clearCardHandler.startCount(templateView, STATIC_TEMPLATE_TIMEOUT)
                        }
                    }
                    false
                }
                view.onInterceptKeyEventListener = View.OnKeyListener { _, keyCode, _ ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (templateContainer?.isVisible == true) {
                            val stopAudioPlayer = Intent(this, EngineService::class.java)
                            stopAudioPlayer.action = EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
                            stopAudioPlayer.putExtra(
                                EngineService.EXTRA_PLAYER_TYPE,
                                AudioPlayer.TYPE_TTS
                            )
                            startService(stopAudioPlayer)

                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearCardHandler.clearCount()

                            // close template
                            clearCurrentTemplate()
                            true
                        } else {
                            false
                        }
                    } else
                        false
                }

                windowManager.addView(view, layoutParams)

                view.alpha = 0f
                view.post {
                    view.isVisible = false
                }

                templateContainer = view
            }

            // 闹钟关闭 UI
            run {
                val view = View.inflate(this, R.layout.layout_alarm_active, null)

                view.alpha = 0f

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutParams.width = 0
                layoutParams.height = 0

                view.findViewById<View>(R.id.close_alarm)?.setOnClickListener {
                    val stopAlarm = Intent(this, EngineService::class.java)
                    stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                    startService(stopAlarm)

                    EvsSpeaker.get(baseContext).isVisualFocusGain = false
                    EvsSpeaker.get(baseContext).refreshNativeAudioFocus(baseContext)

                    clearCurrentTemplate()
                    clearAlarmActiveView()
                }

                windowManager.addView(view, layoutParams)

                alarmView = view
            }

            // 辅助语音唤醒按钮
            run {
                val view = FrameLayout(this)

                view.isClickable = true
                view.isFocusable = true

                val onClickListener = View.OnClickListener {
                    val intent = Intent(this, EngineService::class.java)
                    intent.action = EngineService.ACTION_SEND_AUDIO_IN
                    startService(intent)
                }
                view.setOnTouchListener(object : View.OnTouchListener {
                    private var touchX = 0f
                    private var touchY = 0f

                    private var positionX = 0
                    private var positionY = 0

                    private var actionDownTime = 0L
                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        VoiceButtonUtils.lastTouchTime = System.currentTimeMillis()
                        when (event?.action) {
                            MotionEvent.ACTION_DOWN -> {
                                actionDownTime = System.currentTimeMillis()
                                touchX = event.rawX
                                touchY = event.rawY

                                v?.isPressed = true

                                (v?.layoutParams as? WindowManager.LayoutParams)?.let {
                                    positionX = it.x
                                    positionY = it.y
                                }
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val moveX = event.rawX
                                val moveY = event.rawY
                                (v?.layoutParams as? WindowManager.LayoutParams)?.let {
                                    var changed = false

                                    val offsetX = moveX - touchX
                                    val targetX = max(
                                        0f,
                                        min(
                                            resources.displayMetrics.widthPixels.toFloat() - v.width,
                                            positionX + offsetX
                                        )
                                    )

                                    val offsetY = moveY - touchY
                                    val targetY = max(
                                        0f,
                                        min(
                                            resources.displayMetrics.heightPixels.toFloat() - v.height,
                                            positionY - offsetY
                                        )
                                    )

                                    if (it.x != targetX.toInt()) {
                                        it.x = targetX.toInt()
                                        changed = changed || true
                                    }
                                    if (it.y != targetY.toInt()) {
                                        it.y = targetY.toInt()
                                        changed = changed || true
                                    }

                                    if (changed) {
                                        windowManager.updateViewLayout(v, it)
                                    }
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                v?.isPressed = false

                                val upX = event.rawX
                                val upY = event.rawY

                                if (System.currentTimeMillis() - actionDownTime < 200) {
                                    if ((upX - touchX).pow(2) + (upY - touchY).pow(2)
                                        < (1f * resources.getDimensionPixelSize(R.dimen.dp_16)).pow(
                                            2
                                        )
                                    ) {
                                        onClickListener.onClick(view)
                                    }
                                }

                                moveVoiceButtonToSide()
                            }
                        }
                        return true
                    }
                })

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    view.setBackgroundResource(R.drawable.bg_wake_button)
                } else {
                    view.setBackgroundResource(R.drawable.button_awake)
                    view.stateListAnimator = AnimatorInflater.loadStateListAnimator(
                        this,
                        R.animator.button_awake_animator
                    )
                }

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutParams.gravity = Gravity.BOTTOM or Gravity.START
                layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_88)
                layoutParams.height = resources.getDimensionPixelSize(R.dimen.dp_88)

                view.isVisible = false

                windowManager.addView(view, layoutParams)

                view.post {
                    if (isServiceEnabled())
                        hideVoiceButtonHandler.postScrollEvent()
                }

                voiceButtonView = view
            }

            // IAT 识别 UI
            run {
                val view = View.inflate(this, R.layout.layout_recognize, null)
                view.setOnClickListener {
                    dismissRecognizeView()

                    val intent = Intent(this, EngineService::class.java)
                    intent.action = EngineService.ACTION_REQUEST_CANCEL
                    startService(intent)
                }

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT

                windowManager.addView(view, layoutParams)

                recognizeView = view
            }

            // 背景录音识别 UI
            run {
                val view = View.inflate(this, R.layout.layout_background_recognize, null)

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutParams.gravity = Gravity.BOTTOM or Gravity.START
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT

                windowManager.addView(view, layoutParams)

                view.findViewById<TextView>(R.id.background_iat)?.let { tvText ->
                    tvText.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {

                        }

                        override fun beforeTextChanged(
                            s: CharSequence?, start: Int, count: Int, after: Int
                        ) {

                        }

                        override fun onTextChanged(
                            s: CharSequence?, start: Int, before: Int, count: Int
                        ) {
                            tvText.isVisible = !s.isNullOrEmpty()
                        }

                    })
                }
                view.findViewById<ImageView>(R.id.background_recognize_img)?.setOnClickListener {
                    dismissBackgroundRecognizeView()

                    val intent = Intent(this, EngineService::class.java)
                    intent.action = EngineService.ACTION_REQUEST_CANCEL
                    startService(intent)
                }

                view.alpha = 0f
                view.isVisible = false

                backgroundRecognizeView = view
            }

            // 显示通知
            run {
                val view = View.inflate(this, R.layout.layout_notification, null)

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = resources.getDimensionPixelSize(R.dimen.dp_76)
                layoutParams.gravity = Gravity.TOP

                windowManager.addView(view, layoutParams)

                view.translationY = -layoutParams.height.toFloat()
                view.alpha = 0f

                view.post {
                    view.visibility = View.GONE
                }

                notificationView = view
            }

            // 右侧控制条 UI，显示语音控制音量、亮度调节结果
            run {
                val view = View.inflate(this, R.layout.layout_side_indicator_bar, null)

                view.findViewById<BoxedVertical>(R.id.indicator_slide_bar)?.setValue(35)

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_104)
                layoutParams.height = resources.getDimensionPixelSize(R.dimen.dp_228)
                layoutParams.gravity = Gravity.TOP or Gravity.END

                view.visibility = View.GONE

                view.findViewById<BoxedVertical>(R.id.indicator_slide_bar)?.isPreventTouch = true

                windowManager.addView(view, layoutParams)

                sideIndicatorView = view
            }

            // 下拉控制栏
            run {
                val view = View.inflate(this, R.layout.layout_control_panel, null)

                val panel: ControlPanel = view.findViewById(R.id.control_panel)

                val tvSsid: TextView = panel.findViewById(R.id.tv_ssid)

                WifiUtils.getConnectedSsid(baseContext)?.let { ssid ->
                    if (ssid.isNotEmpty())
                        tvSsid.text = ssid
                }
                panel.onReleaseCallback = object : ControlPanel.OnReleaseCallback {
                    override fun onRequestRelease() {
                        dismissPanel()
                    }

                    override fun onRelease(panel: View?, x: Int, y: Int): Boolean {
                        return if (abs(y) > (panel?.height ?: 0) / 3) {
                            dismissPanel()
                            true
                        } else {
                            false
                        }
                    }
                }
                panel.onInterceptTouchListener = View.OnTouchListener { _, _ ->
                    VoiceButtonUtils.lastTouchTime = System.currentTimeMillis()
                    panelCloseCountHandler.postNewCount()
                    false
                }
                view.findViewById<View>(R.id.panel_background).setOnTouchListener { _, event ->
                    if (isPanelOpened)
                        if (event.action == MotionEvent.ACTION_DOWN) {
                            dismissPanel()
                        }
                    false
                }
                view.findViewById<BoxedHorizontal>(R.id.brightness_slider)?.let { slider ->
                    slider.setOnBoxedPointsChangeListener(object :
                        BoxedHorizontal.OnValuesChangeListener {
                        override fun onPointsChanged(
                            boxedPoints: BoxedHorizontal,
                            points: Int,
                            fromTouch: Boolean
                        ) {
                            if (BrightnessUtils.hasPermission(baseContext) && fromTouch) {
                                BrightnessUtils.setBrightness(baseContext, points)

                                slidePanel?.findViewById<LottieAnimationView>(R.id.brightness_icon)
                                    ?.let { icon ->
                                        icon.progress = points / 100f
                                    }
                            }

                        }

                        override fun onStartTrackingTouch(boxedPoints: BoxedHorizontal) {
                        }

                        override fun onStopTrackingTouch(boxedPoints: BoxedHorizontal) {
                        }

                    })
                }
                view.findViewById<BoxedHorizontal>(R.id.volume_slider)
                    ?.setOnBoxedPointsChangeListener(
                        object : BoxedHorizontal.OnValuesChangeListener {
                            override fun onPointsChanged(
                                boxedPoints: BoxedHorizontal, points: Int, fromTouch: Boolean
                            ) {
                                if (!fromTouch)
                                    return
                                val speaker = EvsSpeaker.get(this@FloatingService)
                                speaker.setVolumeLocally(points)

                                val volume = speaker.getCurrentVolume()
                                slidePanel?.findViewById<LottieAnimationView>(R.id.volume_icon)
                                    ?.let { icon ->
                                        val current = icon.progress
                                        when {
                                            volume == 0 -> {
                                                animateVolumeTo(current, VOLUME_0)
                                            }
                                            volume in 1..32 -> {
                                                animateVolumeTo(current, VOLUME_1)
                                            }
                                            volume in 33..66 -> {
                                                animateVolumeTo(current, VOLUME_2)
                                            }
                                            volume >= 67 -> {
                                                animateVolumeTo(current, VOLUME_3)
                                            }
                                        }
                                    }
                            }

                            override fun onStartTrackingTouch(boxedPoints: BoxedHorizontal) {
                            }

                            override fun onStopTrackingTouch(boxedPoints: BoxedHorizontal) {
                                if (!EvsSpeaker.get(baseContext).isFocusGain)
                                    PromptManager.play(PromptManager.VOLUME)
                            }

                        })
                view.findViewById<View>(R.id.panel_home)?.setOnClickListener {
                    when {
                        alarmView?.alpha == 1f -> {
                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearAlarmActiveView()
                        }
                        templateContainer?.alpha == 1f -> {
                            val stopAudioPlayer = Intent(this, EngineService::class.java)
                            stopAudioPlayer.action = EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
                            stopAudioPlayer.putExtra(
                                EngineService.EXTRA_PLAYER_TYPE,
                                AudioPlayer.TYPE_TTS
                            )
                            startService(stopAudioPlayer)

                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearCardHandler.clearCount()

                            clearCurrentTemplate()
                        }
                    }
                    dismissPanel()
                    try {
                        val intent = Intent(this, EvsLauncherActivity::class.java)
                        intent.action = EvsLauncherActivity.ACTION_OPEN_HOME
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        applicationContext.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                view.findViewById<View>(R.id.panel_settings)?.setOnClickListener {
                    when {
                        alarmView?.alpha == 1f -> {
                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearAlarmActiveView()
                        }
                        templateContainer?.alpha == 1f -> {
                            val stopAudioPlayer = Intent(this, EngineService::class.java)
                            stopAudioPlayer.action = EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
                            stopAudioPlayer.putExtra(
                                EngineService.EXTRA_PLAYER_TYPE,
                                AudioPlayer.TYPE_TTS
                            )
                            startService(stopAudioPlayer)

                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearCardHandler.clearCount()

                            clearCurrentTemplate()
                        }
                    }
                    dismissPanel()
                    try {
                        val intent = Intent(this, EvsLauncherActivity::class.java)
                        intent.action = EvsLauncherActivity.ACTION_OPEN_SETTINGS
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        applicationContext.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                view.findViewById<View>(R.id.panel_wlan)?.setOnClickListener {
                    when {
                        alarmView?.alpha == 1f -> {
                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearAlarmActiveView()
                        }
                        templateContainer?.alpha == 1f -> {
                            val stopAudioPlayer = Intent(this, EngineService::class.java)
                            stopAudioPlayer.action = EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
                            stopAudioPlayer.putExtra(
                                EngineService.EXTRA_PLAYER_TYPE,
                                AudioPlayer.TYPE_TTS
                            )
                            startService(stopAudioPlayer)

                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearCardHandler.clearCount()

                            clearCurrentTemplate()
                        }
                    }
                    dismissPanel()
                    try {
                        val intent = Intent(this, EvsLauncherActivity::class.java)
                        intent.action = EvsLauncherActivity.ACTION_OPEN_WLAN
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        applicationContext.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                view.findViewById<View>(R.id.panel_microphone)?.setOnClickListener {
                    val isMicrophoneEnabled =
                        ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
                    ConfigUtils.putBoolean(
                        ConfigUtils.KEY_VOICE_WAKEUP_ENABLED,
                        !isMicrophoneEnabled
                    )
                }
                view.findViewById<View>(R.id.panel_message_board)?.setOnClickListener {
                    when {
                        alarmView?.alpha == 1f -> {
                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearAlarmActiveView()
                        }
                        templateContainer?.alpha == 1f -> {
                            val stopAudioPlayer = Intent(this, EngineService::class.java)
                            stopAudioPlayer.action = EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
                            stopAudioPlayer.putExtra(
                                EngineService.EXTRA_PLAYER_TYPE,
                                AudioPlayer.TYPE_TTS
                            )
                            startService(stopAudioPlayer)

                            val stopAlarm = Intent(this, EngineService::class.java)
                            stopAlarm.action = EngineService.ACTION_REQUEST_STOP_ALARM
                            startService(stopAlarm)

                            clearCardHandler.clearCount()

                            clearCurrentTemplate()
                        }
                    }
                    dismissPanel()
                    try {
                        val intent = Intent(this, EvsLauncherActivity::class.java)
                        intent.action = EvsLauncherActivity.ACTION_OPEN_MESSAGE_BOARD
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        applicationContext.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                musicPanel = view.findViewById(R.id.music_panel)
                musicCover = view.findViewById(R.id.iv_cover)
                musicTitle = view.findViewById(R.id.tv_music_title)
                tvQuit = view.findViewById(R.id.tv_quit)
                ivPrevious = view.findViewById(R.id.iv_previous)
                playPause = view.findViewById(R.id.iv_play_pause)
                ivNext = view.findViewById(R.id.iv_next)
                musicPanel?.setOnClickListener {
                    val intent = Intent(it.context, EvsLauncherActivity::class.java)
                    intent.action = ACTION_START_PLAYER
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)

                    dismissPanel()
                }
                ivNext?.setOnClickListener {
                    EvsPlaybackController.get()
                        .sendCommand(PlaybackController.Command.Next, object :
                            RequestCallback {
                            override fun onResult(result: Result) {
                                if (!result.isSuccessful) {
                                    val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                                    intent.putExtra(EngineService.EXTRA_RESULT, result)
                                    sendBroadcast(intent)
                                }
                            }
                        })
                }
                ivPrevious?.setOnClickListener {
                    EvsPlaybackController.get()
                        .sendCommand(PlaybackController.Command.Previous, object :
                            RequestCallback {
                            override fun onResult(result: Result) {
                                if (!result.isSuccessful) {
                                    val intent = Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                                    intent.putExtra(EngineService.EXTRA_RESULT, result)
                                    sendBroadcast(intent)
                                }
                            }
                        })
                }
                playPause?.setOnClickListener {
                    val audioPlayer = EvsAudioPlayer.get(this)
                    if (audioPlayer.playbackResourceId.isNullOrEmpty()) {
                        EvsPlaybackController.get()
                            .sendCommand(PlaybackController.Command.Resume, object :
                                RequestCallback {
                                override fun onResult(result: Result) {
                                    if (!result.isSuccessful) {
                                        val intent =
                                            Intent(EngineService.ACTION_SEND_REQUEST_FAILED)
                                        intent.putExtra(EngineService.EXTRA_RESULT, result)
                                        sendBroadcast(intent)
                                    }
                                }
                            })
                    } else {
                        if (audioPlayer.playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING) {
                            playPause?.setImageResource(R.drawable.ic_music_play)
                            EvsAudioPlayer.get(this).pause(AudioPlayer.TYPE_PLAYBACK)
                        } else {
                            playPause?.setImageResource(R.drawable.ic_music_pause)
                            EvsAudioPlayer.get(this).resume(AudioPlayer.TYPE_PLAYBACK)
                        }
                    }
                }
                tvQuit?.setOnClickListener {
                    EvsAudioPlayer.get(this).stop(AudioPlayer.TYPE_PLAYBACK)

                    val storage = ContentStorage.get()
                    storage.isMusicPlaying = false

                    updateMusicPanel(null)
                }

                updateMusicPanel(ContentStorage.get().playerInfo)

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags =
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT

                view.post {
                    val isMicrophoneEnabled =
                        ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
                    if (isMicrophoneEnabled) {
                        enableMicrophoneAnimation()
                    } else {
                        disableMicrophoneAnimation()
                    }

                    animatePanel(0f)
                    view.visibility = View.GONE

                    updateBrightness()
                    updateVolume()
                }

                windowManager.addView(view, layoutParams)

                slidePanel = view
            }

            // 下拉控制栏手势监听区域
            run {
                val view = object : View(this) {
                    var touchY = 0f

                    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
                        event ?: return super.dispatchTouchEvent(event)
                        val max =
                            if (slidePanel?.findViewById<View>(R.id.panel_area)?.height ?: 0 > 0)
                                (slidePanel?.findViewById<View>(R.id.panel_area)?.height
                                    ?: 0).toFloat()
                            else
                                resources.getDimensionPixelSize(R.dimen.panel_min_height).toFloat()
                        if (!isPanelOpened && isControlPanelEnabled && ConfigUtils.getBoolean(
                                ConfigUtils.KEY_SETUP_COMPLETED,
                                false
                            )
                        ) {
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    touchY = event.y

                                    slidePanel?.isVisible = true
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    val y = min(max, max(0f, event.y - touchY))
                                    val percent = y / max
                                    animatePanel(percent)
                                }
                                MotionEvent.ACTION_UP -> {
                                    val y = min(max, max(0f, event.y - touchY))
                                    val percent = y / max

                                    if (percent > 0.5) {
                                        // show
                                        showPanel()
                                    } else {
                                        // disappear
                                        dismissPanel()
                                    }

                                    touchY = 0f
                                }
                            }
                        }
                        return true
                    }
                }

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags =
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = resources.getDimensionPixelSize(R.dimen.dp_12)

                windowManager.addView(view, layoutParams)
            }

            // 左侧右滑 UI 展示
            run {
                val view = LottieAnimationView(this)

                view.alpha = 0f
                view.setAnimation(R.raw.animation_gesture_back)
                view.scaleType = ImageView.ScaleType.CENTER_CROP

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                layoutParams.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_28)
                layoutParams.height = resources.getDimensionPixelSize(R.dimen.dp_64)

                windowManager.addView(view, layoutParams)

                backAnimationView = view
                backAnimationView?.isVisible = false
            }

            // 左侧右滑手势监听区域
            run {
                val view = View(this)

                val layoutParams = WindowManager.LayoutParams()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
                }
                layoutParams.format = PixelFormat.TRANSPARENT
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                layoutParams.gravity = Gravity.START or Gravity.BOTTOM
                layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp_8)
                layoutParams.height =
                    resources.displayMetrics.heightPixels - resources.getDimensionPixelSize(R.dimen.dp_56)

                view.let { touchView ->
                    var touchX = 0f
                    val limit = resources.getDimensionPixelSize(R.dimen.dp_80)
                    touchView.setOnTouchListener { _, event ->
                        VoiceButtonUtils.lastTouchTime = System.currentTimeMillis()
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                touchX = event.x
                            }
                            MotionEvent.ACTION_MOVE -> {
                                val distance = event.x - touchX

                                val percent = max(0f, min(1f, 1f * distance / limit))

                                if (backAnimationView?.isVisible == false) {
                                    backAnimationView?.isVisible = true
                                }

                                backAnimationView?.alpha = percent
                                backAnimationView?.progress = percent
                            }
                            MotionEvent.ACTION_UP -> {
                                backAnimationView?.animate()
                                    ?.alpha(0f)
                                    ?.setDuration(200)
                                    ?.withEndAction {
                                        backAnimationView?.isVisible = false
                                        backAnimationView?.progress = 0f
                                    }
                                    ?.start()
                                val distance = event.x - touchX
                                if (distance >= limit) {
                                    when {
                                        isPanelOpened -> {
                                            dismissPanel()
                                        }
                                        alarmView?.alpha == 1f -> {
                                            val stopAlarm = Intent(this, EngineService::class.java)
                                            stopAlarm.action =
                                                EngineService.ACTION_REQUEST_STOP_ALARM
                                            startService(stopAlarm)

                                            clearAlarmActiveView()
                                        }
                                        templateContainer?.alpha == 1f -> {
                                            val stopAudioPlayer =
                                                Intent(this, EngineService::class.java)
                                            stopAudioPlayer.action =
                                                EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
                                            stopAudioPlayer.putExtra(
                                                EngineService.EXTRA_PLAYER_TYPE,
                                                AudioPlayer.TYPE_TTS
                                            )
                                            startService(stopAudioPlayer)

                                            val stopAlarm = Intent(this, EngineService::class.java)
                                            stopAlarm.action =
                                                EngineService.ACTION_REQUEST_STOP_ALARM
                                            startService(stopAlarm)

                                            clearCardHandler.clearCount()

                                            clearCurrentTemplate()
                                        }
                                        else -> {
                                            NavigationUtils.clickBack()
                                        }
                                    }
                                }
                            }
                        }
                        true
                    }
                }

                windowManager.addView(view, layoutParams)
            }
        }
    }

    private fun moveVoiceButtonToSide() {
        voiceButtonView?.let {
            (voiceButtonView?.layoutParams as? WindowManager.LayoutParams)?.let { layoutParams ->
                val currentX = layoutParams.x
                val width = resources.displayMetrics.widthPixels
                if (currentX + it.width / 2 > width / 2) {
                    animateVoiceButtonToSide(currentX, width - it.width)
                } else {
                    animateVoiceButtonToSide(currentX, 0)
                }
            }
        }
    }

    private fun animateVoiceButtonToSide(startX: Int, endX: Int) {
        val voiceButtonView = voiceButtonView ?: return
        val animator = ValueAnimator.ofInt(startX, endX)
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            (voiceButtonView.layoutParams as? WindowManager.LayoutParams)?.let { layoutParams ->
                if (layoutParams.x != value) {
                    layoutParams.x = value
                    windowManager.updateViewLayout(voiceButtonView, layoutParams)
                }
            }
        }
        animator.duration = 450
        animator.interpolator = FastOutSlowInInterpolator()
        animator.start()
    }

    private fun hideVoiceButton() {
        voiceButtonView?.isVisible = false
    }

    private fun showVoiceButton() {
        voiceButtonView?.isVisible = true
    }

    private fun weakVoiceButton() {
        val voiceButton = voiceButtonView ?: return
        if (voiceButton.isVisible) {
            val isLeft = (voiceButton.layoutParams as? WindowManager.LayoutParams)?.x == 0
            if (isLeft)
                voiceButton.animate().translationX(-voiceButton.width.toFloat()).setDuration(500).start()
            else
                voiceButton.animate().translationX(voiceButton.width.toFloat()).setDuration(500).start()
        }
    }

    private fun strongVoiceButton() {
        val voiceButton = voiceButtonView ?: return
        if (voiceButton.isVisible)
            voiceButton.animate().translationX(0f).setDuration(300).start()
    }

    private fun showNotification(
        icon: Drawable?,
        message: String?,
        positivePair: Pair<String, View.OnClickListener?>?,
        negativePair: Pair<String, View.OnClickListener?>?,
        keeping: Boolean = false,
        tag: String? = null
    ) {
        notificationView?.let { notificationView ->
            val messageView =
                notificationView.findViewById<TextView>(R.id.notification_message)
            if (messageView.text.toString() != message
                && messageView.text.toString().isNotEmpty()
            ) {
                dismissNotification(Runnable {
                    messageView.text = ""
                    showNotification(icon, message, positivePair, negativePair, keeping, tag)
                })
            } else {
                notificationView.alpha = 0f
                notificationView.translationY = -notificationView.height.toFloat()
                notificationView.visibility = View.VISIBLE

                messageView.text = message
                messageView.tag = tag

                val iconView =
                    notificationView.findViewById<ImageView>(R.id.notification_icon)
                icon?.let {
                    iconView.setImageDrawable(icon)
                    iconView.visibility = View.VISIBLE
                } ?: run {
                    iconView.visibility = View.GONE
                }

                val positiveBtn =
                    notificationView.findViewById<Button>(R.id.notification_positive_action)
                positivePair?.let {
                    positiveBtn.text = it.first
                    positiveBtn.setOnClickListener(it.second)
                } ?: run {
                    positiveBtn.visibility = View.GONE
                }

                val negativeBtn =
                    notificationView.findViewById<Button>(R.id.notification_negative_action)
                negativePair?.let {
                    negativeBtn.text = it.first
                    negativeBtn.setOnClickListener(it.second)
                } ?: run {
                    negativeBtn.visibility = View.GONE
                }

                notificationView.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(500)
                    .start()

                if (!keeping) {
                    clearNotificationHandler.startCount()
                }
            }
        }
    }

    private fun dismissNotification(endRunnable: Runnable? = null) {
        notificationView?.let { view ->
            Log.d(TAG, "dismissNotification")
            view.animate()
                .translationY(-view.height.toFloat())
                .alpha(0f)
                .setDuration(450)
                .withEndAction {
                    view.visibility = View.GONE

                    endRunnable?.run()
                }
                .start()
        }
    }

    @Suppress("UNUSED_VARIABLE")
    private fun updateBattery(batteryStatus: Intent?) {
        (batteryStatus
            ?: baseContext?.registerReceiver(
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

            slidePanel?.findViewById<BatteryView>(R.id.battery_view)?.let { batteryView ->
                batteryView.level = batteryLevel
                batteryView.isCharging = isCharging
            }
            slidePanel?.findViewById<TextView>(R.id.tv_battery)?.let { tvBattery ->
                tvBattery.text = getString(R.string.count_of_percent, batteryLevel)
            }
        }
    }

    private fun enableWifiIcon() {
        slidePanel?.let {
            it.findViewById<ImageView>(R.id.iv_wlan).isSelected = false
            it.findViewById<View>(R.id.panel_wlan).isSelected = false
        }
    }

    private fun disableWifiIcon() {
        slidePanel?.let {
            it.findViewById<ImageView>(R.id.iv_wlan).isSelected = true
            it.findViewById<View>(R.id.panel_wlan).isSelected = true
        }
    }

    private fun disableMicrophoneAnimation() {
        slidePanel?.findViewById<View>(R.id.panel_microphone)?.isSelected = true
        slidePanel?.findViewById<ImageView>(R.id.microphone_animation)
            ?.let { animationView ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (animationView.drawable as? AnimatedVectorDrawableCompat)?.let {
                        if (it.isRunning) {
                            it.stop()
                        }
                    }
                    val drawable = AnimatedVectorDrawableCompat.create(
                        this,
                        R.drawable.anim_microphone_enabled_to_disabled
                    )
                    animationView.setImageDrawable(drawable)
                    drawable?.start()
                } else {
                    microphoneAnimator?.cancel()

                    val animator = ValueAnimator.ofFloat(0f, 1f)
                    animator.addUpdateListener {
                        val value = it.animatedValue as Float

                        val scale = 0.8f

                        if (value < 0.5f) {
                            animationView.scaleX = min(1f, 1 - (1 - scale) * value / 0.5f)
                            animationView.scaleY = min(1f, 1 - (1 - scale) * value / 0.5f)
                        } else {
                            if (animationView.tag != R.drawable.ic_microphone_disabled_black_40dp) {
                                animationView.tag = R.drawable.ic_microphone_disabled_black_40dp
                                animationView.setImageResource(R.drawable.ic_microphone_disabled_black_40dp)
                            }
                            animationView.scaleX =
                                min(1f, scale + (1 - scale) * (value - 0.5f) / 0.5f)
                            animationView.scaleY =
                                min(1f, scale + (1 - scale) * (value - 0.5f) / 0.5f)
                        }
                    }
                    animator.duration = 500
                    animator.interpolator = FastOutSlowInInterpolator()
                    animator.start()
                }

            }
    }

    private fun enableMicrophoneAnimation() {
        slidePanel?.findViewById<View>(R.id.panel_microphone)?.isSelected = false
        slidePanel?.findViewById<ImageView>(R.id.microphone_animation)
            ?.let { animationView ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (animationView.drawable as? AnimatedVectorDrawableCompat)?.let {
                        if (it.isRunning) {
                            it.stop()
                        }
                    }
                    val drawable = AnimatedVectorDrawableCompat.create(
                        this,
                        R.drawable.anim_microphone_disabled_to_enabled
                    )
                    animationView.setImageDrawable(drawable)
                    drawable?.start()
                } else {
                    microphoneAnimator?.cancel()

                    val animator = ValueAnimator.ofFloat(0f, 1f)
                    animator.addUpdateListener {
                        val value = it.animatedValue as Float

                        val scale = 0.8f

                        if (value < 0.5f) {
                            animationView.scaleX = min(1f, 1 - (1 - scale) * value / 0.5f)
                            animationView.scaleY = min(1f, 1 - (1 - scale) * value / 0.5f)
                        } else {
                            if (animationView.tag != R.drawable.ic_microphone_enabled_black_40dp) {
                                animationView.tag = R.drawable.ic_microphone_enabled_black_40dp
                                animationView.setImageResource(R.drawable.ic_microphone_enabled_black_40dp)
                            }
                            animationView.scaleX =
                                min(1f, scale + (1 - scale) * (value - 0.5f) / 0.5f)
                            animationView.scaleY =
                                min(1f, scale + (1 - scale) * (value - 0.5f) / 0.5f)
                        }
                    }
                    animator.duration = 500
                    animator.interpolator = FastOutSlowInInterpolator()
                    animator.start()

                    microphoneAnimator = animator
                }
            }
    }

    private fun startTimer() {
        if (timerHandler == null) {
            timerHandler = TimerHandler(this)
        }
        timerHandler?.removeCallbacksAndMessages(null)
        timerHandler?.sendEmptyMessageDelayed(0, 1000)
    }

    private fun updateMusicControlButton(firstPlayId: String?, endPlayId: String?) {
        val playerInfo = ContentStorage.get().playerInfo
        if (TextUtils.equals(playerInfo?.resourceId, firstPlayId)) {
            ivPrevious?.alpha = 0.5f
            ivPrevious?.isEnabled = false
        } else {
            ivPrevious?.alpha = 1f
            ivPrevious?.isEnabled = true
        }
        if (TextUtils.equals(playerInfo?.resourceId, endPlayId)) {
            ivNext?.alpha = 0.5f
            ivNext?.isEnabled = false
        } else {
            ivNext?.alpha = 1f
            ivNext?.isEnabled = true
        }
    }

    @SuppressLint("SetTextI18n")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateMusicPanel(playerInfo: PlayerInfoPayload?) {
        if (playerInfo == null) {
            musicPanel?.isVisible = false
        } else {
            musicPanel?.isVisible = true
            val radius = resources.getDimensionPixelOffset(R.dimen.dp_16)
            musicCover?.let {
                if (!playerInfo.content?.imageUrl.isNullOrEmpty()) {
                    Glide.with(it)
                        .load(playerInfo.content?.imageUrl)
                        .transform(RoundedCornersTransformation(radius, 0))
                        .into(it)
                } else {
                    Glide.with(it)
                        .load(R.drawable.cover_default)
                        .transform(RoundedCornersTransformation(radius, 0))
                        .into(it)
                }
            }
            if (!playerInfo.content?.musicArtist.isNullOrEmpty() &&
                !playerInfo.content?.musicTitle.isNullOrEmpty()
            ) {
                musicTitle?.text =
                    "${playerInfo.content?.musicArtist}-${playerInfo.content?.musicTitle}"
            } else if (playerInfo.content?.musicArtist.isNullOrEmpty() &&
                !playerInfo.content?.musicTitle.isNullOrEmpty()
            ) {
                musicTitle?.text = playerInfo.content?.musicTitle
            } else if (!playerInfo.content?.musicArtist.isNullOrEmpty() &&
                playerInfo.content?.musicTitle.isNullOrEmpty()
            ) {
                musicTitle?.text = playerInfo.content?.musicArtist
            }
            if (EvsAudioPlayer.get(this).playbackState == AudioPlayer.PLAYBACK_STATE_PLAYING) {
                playPause?.setImageResource(R.drawable.ic_music_pause)
            } else {
                playPause?.setImageResource(R.drawable.ic_music_play)
            }
        }
    }

    private fun showPanel() {
        panelCloseCountHandler.postNewCount()
        isPanelOpened = true
        slidePanel?.let { view ->
            val startValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.alpha
            } else {
                view.findViewById<View>(R.id.panel_background).alpha
            }
            val animator = ValueAnimator.ofFloat(startValue, 1f)
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                animatePanel(value)
            }
            animator.doOnEnd {
                slidePanel?.post {
                    slidePanel?.let { panel ->
                        val tvSsid: TextView = panel.findViewById(R.id.tv_ssid)
                        WifiUtils.getConnectedSsid(baseContext)?.let { ssid ->
                            if (ssid.isNotEmpty()) {
                                tvSsid.text = ssid
                            } else {
                                tvSsid.setText(R.string.wlan)
                            }
                        } ?: run {
                            tvSsid.setText(R.string.wlan)
                        }
                    }
                }
                view.findViewById<TextView>(R.id.tv_ssid).requestFocus()
            }
            animator.duration = 200
            animator.start()
        }
    }

    private fun animatePanel(percent: Float) {
        if (slidePanel?.visibility == View.GONE) {
            slidePanel?.visibility = View.VISIBLE
        }
        slidePanel?.let { slidePanel ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                slidePanel.alpha = percent
            } else {
                slidePanel.findViewById<View>(R.id.panel_background).alpha = percent
            }

            val max = if (slidePanel.findViewById<View>(R.id.panel_area)?.height ?: 0 > 0)
                (slidePanel.findViewById<View>(R.id.panel_area)?.height ?: 0).toFloat()
            else
                resources.getDimensionPixelSize(R.dimen.panel_min_height).toFloat()
            slidePanel.findViewById<View>(R.id.panel_area)?.translationY = -(1 - percent) * max
        }
    }

    private fun dismissPanel() {
        panelCloseCountHandler.clearCount()
        isPanelOpened = false
        slidePanel?.let { view ->
            val startValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.alpha
            } else {
                view.findViewById<View>(R.id.panel_background).alpha
            }
            val animator = ValueAnimator.ofFloat(startValue, 0f)
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                animatePanel(value)
            }
            animator.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    view.visibility = View.GONE
                }

                override fun onAnimationCancel(animation: Animator?) {
                    view.visibility = View.GONE
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })
            animator.duration = 200
            animator.start()
        }
    }

    private fun updateVolume() {
        val volume = EvsSpeaker.get(this@FloatingService).getCurrentVolume()
        slidePanel?.findViewById<BoxedHorizontal>(R.id.volume_slider)?.let { slider ->
            slider.value = volume
        }
        slidePanel?.findViewById<LottieAnimationView>(R.id.volume_icon)?.let { icon ->
            val current = icon.progress
            when {
                volume == 0 -> {
                    animateVolumeTo(current, VOLUME_0)
                }
                volume in 1..32 -> {
                    animateVolumeTo(current, VOLUME_1)
                }
                volume in 33..66 -> {
                    animateVolumeTo(current, VOLUME_2)
                }
                volume >= 67 -> {
                    animateVolumeTo(current, VOLUME_3)
                }
            }
        }
    }

    private fun animateVolumeTo(from: Float, progress: Float) {
        if (from == progress)
            return
        if (volumeAnimator?.isStarted == true) {
            if (animatingVolumeTo == progress) {
                // ignore
            } else {
                volumeAnimator?.cancel()
            }
        } else {
            val animator = ValueAnimator.ofFloat(from, progress)
            animatingVolumeTo = progress
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                slidePanel?.findViewById<LottieAnimationView>(R.id.volume_icon)?.progress = value
            }
            animator.duration = (500 * abs(from - progress)).toLong()
            animator.start()
            volumeAnimator = animator
        }
    }

    private fun updateBrightness() {
        val currentBrightness = BrightnessUtils.getBrightness(this)
        val mode = BrightnessUtils.getBrightnessMode(this)

        slidePanel?.findViewById<BoxedHorizontal>(R.id.brightness_slider)?.let { slider ->
            slider.isEnable = mode != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            slider.value = currentBrightness
        }
        slidePanel?.findViewById<LottieAnimationView>(R.id.brightness_icon)?.let { icon ->
            icon.progress = currentBrightness / 100f
        }
    }

    private fun updateRecordVolume(volume: Double) {
        recognizeView?.let { view ->
            val recognizeWaveView: RecognizeWaveView? = view.findViewById(R.id.recognize_view)

            recognizeWaveView?.updateVolume(volume)
        }
    }

    private fun updateIntermediateText(text: String) {
        if (backgroundRecognizeView?.isVisible == true) {
            backgroundRecognizeView?.let { view ->
                val tvText = view.findViewById<TextView>(R.id.background_iat)

                tvText.text = text
            }
        } else {
            recognizeView?.let { view ->
                val tvText = view.findViewById<TextView>(R.id.iat_text)

                tvText.text = text
            }
        }
    }

    private fun getRes(arrayResId: Int): IntArray {
        val typedArray = resources.obtainTypedArray(arrayResId)
        val len = typedArray.length()
        val resId = IntArray(len)
        for (i in 0 until len) {
            resId[i] = typedArray.getResourceId(i, -1)
        }
        typedArray.recycle()
        return resId
    }

    private fun showBackgroundRecognizeView() {
        backgroundRecognizeView?.let { view ->
            VoiceButtonUtils.isBackgroundRecognizing = true
            view.findViewById<ImageView>(R.id.background_recognize_img)?.let { imageView ->
                val animation = imageView.tag as? FrameAnimation
                if (animation == null) {
                    val animations = getRes(R.array.animation_listening_res)
                    val frameAnimation = FrameAnimation(imageView, animations, 30, true)
                    frameAnimation.startAnimation()
                    imageView.tag = frameAnimation
                }
            }
            view.isVisible = true
            view.animate()
                .alpha(1f)
                .setDuration(250)
                .start()

            view.findViewById<TextView>(R.id.background_iat)?.text = ""
        }
    }

    private fun dismissBackgroundRecognizeView() {
        backgroundRecognizeView?.let { view ->
            VoiceButtonUtils.isBackgroundRecognizing = false

            view.findViewById<ImageView>(R.id.background_recognize_img)?.let { imageView ->
                val animation = imageView.tag as? FrameAnimation
                if (animation != null) {
                    animation.stopAnimation()
                    imageView.tag = null
                }
            }
            view.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction {
                    view.isVisible = false
                }
                .start()
        }
    }

    private fun dismissRecognizeView() {
        recognizeView?.let { view ->
            Log.d(TAG, "dismissRecognizeView")

            isShowRecognize = false

            view.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    view.visibility = View.GONE
                }
                .start()
        }
    }

    private fun showRecognizeView() {
        recognizeView?.let { view ->
            Log.d(TAG, "showRecognizeView")
            view.visibility = View.VISIBLE

            isShowRecognize = true

            val recognizeWaveView = view.findViewById<RecognizeWaveView>(R.id.recognize_view)
            recognizeWaveView.startEnterAnimation()

            view.animate().alpha(1f).setDuration(200).start()
            val tvText = view.findViewById<TextView>(R.id.iat_text)
            tvText.text = ""
        }
    }

    private fun isServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService<AccessibilityManager>()
            ?: return false
        val accessibilityServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in accessibilityServices) {
            if (info.id == "$packageName/.accessibility.TouchAccessibility") {
                return true
            }
        }
        return false
    }

    fun updateCurrentTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        slidePanel?.findViewById<TextView>(R.id.time)?.text =
            String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private class TimerHandler internal constructor(
        floatingService: FloatingService,
        private val reference: SoftReference<FloatingService> =
            SoftReference(floatingService)
    ) : Handler() {

        override fun handleMessage(msg: Message) {
            val service = reference.get()
            if (service != null) {
                service.updateCurrentTime()
                sendEmptyMessageDelayed(0, 1000)
            }
        }
    }

    private class HideVoiceButtonHandler(service: FloatingService) : Handler() {
        private val softRef = SoftReference(service)
        private val delay = 10 * 1000L
        private var flag = -1
        fun postScrollEvent() {
            Log.d("HideVoiceButtonHandler", "startCount")
            flag = UUID.randomUUID().hashCode()
            val msg = Message.obtain()
            msg.what = 1
            msg.arg1 = flag
            sendMessageDelayed(msg, delay)
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            val service = softRef.get() ?: return
            if (msg?.what == 1) {
                val flag = flag
                if (msg.arg1 == flag) {
                    service.weakVoiceButton()
                }
            }
        }
    }

    private class ClearCardHandler(service: FloatingService) : Handler() {
        private val softRef = SoftReference(service)
        private var flag = -1

        fun startCount(currentTemplateView: View, delay: Long) {
            Log.d("ClearCardHandler", "startCount")
            flag = UUID.randomUUID().hashCode()
            val msg = Message.obtain()
            msg.what = 1
            msg.arg1 = flag
            msg.obj = currentTemplateView
            sendMessageDelayed(msg, delay)
        }

        fun clearCount() {
            Log.d("ClearCardHandler", "clearCount")
            flag = -1
            removeCallbacksAndMessages(null)
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                1 -> {
                    if (flag == msg.arg1) {
                        Log.d("ClearCardHandler", "handleMessage")
                        softRef.get()?.let { service ->
                            if (service.templateContainer?.getChildAt(0) == msg.obj) {
                                service.clearCurrentTemplate()
                            }
                        }
                    }
                }
            }
        }
    }

    private class ClearNotificationHandler(service: FloatingService) : Handler() {
        private val softRef = SoftReference(service)
        private var currentTag = 0L

        fun startCount() {
            val current = System.currentTimeMillis()
            softRef.get()?.notificationView?.tag = current
            currentTag = current
            sendEmptyMessageDelayed(1, 20 * 1000)
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                1 -> {
                    softRef.get()?.let { service ->
                        service.notificationView?.let { view ->
                            if ((view.tag as? Long) == currentTag) {
                                service.dismissNotification()
                            }
                        }
                    }
                }
            }
        }
    }

    private class ClearSideIndicatorHandler(service: FloatingService) : Handler() {
        private val softRef = SoftReference(service)
        private var currentTag = 0L

        fun startCount() {
            val current = System.currentTimeMillis()
            softRef.get()?.sideIndicatorView?.tag = current
            currentTag = current
            sendEmptyMessageDelayed(1, 2 * 1000)
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                1 -> {
                    softRef.get()?.let { service ->
                        service.sideIndicatorView?.let { view ->
                            if ((view.tag as? Long) == currentTag) {
                                service.hideSideIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    private class PanelCloseCountHandler(service: FloatingService) : Handler() {
        private val softRef = SoftReference(service)
        private val countTime = TimeUnit.SECONDS.toMillis(10)
        private var time = 0L

        fun postNewCount() {
            val newTime = System.currentTimeMillis()
            val msg = Message.obtain()
            msg.what = 1
            msg.obj = newTime
            sendMessageDelayed(msg, countTime)

            this.time = newTime
        }

        fun clearCount() {
            removeCallbacksAndMessages(null)
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg?.what == 1) {
                val currentTime = msg.obj as? Long
                if (currentTime == time) {
                    softRef.get()?.let { service ->
                        if (service.isPanelOpened)
                            service.dismissPanel()
                    }
                }
            }
        }
    }
}