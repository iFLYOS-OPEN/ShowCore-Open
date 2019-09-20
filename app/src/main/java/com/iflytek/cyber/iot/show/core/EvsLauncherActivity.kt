package com.iflytek.cyber.iot.show.core

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.net.*
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.EvsError
import com.iflytek.cyber.evs.sdk.agent.*
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.fragment.*
import com.iflytek.cyber.iot.show.core.impl.system.EvsSystem
import com.iflytek.cyber.iot.show.core.impl.template.EvsTemplate
import com.iflytek.cyber.iot.show.core.message.MessageRecorder
import com.iflytek.cyber.iot.show.core.model.ActionConstant
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.PlayerInfoPayload
import com.iflytek.cyber.iot.show.core.model.Video
import com.iflytek.cyber.iot.show.core.record.GlobalRecorder
import com.iflytek.cyber.iot.show.core.record.RecordVolumeUtils
import com.iflytek.cyber.iot.show.core.task.SleepWorker
import com.iflytek.cyber.iot.show.core.utils.*
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.iflytek.cyber.product.ota.OtaService
import com.iflytek.cyber.product.ota.PackageEntityNew
import me.yokeyword.fragmentation.Fragmentation
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.ref.SoftReference
import kotlin.math.roundToInt


class EvsLauncherActivity : BaseActivity() {
    // mutable
    private var engineService: EngineService? = null
    private var requestFloatPermission = false
    private var requestFloatPermissionAlert: AlertDialog? = null
    private var requestWriteSystemAlert: AlertDialog? = null
    private var requestUsageDialog: AlertDialog? = null
    private var requestOtaDialog: StyledAlertDialog? = null
    private var requestNotifyPolicyDialog: AlertDialog? = null
    private var reconnectHandler: ReconnectHandler? = null

    private var callbacks = ArrayList<EvsTemplate.RenderCallback>()

    private var networkCallback: Any? = null // 不声明 NetworkCallback 的类，否则 L 以下会找不到类

    private var isActivityVisible = false

    private var handler = Handler(Looper.getMainLooper())

