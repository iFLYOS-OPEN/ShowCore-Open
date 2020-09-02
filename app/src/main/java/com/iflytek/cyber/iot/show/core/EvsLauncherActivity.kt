package com.iflytek.cyber.iot.show.core

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.lottie.LottieDrawable
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.agent.*
import com.iflytek.cyber.evs.sdk.utils.AppUtil
import com.iflytek.cyber.iot.show.core.fragment.*
import com.iflytek.cyber.iot.show.core.impl.alarm.EvsAlarm
import com.iflytek.cyber.iot.show.core.impl.audioplayer.EvsAudioPlayer
import com.iflytek.cyber.iot.show.core.impl.haotu.HaotuExternalPlayerImpl
import com.iflytek.cyber.iot.show.core.impl.launcher.EvsLauncher
import com.iflytek.cyber.iot.show.core.impl.playback.EvsPlaybackController
import com.iflytek.cyber.iot.show.core.impl.system.EvsSystem
import com.iflytek.cyber.iot.show.core.impl.template.EvsTemplate
import com.iflytek.cyber.iot.show.core.impl.videoplayer.EvsVideoPlayer
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.Video
import com.iflytek.cyber.iot.show.core.record.GlobalRecorder
import com.iflytek.cyber.iot.show.core.utils.*
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.iflytek.cyber.product.ota.OtaService
import com.iflytek.cyber.product.ota.PackageEntityNew
import kotlinx.android.synthetic.main.activity_evs_launcher.*
import me.yokeyword.fragmentation.Fragmentation
import me.yokeyword.fragmentation.SupportHelper
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class EvsLauncherActivity : BaseActivity() {
    // mutable
    private var engineService: EngineService? = null
    private var requestFloatPermission = false
    private var requestFloatPermissionAlert: AlertDialog? = null
    private var requestWriteSystemAlert: AlertDialog? = null
    private var requestUsageDialog: AlertDialog? = null
    private var requestOtaDialog: StyledAlertDialog? = null
    private var requestNotifyPolicyDialog: AlertDialog? = null

    private var callbacks = ArrayList<EvsTemplate.RenderCallback>()
    private var recognizerCallbacks = ArrayList<Recognizer.RecognizerCallback>()

    var isActivityVisible = false

    private var handler = Handler(Looper.getMainLooper())

    private var hadInitVolume = false

    fun registerCallback(renderCallback: EvsTemplate.RenderCallback) {
        callbacks.add(renderCallback)
    }

    fun unregisterCallback(renderCallback: EvsTemplate.RenderCallback) {
        callbacks.remove(renderCallback)
    }

    fun getService(): EngineService? {
        return engineService
    }

    // final
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is EngineService.EngineServiceBinder) {
                service.getService().let {
                    it.setRecognizerCallback(recognizerCallback)
                    if (it.getAuthResponse() != null) {
                        if (!it.isEvsConnected) {
                            it.connectEvs(DeviceUtils.getDeviceId(it))
                        }
                    }
                    it.setTemplateRenderCallback(templateRenderCallback)
                    (it.getSystem() as? EvsSystem)?.onDeviceModeChangeListener =
                        onDeviceModeChangeListener
                    EvsLauncher.get().pageScrollCallback = pageScrollCallback
                    it.getAudioPlayer().addListener(audioPlayerStateChangedListener)

                    it.setActivity(this@EvsLauncherActivity)

                    if (isResume) {
                        it.requestLauncherVisualFocus()
                    }

                    engineService = it
                }

                initUi()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            EvsLauncher.get().pageScrollCallback = null
            engineService?.getAudioPlayer()?.removeListener(audioPlayerStateChangedListener)
            engineService?.setRecognizerCallback(null)
            engineService = null
        }
    }
    private val recognizerCallback = object : Recognizer.RecognizerCallback {
        override fun onBackgroundRecognizeStateChanged(isBackgroundRecognize: Boolean) {
        }

        override fun onRecognizeStarted(isExpectReply: Boolean) {
        }

        override fun onRecognizeStopped() {
        }

        override fun onIntermediateText(text: String, isLast: Boolean) {
        }

    }

    private fun restartApp() {
        if (!isActivityVisible) {
            val intent = Intent(applicationContext, EvsLauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            applicationContext.startActivity(intent)
            ContextWrapper.startActivityAsUser(this, intent, "CURRENT")
        }
    }

    private val templateRenderCallback = object : EvsTemplate.RenderCallback {
        override fun renderCustomTemplate(
            type: String,
            templateId: String,
            showingDuration: String?,
            htmlSourceCode: String
        ) {
            callbacks.map {
                it.renderCustomTemplate(type, templateId, showingDuration, htmlSourceCode)
            }

            val foregroundApp = AppUtil.getForegroundApp(this@EvsLauncherActivity)

            val videoPlayPage = (getTopFragment() is VideoFragment) ||
                TextUtils.equals(foregroundApp?.pkgName, "com.dianshijia.newlive") ||
                TextUtils.equals(foregroundApp?.pkgName, "com.qiyi.video.speaker") ||
                HaotuExternalPlayerImpl.getInstance(baseContext).isActive

            var templateShowingDuration = -1L //默认custom_template为不关闭
            if (showingDuration == "LONG") {
                templateShowingDuration = if (videoPlayPage) {
                    5 * 1000
                } else {
                    15 * 1000
                }
            } else if (showingDuration == "SHORT") {
                templateShowingDuration = if (videoPlayPage) {
                    3000
                } else {
                    5000
                }
            }

            val intent = Intent(this@EvsLauncherActivity, FloatingService::class.java)
            intent.action = FloatingService.ACTION_RENDER_CUSTOM_TEMPLATE
            intent.putExtra(FloatingService.EXTRA_TEMPLATE_TYPE, type)
            intent.putExtra(FloatingService.EXTRA_TEMPLATE_ID, templateId)
            intent.putExtra(FloatingService.EXTRA_TEMPLATE_HTML, htmlSourceCode)
            intent.putExtra(
                FloatingService.EXTRA_CUSTOM_TEMPLATE_SHOWING_DURATION,
                templateShowingDuration
            )
            ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")
        }

        override fun renderTemplate(payload: String) {
            callbacks.forEach {
                it.renderTemplate(payload)
            }
        }

        override fun notifyPlayerInfoUpdated(resourceId: String, payload: String) {
            callbacks.forEach {
                it.notifyPlayerInfoUpdated(resourceId, payload)
            }
        }

        override fun renderPlayerInfo(payload: String) {
            val main = findFragment(MainFragment2::class.java)
            main?.setupCover()
            callbacks.forEach {
                it.renderPlayerInfo(payload)
            }
        }

        override fun exitCustomTemplate() {
            callbacks.forEach {
                it.exitCustomTemplate()
            }
        }

        override fun exitStaticTemplate(type: String?) {
            callbacks.forEach {
                it.exitStaticTemplate(type)
            }
        }

        override fun exitPlayerInfo() {
            callbacks.forEach {
                it.exitPlayerInfo()
            }
        }

        override fun renderVideoPlayerInfo(payload: String) {
            val video = Gson().fromJson(payload, Video::class.java)

            if (getTopFragment() is VideoFragment) {
                if (!isActivityVisible) {
                    val intent = Intent(baseContext, EvsLauncherActivity::class.java)
                    intent.action = ACTION_START_VIDEO_PLAYER
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    applicationContext.startActivity(intent)
                }
            } else {
                if (video?.resourceId != ContentStorage.get().video?.resourceId) {
                    if (isActivityVisible) {
                        post {
                            findFragment(MainFragment2::class.java)?.startVideoPlayer()
                        }
                    } else {
                        val intent = Intent(baseContext, EvsLauncherActivity::class.java)
                        intent.action = ACTION_START_VIDEO_PLAYER
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        applicationContext.startActivity(intent)
                    }
                }
            }

            ContentStorage.get().saveVideo(video)

            callbacks.forEach {
                it.renderVideoPlayerInfo(payload)
            }
        }
    }
    private val evsStatusReceiver = object : SelfBroadcastReceiver(
        EngineService.ACTION_EVS_CONNECTED,
        EngineService.ACTION_REQUEST_CLOSE_VIDEO
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                EngineService.ACTION_EVS_CONNECTED -> {
                }
                EngineService.ACTION_REQUEST_CLOSE_VIDEO -> {
                    if (getTopFragment() is VideoFragment) {
                        pop()
                    }
                }
            }
        }

    }

    fun dismissOtaDialog() {
        if (requestOtaDialog?.isVisible == true) {
            requestOtaDialog?.dismissAllowingStateLoss()
        }
    }

    private val otaReceiver = object : SelfBroadcastReceiver(
        OtaService.ACTION_CHECK_UPDATE_RESULT,
        ACTION_OPEN_CHECK_UPDATE
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            if (ConfigUtils.getBoolean(ConfigUtils.KEY_OTA_DISABLED, false))
                return
            when (action) {
                OtaService.ACTION_CHECK_UPDATE_RESULT -> {
                    val packageEntity =
                        intent.getParcelableExtra<PackageEntityNew>(OtaService.EXTRA_PACKAGE_ENTITY)
                    if (packageEntity == null)
                        return
                    if (!isResume) {
                        val showNotification =
                            Intent(this@EvsLauncherActivity, FloatingService::class.java)
                        showNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
                        showNotification.putExtra(
                            FloatingService.EXTRA_ICON_RES,
                            R.drawable.ic_update_black_40dp
                        )
                        showNotification.putExtra(FloatingService.EXTRA_TAG, "update")
                        showNotification.putExtra(
                            FloatingService.EXTRA_MESSAGE,
                            getString(R.string.new_version_of_system_firmware)
                        )
                        showNotification.putExtra(
                            FloatingService.EXTRA_NEGATIVE_BUTTON_TEXT,
                            getString(R.string.do_it_later)
                        )
                        showNotification.putExtra(FloatingService.EXTRA_KEEPING, true)
                        showNotification.putExtra(
                            FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                            getString(R.string.check_detail)
                        )
                        showNotification.putExtra(
                            FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                            ACTION_OPEN_CHECK_UPDATE
                        )
                        ContextWrapper.startServiceAsUser(baseContext, showNotification, "CURRENT")
                    } else if (findFragment(CheckUpdateFragment::class.java) == null) {
                        val fragment = getTopFragment()
                        if (fragment is VideoFragment || fragment is PlayerInfoFragment2) {
                            val showNotification =
                                Intent(this@EvsLauncherActivity, FloatingService::class.java)
                            showNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
                            showNotification.putExtra(
                                FloatingService.EXTRA_ICON_RES,
                                R.drawable.ic_update_black_40dp
                            )
                            showNotification.putExtra(FloatingService.EXTRA_TAG, "update")
                            showNotification.putExtra(
                                FloatingService.EXTRA_MESSAGE,
                                getString(R.string.new_version_of_system_firmware)
                            )
                            showNotification.putExtra(
                                FloatingService.EXTRA_NEGATIVE_BUTTON_TEXT,
                                getString(R.string.do_it_later)
                            )
                            showNotification.putExtra(FloatingService.EXTRA_KEEPING, true)
                            showNotification.putExtra(
                                FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                getString(R.string.check_detail)
                            )
                            showNotification.putExtra(
                                FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                                ACTION_OPEN_CHECK_UPDATE
                            )
                            ContextWrapper.startServiceAsUser(
                                baseContext,
                                showNotification,
                                "CURRENT"
                            )
                        } else {
                            val dismissNotification =
                                Intent(baseContext, FloatingService::class.java)
                            dismissNotification.action = FloatingService.ACTION_DISMISS_NOTIFICATION
                            dismissNotification.putExtra(FloatingService.EXTRA_TAG, "update")
                            ContextWrapper.startServiceAsUser(
                                baseContext,
                                dismissNotification,
                                "CURRENT"
                            )

                            val packageEntityNew =
                                intent.getParcelableExtra<PackageEntityNew>(OtaService.EXTRA_PACKAGE_ENTITY)
                            val path = intent.getStringExtra(OtaService.EXTRA_PATH)
                            requestOtaDialog?.dismiss()
                            requestOtaDialog = StyledAlertDialog.Builder()
                                .setIcon(resources.getDrawable(R.drawable.ic_update_black_40dp))
                                .setTitle("发现新版本 ${packageEntityNew.versionName}")
                                .setMessage(packageEntityNew.description.toString())
                                .setPositiveButton(
                                    getString(R.string.check_detail),
                                    View.OnClickListener {
                                        val startMain =
                                            Intent(baseContext, EvsLauncherActivity::class.java)
                                        startMain.action = ACTION_OPEN_CHECK_UPDATE
                                        startMain.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        startActivity(startMain)
                                    })
                                .setNegativeButton(getString(R.string.do_it_later), null)
                                .show(supportFragmentManager)
                        }
                    }
                }
                ACTION_OPEN_CHECK_UPDATE -> {
                    val startMain = Intent(baseContext, EvsLauncherActivity::class.java)
                    startMain.action = ACTION_OPEN_CHECK_UPDATE
                    startMain.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(startMain)
                }
            }
        }
    }
    private val onDeviceModeChangeListener = object : EvsSystem.OnDeviceModeChangeListener {
        override fun onDeviceModeChanged(kid: Boolean) {
            handler.post {
                val storage = ContentStorage.get()
                storage.isMusicPlaying = false
                storage.savePlayInfo(null)
                updatePlayingState()
                val intent = Intent(baseContext, FloatingService::class.java).apply {
                    action = FloatingService.ACTION_UPDATE_MUSIC
                }
                ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")
                getService()?.getAudioPlayer()?.stop(AudioPlayer.TYPE_PLAYBACK)
                getService()?.getVideoPlayer()?.stop()
                val topFragment = getTopFragment()
                if (topFragment is PlayerInfoFragment2 || topFragment is VideoFragment) {
                    handler.post {
                        pop()
                    }
                }
            }
        }
    }
    private val fragmentLifecycleCallback = object : FragmentManager.FragmentLifecycleCallbacks() {
        private var setBackgroundToWhite = false
        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            super.onFragmentResumed(fm, f)

            if (!setBackgroundToWhite) {
                window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
                setBackgroundToWhite = false
            }

            if (f is PairFragment2) {
                VoiceButtonUtils.isPairing = true
            }
        }

        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            super.onFragmentPaused(fm, f)

            if (f is PairFragment2) {
                VoiceButtonUtils.isPairing = false
            }
        }
    }
    private val pageScrollCallback = object : EvsLauncher.PageScrollCallback {
        override fun onScrollToPrevious(): EvsLauncher.ScrollResult {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val screenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                pm.isInteractive
            } else {
                pm.isScreenOn
            }
            if (isResume) {
                getTopFragment()?.let {
                    return if (it is PageScrollable) {
                        if (it.scrollToPrevious()) {
                            EvsLauncher.ScrollResult(true)
                        } else {
                            EvsLauncher.ScrollResult(false, "没有上一页了")
                        }
                    } else {
                        EvsLauncher.ScrollResult(false, "该页面不支持此操作")
                    }
                } ?: run {
                    return EvsLauncher.ScrollResult(false)
                }
            } else {
                return when {
                    EvsAudioPlayer.get(baseContext).playbackState
                        == AudioPlayer.PLAYBACK_STATE_PLAYING -> {
                        EvsPlaybackController.get().sendCommand(PlaybackController.Command.Previous)
                        EvsLauncher.ScrollResult(true, "")
                    }
                    screenOn -> EvsLauncher.ScrollResult(false, "请先回到首页")
                    else -> EvsLauncher.ScrollResult(true, "")
                }
            }
        }

        override fun onScrollToNext(): EvsLauncher.ScrollResult {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val screenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                pm.isInteractive
            } else {
                pm.isScreenOn
            }
            if (isResume) {
                getTopFragment()?.let {
                    return if (it is PageScrollable) {
                        if (it.scrollToNext()) {
                            EvsLauncher.ScrollResult(true)
                        } else {
                            EvsLauncher.ScrollResult(false, "没有下一页了")
                        }
                    } else {
                        EvsLauncher.ScrollResult(false, "该页面不支持此操作")
                    }
                } ?: run {
                    return EvsLauncher.ScrollResult(false)
                }
            } else {
                return when {
                    EvsAudioPlayer.get(baseContext).playbackState
                        == AudioPlayer.PLAYBACK_STATE_PLAYING -> {
                        EvsPlaybackController.get().sendCommand(PlaybackController.Command.Next)
                        EvsLauncher.ScrollResult(true, "")
                    }
                    screenOn -> EvsLauncher.ScrollResult(false, "请先回到首页")
                    else -> EvsLauncher.ScrollResult(true, "")
                }
            }
        }
    }
    private val audioPlayerStateChangedListener = object : AudioPlayer.MediaStateChangedListener {
        override fun onStarted(player: AudioPlayer, type: String, resourceId: String) {
            if (type == AudioPlayer.TYPE_PLAYBACK) {
                ContentStorage.get().isMusicPlaying = true
                updatePlayingState()
            }
        }

        override fun onResumed(player: AudioPlayer, type: String, resourceId: String) {
            if (type == AudioPlayer.TYPE_PLAYBACK) {
                ContentStorage.get().isMusicPlaying = true
                updatePlayingState()
            }
        }

        override fun onPaused(player: AudioPlayer, type: String, resourceId: String) {
            if (type == AudioPlayer.TYPE_PLAYBACK) {
                ContentStorage.get().isMusicPlaying = false
                updatePlayingState()
            }
        }

        override fun onStopped(player: AudioPlayer, type: String, resourceId: String) {
            if (type == AudioPlayer.TYPE_PLAYBACK) {
                ContentStorage.get().isMusicPlaying = false
                updatePlayingState()
            }
        }

        override fun onCompleted(player: AudioPlayer, type: String, resourceId: String) {
            if (type == AudioPlayer.TYPE_PLAYBACK) {
                ContentStorage.get().isMusicPlaying = false
                updatePlayingState()
            }
        }

        override fun onPositionUpdated(
            player: AudioPlayer,
            type: String,
            resourceId: String,
            position: Long
        ) {
        }

        override fun onError(
            player: AudioPlayer,
            type: String,
            resourceId: String,
            errorCode: String
        ) {
        }

    }

    companion object {
        private const val TAG = "EvsLauncherActivity"
        private const val REQUEST_PERMISSION_CODE = 1001
        private const val REQUEST_OVERLAY_PERMISSION = 1002

        private const val ACTION_PREFIX = "com.iflytek.cyber.iot.show.core"
        const val ACTION_OPEN_HOME = "$ACTION_PREFIX.OPEN_HOME"
        const val ACTION_OPEN_SETTINGS = "$ACTION_PREFIX.OPEN_SETTINGS"
        const val ACTION_OPEN_WLAN = "$ACTION_PREFIX.OPEN_WLAN"
        const val ACTION_OPEN_AUTH = "$ACTION_PREFIX.OPEN_AUTH"
        const val ACTION_OPEN_SKILLS = "$ACTION_PREFIX.OPEN_SKILLS"
        const val ACTION_OPEN_CONTENT = "$ACTION_PREFIX.OPEN_CONTENT"
        const val ACTION_OPEN_ALARM = "$ACTION_PREFIX.OPEN_ALARM"
        const val ACTION_OPEN_CHECK_UPDATE = "$ACTION_PREFIX.OPEN_CHECK_UPDATE"
        const val ACTION_OPEN_MESSAGE_BOARD = "$ACTION_PREFIX.OPEN_MESSAGE_BOARD"
        const val ACTION_LAUNCHER_CONTROL = "$ACTION_PREFIX.LAUNCHER_CONTROL"
        const val ACTION_OPEN_WEB_PAGE = "$ACTION_PREFIX.ACTION_OPEN_WEB_PAGE"
        const val ACTION_OPEN_SEARCH = "$ACTION_PREFIX.ACTION_OPEN_SEARCH"
        const val ACTION_CLOSE_PLAYER_INFO = "$ACTION_PREFIX.ACTION_CLOSE_PLAYER_INFO"
        const val ACTION_OPEN_SMART_HOME = "$ACTION_PREFIX.ACTION_OPEN_SMART_HOME"
        const val PACKAGE_INSTALLED_ACTION = "$ACTION_PREFIX.PACKAGE_INSTALLED_ACTION"

        const val ACTION_START_PLAYER = "$ACTION_PREFIX.START_PLAYER"
        const val ACTION_START_VIDEO_PLAYER = "$ACTION_PREFIX.START_VIDEO_PLAYER"
        const val ACTION_PLAY = "$ACTION_PREFIX.PLAY"
        const val ACTION_PAUSE = "$ACTION_PREFIX.PAUSE"
        const val ACTION_NEXT = "$ACTION_PREFIX.NEXT"
        const val ACTION_PREVIOUS = "$ACTION_PREFIX.PREVIOUS"
        const val ACTION_QUIT = "$ACTION_PREFIX.QUIT"
        const val ACTION_WAKE_UP = "$ACTION_PREFIX.WAKE_UP"

        const val EXTRA_PAGE = "page"
        const val EXTRA_URL = "url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evs_launcher)

        Fragmentation.FragmentationBuilder()
//                .stackViewMode(if (BuildConfig.DEBUG)
//                    Fragmentation.BUBBLE else Fragmentation.NONE)
            .debug(BuildConfig.DEBUG)
            .install()
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallback, true)

        val intent = Intent(this, EngineService::class.java)
        ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")
        bindService(
            Intent(this, EngineService::class.java),
            serviceConnection, Context.BIND_AUTO_CREATE
        )

        evsStatusReceiver.register(this)

        ToneManager[this]

        ConfigUtils.init(this)

        // init config
        if (ConfigUtils.getString(ConfigUtils.KEY_RECOGNIZER_PROFILE, null) == null) {
            // 默认设为近场
            ConfigUtils.putString(
                ConfigUtils.KEY_RECOGNIZER_PROFILE,
                Recognizer.Profile.CloseTalk.toString()
            )
        }

