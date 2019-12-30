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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.airbnb.lottie.LottieDrawable
import com.google.gson.Gson
import com.iflytek.cyber.evs.sdk.EvsError
import com.iflytek.cyber.evs.sdk.agent.*
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.fragment.*
import com.iflytek.cyber.iot.show.core.impl.audioplayer.EvsAudioPlayer
import com.iflytek.cyber.iot.show.core.impl.launcher.EvsLauncher
import com.iflytek.cyber.iot.show.core.impl.playback.EvsPlaybackController
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.impl.system.EvsSystem
import com.iflytek.cyber.iot.show.core.impl.template.EvsTemplate
import com.iflytek.cyber.iot.show.core.impl.videoplayer.EvsVideoPlayer
import com.iflytek.cyber.iot.show.core.message.MessageRecorder
import com.iflytek.cyber.iot.show.core.model.ActionConstant
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.PlayerInfoPayload
import com.iflytek.cyber.iot.show.core.model.Video
import com.iflytek.cyber.iot.show.core.record.GlobalRecorder
import com.iflytek.cyber.iot.show.core.record.RecordVolumeUtils
import com.iflytek.cyber.iot.show.core.utils.*
import com.iflytek.cyber.iot.show.core.utils.ContextWrapper
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.iflytek.cyber.product.ota.OtaService
import com.iflytek.cyber.product.ota.PackageEntityNew
import kotlinx.android.synthetic.main.activity_evs_launcher.*
import me.yokeyword.fragmentation.Fragmentation
import me.yokeyword.fragmentation.SupportHelper
import java.lang.ref.SoftReference
import java.util.*
import kotlin.collections.ArrayList
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

    private var hadInitVolume = false

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

                        ContextWrapper.startServiceAsUser(
                            baseContext,
                            Intent(baseContext, TimeService::class.java),
                            "CURRENT"
                        )
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
                    EvsLauncher.get().pageScrollCallback = pageScrollCallback
                    it.getAudioPlayer().addListener(audioPlayerStateChangedListener)

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
            ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")
        }

        override fun onWakeUp(angle: Int, beam: Int, params: String?) {
            // ignore
        }
    }

    private fun restartApp() {
        if (!isActivityVisible) {
            val intent = Intent(applicationContext, EvsLauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            applicationContext.startActivity(intent)
            ContextWrapper.startActivityAsUser(this, intent, "CURRENT")
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
            ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")
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
            ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")
            callbacks.forEach {
                it.renderPlayerInfo(payload)
            }
        }

        override fun exitCustomTemplate() {
            val intent = Intent(baseContext, FloatingService::class.java)
            intent.action = FloatingService.ACTION_CLEAR_TEMPLATE
            ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")

            callbacks.forEach {
                it.exitCustomTemplate()
            }
        }

        override fun exitStaticTemplate(type: String?) {
            val intent = Intent(baseContext, FloatingService::class.java)
            intent.action = FloatingService.ACTION_CLEAR_TEMPLATE
            intent.putExtra(FloatingService.EXTRA_TYPE, type)
            ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")

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
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
                        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
                ConfigUtils.KEY_VOICE_WAKEUP_ENABLED -> {
                    if (value == false) {
                        showErrorBar()
                    } else {
                        hideErrorBar()
                    }
                }
            }
        }
    }
    private val evsStatusReceiver = object : SelfBroadcastReceiver(
        EngineService.ACTION_EVS_CONNECTED,
        EngineService.ACTION_EVS_CONNECT_FAILED,
        EngineService.ACTION_EVS_DISCONNECTED,
        EngineService.ACTION_REQUEST_CLOSE_VIDEO
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                EngineService.ACTION_EVS_CONNECTED -> {
                    val dismissNotification =
                        Intent(baseContext, FloatingService::class.java)
                    dismissNotification.action = FloatingService.ACTION_DISMISS_NOTIFICATION
                    dismissNotification.putExtra(FloatingService.EXTRA_TAG, "network_error")
                    ContextWrapper.startServiceAsUser(baseContext, dismissNotification, "CURRENT")

                    reconnectHandler?.clearRetryCount()

                    val microphoneEnabled =
                        ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)

                    val enableWakeUp = Intent(baseContext, EngineService::class.java)
                    enableWakeUp.action = EngineService.ACTION_SET_WAKE_UP_ENABLED
                    enableWakeUp.putExtra(EngineService.EXTRA_ENABLED, microphoneEnabled)
                    ContextWrapper.startServiceAsUser(baseContext, enableWakeUp, "CURRENT")

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
                                MainFragment2.ACTION_OPEN_AUTH
                            )
                            disconnectNotification.putExtra(
                                FloatingService.EXTRA_KEEPING, true
                            )
                            ContextWrapper.startServiceAsUser(
                                baseContext,
                                disconnectNotification,
                                "CURRENT"
                            )
                        } else {
                            if (code == EvsError.Code.ERROR_SERVER_DISCONNECTED ||
                                code == EvsError.Code.ERROR_CLIENT_DISCONNECTED
                            ) {
                                val disconnectNotification =
                                    Intent(baseContext, FloatingService::class.java)
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
                                ContextWrapper.startServiceAsUser(
                                    baseContext,
                                    disconnectNotification,
                                    "CURRENT"
                                )
                            }
                            if (reconnectHandler?.isCounting() != true) {
                                reconnectHandler?.postReconnectEvs()
                            }
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
                            ContextWrapper.startServiceAsUser(
                                baseContext,
                                disconnectNotification,
                                "CURRENT"
                            )
                        }
                        if (handler?.isCounting() != true) {
                            handler?.postReconnectEvs()
                        }
                    }

                    if (EvsAudioPlayer.get(baseContext).playbackState
                        == AudioPlayer.PLAYBACK_STATE_PLAYING
                    ) {
                        PromptManager.play(PromptManager.NETWORK_LOST)
                    } else if (EvsVideoPlayer.get(baseContext).state
                        == VideoPlayer.STATE_RUNNING
                    ) {
                        PromptManager.play(PromptManager.NETWORK_LOST)
                    }
                }
                EngineService.ACTION_EVS_CONNECT_FAILED -> {
                    val handler = reconnectHandler
                    if (handler?.isCounting() != true) {
                        handler?.postReconnectEvs()
                    }
                }
                EngineService.ACTION_REQUEST_CLOSE_VIDEO -> {
                    if (getTopFragment() is VideoFragment) {
                        pop()
                    }
                }
            }
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
                        ContextWrapper.startServiceAsUser(
                            baseContext,
                            showNotification,
                            "CURRENT"
                        )
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

            if (f is VideoFragment) {
                hideErrorBar()
            } else if (f is PairFragment2) {
                VoiceButtonUtils.isPairing = true
            }
        }

        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            super.onFragmentPaused(fm, f)

            if (f is VideoFragment) {
                if (!ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)) {
                    showErrorBar()
                }
            } else if (f is PairFragment2) {
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

        const val ACTION_START_PLAYER = "$ACTION_PREFIX.START_PLAYER"
        const val ACTION_START_VIDEO_PLAYER = "$ACTION_PREFIX.START_VIDEO_PLAYER"
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
        supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallback, true)

        val intent = Intent(this, EngineService::class.java)
        ContextWrapper.startServiceAsUser(baseContext, intent, "CURRENT")
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
        otaService.putExtra(OtaService.EXTRA_DOWNLOAD_PATH, "${externalCacheDir?.path}/ota")
        otaService.putExtra(OtaService.EXTRA_PACKAGE_NAME, packageName)
