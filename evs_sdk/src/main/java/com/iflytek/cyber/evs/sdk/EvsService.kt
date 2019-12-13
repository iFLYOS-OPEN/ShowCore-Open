package com.iflytek.cyber.evs.sdk

import android.app.Service
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import com.iflytek.cyber.evs.sdk.agent.*
import com.iflytek.cyber.evs.sdk.agent.impl.*
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.evs.sdk.focus.*
import com.iflytek.cyber.evs.sdk.model.*
import com.iflytek.cyber.evs.sdk.socket.RequestBuilder
import com.iflytek.cyber.evs.sdk.socket.SocketManager
import com.iflytek.cyber.evs.sdk.utils.AppUtil
import com.iflytek.cyber.evs.sdk.utils.Log
import java.lang.Exception
import kotlin.IllegalArgumentException
import kotlin.math.roundToInt

/**
 * EVS服务抽象类，需要派生出具体类来使用EVS服务。
 */
abstract class EvsService : Service() {
    companion object {
        private const val TAG = "EvsService"

        private const val ACTION_PREFIX = "com.iflytek.cyber.evs.sdk.action"
        const val ACTION_CONNECT = "$ACTION_PREFIX.CONNECT"
        const val ACTION_DISCONNECT = "$ACTION_PREFIX.DISCONNECT"

        const val EXTRA_DEVICE_ID = "device_id"
    }

    private lateinit var audioPlayer: AudioPlayer
    private var alarm: Alarm? = null
    private var appAction: AppAction? = null
    private var interceptor: Interceptor? = null
    private var launcher: Launcher? = null
    private var playbackController: PlaybackController? = null
    private lateinit var recognizer: Recognizer
    private var screen: Screen? = null
    private lateinit var speaker: Speaker
    private lateinit var system: System
    private var template: Template? = null
    private var videoPlayer: VideoPlayer? = null
    private var wakeWord: WakeWord? = null

    private var externalAudioFocusChannels: List<AudioFocusChannel> = emptyList()
    private var externalVisualFocusChannels: List<VisualFocusChannel> = emptyList()

    private val handler = Handler()

    private var handlerThread: HandlerThread? = null
    private var requestHandler: Handler? = null

    private var responseSoundPool: SoundPool? = null
    private var responseSoundIds = mutableListOf<Int>()

    var isEvsConnected = false
        private set