//        MessageSocket.get().connectEvs(this)

        // 启动 OTA 服务
        val otaService = Intent(baseContext, OtaService::class.java)
        otaService.action = OtaService.ACTION_START_SERVICE
        otaService.putExtra(OtaService.EXTRA_CLIENT_ID, BuildConfig.CLIENT_ID)
        otaService.putExtra(OtaService.EXTRA_DEVICE_ID, DeviceUtils.getDeviceId(this))
        otaService.putExtra(OtaService.EXTRA_VERSION_ID, DeviceUtils.getSystemVersion())
        otaService.putExtra(OtaService.EXTRA_OTA_SECRET, BuildConfig.OTA_SECRET)
        otaService.putExtra(OtaService.EXTRA_VERSION, BuildConfig.VERSION_CODE)
        otaService.putExtra(OtaService.EXTRA_DOWNLOAD_PATH, "${externalCacheDir?.path}/ota")
        otaService.putExtra(OtaService.EXTRA_PACKAGE_NAME, packageName)
//        otaService.putExtra(OtaService.EXTRA_DOWNLOAD_DIRECTLY, true)
        ContextWrapper.startServiceAsUser(baseContext, otaService, "CURRENT")

        otaReceiver.register(this)

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
    }

    private fun updatePlayingState() {
        val main = findFragment(MainFragment2::class.java)
        main?.setupCover()
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        setImmersiveFlags()

        if (!isAllPermissionsReady()) {
            requestPermission()
        } else {
            if (!GlobalRecorder.isRecording) {
                GlobalRecorder.init(this)
                GlobalRecorder.startRecording()
            }

            val overlayPermissionCallback = { result: Boolean ->
                if (!result) {
                    if (!requestFloatPermission) {
                        requestFloatPermission = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            intent.data = Uri.parse("package:$packageName")
                            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
                        }
                    } else {
                        requestFloatPermissionAlert?.dismiss()
                        requestFloatPermissionAlert = AlertDialog.Builder(this)
                            .setTitle("请先授予悬浮窗显示权限")
                            .setCancelable(false)
                            .setPositiveButton("去允许") { _, _ ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                    intent.data = Uri.parse("package:$packageName")
                                    startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
                                }
                            }
                            .show()
                    }
                } else {
                    // init overlay
                    val overlay = Intent(this, FloatingService::class.java)
                    overlay.action = FloatingService.ACTION_INIT_OVERLAY
                    ContextWrapper.startServiceAsUser(baseContext, overlay, "CURRENT")

                    requestFloatPermissionAlert?.dismiss()

                    if (!isCanWriteSystem()) {
                        requestWriteSystemAlert?.dismiss()
                        requestWriteSystemAlert = AlertDialog.Builder(this)
                            .setTitle("请先授予修改系统设置权限")
                            .setCancelable(false)
                            .setPositiveButton("去允许") { _, _ ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    intent.data =
                                        Uri.parse("package:com.iflytek.cyber.iot.show.core")
                                    startActivity(intent)
                                }
                            }
                            .show()
                    } else {
                        requestWriteSystemAlert?.dismiss()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                            val mode = appOps.checkOpNoThrow(
                                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(),
                                packageName
                            )

                            if (mode != AppOpsManager.MODE_ALLOWED) {
                                requestUsageDialog?.dismiss()
                                requestUsageDialog = AlertDialog.Builder(this)
                                    .setTitle("请先授予应用使用情况读取权限")
                                    .setCancelable(false)
                                    .setPositiveButton(android.R.string.yes) { _, _ ->
                                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }
                                    .show()
                            } else {
                                requestUsageDialog?.dismiss()

                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                                    val notificationManager =
                                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    if (!notificationManager.isNotificationPolicyAccessGranted) {
                                        requestNotifyPolicyDialog?.dismiss()
                                        requestNotifyPolicyDialog = AlertDialog.Builder(this)
                                            .setTitle("请先授予应用控制免打扰状态权限")
                                            .setCancelable(false)
                                            .setPositiveButton(android.R.string.yes) { _, _ ->
                                                val notificationPolicy =
                                                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                                notificationPolicy.flags =
                                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                                startActivity(notificationPolicy)
                                            }
                                            .show()
                                    } else {
                                        requestNotifyPolicyDialog?.dismiss()

                                        if (SupportHelper.getTopFragment(supportFragmentManager) == null)
                                            initUi()
                                    }
                                }

                                if (!hadInitVolume) {
                                    hadInitVolume = true

                                    val initVolume = Intent(this, EngineService::class.java)
                                    initVolume.action = EngineService.ACTION_INIT_VOLUME
                                    startService(initVolume)
                                }
//                                    }
//                            }
                            }
                        }
                    }
                }
            }
            if (requestFloatPermission) {
                OverlayUtils.hasPermissionOnActivityResultAwait(this, overlayPermissionCallback)
            } else {
                OverlayUtils.hasPermissionAwait(this, overlayPermissionCallback)
            }
        }

        if (!EvsTemplate.get().isOtherTemplateFocused && !EvsAlarm.get(this).isPlaying())
            getTopFragment()?.let { fragment ->
                if (fragment !is VideoFragment
                    && EvsVideoPlayer.get(baseContext).state != VideoPlayer.STATE_PLAYING
                ) {
                    // * 顶部为视频时说明视觉焦点在视频上，不需要申请 launcher 的视觉焦点
                    // * 若顶部不是视频，但是 VideoPlayer 状态为播放，说明当前正在打开视
                    //   频界面，不需要申请 launcher 的视觉焦点
                    engineService?.requestLauncherVisualFocus()
                }
            } ?: run {
                engineService?.requestLauncherVisualFocus()
            }
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false

        getTopFragment().let {
            if (it is VideoFragment) {
                pop()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)

        unbindService(serviceConnection)

        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallback)

        evsStatusReceiver.unregister(this)

        otaReceiver.unregister(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        when (intent?.action) {
            ACTION_OPEN_SETTINGS -> {
                if (getTopFragment() !is SettingsFragment2) {
                    popTo(MainFragment2::class.java, false)
                    start(SettingsFragment2())
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_WLAN -> {
                if (getTopFragment() !is WifiSettingsFragment) {
                    popTo(MainFragment2::class.java, false)
                    start(WifiSettingsFragment.newInstance())
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_AUTH -> {
                popTo(MainFragment2::class.java, false)
                startWithPopTo(PairFragment2(), PairFragment2::class.java, true)
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_HOME -> {
                if (findFragment(MainFragment2::class.java) == null)
                    start(MainFragment2())
                else {
                    if (getTopFragment() !is MainFragment2)
                        popTo(MainFragment2::class.java, false)
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_MESSAGE_BOARD -> {
                if (getTopFragment() !is MessageBoardFragment) {
                    popTo(MainFragment2::class.java, false)
                    start(MessageBoardFragment())
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_LAUNCHER_CONTROL -> {
                val page = intent.getStringExtra(EXTRA_PAGE)
                openLauncherPage(page)

                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_CHECK_UPDATE -> {
                if (getTopFragment() !is CheckUpdateFragment) {
                    popTo(MainFragment2::class.java, false)
                    start(CheckUpdateFragment())
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_START_PLAYER -> {
                if (getTopFragment() !is PlayerInfoFragment2) {
                    val main = findFragment(MainFragment2::class.java)
                    main?.startPlayerInfo()
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_SKILLS -> {
                if (getTopFragment() !is SkillsFragment) {
                    popTo(MainFragment2::class.java, false)
                    start(SkillsFragment())
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_CONTENT -> {
                if (getTopFragment() !is MediaFragment) {
                    popTo(MainFragment2::class.java, false)
                    start(MediaFragment())
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_ALARM -> {
                if (getTopFragment() !is AlarmFragment) {
                    popTo(MainFragment2::class.java, false)
                    findFragment(MainFragment2::class.java)?.startAlarm()
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_SEARCH -> {
                if (getTopFragment() !is SearchFragment) {
                    popTo(MainFragment2::class.java, false)
                    start(SearchFragment())
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_SMART_HOME -> {
                if (getTopFragment() !is SmartHomeFragment) {
                    popTo(MainFragment2::class.java, false)
                    start(SmartHomeFragment())
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_START_VIDEO_PLAYER -> {
                if (getTopFragment() !is VideoFragment) {
                    val main = findFragment(MainFragment2::class.java)
                    post {
                        main?.startVideoPlayer()
                    }
                }
            }
            ACTION_OPEN_WEB_PAGE -> {
                intent.getStringExtra(EXTRA_URL)?.let { url ->
                    val webViewFragment = WebViewFragment()
                    val arguments = Bundle()
                    arguments.putString("url", url)
                    webViewFragment.arguments = arguments
                    start(webViewFragment)
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFloatingServiceAction(action: String) {
        when (action) {
            ACTION_CLOSE_PLAYER_INFO -> {
                updatePlayingState()
                if (getTopFragment() is PlayerInfoFragment2) {
                    pop()
                } else {
                    val fragment = findFragment(PlayerInfoFragment2::class.java)
                    if (fragment != null) {
                        popTo(PlayerInfoFragment2::class.java, false)
                    }
                }
            }
        }
    }

    fun openLauncherPage(page: String, preventPopToMain: Boolean = false) {
        when (page) {
            Launcher.PAGE_ALARMS -> {
                if (getTopFragment() !is AlarmFragment) {
                    if (!preventPopToMain)
                        popTo(MainFragment2::class.java, false)
                    findFragment(MainFragment2::class.java)?.startAlarm()
                }
            }
            Launcher.PAGE_CONTENTS -> {
                if (getTopFragment() !is MediaFragment) {
                    if (!preventPopToMain)
                        popTo(MainFragment2::class.java, false)
                    start(MediaFragment())
                }
            }
            Launcher.PAGE_HOME -> {
                if (getTopFragment() !is MainFragment2)
                    popTo(MainFragment2::class.java, false)
            }
            Launcher.PAGE_MESSAGES -> {
                if (getTopFragment() !is MessageBoardFragment) {
                    if (!preventPopToMain)
                        popTo(MainFragment2::class.java, false)
                    start(MessageBoardFragment())
                }
            }
            Launcher.PAGE_SETTINGS -> {
                if (getTopFragment() !is SettingsFragment2) {
                    if (!preventPopToMain)
                        popTo(MainFragment2::class.java, false)
                    start(SettingsFragment2())
                }
            }
            Launcher.PAGE_SKILLS -> {
                if (getTopFragment() !is SkillsFragment) {
                    if (!preventPopToMain)
                        popTo(MainFragment2::class.java, false)
                    start(SkillsFragment())
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        setImmersiveFlags()
    }

    fun setImmersiveFlags() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun handleNetworkLost() {

    }

    fun showWelcome() {
        runOnUiThread {
            VoiceButtonUtils.isWelcoming = true

            welcome_container.isVisible = true
            welcome_container.translationX = 0f
            welcome_container.alpha = 1f
            welcome_loading.playAnimation()
            welcome_loading.repeatCount = LottieDrawable.INFINITE
            welcome_container.postDelayed({
                welcome_loading.playAnimation()
                welcome_container.animate()
                    .alpha(0f)
                    .translationX(-welcome_container.width.toFloat())
                    .setDuration(300)
                    .withEndAction {
                        welcome_container.isVisible = false

                        VoiceButtonUtils.isWelcoming = false
                    }
                    .start()
            }, 5000)
        }
    }

    private fun initUi() {
        val engineService = engineService ?: return
        if (!isAllPermissionsReady())
            return
        if (engineService.getAuthResponse() == null) {
            if (ConfigUtils.getBoolean(ConfigUtils.KEY_SETUP_COMPLETED, false)
                && !WifiUtils.getConnectedSsid(this).isNullOrEmpty()
            ) {
                if (findFragment(PairFragment2::class.java) == null) {
                    loadRootFragment(R.id.fragment_container, PairFragment2())
                }
            } else {
                // 这里有可能是 null 取了默认值 false，存一下 false 保证其他地方读到的值是一样的
                ConfigUtils.putBoolean(ConfigUtils.KEY_SETUP_COMPLETED, false)
                if (findFragment(WifiSettingsFragment::class.java) == null) {
                    loadRootFragment(
                        R.id.fragment_container,
                        WifiSettingsFragment.newInstance(true)
                    )
                }

                // 引导配置时应先禁用唤醒
                val enableWakeUp = Intent(this, EngineService::class.java)
                enableWakeUp.action = EngineService.ACTION_SET_WAKE_UP_ENABLED
                enableWakeUp.putExtra(EngineService.EXTRA_ENABLED, false)
                ContextWrapper.startServiceAsUser(baseContext, enableWakeUp, "CURRENT")
            }
        } else {
            ConfigUtils.putBoolean(ConfigUtils.KEY_SETUP_COMPLETED, true)
            if (findFragment(MainFragment2::class.java) == null) {
                loadRootFragment(R.id.fragment_container, MainFragment2.newInstance())
            }
        }
    }

    private fun isCanWriteSystem() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(this)
        } else {
            true
        }

    private fun isAllPermissionsReady() =
        (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PermissionChecker.PERMISSION_GRANTED && PermissionChecker.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        )
            == PermissionChecker.PERMISSION_GRANTED && PermissionChecker.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )
            == PermissionChecker.PERMISSION_GRANTED && PermissionChecker.checkSelfPermission(
            this, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
            == PermissionChecker.PERMISSION_GRANTED)

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            REQUEST_PERMISSION_CODE
        )
    }

}