//        otaService.putExtra(OtaService.EXTRA_DOWNLOAD_DIRECTLY, true)
        ContextWrapper.startServiceAsUser(baseContext, otaService, "CURRENT")

        otaReceiver.register(this)

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

        if (ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)) {
            hideErrorBar()
        } else {
            showErrorBar()
        }

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
        unbindService(serviceConnection)

        supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallback)

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
            ACTION_START_VIDEO_PLAYER -> {
                if (getTopFragment() !is VideoFragment) {
                    val main = findFragment(MainFragment2::class.java)
                    post {
                        main?.startVideoPlayer()
                    }
                }
            }
        }
    }

    fun openLauncherPage(page: String) {
        when (page) {
            Launcher.PAGE_ALARMS -> {
                if (getTopFragment() !is AlarmFragment) {
                    popTo(MainFragment2::class.java, false)
                    findFragment(MainFragment2::class.java)?.startAlarm()
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

    private fun showErrorBar() {
        if (getTopFragment() is VideoFragment)
            return
        error_bar.let { view ->
            view.isVisible = true
            if (isActivityVisible) {
                view.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .start()
            } else {
                view.alpha = 1f
            }
        }
    }

    private fun hideErrorBar() {
        error_bar?.let { view ->
            if (isActivityVisible) {
                view.animate()
                    .alpha(0f)
                    .setDuration(350)
                    .withEndAction {
                        view.isVisible = false
                    }
                    .start()
            } else {
                view.isVisible = false
            }
        }
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