    private val connectStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            @Suppress("DEPRECATION")
            when (intent.action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    // api 21 以上应使用 networkCallback
                    val connectivityManager =
                        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                    if (connectivityManager.activeNetworkInfo?.isConnected == true) {
                        engineService?.connectEvs(DeviceUtils.getDeviceId(context))
                    }
                }
            }
        }
    }

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

                    reconnectHandler = ReconnectHandler(it)

                    if (isResume) {
                        it.requestLauncherVisualFocus()
                    }

                    engineService = it
                }

                initUi()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
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

        override fun onIntermediateText(text: String) {
        }

    }
    private val authReceiver = object : SelfBroadcastReceiver(
        ActionConstant.ACTION_CLIENT_AUTH_REFRESHED
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                ActionConstant.ACTION_CLIENT_AUTH_REFRESHED -> {
                    engineService?.connectEvs(DeviceUtils.getDeviceId(baseContext))
                }
            }
        }
    }
    private val recordObserver = object : GlobalRecorder.Observer {
        override fun onAudioData(array: ByteArray, offset: Int, length: Int) {
            val volume = RecordVolumeUtils.calculateVolume(array, length)

            val intent = Intent(baseContext, FloatingService::class.java)
            intent.action = FloatingService.ACTION_UPDATE_VOLUME
            intent.putExtra(FloatingService.EXTRA_VOLUME, volume)
            startService(intent)
        }
    }

    private fun restartApp() {
        if (!isActivityVisible) {
            val intent = Intent(applicationContext, EvsLauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            applicationContext.startActivity(intent)
        }
    }

    private val templateRenderCallback = object : EvsTemplate.RenderCallback {
        override fun renderTemplate(payload: String) {
            callbacks.forEach {
                it.renderTemplate(payload)
            }
            val intent = Intent(this@EvsLauncherActivity, FloatingService::class.java)
            intent.action = FloatingService.ACTION_RENDER_TEMPLATE
            intent.putExtra(FloatingService.EXTRA_PAYLOAD, payload)
            startService(intent)
        }

        override fun notifyPlayerInfoUpdated(resourceId: String, payload: String) {
            callbacks.forEach {
                it.notifyPlayerInfoUpdated(resourceId, payload)
            }
        }

        override fun renderPlayerInfo(payload: String) {
            val playerInfo = Gson().fromJson(payload, PlayerInfoPayload::class.java)
            ContentStorage.get().savePlayInfo(playerInfo)
            ContentStorage.get().isMusicPlaying = true
            val main = findFragment(MainFragment2::class.java)
            main?.setupCover()
            if (playerInfo.shouldPopup) {
                restartApp()
                if (getTopFragment() !is PlayerInfoFragment2) {
                    main?.startPlayerInfo()
                }
            }
            val intent = Intent(baseContext, FloatingService::class.java).apply {
                action = FloatingService.ACTION_UPDATE_MUSIC
            }
            startService(intent)
            callbacks.forEach {
                it.renderPlayerInfo(payload)
            }
        }

        override fun exitCustomTemplate() {
            val intent = Intent(baseContext, FloatingService::class.java)
            intent.action = FloatingService.ACTION_CLEAR_TEMPLATE
            startService(intent)

            callbacks.forEach {
                it.exitCustomTemplate()
            }
        }

        override fun exitStaticTemplate(type: String?) {
            val intent = Intent(baseContext, FloatingService::class.java)
            intent.action = FloatingService.ACTION_CLEAR_TEMPLATE
            intent.putExtra(FloatingService.EXTRA_TYPE, type)
            startService(intent)

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
            ContentStorage.get().saveVideo(video)
            restartApp()
            if (getTopFragment() !is VideoFragment) {
                val main = findFragment(MainFragment2::class.java)
                main?.startVideoPlayer()
            }
            callbacks.forEach {
                it.renderVideoPlayerInfo(payload)
            }
        }
    }
    private val onConfigChangedListener = object : ConfigUtils.OnConfigChangedListener {
        override fun onConfigChanged(key: String, value: Any?) {
            when (key) {
                ConfigUtils.KEY_RECOGNIZER_PROFILE -> {
                    if (value == Recognizer.Profile.CloseTalk.toString()) {
                        engineService?.getRecognizer()?.profile = Recognizer.Profile.CloseTalk
                    } else if (value == Recognizer.Profile.FarField.toString()) {
                        engineService?.getRecognizer()?.profile = Recognizer.Profile.FarField
                    }
                }
            }
        }
    }
    private val evsStatusReceiver = object : SelfBroadcastReceiver(
        EngineService.ACTION_EVS_CONNECTED,
        EngineService.ACTION_EVS_CONNECT_FAILED,
        EngineService.ACTION_EVS_DISCONNECTED
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                EngineService.ACTION_EVS_CONNECTED -> {
                    val dismissNotification =
                        Intent(baseContext, FloatingService::class.java)
                    dismissNotification.action = FloatingService.ACTION_DISMISS_NOTIFICATION
                    dismissNotification.putExtra(FloatingService.EXTRA_TAG, "network_error")
                    startService(dismissNotification)

                    reconnectHandler?.clearRetryCount()

                    val microphoneEnabled =
                        ConfigUtils.getBoolean(ConfigUtils.KEY_MICROPHONE_ENABLED, true)

                    val enableWakeUp = Intent(baseContext, EngineService::class.java)
                    enableWakeUp.action = EngineService.ACTION_SET_WAKE_UP_ENABLED
                    enableWakeUp.putExtra(EngineService.EXTRA_ENABLED, microphoneEnabled)
                    startService(enableWakeUp)

                    ConfigUtils.getBoolean(ConfigUtils.KEY_OTA_REQUEST, false).let { otaRequest ->
                        if (otaRequest) {
                            Log.d(TAG, ConfigUtils.getAll().toString())
                            val versionId = ConfigUtils.getInt(ConfigUtils.KEY_OTA_VERSION_ID, -1)
                            if (BuildConfig.VERSION_CODE >= versionId) {
                                val versionName =
                                    ConfigUtils.getString(ConfigUtils.KEY_OTA_VERSION_NAME, null)
                                val versionDescription = ConfigUtils.getString(
                                    ConfigUtils.KEY_OTA_VERSION_DESCRIPTION,
                                    null
                                )

                                EvsSystem.get().sendUpdateSoftwareFinished(
                                    versionName.toString(), versionDescription
                                )
                            } else {
                                EvsSystem.get()
                                    .sendUpdateSoftwareFailed(System.ERROR_TYPE_INSTALL_ERROR)
                            }
                            ConfigUtils.remove(ConfigUtils.KEY_OTA_REQUEST)
                            ConfigUtils.remove(ConfigUtils.KEY_OTA_VERSION_DESCRIPTION)
                            ConfigUtils.remove(ConfigUtils.KEY_OTA_VERSION_NAME)
                        }
                    }
                }
                EngineService.ACTION_EVS_DISCONNECTED -> {
                    val code =
                        intent.getIntExtra(EngineService.EXTRA_CODE, EvsError.Code.ERROR_UNKNOWN)
                    val message = intent.getStringExtra(EngineService.EXTRA_MESSAGE)
                    val fromRemote = intent.getBooleanExtra(EngineService.EXTRA_FROM_REMOTE, false)
                    if (fromRemote) {
                        if (code == EvsError.Code.ERROR_AUTH_FAILED) {
                            val disconnectNotification =
                                Intent(baseContext, FloatingService::class.java)
                            disconnectNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
                            disconnectNotification.putExtra(
                                FloatingService.EXTRA_MESSAGE,
                                getString(R.string.message_evs_auth_expired)
                            )
                            disconnectNotification.putExtra(FloatingService.EXTRA_TAG, "auth_error")
                            disconnectNotification.putExtra(
                                FloatingService.EXTRA_ICON_RES,
                                R.drawable.ic_default_error_white_40dp
                            )
                            disconnectNotification.putExtra(
                                FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                getString(R.string.re_auth)
                            )
                            disconnectNotification.putExtra(
                                FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                                getString(R.string.re_auth)
                            )
                            disconnectNotification.putExtra(
                                FloatingService.EXTRA_KEEPING, true
                            )
                            startService(disconnectNotification)
                        }
                    } else {
                        val handler = reconnectHandler
                        if (!WifiUtils.getConnectedSsid(baseContext).isNullOrEmpty()) {
                            val disconnectNotification =
                                Intent(baseContext, FloatingService::class.java)
                            disconnectNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
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
                            startService(disconnectNotification)
                        }
                        if (handler?.isCounting() != true) {
                            handler?.postReconnectEvs()
                        }
                    }
                }
                EngineService.ACTION_EVS_CONNECT_FAILED -> {
                    val handler = reconnectHandler
                    if (handler?.isCounting() != true) {
                        handler?.postReconnectEvs()
                    }
                }
            }
        }

    }
    private val otaReceiver = object : SelfBroadcastReceiver(
        OtaService.ACTION_NEW_UPDATE_DOWNLOADED,
        ACTION_OPEN_CHECK_UPDATE
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                OtaService.ACTION_NEW_UPDATE_DOWNLOADED -> {
                    if (!isResume) {
                        val showNotification =
                            Intent(this@EvsLauncherActivity, FloatingService::class.java)
                        showNotification.action = FloatingService.ACTION_SHOW_NOTIFICATION
                        showNotification.putExtra(
                            FloatingService.EXTRA_ICON_RES,
                            R.drawable.ic_update_black_40dp
                        )
                        showNotification.putExtra(FloatingService.EXTRA_TAG, "update")
                        showNotification.putExtra(FloatingService.EXTRA_MESSAGE, "系统有新版本可用")
                        showNotification.putExtra(
                            FloatingService.EXTRA_NEGATIVE_BUTTON_TEXT,
                            "稍后再说"
                        )
                        showNotification.putExtra(FloatingService.EXTRA_KEEPING, true)
                        showNotification.putExtra(
                            FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                            "立即升级"
                        )
                        showNotification.putExtra(
                            FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                            ACTION_OPEN_CHECK_UPDATE
                        )
                        startService(showNotification)
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
                            showNotification.putExtra(FloatingService.EXTRA_MESSAGE, "系统有新版本可用")
                            showNotification.putExtra(
                                FloatingService.EXTRA_NEGATIVE_BUTTON_TEXT,
                                "稍后再说"
                            )
                            showNotification.putExtra(FloatingService.EXTRA_KEEPING, true)
                            showNotification.putExtra(
                                FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                                "立即升级"
                            )
                            showNotification.putExtra(
                                FloatingService.EXTRA_POSITIVE_BUTTON_ACTION,
                                ACTION_OPEN_CHECK_UPDATE
                            )
                            startService(showNotification)
                        } else {
                            val dismissNotification =
                                Intent(baseContext, FloatingService::class.java)
                            dismissNotification.action = FloatingService.ACTION_DISMISS_NOTIFICATION
                            dismissNotification.putExtra(FloatingService.EXTRA_TAG, "update")
                            startService(dismissNotification)

                            val packageEntityNew =
                                intent.getParcelableExtra<PackageEntityNew>(OtaService.EXTRA_PACKAGE_ENTITY)
                            val path = intent.getStringExtra(OtaService.EXTRA_PATH)
                            requestOtaDialog?.dismiss()
                            requestOtaDialog = StyledAlertDialog.Builder()
                                .setIcon(resources.getDrawable(R.drawable.ic_update_black_40dp))
                                .setTitle("发现新版本 v ${packageEntityNew.versionName}")
                                .setMessage(packageEntityNew.description.toString())
                                .setPositiveButton("现在安装", View.OnClickListener {
                                    PackageUtils.notifyInstallApk(it.context, path)

                                    ConfigUtils.putBoolean(ConfigUtils.KEY_OTA_REQUEST, true)
                                    ConfigUtils.putInt(
                                        ConfigUtils.KEY_OTA_VERSION_ID,
                                        packageEntityNew.versionId?.toInt()
                                            ?: 0
                                    )
                                    ConfigUtils.putString(
                                        ConfigUtils.KEY_OTA_VERSION_NAME,
                                        packageEntityNew.versionName
                                    )
                                    ConfigUtils.putString(
                                        ConfigUtils.KEY_OTA_VERSION_DESCRIPTION,
                                        packageEntityNew.description
                                    )
                                })
                                .setNegativeButton("稍后再说", null)
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
                startService(intent)
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

    companion object {
        private const val TAG = "EvsLauncherActivity"
        private const val REQUEST_PERMISSION_CODE = 1001
        private const val REQUEST_OVERLAY_PERMISSION = 1002

        private const val ACTION_PREFIX = "com.iflytek.cyber.iot.show.core"
        const val ACTION_OPEN_HOME = "$ACTION_PREFIX.OPEN_HOME"
        const val ACTION_OPEN_SETTINGS = "$ACTION_PREFIX.OPEN_SETTINGS"
        const val ACTION_OPEN_WLAN = "$ACTION_PREFIX.OPEN_WLAN"
        const val ACTION_OPEN_AUTH = "$ACTION_PREFIX.OPEN_AUTH"
        const val ACTION_OPEN_CHECK_UPDATE = "$ACTION_PREFIX.OPEN_CHECK_UPDATE"
        const val ACTION_OPEN_MESSAGE_BOARD = "$ACTION_PREFIX.OPEN_MESSAGE_BOARD"
        const val ACTION_LAUNCHER_CONTROL = "$ACTION_PREFIX.LAUNCHER_CONTROL"

        const val ACTION_START_PLAYER = "$ACTION_PREFIX.START_PLAYER"
        const val ACTION_PLAY = "$ACTION_PREFIX.PLAY"
        const val ACTION_PAUSE = "$ACTION_PREFIX.PAUSE"
        const val ACTION_NEXT = "$ACTION_PREFIX.NEXT"
        const val ACTION_PREVIOUS = "$ACTION_PREFIX.PREVIOUS"
        const val ACTION_QUIT = "$ACTION_PREFIX.QUIT"
        const val ACTION_WAKE_UP = "$ACTION_PREFIX.WAKE_UP"

        const val EXTRA_PAGE = "page"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_evs_launcher)

        Fragmentation.FragmentationBuilder()
//                .stackViewMode(if (BuildConfig.DEBUG)
//                    Fragmentation.BUBBLE else Fragmentation.NONE)
            .debug(BuildConfig.DEBUG)
            .install()

        val intent = Intent(this, EngineService::class.java)
        startService(intent)
        bindService(
            Intent(this, EngineService::class.java),
            serviceConnection, Context.BIND_AUTO_CREATE
        )

        authReceiver.register(this)

        evsStatusReceiver.register(this)

        ToneManager[this]
        GlobalRecorder.registerObserver(recordObserver)
        GlobalRecorder.registerObserver(MessageRecorder)

        ConfigUtils.init(this)
        ConfigUtils.registerOnConfigChangedListener(onConfigChangedListener)

        // init config
        if (ConfigUtils.getString(ConfigUtils.KEY_RECOGNIZER_PROFILE, null) == null) {
            // 默认设为远场
            ConfigUtils.putString(
                ConfigUtils.KEY_RECOGNIZER_PROFILE,
                Recognizer.Profile.FarField.toString()
            )
        }

//        MessageSocket.get().connectEvs(this)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            val connectStateFilter = IntentFilter()
            @Suppress("DEPRECATION")
            connectStateFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            registerReceiver(connectStateReceiver, connectStateFilter)
        } else {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network?) {
                    super.onAvailable(network)

                    engineService?.connectEvs(DeviceUtils.getDeviceId(baseContext))
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)

            this.networkCallback = networkCallback
        }

        // 启动 OTA 服务
        val otaService = Intent(baseContext, OtaService::class.java)
        otaService.action = OtaService.ACTION_START_SERVICE
        otaService.putExtra(OtaService.EXTRA_CLIENT_ID, BuildConfig.CLIENT_ID)
        otaService.putExtra(OtaService.EXTRA_DEVICE_ID, DeviceUtils.getDeviceId(this))
        otaService.putExtra(OtaService.EXTRA_VERSION_ID, BuildConfig.VERSION_CODE)
        otaService.putExtra(OtaService.EXTRA_OTA_SECRET, BuildConfig.OTA_SECRET)
        otaService.putExtra(OtaService.EXTRA_VERSION, BuildConfig.VERSION_CODE)
        otaService.putExtra(OtaService.EXTRA_DOWNLOAD_PATH, externalCacheDir?.path.toString())
        otaService.putExtra(OtaService.EXTRA_PACKAGE_NAME, packageName)
        otaService.putExtra(OtaService.EXTRA_DOWNLOAD_DIRECTLY, true)
        startService(otaService)

        otaReceiver.register(this)
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        isActivityVisible = false
        EventBus.getDefault().unregister(this)
    }

    private fun updatePlayingState() {
        val main = findFragment(MainFragment2::class.java)
        main?.setupCover()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updateMusicControlAction(action: String) {
        when (action) {
            ACTION_START_PLAYER -> {
                if (getTopFragment() !is PlayerInfoFragment2) {
                    val main = findFragment(MainFragment2::class.java)
                    main?.startPlayerInfo()
                }
            }
            ACTION_NEXT -> {
                val playback = getService()?.getPlaybackController()
                playback?.sendCommand(PlaybackController.Command.Next)
            }
            ACTION_PREVIOUS -> {
                val playback = getService()?.getPlaybackController()
                playback?.sendCommand(PlaybackController.Command.Previous)
            }
            ACTION_PLAY -> {
                ContentStorage.get().isMusicPlaying = true
                val player = getService()?.getAudioPlayer()
                player?.resume(AudioPlayer.TYPE_PLAYBACK)
                updatePlayingState()
            }
            ACTION_PAUSE -> {
                ContentStorage.get().isMusicPlaying = false
                val player = getService()?.getAudioPlayer()
                player?.pause(AudioPlayer.TYPE_PLAYBACK)
                updatePlayingState()
            }
            ACTION_QUIT -> {
                val player = getService()?.getAudioPlayer()
                player?.stop(AudioPlayer.TYPE_PLAYBACK)
                val storage = ContentStorage.get()
                storage.isMusicPlaying = false
                storage.savePlayInfo(null)
                updatePlayingState()
                val intent = Intent(baseContext, FloatingService::class.java).apply {
                    setAction(FloatingService.ACTION_UPDATE_MUSIC)
                }
                startService(intent)
                val topFragment = getTopFragment()
                if (topFragment is PlayerInfoFragment2) {
                    handler.post {
                        pop()
                    }
                }
            }
            ACTION_WAKE_UP -> {
                SleepWorker.get(this).hideSleepView(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
        setImmersiveFlags()

        if (!isAllPermissionsReady()) {
            requestPermission()
        } else {
            if (!GlobalRecorder.isRecording) {
                GlobalRecorder.init()
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
                    startService(overlay)

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
                                "android:get_usage_stats", Process.myUid(),
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

                                        initVolume()
                                    }
                                }
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

        engineService?.requestLauncherVisualFocus()
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)

        GlobalRecorder.unregisterObserver(recordObserver)
        GlobalRecorder.unregisterObserver(MessageRecorder)

        authReceiver.unregister(this)

        evsStatusReceiver.unregister(this)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            unregisterReceiver(connectStateReceiver)
        } else {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.let { connectivityManager ->
                    val networkCallback =
                        (this.networkCallback as? ConnectivityManager.NetworkCallback)
                            ?: return
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
        }

        ConfigUtils.unregisterOnConfigChangedListener(onConfigChangedListener)
        ConfigUtils.destroy()

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
                if (getTopFragment() !is PairFragment2) {
                    popTo(MainFragment2::class.java, false)
                    start(PairFragment2())
                }
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
                when (intent.getStringExtra(EXTRA_PAGE)) {
                    Launcher.PAGE_ALARMS -> {
                        if (getTopFragment() !is AlarmFragment) {
                            popTo(MainFragment2::class.java, false)
                            start(AlarmFragment())
                        }
                    }
                    Launcher.PAGE_CONTENTS -> {
                        if (getTopFragment() !is MediaFragment) {
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
                            popTo(MainFragment2::class.java, false)
                            start(MessageBoardFragment())
                        }
                    }
                    Launcher.PAGE_SETTINGS -> {
                        if (getTopFragment() !is SettingsFragment2) {
                            popTo(MainFragment2::class.java, false)
                            start(SettingsFragment2())
                        }
                    }
                    Launcher.PAGE_SKILLS -> {
                        if (getTopFragment() !is SkillsFragment) {
                            popTo(MainFragment2::class.java, false)
                            start(SkillsFragment())
                        }
                    }
                }
                engineService?.requestLauncherVisualFocus()
            }
            ACTION_OPEN_CHECK_UPDATE -> {
                if (getTopFragment() !is CheckUpdateFragment) {
                    popTo(MainFragment2::class.java, false)
                    start(CheckUpdateFragment())
                }
                engineService?.requestLauncherVisualFocus()
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

    private fun initVolume() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

            val alarmMax = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, (alarmMax * .8).roundToInt(), 0)

            if (am.ringerMode != AudioManager.RINGER_MODE_NORMAL)
                am.ringerMode = AudioManager.RINGER_MODE_NORMAL

            val notificationMax = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
            am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notificationMax, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initUi() {
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        if (engineService?.getAuthResponse() == null) {
            if (ConfigUtils.getBoolean(ConfigUtils.KEY_SETUP_COMPLETED, false)) {
                if (findFragment(PairFragment2::class.java) == null) {
                    loadRootFragment(R.id.fragment_container, PairFragment2())
                }
            } else {
                // 这里有可能是 null 取了默认值 false，存一下 false 保证其他地方读到的值是一样的
                ConfigUtils.putBoolean(ConfigUtils.KEY_SETUP_COMPLETED, false)
                if (findFragment(WelcomeFragment::class.java) == null) {
                    loadRootFragment(R.id.fragment_container, WelcomeFragment())
                }
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
            == PermissionChecker.PERMISSION_GRANTED)

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
            ),
            REQUEST_PERMISSION_CODE
        )
    }

    private class ReconnectHandler(engineService: EngineService) : Handler() {
        var retryCount = -1
            private set
        private val serviceRef = SoftReference(engineService)

        private val shortDelay = 3L * 1000
        private val middleDelay = 6L * 1000
        private val longDelay = 10L * 1000

        fun isCounting() = retryCount >= 0

        fun postReconnectEvs() {
            removeCallbacksAndMessages(null)
            retryCount = 0
            sendEmptyMessageDelayed(0, shortDelay)
        }

        fun clearRetryCount() {
            retryCount = -1
        }

        override fun handleMessage(msg: Message?) {
            if (msg?.what == 0) {
                serviceRef.get()?.let { service ->
                    if (!service.isEvsConnected &&
                        AuthDelegate.getAuthResponseFromPref(service) != null
                    ) {
                        service.connectEvs(DeviceUtils.getDeviceId(service))

                        retryCount++
                        val delay = when (retryCount) {
                            in 0..20 -> {
                                shortDelay
                            }
                            in 21..30 -> {
                                middleDelay
                            }
                            else -> {
                                longDelay
                            }
                        }
                        sendEmptyMessageDelayed(0, delay)
                    } else {
                        clearRetryCount()
                    }
                }
            }
        }
    }
}