    private val socketListener = object : SocketManager.SocketListener() {
        override fun onSend(message: Any) {
            onRequestRaw(message)
        }

        override fun onConnected() {
            isEvsConnected = true
            onEvsConnected()
            handler.post {
                getSystem().sendStateSync()
            }
        }

        override fun onDisconnected(code: Int, reason: String?, remote: Boolean) {
            isEvsConnected = false
            recognizer.stopCapture()

            onEvsDisconnected(code, reason, remote)
        }

        override fun onMessage(message: String) {
            ResponseProcessor.putResponses(message)

            Thread {
                try {
                    onResponsesRaw(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        override fun onConnectFailed(t: Throwable?) {
            isEvsConnected = false
            this@EvsService.onConnectFailed(t)
        }

        override fun onSendFailed(code: Int, reason: String?) {
            this@EvsService.onSendFailed(code, reason)
        }
    }

    private val audioFocusObserver = object : AudioFocusManager.AudioFocusObserver {
        override fun onAudioFocusChanged(channel: String, type: String, status: FocusStatus) {
            Log.d(TAG, "onAudioFocusChanged($channel,$type,$status)")
            var isConsumed = true
            when (channel) {
                AudioFocusManager.CHANNEL_ALARM -> {
                    when (type) {
                        AudioFocusManager.TYPE_RING -> when (status) {
                            FocusStatus.Idle -> audioPlayer.stop(AudioPlayer.TYPE_RING)
                            FocusStatus.Background -> audioPlayer.moveToBackground(AudioPlayer.TYPE_RING)
                            else -> audioPlayer.moveToForegroundIfAvailable(AudioPlayer.TYPE_RING)
                        }
                        AudioFocusManager.TYPE_ALARM -> {
                            alarm?.let { alarm ->
                                when (status) {
                                    FocusStatus.Background, FocusStatus.Idle ->
                                        alarm.stop()
                                    else -> {
                                        // ignore
                                    }
                                }
                            }
                        }
                        else -> isConsumed = false
                    }
                }
                AudioFocusManager.CHANNEL_CONTENT -> {
                    when (type) {
                        AudioFocusManager.TYPE_PLAYBACK -> when (status) {
                            FocusStatus.Background -> audioPlayer.moveToBackground(AudioPlayer.TYPE_PLAYBACK)
                            FocusStatus.Idle -> audioPlayer.stop(AudioPlayer.TYPE_PLAYBACK)
                            else -> audioPlayer.moveToForegroundIfAvailable(AudioPlayer.TYPE_PLAYBACK)
                        }
                        AudioFocusManager.TYPE_VIDEO -> {
                            videoPlayer?.let {
                                when (status) {
                                    FocusStatus.Idle -> it.stop()
                                    FocusStatus.Background -> it.moveToBackground()
                                    else -> it.moveToForegroundIfAvailable()
                                }
                            }
                        }
                        else -> {
                            isConsumed = false
                        }
                    }
                }
                AudioFocusManager.CHANNEL_DIAL -> {
                    isConsumed = false
                }
                AudioFocusManager.CHANNEL_INPUT -> {
                    if (type == AudioFocusManager.TYPE_RECOGNIZE ||
                        type == AudioFocusManager.TYPE_RECOGNIZE_V
                    ) {
                        // 有全双工实现后，此处逻辑需要更改
                        if (status == FocusStatus.Idle) {
                            if (recognizer.isBackgroundRecognize) {

                            } else {
                                // 直接取消录音，不需要结果了
                                recognizer.requestCancel()
                            }
                        } else {
                            // ignore
                        }
                    } else {
                        isConsumed = false
                    }
                }
                AudioFocusManager.CHANNEL_OUTPUT -> {
                    if (type == AudioFocusManager.TYPE_TTS) {
                        if (status == FocusStatus.Idle || status == FocusStatus.Background) {
                            audioPlayer.stop(AudioPlayer.TYPE_TTS)

                            // 判断是否需要联动视觉焦点管理
                            if (VisualFocusManager.getForegroundChannel()
                                == VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE
                            ) {
                                VisualFocusManager.getForegroundChannelType()?.let {
                                    if (it == VisualFocusManager.TYPE_STATIC_TEMPLATE
                                        || it == VisualFocusManager.TYPE_CUSTOM_TEMPLATE
                                    ) {
                                        if (getTemplate()?.isTemplatePermanent() == true)
                                            return
                                        val thread = HandlerThread("Timer")
                                        thread.start()
                                        Handler(thread.looper).postDelayed({
                                            clearCurrentTemplateFocus()
                                        }, getTemplate()?.clearTimeout?.toLong() ?: 0)
                                    } else if (it == VisualFocusManager.TYPE_PLAYING_TEMPLATE) {
                                        if (getTemplate()?.isPlayerInfoPermanent() == true)
                                            return
                                        val thread = HandlerThread("Timer")
                                        thread.start()
                                        Handler(thread.looper).postDelayed({
                                            val foregroundType =
                                                VisualFocusManager.getForegroundChannelType()
                                            if (VisualFocusManager.getForegroundChannel()
                                                == VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE
                                                && foregroundType ==
                                                VisualFocusManager.TYPE_PLAYING_TEMPLATE
                                            )
                                                getTemplate()?.exitPlayerInfo()
                                        }, getTemplate()?.clearTimeout?.toLong() ?: 0)
                                    } else {

                                    }
                                }
                            }
                        }
                    } else {
                        isConsumed = false
                    }
                }
                else -> {
                    isConsumed = false
                }
            }

            if (!isConsumed) {
                externalAudioFocusChannels.map { audioFocusChannel ->
                    if (audioFocusChannel.getChannelName() == channel
                        && audioFocusChannel.getExternalType() == type
                    ) {
                        handler.post {
                            audioFocusChannel.onFocusChanged(status)
                        }
                    }
                }
            }

        }
    }
    private val visualFocusObserver = object : VisualFocusManager.VisualFocusObserver {
        override fun onVisualFocusChanged(channel: String, type: String, status: FocusStatus) {
            var isConsumed = false
            when (channel) {
                VisualFocusManager.CHANNEL_OVERLAY -> {
                    // ignore
                }
                VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE -> {
                    if (status == FocusStatus.Idle) {
                        if (type == VisualFocusManager.TYPE_CUSTOM_TEMPLATE) {
                            getTemplate()?.exitCustomTemplate()
                            isConsumed = true
                        } else if (type == VisualFocusManager.TYPE_STATIC_TEMPLATE) {
                            getTemplate()?.exitStaticTemplate(getTemplate()?.getFocusTemplateType())
                            isConsumed = true
                        }
                    }
                }
                VisualFocusManager.CHANNEL_APP -> {
                    // ignore
                }
            }

            if (!isConsumed) {
                externalVisualFocusChannels.map { visualFocusChannel ->
                    if (visualFocusChannel.getChannelName() == channel
                        && visualFocusChannel.getExternalType() == type
                    ) {
                        handler.post {
                            visualFocusChannel.onFocusChanged(status)
                        }
                    }
                }
            }
        }
    }

    fun clearCurrentTemplateFocus() {
        val foregroundType =
            VisualFocusManager.getForegroundChannelType()
        if (VisualFocusManager.getForegroundChannel()
            == VisualFocusManager.CHANNEL_OVERLAY_TEMPLATE
        ) {
            foregroundType?.let { type ->
                getTemplate()?.requestClearFocus(type)
            }
        }
    }

    /**
     * 与云端连接成功回调。
     */
    open fun onEvsConnected() {

    }

    /**
     * 与云端断开连接回调。
     * @param code 错误码
     * @param message 提示信息
     */
    open fun onEvsDisconnected(code: Int, message: String?, fromRemote: Boolean) {

    }

    open fun onConnectFailed(t: Throwable?) {

    }

    open fun onSendFailed(code: Int, reason: String?) {

    }

    override fun onCreate() {
        super.onCreate()

        audioPlayer = overrideAudioPlayer()
        alarm = overrideAlarm()
        appAction = overrideAppAction()
        interceptor = overrideInterceptor()
        launcher = overrideLauncher()
        playbackController = overridePlaybackController()
        recognizer = overrideRecognizer()
        screen = overrideScreen()
        speaker = overrideSpeaker()
        system = overrideSystem()
        template = overrideTemplate()
        videoPlayer = overrideVideoPlayer()
        wakeWord = overrideWakeWord()
        initResponseSoundPool()

        ResponseProcessor.init(
            this,
            alarm,
            appAction,
            audioPlayer,
            interceptor,
            launcher,
            playbackController,
            recognizer,
            screen,
            speaker,
            system,
            template,
            videoPlayer,
            wakeWord
        )
        RequestBuilder.init(
            alarm,
            appAction,
            audioPlayer,
            interceptor,
            launcher,
            playbackController,
            recognizer,
            screen,
            speaker,
            system,
            template,
            videoPlayer,
            wakeWord
        )

        ResponseProcessor.initHandler(handler)

        handlerThread = HandlerThread("request-handler")
        handlerThread?.start()
        requestHandler = Handler(handlerThread?.looper)
        requestHandler?.post {
            AppUtil.getForegroundApp(this@EvsService)
        }

        RequestManager.initHandler(requestHandler, this)

        AudioFocusManager.setFocusObserver(audioFocusObserver)
        VisualFocusManager.setFocusObserver(visualFocusObserver)

        SocketManager.addListener(socketListener)

        // check external audio focus channels available
        getExternalAudioFocusChannels().let { channels ->
            val audioFocusChannelTypes = HashSet<String>()
            channels.map { audioFocusChannel ->
                val channel = audioFocusChannel.getChannelName()
                val type = audioFocusChannel.getType()
                if (!AudioFocusManager.isManageableChannel(channel)) {
                    throw IllegalArgumentException(
                        "Illegal audio focus channel name {$channel}, channel name must be one of ${
                        AudioFocusManager.sortedChannels.contentToString()
                        }"
                    )
                } else {
                    if (audioFocusChannelTypes.contains(type)) {
                        throw IllegalArgumentException(
                            "Illegal audio focus channel type {$type} is duplicate."
                        )
                    } else {
                        audioFocusChannel.setupManager(AudioFocusManager)
                    }
                }
            }
            externalAudioFocusChannels = channels
        }

        // check external visual focus channels available
        getExternalVisualFocusChannels().let { channels ->

            val visualFocusChannelTypes = HashSet<String>()
            channels.map { visualFocusChannel ->
                val channel = visualFocusChannel.getChannelName()
                val type = visualFocusChannel.getType()
                if (!VisualFocusManager.isManageableChannel(channel)) {
                    throw IllegalArgumentException(
                        "Illegal visual focus channel name {$channel}, channel name must be one of ${
                        VisualFocusManager.sortedChannels.contentToString()
                        }"
                    )
                } else {
                    if (visualFocusChannelTypes.contains(type)) {
                        throw IllegalArgumentException(
                            "Illegal visual focus channel type {$type} is duplicate."
                        )
                    } else {
                        visualFocusChannel.setupManager(VisualFocusManager)
                    }
                }
            }
            externalVisualFocusChannels = channels
        }

        AuthDelegate.registerTokenChangedListener(this, tokenChangeListener)
    }

    private val tokenChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->

            if (key == AuthDelegate.PREF_KEY) {
                disconnect()
            }
        }

    override fun onDestroy() {
        super.onDestroy()

        getRecognizer().stopCapture()
        getRecognizer().onDestroy()

        SocketManager.removeListener(socketListener)
        SocketManager.disconnect()

        AudioFocusManager.removeFocusObserver()
        VisualFocusManager.removeFocusObserver()

        ResponseProcessor.destroy()

        handlerThread?.quit()
        handlerThread = null

        AuthDelegate.unregisterTokenChangedListener(this, tokenChangeListener)
    }

    /**
     * 连接iFLYOS云端服务。
     * @param deviceId 设备id
     */
    fun connect(deviceId: String) {
        connect("", deviceId)
    }

    /**
     * 连接到指定url的iFLYOS云端服务。
     * @param serverUrl 服务端url
     * @param deviceId 设备id
     */
    fun connect(serverUrl: String?, deviceId: String) {
        getAuthResponse()?.let {
            val token = it.accessToken
            if (deviceId.isEmpty() || token.isEmpty()) {
                onConnectFailed(EvsError.AuthorizationExpiredException())
                Log.e(
                    TAG,
                    "Illegal params while requesting connection. {deviceId: $deviceId, token: $token}"
                )
            } else {
                val current = java.lang.System.currentTimeMillis() / 1000
                if (current - it.createdAt >= it.expiresIn) {
                    Log.w(
                        TAG, "Access token {$token} is expired, " +
                            "try to refresh. {refreshToken: ${it.refreshToken}}"
                    )

                    AuthDelegate.refreshAccessToken(this, it.refreshToken,
                        object : AuthDelegate.RefreshCallBack {
                            override fun onRefreshFailed(
                                httpCode: Int,
                                errorBody: String?,
                                throwable: Throwable?
                            ) {
                                val message = throwable?.message ?: errorBody
                                Log.e(TAG, "Fail to refresh token, $message")

                                onConnectFailed(EvsError.AuthorizationExpiredException())
                            }

                            override fun onRefreshSuccess(authResponse: AuthResponse) {
                                Log.d(TAG, "Refresh access token success.")

                                RequestBuilder.setDeviceAuthInfo(deviceId, authResponse.accessToken)
                                SocketManager.connect(serverUrl, deviceId, authResponse.accessToken)
                            }
                        })
                } else {
                    RequestBuilder.setDeviceAuthInfo(deviceId, token)
                    SocketManager.connect(serverUrl, deviceId, token)
                }
            }
        } ?: run {
            onConnectFailed(IllegalArgumentException("Auth response is null. You should use AuthDelegate to get a token by login."))
            Log.w(TAG, "Auth response is null. Ignore WebSocket Connection Action.")
        }

    }

    fun setCustomIflyosContext(customContext: String?) {
        RequestBuilder.customIflyosContext = customContext
    }

    fun getCurrentIflyosContext(): String {
        return RequestBuilder.buildContext().toString()
    }

    /**
     * 断开与iFLYOS的连接。
     */
    fun disconnect() {
        SocketManager.disconnect()
    }

    /**
     * 获取App操作模块。
     */
    @Suppress("unused")
    fun getAppAction() = appAction

    /**
     * 获取闹钟模块。
     */
    @Suppress("unused")
    fun getAlarm() = alarm

    /**
     * 获取音频播放器。
     */
    @Suppress("unused")
    fun getAudioPlayer() = audioPlayer

    /**
     * 获取拦截器。
     */
    @Suppress("unused")
    fun getInterceptor() = interceptor

    /**
     * 获取播放控制器。
     */
    @Suppress("unused")
    fun getPlaybackController() = playbackController

    /**
     * 获取识别模块。
     */
    @Suppress("unused")
    fun getRecognizer() = recognizer

    /**
     * 获取扬声器控制模块。
     */
    @Suppress("unused")
    fun getSpeaker() = speaker

    /**
     * 获取系统模块。
     */
    @Suppress("unused")
    fun getSystem() = system

    /**
     * 获取模板展示模块。
     */
    @Suppress("unused")
    fun getTemplate() = template

    /**
     * 获取视频播放器。
     */
    @Suppress("unused")
    fun getVideoPlayer() = videoPlayer

    /**
     * 获取启动器。
     */
    @Suppress("unused")
    fun getLauncher() = launcher

    /**
     * 获得语音唤醒词实例。
     */
    @Suppress("unused")
    fun getWakeWord() = wakeWord


    /**
     * 创建App操作模块，返回null则不启动该模块。
     */
    open fun overrideAppAction(): AppAction? {
        return AppActionImpl(this)
    }

    /**
     * 创建识别模块。
     */
    open fun overrideRecognizer(): Recognizer {
        return RecognizerImpl()
    }

    /**
     * 创建拦截器，返回null则不启动该模块。
     */
    open fun overrideInterceptor(): Interceptor? {
        return null
    }

    /**
     * 创建启动器，返回null则不启动该模块。
     */
    open fun overrideLauncher(): Launcher? {
        return null
    }

    /**
     * 创建扬声器控制模块。
     */
    open fun overrideSpeaker(): Speaker {
        return SpeakerImpl(this)
    }

    /**
     * 创建闹钟模块。
     * @return 闹钟模块实例。返回 null则将使用云端闹钟能力，默认返回了基于 AlarmManager 的内置闹钟能力
     */
    open fun overrideAlarm(): Alarm? {
        return AlarmImpl(this)
    }

    open fun overrideAudioPlayer(): AudioPlayer {
        return AudioPlayerImpl(this)
    }

    /**
     * 创建屏幕控制模块，返回null则不启用该模块。
     */
    open fun overrideScreen(): Screen? {
        return ScreenImpl(this)
    }

    /**
     * 创建系统模块，返回null则不启用该模块。
     */
    open fun overrideSystem(): System {
        return SystemImpl()
    }

    /**
     * 创建模板展示模块，返回null则不启用该模块。
     */
    open fun overrideTemplate(): Template? {
        return null
    }

    /**
     * 创建视频播放器，返回null则不启用该模块。
     */
    open fun overrideVideoPlayer(): VideoPlayer? {
        return null
    }

    /**
     * 创建语音唤醒词端能力，返回 null 则不启用该模块
     */
    open fun overrideWakeWord(): WakeWord? {
        return null
    }

    /**
     * 创建播放控制器，返回null则不启用该模块。
     */
    open fun overridePlaybackController(): PlaybackController? {
        return PlaybackControllerImpl()
    }

    /**
     * WebSocket接收数据回调。
     * @param json 返回的响应 json
     */
    open fun onResponsesRaw(json: String) {

    }

    /**
     * 获取认证授权结果。
     * return 认证授权结果，null则表示还未授权
     */
    fun getAuthResponse(): AuthResponse? {
        return AuthDelegate.getAuthResponseFromPref(this)
    }

    /**
     * WebSocket发送数据回调。
     * @param obj 参数可能为 [String], [ByteArray] 中的一个
     */
    open fun onRequestRaw(obj: Any) {

    }

    /**
     * 出错回调。
     * @param code 错误码
     * @param message 提示消息
     */
    open fun onError(code: Int, message: String) {

    }

    open fun getExternalAudioFocusChannels(): List<AudioFocusChannel> {
        return emptyList()
    }

    open fun getExternalVisualFocusChannels(): List<VisualFocusChannel> {
        return emptyList()
    }

    open fun isResponseSoundEnabled(): Boolean {
        return true
    }

    open fun getResponseSoundVolume(): Float {
        return 1f
    }

    open fun getResponseSoundPool(): SoundPool? {
        val maxStream = 3
        val streamType = AudioManager.STREAM_MUSIC
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder()
                .setMaxStreams(maxStream)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setLegacyStreamType(streamType)
                        .build()
                )
                .build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(maxStream, streamType, 0)
        }
    }

    open fun getResponseSoundResources(): Array<Int> {
        return arrayOf(R.raw.response_sound_1, R.raw.response_sound_2)
    }

    fun initResponseSoundPool() {
        responseSoundPool = getResponseSoundPool()
        responseSoundPool?.let { soundPool ->
            responseSoundIds.clear()
            getResponseSoundResources().map {
                responseSoundIds.add(soundPool.load(baseContext, it, 1))
            }
        }
    }

    fun playResponseSound() {
        responseSoundPool?.let { soundPool ->
            val random = Math.random()
            val index = ((responseSoundIds.size - 1) * random).roundToInt()
            soundPool.play(
                responseSoundIds[index],
                getResponseSoundVolume(),
                getResponseSoundVolume(),
                1,
                0,
                1f
            )
        }
    }

    /**
     * 获取版本号。
     */
    open fun getVersion(): String {
        return "1.4"
    }
}