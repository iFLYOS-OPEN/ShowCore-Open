package com.iflytek.cyber.evs.sdk

import android.content.Context
import android.os.Handler
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.agent.*
import com.iflytek.cyber.evs.sdk.focus.AudioFocusManager
import com.iflytek.cyber.evs.sdk.focus.VisualFocusManager
import com.iflytek.cyber.evs.sdk.model.Constant
import com.iflytek.cyber.evs.sdk.model.OsResponse
import com.iflytek.cyber.evs.sdk.model.OsResponseBody
import com.iflytek.cyber.evs.sdk.socket.RequestBuilder
import com.iflytek.cyber.evs.sdk.utils.Log
import java.lang.ref.SoftReference
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

internal object ResponseProcessor {
    private const val TAG = "ResponseProcessor"

    /**
     * 缓存的 PlayerInfo 最大数量
     */
    private const val MAX_PLAYER_INFO_CACHE_SIZE = 40
    private const val MAX_PARALLEL_CACHE_SIZE = 40

    private var contextRef: SoftReference<EvsService>? = null

    private var alarm: Alarm? = null
    private var appAction: AppAction? = null
    private var audioPlayer: AudioPlayer? = null
    private var interceptor: Interceptor? = null
    private var launcher: Launcher? = null
    private var playbackController: PlaybackController? = null
    private var recognizer: Recognizer? = null
    private var screen: Screen? = null
    private var speaker: Speaker? = null
    private var system: System? = null
    private var template: Template? = null
    private var videoPlayer: VideoPlayer? = null
    private var wakeWord: WakeWord? = null

    private val currentManualRequestId: String?
        get() = RequestManager.currentManualRequestId

    private var isTtsPlaying = false
    private var currentTtsResourceId = ""

    private val playerInfoMap = HashMap<String, JSONObject>() // resourceId -> json(payload)
    private val ttsParallelMap = HashMap<String, Boolean?>()
    private val upcomingAudioPlaybackResources = mutableListOf<JSONObject>()
    private val upcomingVideoPlaybackResources = mutableListOf<JSONObject>()

    private var handler: Handler? = null

    //    private var currentRequestId = ""
    //    private val pendingExecuteResponses =
//        HashMap<String, MutableList<OsResponse>>() // requestId -> responses
    private val audioNearlyFinishedSentMap = HashMap<String, Boolean>() // resourceId -> isSent
    private val videoNearlyFinishedSentMap = HashMap<String, Boolean>() // resourceId -> isSent
    private val needSetOffsetResources = HashMap<String, Long>() // resourceId -> offset
    private val resourceIdRequestIdMap = HashMap<String, String>() // resourceId -> requestId

    private val normalPendingExecutingResponses =
        ArrayDeque<Pair<String, OsResponse>>() // requestId -> response
    private val manualPendingExecutingResponses =
        ArrayDeque<Pair<String, OsResponse>>() // requestId -> response
    private var hadPlayResponseSoundRequestIdSet = HashSet<String>() // requestId

    private val audioPlayerListener = object : AudioPlayer.MediaStateChangedListener {
        override fun onStarted(player: AudioPlayer, type: String, resourceId: String) {
            handler?.post {
                val payload = JSONObject()
                payload[AudioPlayer.KEY_TYPE] = AudioPlayer.SYNC_TYPE_STARTED
                payload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                when (type) {
                    AudioPlayer.TYPE_PLAYBACK -> {
                        audioNearlyFinishedSentMap[resourceId] = false

                        payload[AudioPlayer.KEY_OFFSET] = player.getOffset(type)
                        RequestManager.sendRequest(AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC, payload)

                        if (needSetOffsetResources.containsKey(resourceId)) {
                            player.seekTo(type, needSetOffsetResources[resourceId] ?: 0)
                            needSetOffsetResources.remove(resourceId)
                        }

                        if (playerInfoMap.containsKey(resourceId)) {
                            playerInfoMap[resourceId]?.let { playerInfo ->
                                template?.renderPlayerInfo(playerInfo.toString())
                            }
                        }

                        AudioFocusManager.requestActive(
                            AudioFocusManager.CHANNEL_CONTENT,
                            AudioFocusManager.TYPE_PLAYBACK
                        )
                    }
                    AudioPlayer.TYPE_RING -> {
                        RequestManager.sendRequest(AudioPlayer.NAME_RING_PROGRESS_SYNC, payload)

                        AudioFocusManager.requestActive(
                            AudioFocusManager.CHANNEL_ALARM,
                            AudioFocusManager.TYPE_RING
                        )
                    }
                    AudioPlayer.TYPE_TTS -> {
                        isTtsPlaying = true
                        currentTtsResourceId = resourceId
                        RequestManager.sendRequest(AudioPlayer.NAME_TTS_PROGRESS_SYNC, payload)

                        AudioFocusManager.requestActive(
                            AudioFocusManager.CHANNEL_OUTPUT,
                            AudioFocusManager.TYPE_TTS
                        )
                    }
                }
            }
        }

        override fun onResumed(player: AudioPlayer, type: String, resourceId: String) {
            handler?.post {
                val payload = JSONObject()
                payload[AudioPlayer.KEY_TYPE] = AudioPlayer.SYNC_TYPE_STARTED
                payload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                when (type) {
                    AudioPlayer.TYPE_PLAYBACK -> {
                        payload[AudioPlayer.KEY_OFFSET] = player.getOffset(type)
                        RequestManager.sendRequest(AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC, payload)

                        if (needSetOffsetResources.containsKey(resourceId)) {
                            player.seekTo(type, needSetOffsetResources[resourceId] ?: 0)
                            needSetOffsetResources.remove(resourceId)
                        }

                        AudioFocusManager.requestActive(
                            AudioFocusManager.CHANNEL_CONTENT,
                            AudioFocusManager.TYPE_PLAYBACK
                        )
                    }
                    AudioPlayer.TYPE_RING -> {
                        RequestManager.sendRequest(AudioPlayer.NAME_RING_PROGRESS_SYNC, payload)

                        AudioFocusManager.requestActive(
                            AudioFocusManager.CHANNEL_ALARM,
                            AudioFocusManager.TYPE_RING
                        )
                    }
                    AudioPlayer.TYPE_TTS -> {
                        isTtsPlaying = true
                        currentTtsResourceId = resourceId

                        RequestManager.sendRequest(AudioPlayer.NAME_TTS_PROGRESS_SYNC, payload)

                        AudioFocusManager.requestActive(
                            AudioFocusManager.CHANNEL_OUTPUT,
                            AudioFocusManager.TYPE_TTS
                        )
                    }
                }
            }
        }

        override fun onPaused(player: AudioPlayer, type: String, resourceId: String) {
            handler?.post {
                val payload = JSONObject()
                payload[AudioPlayer.KEY_TYPE] = AudioPlayer.SYNC_TYPE_PAUSED
                payload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                payload[AudioPlayer.KEY_OFFSET] = player.getOffset(type)

                when (type) {
                    AudioPlayer.TYPE_PLAYBACK -> {
                        RequestManager.sendRequest(AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC, payload)

                        AudioFocusManager.requestAbandon(
                            AudioFocusManager.CHANNEL_CONTENT,
                            AudioFocusManager.TYPE_PLAYBACK
                        )
                    }
                    AudioPlayer.TYPE_RING -> {
                        AudioFocusManager.requestAbandon(
                            AudioFocusManager.CHANNEL_ALARM,
                            AudioFocusManager.TYPE_RING
                        )
                    }
                    AudioPlayer.TYPE_TTS -> {
                        isTtsPlaying = false
                        if (currentTtsResourceId == resourceId) {
                            currentTtsResourceId = ""

                            handler?.post {
                                if (resourceIdRequestIdMap[resourceId]?.startsWith(RequestBuilder.PREFIX_MANUAL) == true) {
                                    startExecuting(manualPendingExecutingResponses)
                                } else {
                                    startExecuting(normalPendingExecutingResponses)
                                }
                            }
                        }
                        AudioFocusManager.requestAbandon(
                            AudioFocusManager.CHANNEL_OUTPUT,
                            AudioFocusManager.TYPE_TTS
                        )
                    }
                }
            }
        }

        override fun onStopped(player: AudioPlayer, type: String, resourceId: String) {
            // nothing needs to do
            when (type) {
                AudioPlayer.TYPE_PLAYBACK -> {
                    AudioFocusManager.requestAbandon(
                        AudioFocusManager.CHANNEL_CONTENT,
                        AudioFocusManager.TYPE_PLAYBACK
                    )
                }
                AudioPlayer.TYPE_RING -> {
                    AudioFocusManager.requestAbandon(
                        AudioFocusManager.CHANNEL_ALARM,
                        AudioFocusManager.TYPE_RING
                    )
                }
                AudioPlayer.TYPE_TTS -> {
                    AudioFocusManager.requestAbandon(
                        AudioFocusManager.CHANNEL_OUTPUT,
                        AudioFocusManager.TYPE_TTS
                    )
                }
            }
            if (type == AudioPlayer.TYPE_TTS) {
                isTtsPlaying = false
                if (currentTtsResourceId == resourceId) {
                    currentTtsResourceId = ""
                }
                handler?.post {
                    if (resourceIdRequestIdMap[resourceId]?.startsWith(RequestBuilder.PREFIX_MANUAL) == true) {
                        startExecuting(manualPendingExecutingResponses)
                    } else {
                        startExecuting(normalPendingExecutingResponses)
                    }
                }
            }
        }

        override fun onCompleted(player: AudioPlayer, type: String, resourceId: String) {
            handler?.post {
                val payload = JSONObject()
                payload[AudioPlayer.KEY_TYPE] = AudioPlayer.SYNC_TYPE_FINISHED
                payload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                when (type) {
                    AudioPlayer.TYPE_PLAYBACK -> {
                        if (audioNearlyFinishedSentMap[resourceId] != true) {
                            val nearlyFinishedPayload = JSONObject()
                            nearlyFinishedPayload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                            nearlyFinishedPayload[AudioPlayer.KEY_OFFSET] =
                                audioPlayer?.playbackOffset
                            nearlyFinishedPayload[AudioPlayer.KEY_TYPE] =
                                AudioPlayer.SYNC_TYPE_NEARLY_FINISHED
                            RequestManager.sendRequest(
                                AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC,
                                nearlyFinishedPayload
                            )
                            audioNearlyFinishedSentMap[resourceId] = true
                        }

                        payload[AudioPlayer.KEY_OFFSET] = player.getOffset(type)
                        RequestManager.sendRequest(AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC, payload)

                        if (upcomingAudioPlaybackResources.isNotEmpty()) {
                            val nextPayload = upcomingAudioPlaybackResources[0]

                            val nextResourceId = nextPayload.getString(AudioPlayer.KEY_RESOURCE_ID)
                            val url = nextPayload.getString(AudioPlayer.KEY_URL)

                            player.play(type, nextResourceId, url)

                            upcomingAudioPlaybackResources.removeAt(0)
                        } else {
                            AudioFocusManager.requestAbandon(
                                AudioFocusManager.CHANNEL_CONTENT,
                                AudioFocusManager.TYPE_PLAYBACK
                            )
                        }
                    }
                    AudioPlayer.TYPE_RING -> {
                        RequestManager.sendRequest(AudioPlayer.NAME_RING_PROGRESS_SYNC, payload)

                        AudioFocusManager.requestAbandon(
                            AudioFocusManager.CHANNEL_ALARM,
                            AudioFocusManager.TYPE_RING
                        )
                    }
                    AudioPlayer.TYPE_TTS -> {

                        RequestManager.sendRequest(AudioPlayer.NAME_TTS_PROGRESS_SYNC, payload)

//                        if (currentRequestId.isNotEmpty()) {
//                            val responses: MutableList<OsResponse>?
//                            synchronized(pendingExecuteResponses) {
//                                responses = pendingExecuteResponses[currentRequestId]
//                            }
//
//                            var findTarget = false
//                            responses?.map {
//                                if (it.header.name.startsWith(Constant.NAMESPACE_AUDIO_PLAYER)) {
//                                    val cachePayload = it.payload
//                                    val cacheResourceId =
//                                        cachePayload.getString(AudioPlayer.KEY_RESOURCE_ID)
//                                    findTarget = (cacheResourceId == resourceId) || findTarget
//                                }
//                            }
//                            if (findTarget) {
//                                responses?.removeAt(0)
//                                startExecuteResponses()
//                            }
//                        }

                        AudioFocusManager.requestAbandon(
                            AudioFocusManager.CHANNEL_OUTPUT,
                            AudioFocusManager.TYPE_TTS
                        )
                    }
                }
            }
            if (type == AudioPlayer.TYPE_TTS) {
                isTtsPlaying = false
                handler?.post {
                    if (currentTtsResourceId == resourceId) {
                        currentTtsResourceId = ""

                        if (resourceIdRequestIdMap[resourceId]?.startsWith(RequestBuilder.PREFIX_MANUAL) == true) {
                            startExecuting(manualPendingExecutingResponses)
                        } else {
                            startExecuting(normalPendingExecutingResponses)
                        }
                    }
                }
            }
        }

        override fun onPositionUpdated(
            player: AudioPlayer,
            type: String,
            resourceId: String,
            position: Long
        ) {
            if (type == AudioPlayer.TYPE_PLAYBACK && audioNearlyFinishedSentMap[resourceId] == false) {
                handler?.post {
                    val duration = player.getDuration(type)
//                    if (position > 5000) { // for test
                    if (position * 3 > duration) {
                        audioNearlyFinishedSentMap[resourceId] = true

                        val payload = JSONObject()
                        payload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                        payload[AudioPlayer.KEY_OFFSET] = position
                        payload[AudioPlayer.KEY_TYPE] = AudioPlayer.SYNC_TYPE_NEARLY_FINISHED
                        RequestManager.sendRequest(AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC, payload)
                    }
                }
            }
        }

        override fun onError(
            player: AudioPlayer,
            type: String,
            resourceId: String,
            errorCode: String
        ) {
            if (type == AudioPlayer.TYPE_PLAYBACK) {
                val payload = JSONObject()
                payload[AudioPlayer.KEY_TYPE] = AudioPlayer.SYNC_TYPE_FAILED
                payload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                payload[AudioPlayer.KEY_FAILURE_CODE] = errorCode.toInt()
                RequestManager.sendRequest(AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC, payload)
            } else if (type == AudioPlayer.TYPE_TTS) {
                isTtsPlaying = false
                if (currentTtsResourceId == resourceId) {
                    currentTtsResourceId = ""

                    handler?.post {
                        if (resourceIdRequestIdMap[resourceId]?.startsWith(RequestBuilder.PREFIX_MANUAL) == true) {
                            startExecuting(manualPendingExecutingResponses)
                        } else {
                            startExecuting(normalPendingExecutingResponses)
                        }
                    }
                }
            }
        }
    }

    private val videoPlayerListener = object : VideoPlayer.VideoStateChangedListener {
        override fun onStarted(player: VideoPlayer, resourceId: String) {
            handler?.post {
                val payload = JSONObject()
                payload[VideoPlayer.KEY_TYPE] = VideoPlayer.SYNC_TYPE_STARTED
                payload[VideoPlayer.KEY_RESOURCE_ID] = resourceId
                videoNearlyFinishedSentMap[resourceId] = false
                payload[VideoPlayer.KEY_OFFSET] = player.videoOffset
                RequestManager.sendRequest(VideoPlayer.NAME_PROGRESS_SYNC, payload)

                if (needSetOffsetResources.containsKey(resourceId)) {
                    player.seekTo(needSetOffsetResources[resourceId] ?: 0)
                    needSetOffsetResources.remove(resourceId)
                }

                if (playerInfoMap.containsKey(resourceId)) {
                    playerInfoMap[resourceId]?.let { playerInfo ->
                        template?.renderVideoPlayerInfo(playerInfo.toString())
                    }
                }

                AudioFocusManager.requestActive(
                    AudioFocusManager.CHANNEL_CONTENT,
                    AudioFocusManager.TYPE_VIDEO
                )
            }
        }

        override fun onResumed(player: VideoPlayer, resourceId: String) {
            handler?.post {
                val payload = JSONObject()
                payload[VideoPlayer.KEY_TYPE] = VideoPlayer.SYNC_TYPE_STARTED
                payload[VideoPlayer.KEY_RESOURCE_ID] = resourceId
                payload[VideoPlayer.KEY_OFFSET] = player.videoOffset
                RequestManager.sendRequest(VideoPlayer.NAME_PROGRESS_SYNC, payload)

                if (needSetOffsetResources.containsKey(resourceId)) {
                    player.seekTo(needSetOffsetResources[resourceId] ?: 0)
                    needSetOffsetResources.remove(resourceId)
                }

                AudioFocusManager.requestActive(
                    AudioFocusManager.CHANNEL_CONTENT,
                    AudioFocusManager.TYPE_VIDEO
                )
            }
        }

        override fun onPaused(player: VideoPlayer, resourceId: String) {
            AudioFocusManager.requestAbandon(
                AudioFocusManager.CHANNEL_CONTENT,
                AudioFocusManager.TYPE_VIDEO
            )
        }

        override fun onStopped(player: VideoPlayer, resourceId: String) {
            AudioFocusManager.requestAbandon(
                AudioFocusManager.CHANNEL_CONTENT,
                AudioFocusManager.TYPE_VIDEO
            )
        }

        override fun onCompleted(player: VideoPlayer, resourceId: String) {
            handler?.post {
                if (videoNearlyFinishedSentMap[resourceId] != true) {
                    val payload = JSONObject()
                    payload[VideoPlayer.KEY_TYPE] = VideoPlayer.SYNC_TYPE_NEARLY_FINISHED
                    payload[VideoPlayer.KEY_RESOURCE_ID] = resourceId
                    payload[VideoPlayer.KEY_OFFSET] = player.videoOffset
                    RequestManager.sendRequest(VideoPlayer.NAME_PROGRESS_SYNC, payload)

                    videoNearlyFinishedSentMap[resourceId] = true
                }

                val payload = JSONObject()
                payload[VideoPlayer.KEY_TYPE] = VideoPlayer.SYNC_TYPE_FINISHED
                payload[VideoPlayer.KEY_RESOURCE_ID] = resourceId
                payload[VideoPlayer.KEY_OFFSET] = player.videoOffset
                RequestManager.sendRequest(VideoPlayer.NAME_PROGRESS_SYNC, payload)

                AudioFocusManager.requestAbandon(
                    AudioFocusManager.CHANNEL_CONTENT,
                    AudioFocusManager.TYPE_VIDEO
                )

                if (upcomingVideoPlaybackResources.isNotEmpty()) {
                    val nextPayload = upcomingVideoPlaybackResources[0]

                    val nextResourceId = nextPayload.getString(VideoPlayer.KEY_RESOURCE_ID)
                    val url = nextPayload.getString(VideoPlayer.KEY_URL)

                    player.play(nextResourceId, url)

                    upcomingVideoPlaybackResources.removeAt(0)
                }
            }
        }

        override fun onPositionUpdated(player: VideoPlayer, resourceId: String, position: Long) {
            if (videoNearlyFinishedSentMap[resourceId] != true) {
                handler?.post {
                    val duration = player.getDuration()
                    if (position * 3 > duration) {
                        videoNearlyFinishedSentMap[resourceId] = true
                        val payload = JSONObject()
                        payload[VideoPlayer.KEY_TYPE] = VideoPlayer.SYNC_TYPE_NEARLY_FINISHED
                        payload[VideoPlayer.KEY_RESOURCE_ID] = resourceId
                        payload[VideoPlayer.KEY_OFFSET] = position
                        RequestManager.sendRequest(VideoPlayer.NAME_PROGRESS_SYNC, payload)
                    }
                }
            }
        }

        override fun onError(player: VideoPlayer, resourceId: String, errorCode: String) {
            val payload = JSONObject()
            payload[VideoPlayer.KEY_RESOURCE_ID] = resourceId
            payload[VideoPlayer.KEY_FAILURE_CODE] = errorCode.toInt()
            payload[VideoPlayer.KEY_TYPE] = VideoPlayer.SYNC_TYPE_FAILED
            RequestManager.sendRequest(VideoPlayer.NAME_PROGRESS_SYNC, payload)
        }
    }

    /**
     * 初始化各个端能力
     * @param context 上下文
     * @param alarm 本地闹钟端能力，传空则表示不处理本地闹钟
     * @param audioPlayer 音频播放端能力，包含远端闹钟音频播放
     * @param interceptor 自定义拦截器，解析自定义指令
     * @param launcher 桌面跳转端能力
     * @param playbackController 播放控制端能力
     * @param recognizer 录音识别端能力
     * @param screen 模板信息渲染端能力，在带屏设备上使用
     * @param speaker 扬声器控制端能力
     * @param system 系统端能力
     * @param template 模板渲染端能力
     * @param videoPlayer 视频播放端能力
     */
    fun init(
        evsService: EvsService,
        alarm: Alarm?,
        appAction: AppAction?,
        audioPlayer: AudioPlayer,
        interceptor: Interceptor?,
        launcher: Launcher?,
        playbackController: PlaybackController?,
        recognizer: Recognizer,
        screen: Screen?,
        speaker: Speaker,
        system: System,
        template: Template?,
        videoPlayer: VideoPlayer?,
        wakeWord: WakeWord?
    ) {
        this.contextRef = SoftReference(evsService)

        this.audioPlayer?.removeListener(audioPlayerListener)
        this.videoPlayer?.removeListener(videoPlayerListener)

        this.alarm = alarm
        this.appAction = appAction
        this.audioPlayer = audioPlayer
        this.interceptor = interceptor
        this.launcher = launcher
        this.playbackController = playbackController
        this.recognizer = recognizer
        this.screen = screen
        this.speaker = speaker
        this.system = system
        this.template = template
        this.videoPlayer = videoPlayer
        this.wakeWord = wakeWord

        this.videoPlayer?.addListener(videoPlayerListener)
        this.audioPlayer?.addListener(audioPlayerListener)
    }

//    fun updateCurrentRequestId(requestId: String) {
//        currentRequestId = requestId
//    }

    fun initHandler(handler: Handler) {
        this.handler = handler
    }

//    private fun startExecuteResponses() {
//        val responses: MutableList<OsResponse>?
//        synchronized(pendingExecuteResponses) {
//            responses = pendingExecuteResponses[currentRequestId]
//        }
//
//        var markAsExecuteFinished = true
//
//        if (responses?.isNotEmpty() == true) {
//            val first = responses[0]
//            val name = first.header.name
//            val payload = first.payload
//
//            when {
//                name.startsWith(Constant.NAMESPACE_AUDIO_PLAYER) -> {
//                    audioPlayer?.let { audioPlayer ->
//                        if (name == AudioPlayer.NAME_AUDIO_OUT) {
//                            val resourceId = payload.getString(AudioPlayer.KEY_RESOURCE_ID)
//                            when (val type = payload.getString(AudioPlayer.KEY_TYPE)) {
//                                AudioPlayer.TYPE_PLAYBACK -> {
//                                    when (payload.getString(AudioPlayer.KEY_CONTROL)) {
//                                        AudioPlayer.CONTROL_PLAY -> {
//                                            val url = payload.getString(AudioPlayer.KEY_URL)
//                                            when (payload.getString(AudioPlayer.KEY_BEHAVIOR)) {
//                                                AudioPlayer.BEHAVIOR_IMMEDIATELY -> {
//                                                    upcomingAudioPlaybackResources.clear()
//                                                    val offset =
//                                                        payload.getLongValue(AudioPlayer.KEY_OFFSET)
//                                                    needSetOffsetResources[resourceId] = offset
//                                                    audioPlayer.play(type, resourceId, url)
//                                                }
//                                                AudioPlayer.BEHAVIOR_UPCOMING -> {
//                                                    upcomingAudioPlaybackResources.clear()
//                                                    upcomingAudioPlaybackResources.add(payload)
//                                                }
//                                                else -> {
//
//                                                }
//                                            }
//                                        }
//                                        AudioPlayer.CONTROL_PAUSE -> {
//                                            handler?.post {
//                                                ResponseProcessor.audioPlayer?.pause(
//                                                    type
//                                                )
//                                            }
//                                        }
//                                        AudioPlayer.CONTROL_RESUME -> {
//                                            handler?.post {
//                                                ResponseProcessor.audioPlayer?.resume(
//                                                    type
//                                                )
//                                            }
//                                        }
//                                        else -> {
//                                            // ignore
//                                        }
//                                    }
//                                }
//                                AudioPlayer.TYPE_TTS -> {
//                                    markAsExecuteFinished = false
//                                    val url = payload.getString(AudioPlayer.KEY_URL)
//                                    audioPlayer.play(type, resourceId, url)
//
//                                    val metadata = payload.getJSONObject(AudioPlayer.KEY_METADATA)
//                                    val text = metadata.getString(AudioPlayer.KEY_TEXT)
//                                    if (!text.isNullOrEmpty()) {
//                                        audioPlayer.onTtsText(text)
//                                    } else {
//                                        // ignore
//                                    }
//
//                                    when (payload.getString(AudioPlayer.KEY_BEHAVIOR)) {
//                                        AudioPlayer.BEHAVIOR_SERIAL -> {
//
//                                        }
//                                        AudioPlayer.BEHAVIOR_PARALLEL -> {
//                                            markAsExecuteFinished = true
//                                        }
//                                        else -> {
//
//                                        }
//                                    }
//                                }
//                                AudioPlayer.TYPE_RING -> {
//                                    val url = payload.getString(AudioPlayer.KEY_URL)
//                                    audioPlayer.play(type, resourceId, url)
//                                }
//                                else -> {
//                                    // ignore
//                                }
//                            }
//                        } else {
//
//                        }
//                    }
//                }
//                name.startsWith(Constant.NAMESPACE_RECOGNIZER) -> {
//                    recognizer?.let {
//                        when (name) {
//                            Recognizer.NAME_EXPECT_REPLY -> {
//                                val replyKey = payload.getString(Recognizer.KEY_REPLY_KEY)
//                                val backgroundRecognize =
//                                    payload.getBoolean(Recognizer.KEY_BACKGROUND_RECOGNIZE)
//                                val timeoutMillis = payload.getLong(Recognizer.KEY_TIMEOUT)
//
//                                if (backgroundRecognize == null) {
//                                    it.isBackgroundRecognize = false
//                                } else {
//                                    it.isBackgroundRecognize = backgroundRecognize
//                                }
//
//                                it.expectReply(replyKey)
//                            }
//                            Recognizer.NAME_INTERMEDIATE_TEXT -> {
//                                val text = payload.getString(Recognizer.KEY_TEXT)
//                                it.onIntermediateText(text)
//                            }
//                            Recognizer.NAME_STOP_CAPTURE -> {
//                                it.stopCapture()
//                            }
//                            else -> {
//                                Log.e(TAG, "unsupported name $name")
//                            }
//                        }
//                    }
//                }
//                name.startsWith(Constant.NAMESPACE_VIDEO_PLAYER) -> {
//                    videoPlayer?.let {
//                        if (name == VideoPlayer.NAME_VIDEO_OUT) {
//                            val resourceId = payload.getString(VideoPlayer.KEY_RESOURCE_ID)
//                            if (playerInfoMap.containsKey(resourceId)) {
//                                playerInfoMap[resourceId] = payload
//                            } else {
//                                if (playerInfoMap.size > MAX_PLAYER_INFO_CACHE_SIZE) {
//                                    playerInfoMap.clear()
//                                } else if (!resourceId.isNullOrEmpty()) {
//                                    playerInfoMap[resourceId] = payload
//                                }
//                            }
//                            when (payload.getString(VideoPlayer.KEY_CONTROL)) {
//                                VideoPlayer.CONTROL_PLAY -> {
//                                    val url = payload.getString(VideoPlayer.KEY_URL)
//                                    when (payload.getString(VideoPlayer.KEY_BEHAVIOR)) {
//                                        VideoPlayer.BEHAVIOR_IMMEDIATELY -> {
//                                            upcomingVideoPlaybackResources.clear()
//                                            val offset =
//                                                payload.getLongValue(AudioPlayer.KEY_OFFSET)
//                                            needSetOffsetResources[resourceId] = offset
//                                            it.play(resourceId, url)
//                                        }
//                                        VideoPlayer.BEHAVIOR_UPCOMING -> {
//                                            upcomingVideoPlaybackResources.clear()
//                                            upcomingVideoPlaybackResources.add(payload)
//                                        }
//                                        else -> {
//                                        }
//                                    }
//                                }
//                                VideoPlayer.CONTROL_PAUSE -> {
//                                    handler?.post { it.pause() }
//                                }
//                                VideoPlayer.CONTROL_RESUME -> {
//                                    handler?.post { it.resume() }
//                                }
//                                VideoPlayer.CONTROL_EXIT -> {
//                                    handler?.post { it.exit() }
//                                }
//                                else -> {
//                                    // ignore
//                                }
//                            }
//                        }
//                    }
//                }
//                else -> {
//                }
//            }
//
//            if (markAsExecuteFinished) {
//                try {
//                    if (responses.isNotEmpty()) {
//                        responses.removeAt(0)
//                    }
//                } catch (t: Throwable) {
//                    t.printStackTrace()
//                }
//
//                startExecuteResponses()
//            }
//        } else {
//            synchronized(pendingExecuteResponses) {
//                pendingExecuteResponses.remove(currentRequestId)
//                currentRequestId = ""
//            }
//        }
//    }

    private fun startExecuting(pendingResponses: ArrayDeque<Pair<String, OsResponse>>) {
        synchronized(pendingResponses) {
            if (pendingResponses.isEmpty()) {
                Log.v(TAG, "pendingResponses is empty")
                return
            }
            val next = pendingResponses.poll()

            if (
                next.first.startsWith(RequestBuilder.PREFIX_MANUAL) &&
                next.first != currentManualRequestId
            ) {
                Log.v(
                    TAG,
                    "pending request{${next.first}} not current{$currentManualRequestId}, ignore response{${next.second.header.name}}"
                )
                handler?.post {
                    startExecuting(pendingResponses)
                }
                return
            }

            executeResponse(next.first, next.second)
        }
    }

    private fun executeResponse(requestId: String, response: OsResponse) {
        val targetResponses =
            if (requestId.startsWith(RequestBuilder.PREFIX_MANUAL)) {
                manualPendingExecutingResponses
            } else {
                normalPendingExecutingResponses
            }
        synchronized(targetResponses) {
            var markAsExecuteFinished = true
            val name = response.header.name
            Log.v(TAG, "executeResponse $name")
            val payload = response.payload
            when {
                name.startsWith(Constant.NAMESPACE_ALARM) -> {
                    // 响闹钟需要音频焦点，但是这个响应中只有设置闹钟和删除闹钟，故立即处理
                    alarm?.let { alarm ->
                        when (name) {
                            Alarm.NAME_SET_ALARM -> {
                                val alarmId = payload.getString(Alarm.KEY_ALARM_ID)
                                val timestamp = payload.getLongValue(Alarm.KEY_TIMESTAMP)
                                val url = payload.getString(Alarm.KEY_URL)
                                val alarmItem = Alarm.Item(alarmId, timestamp, url)
                                alarm.setAlarm(alarmItem)
                            }
                            Alarm.NAME_DELETE_ALARM -> {
                                val alarmId = payload.getString(Alarm.KEY_ALARM_ID)
                                alarm.deleteAlarm(alarmId)
                            }
                            else -> {
                                Log.e(TAG, "unsupported name $name")
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_APP_ACTION) -> {
                    appAction?.let {
                        when (name) {
                            AppAction.NAME_CHECK -> {
                                val checkResult: JSONObject = it.check(payload)
                                RequestManager.sendRequest(
                                    AppAction.NAME_CHECK_RESULT,
                                    checkResult
                                )
                            }
                            AppAction.NAME_EXECUTE -> {
                                val result = JSONObject()
                                if (it.execute(payload, result)) {
                                    VisualFocusManager.requestActive(
                                        VisualFocusManager.CHANNEL_APP,
                                        VisualFocusManager.TYPE_APP_ACTION
                                    )
                                    RequestManager.sendRequest(
                                        AppAction.NAME_EXECUTE_SUCCEED,
                                        result
                                    )
                                } else {
                                    RequestManager.sendRequest(
                                        AppAction.NAME_EXECUTE_FAILED,
                                        result
                                    )
                                }
                            }
                            else -> {
                                Log.e(TAG, "unsupported name $name")
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_INTERCEPTOR) -> {
                    // 拦截器返回的 Custom 指令在 EVS 中转换为 interceptor 响应
                    interceptor?.onResponse(name, response.payload)
                }
                name.startsWith(Constant.NAMESPACE_LAUNCHER) -> {
                    launcher?.let {
                        when (name) {
                            Launcher.NAME_START_ACTIVITY -> {
                                val page = payload.getString(Launcher.PAYLOAD_PAGE)
                                val callback = object : Launcher.ExecuteCallback() {
                                    init {
                                        this.page = page
                                    }

                                    override fun onFailed(
                                        failureCode: String?,
                                        feedbackText: String?
                                    ) {
                                        this.failureCode = failureCode
                                        this.feedbackText = feedbackText
                                        this.result = Launcher.RESULT_FAILED
                                    }

                                    override fun onSuccess(feedbackText: String?) {
                                        this.result = Launcher.RESULT_SUCCEED
                                        this.feedbackText = feedbackText
                                    }
                                }
                                val result = it.startActivity(page, callback)
                                if (result) {
                                    when (callback.result) {
                                        Launcher.RESULT_SUCCEED -> {
                                            it.sendStartActivitySucceed(
                                                callback.page,
                                                callback.feedbackText
                                            )
                                        }
                                        Launcher.RESULT_FAILED -> {
                                            it.sendStartActivityFailed(
                                                callback.page,
                                                callback.failureCode, callback.feedbackText
                                            )
                                        }
                                    }
                                }
                            }
                            Launcher.NAME_BACK -> {
                                it.back(object : Launcher.ExecuteCallback() {
                                    override fun onSuccess(feedbackText: String?) {
                                        it.sendBackSucceed(feedbackText)
                                    }

                                    override fun onFailed(
                                        failureCode: String?,
                                        feedbackText: String?
                                    ) {
                                        it.sendBackFailed(feedbackText)
                                    }

                                })
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_SCREEN) -> {
                    screen?.let {
                        when (name) {
                            Screen.NAME_SET_STATE -> {
                                val state = payload.getString(Screen.KEY_STATE)
                                it.setState(state)
                            }
                            Screen.NAME_SET_BRIGHTNESS -> {
                                val brightness = payload.getLongValue(Screen.KEY_BRIGHTNESS)
                                it.setBrightness(brightness)
                            }
                            else -> {

                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_TEMPLATE) -> {
                    template?.let {
                        when (name) {
                            Template.NAME_STATIC_TEMPLATE -> {
                                it.renderStaticTemplate(payload.toString())
                            }
                            Template.NAME_PLAYING -> {
                                val resourceId = payload.getString(AudioPlayer.KEY_RESOURCE_ID)
                                it.notifyPlayerInfoUpdated(resourceId, payload.toString())
                                if (playerInfoMap.containsKey(resourceId)) {
                                    playerInfoMap[resourceId] = payload
                                } else {
                                    if (playerInfoMap.size > MAX_PLAYER_INFO_CACHE_SIZE) {
                                        playerInfoMap.clear()
                                    }
                                    playerInfoMap[resourceId] = payload
                                }
                                if (audioPlayer?.playbackState != AudioPlayer.PLAYBACK_STATE_IDLE
                                    && audioPlayer?.playbackResourceId == resourceId
                                ) {
                                    it.renderPlayerInfo(payload.toString())
                                }
                            }
                            Template.NAME_CUSTOM_TEMPLATE -> {
                                it.renderCustomTemplate(payload.toString())
                            }
                            Template.NAME_EXIT -> {
                                when (val type = payload.getString(Template.KEY_TYPE)) {
                                    Template.TYPE_PLAYER_INFO -> {
                                        it.exitPlayerInfo()
                                    }
                                    Template.TYPE_BODY_1,
                                    Template.TYPE_BODY_2,
                                    Template.TYPE_BODY_3,
                                    Template.TYPE_LIST_1,
                                    Template.TYPE_OPTION_1,
                                    Template.TYPE_OPTION_2,
                                    Template.TYPE_OPTION_3,
                                    Template.TYPE_WEATHER -> {
                                        it.exitStaticTemplate(type)
                                    }
                                    Template.TYPE_CUSTOM -> {
                                        it.exitCustomTemplate()
                                    }
                                }
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_SPEAKER) -> {
                    speaker?.let {
                        when (name) {
                            Speaker.NAME_SET_VOLUME -> {
                                val volume = payload.getIntValue(Speaker.KEY_VOLUME)
//                                    val type = payload.getString(Speaker.KEY_TYPE) 暂时未使用
                                it.setVolume(volume)

                                system?.sendStateSync()
                            }
                            else -> {
                                Log.e(TAG, "unsupported name $name")
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_SYSTEM) -> {
                    system?.let {
                        when (name) {
                            System.NAME_PING -> {
                                val timestamp =
                                    (payload[System.PAYLOAD_TIMESTAMP] as Number).toLong()
                                it.onPing(timestamp)
                            }
                            System.NAME_ERROR -> {
                                payload.getIntValue(System.PAYLOAD_CODE).let { code ->
                                    when {
                                        code >= EvsError.Code.ERROR_SERVER_INTERNAL -> {
                                            if (recognizer?.isRecording() == true) {
                                                recognizer?.requestCancel()
                                            }
                                        }
                                        code == EvsError.Code.ERROR_WRONG_PARAMS -> {
                                            if (recognizer?.isRecording() == true) {
                                                recognizer?.stopCapture()
                                            }
                                        }
                                    }
                                }
                                it.onError(payload)
                            }
                            System.NAME_REVOKE_AUTHORIZATION -> {
                                val context = contextRef?.get()
                                it.revokeAuth(context)
                            }
                            System.NAME_UPDATE_SOFTWARE -> {
                                it.updateSoftware()
                            }
                            System.NAME_CHECK_SOFTWARE_UPDATE -> {
                                it.checkSoftWareUpdate()
                            }
                            System.NAME_UPDATE_DEVICE_MODES -> {
                                val kid = payload.getBoolean(System.PAYLOAD_KID)
                                it.onDeviceModeChanged(kid)
                            }
                            System.NAME_POWER_OFF -> {
                                it.onPowerOff(payload)
                            }
                            System.NAME_FACTORY_RESET -> {
                                it.onFactoryReset()
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_RECOGNIZER) -> {
                    recognizer?.let {
                        when (name) {
                            Recognizer.NAME_INTERMEDIATE_TEXT -> {
                                val text = payload.getString(Recognizer.KEY_TEXT)
                                it.onIntermediateText(text)
                            }
                            Recognizer.NAME_STOP_CAPTURE -> {
                                it.stopCapture()
                            }
                            Recognizer.NAME_EXPECT_REPLY -> {
                                if (!it.isPreventExpectReply) {
                                    val replyKey = payload.getString(Recognizer.KEY_REPLY_KEY)
                                    val backgroundRecognize =
                                        payload.getBoolean(Recognizer.KEY_BACKGROUND_RECOGNIZE)

                                    if (backgroundRecognize == null) {
                                        it.isBackgroundRecognize = false
                                    } else {
                                        it.isBackgroundRecognize = backgroundRecognize
                                    }

                                    it.expectReply(replyKey)
                                }
                            }
                            else -> {
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_AUDIO_PLAYER) -> {
                    audioPlayer?.let { audioPlayer ->
                        if (name == AudioPlayer.NAME_AUDIO_OUT) {
                            val resourceId = payload.getString(AudioPlayer.KEY_RESOURCE_ID)
                            when (val type = payload.getString(AudioPlayer.KEY_TYPE)) {
                                AudioPlayer.TYPE_PLAYBACK -> {
                                    when (payload.getString(AudioPlayer.KEY_CONTROL)) {
                                        AudioPlayer.CONTROL_PLAY -> {
                                            val url = payload.getString(AudioPlayer.KEY_URL)
                                            if (url.isNullOrEmpty()) {
                                                val errorPayload = JSONObject()
                                                errorPayload[AudioPlayer.KEY_TYPE] =
                                                    AudioPlayer.SYNC_TYPE_FAILED
                                                errorPayload[AudioPlayer.KEY_RESOURCE_ID] =
                                                    resourceId
                                                errorPayload[AudioPlayer.KEY_FAILURE_CODE] =
                                                    AudioPlayer.MEDIA_ERROR_INVALID_REQUEST
                                                RequestManager.sendRequest(
                                                    AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC,
                                                    errorPayload
                                                )
                                            } else {
                                                when (payload.getString(AudioPlayer.KEY_BEHAVIOR)) {
                                                    AudioPlayer.BEHAVIOR_IMMEDIATELY -> {
                                                        upcomingAudioPlaybackResources.clear()
                                                        val offset =
                                                            payload.getLongValue(AudioPlayer.KEY_OFFSET)
                                                        needSetOffsetResources[resourceId] = offset
                                                        audioPlayer.play(type, resourceId, url)
                                                    }
                                                    AudioPlayer.BEHAVIOR_UPCOMING -> {
                                                        upcomingAudioPlaybackResources.clear()
                                                        upcomingAudioPlaybackResources.add(payload)
                                                    }
                                                    else -> {

                                                    }
                                                }
                                            }
                                        }
                                        AudioPlayer.CONTROL_PAUSE -> {
                                            handler?.post {
                                                audioPlayer.pause(
                                                    type
                                                )
                                            }
                                        }
                                        AudioPlayer.CONTROL_RESUME -> {
                                            handler?.post {
                                                audioPlayer.resume(
                                                    type
                                                )
                                            }
                                        }
                                        else -> {
                                            // ignore
                                        }
                                    }
                                }
                                AudioPlayer.TYPE_TTS -> {
                                    val url = payload.getString(AudioPlayer.KEY_URL)
                                    if (url.isNullOrEmpty()) {
                                        val errorPayload = JSONObject()
                                        errorPayload[AudioPlayer.KEY_TYPE] =
                                            AudioPlayer.SYNC_TYPE_FAILED
                                        errorPayload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                                        errorPayload[AudioPlayer.KEY_FAILURE_CODE] =
                                            AudioPlayer.MEDIA_ERROR_INVALID_REQUEST
                                        RequestManager.sendRequest(
                                            AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC,
                                            errorPayload
                                        )
                                    } else {
                                        markAsExecuteFinished = false

                                        resourceIdRequestIdMap[resourceId] = requestId

                                        audioPlayer.play(type, resourceId, url)

                                        val metadata =
                                            payload.getJSONObject(AudioPlayer.KEY_METADATA)
                                        val text = metadata.getString(AudioPlayer.KEY_TEXT)
                                        if (!text.isNullOrEmpty()) {
                                            audioPlayer.onTtsText(text)
                                        }

                                        when (payload.getString(AudioPlayer.KEY_BEHAVIOR)) {
                                            AudioPlayer.BEHAVIOR_SERIAL -> {

                                            }
                                            AudioPlayer.BEHAVIOR_PARALLEL -> {
                                                markAsExecuteFinished = true

                                                if (ttsParallelMap.size > MAX_PARALLEL_CACHE_SIZE) {
                                                    ttsParallelMap.clear()
                                                }
                                                ttsParallelMap[resourceId] = true
                                            }
                                            else -> {

                                            }
                                        }
                                    }
                                }
                                AudioPlayer.TYPE_RING -> {
                                    val url = payload.getString(AudioPlayer.KEY_URL)
                                    if (url.isNullOrEmpty()) {
                                        val errorPayload = JSONObject()
                                        errorPayload[AudioPlayer.KEY_TYPE] =
                                            AudioPlayer.SYNC_TYPE_FAILED
                                        errorPayload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                                        errorPayload[AudioPlayer.KEY_FAILURE_CODE] =
                                            AudioPlayer.MEDIA_ERROR_INVALID_REQUEST
                                        RequestManager.sendRequest(
                                            AudioPlayer.NAME_PLAYBACK_PROGRESS_SYNC,
                                            errorPayload
                                        )
                                    } else {
                                        audioPlayer.play(type, resourceId, url)
                                    }
                                }
                                else -> {
                                    // ignore
                                }
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_VIDEO_PLAYER) -> {
                    videoPlayer?.let {
                        if (name == VideoPlayer.NAME_VIDEO_OUT) {
                            val resourceId = payload.getString(VideoPlayer.KEY_RESOURCE_ID)
                            if (playerInfoMap.containsKey(resourceId)) {
                                playerInfoMap[resourceId] = payload
                            } else {
                                if (playerInfoMap.size > MAX_PLAYER_INFO_CACHE_SIZE) {
                                    playerInfoMap.clear()
                                } else if (!resourceId.isNullOrEmpty()) {
                                    playerInfoMap[resourceId] = payload
                                }
                            }
                            when (payload.getString(VideoPlayer.KEY_CONTROL)) {
                                VideoPlayer.CONTROL_PLAY -> {
                                    val url = payload.getString(VideoPlayer.KEY_URL)
                                    if (url.isNullOrEmpty()) {
                                        val errorPayload = JSONObject()
                                        errorPayload[AudioPlayer.KEY_TYPE] =
                                            VideoPlayer.SYNC_TYPE_FAILED
                                        errorPayload[AudioPlayer.KEY_RESOURCE_ID] = resourceId
                                        errorPayload[AudioPlayer.KEY_FAILURE_CODE] =
                                            VideoPlayer.MEDIA_ERROR_INVALID_REQUEST
                                        RequestManager.sendRequest(
                                            VideoPlayer.NAME_PROGRESS_SYNC,
                                            errorPayload
                                        )
                                    } else {
                                        when (payload.getString(VideoPlayer.KEY_BEHAVIOR)) {
                                            VideoPlayer.BEHAVIOR_IMMEDIATELY -> {
                                                upcomingVideoPlaybackResources.clear()
                                                val offset =
                                                    payload.getLongValue(AudioPlayer.KEY_OFFSET)
                                                needSetOffsetResources[resourceId] = offset
                                                it.play(resourceId, url)
                                            }
                                            VideoPlayer.BEHAVIOR_UPCOMING -> {
                                                upcomingVideoPlaybackResources.clear()
                                                upcomingVideoPlaybackResources.add(payload)
                                            }
                                            else -> {
                                            }
                                        }
                                    }
                                }
                                VideoPlayer.CONTROL_PAUSE -> {
                                    handler?.post { it.pause() }
                                }
                                VideoPlayer.CONTROL_RESUME -> {
                                    handler?.post { it.resume() }
                                }
                                VideoPlayer.CONTROL_EXIT -> {
                                    handler?.post { it.exit() }
                                }
                                else -> {
                                    // ignore
                                }
                            }
                        }
                    }
                }
                name.startsWith(Constant.NAMESPACE_WAKE_WORD) -> {
                    wakeWord?.let {
                        if (name == WakeWord.NAME_SET_WAKE_WORD) {
                            val wakeWord = payload.getString(WakeWord.PAYLOAD_WAKE_WORD)
                            val wakeWordId = payload.getString(WakeWord.PAYLOAD_WAKE_WORD_ID)
                            val url = payload.getString(WakeWord.PAYLOAD_URL)

                            it.setWakeWord(wakeWordId, wakeWord, url)
                        }
                    }
                }
                else -> {

                }
            }
            if (markAsExecuteFinished) {
                if (targetResponses.isEmpty()) {
                    Log.v(TAG, "markAsExecuteFinished, but no more responses")
                    return
                }
                Log.v(TAG, "markAsExecuteFinished, start executing next response")
                handler?.post {
                    startExecuting(targetResponses)
                }
            }
        }
    }

    fun clearPendingManualExecuting() {
        synchronized(manualPendingExecutingResponses) {
            Log.v(TAG, "clearPendingManualExecuting")
            val isParallel = ttsParallelMap[currentTtsResourceId] == true
            val isManual =
                resourceIdRequestIdMap[currentTtsResourceId]
                    ?.startsWith(RequestBuilder.PREFIX_MANUAL) == true
            if (isTtsPlaying && !isParallel && isManual) {
                handler?.post {
                    audioPlayer?.stop(AudioPlayer.TYPE_TTS)
                }
            }
            manualPendingExecutingResponses.clear()
        }
    }

    fun putResponses(json: String) {
        synchronized(manualPendingExecutingResponses) {
            synchronized(normalPendingExecutingResponses) {
                try {
                    val responseBody = OsResponseBody.fromJSONObject(JSON.parseObject(json))
                    val isLast = responseBody.meta.isLast
                    val requestId = responseBody.meta.requestId
                    val responses = responseBody.responses
                    if (responses.isNullOrEmpty())
                        return
                    val pairRequestId = if (requestId.isNullOrEmpty()) {
                        UUID.randomUUID().toString()
                    } else {
                        requestId
                    }
                    if (pairRequestId.startsWith(RequestBuilder.PREFIX_MANUAL)) {
                        if (pairRequestId != currentManualRequestId) {
                            Log.d(
                                TAG,
                                "request_id{$pairRequestId} is not equals with " +
                                    "current request_id{$currentManualRequestId}"
                            )
                            return
                        }
                    }
                    val isExecuting =
                        if (pairRequestId.startsWith(RequestBuilder.PREFIX_MANUAL))
                            manualPendingExecutingResponses.isNotEmpty()
                        else
                            normalPendingExecutingResponses.isNotEmpty()
                    var hasToStopCapture = false
                    var avoidPlayResponseSound = false
                    responses.map {
                        val name = it.header.name
                        Log.v(TAG, "add response $name")
                        when {
                            name.startsWith(Constant.NAMESPACE_RECOGNIZER) -> {
                                if (name == Recognizer.NAME_EXPECT_REPLY) {
                                    hasToStopCapture = true
                                } else if (name == Recognizer.NAME_STOP_CAPTURE
                                    || name == Recognizer.NAME_INTERMEDIATE_TEXT
                                ) {
                                    avoidPlayResponseSound = true
                                }
                            }
                            name.startsWith(Constant.NAMESPACE_AUDIO_PLAYER) -> {
                                if (name == AudioPlayer.NAME_AUDIO_OUT) {
                                    val behavior = it.payload.getString(AudioPlayer.KEY_BEHAVIOR)
                                    val type = it.payload.getString(AudioPlayer.KEY_TYPE)
                                    if (type == AudioPlayer.TYPE_TTS
                                        || (type == AudioPlayer.TYPE_PLAYBACK && behavior == AudioPlayer.BEHAVIOR_IMMEDIATELY)
                                    ) {
                                        hasToStopCapture = true
                                    }
                                }
                            }
                            name.startsWith(Constant.NAMESPACE_VIDEO_PLAYER) -> {
                                if (name == VideoPlayer.NAME_VIDEO_OUT) {
                                    val behavior = it.payload.getString(AudioPlayer.KEY_BEHAVIOR)
                                    if (behavior == VideoPlayer.BEHAVIOR_IMMEDIATELY) {
                                        hasToStopCapture = true
                                    }
                                }
                            }
                            name.startsWith(Constant.NAMESPACE_SYSTEM) -> {
                                if (name == System.NAME_PING) {
                                    avoidPlayResponseSound = true
                                }
                            }
                        }
                        if (pairRequestId.startsWith(RequestBuilder.PREFIX_MANUAL)) {
                            manualPendingExecutingResponses.add(Pair(pairRequestId, it))
                        } else {
                            normalPendingExecutingResponses.add(Pair(pairRequestId, it))
                        }
                    }
                    if (responses.isNotEmpty()) {
                        contextRef?.get()?.let { service ->
                            if (!avoidPlayResponseSound
                                && service.isResponseSoundEnabled()
                                && pairRequestId.startsWith(RequestBuilder.PREFIX_MANUAL)
                                && !hadPlayResponseSoundRequestIdSet.contains(pairRequestId)
                            ) {
                                if (hadPlayResponseSoundRequestIdSet.size > 100) {
                                    hadPlayResponseSoundRequestIdSet.clear()
                                }
                                hadPlayResponseSoundRequestIdSet.add(pairRequestId)
                                service.playResponseSound()
                            }
                        }
                    }
                    if (pairRequestId.startsWith(RequestBuilder.PREFIX_MANUAL)) {
                        normalPendingExecutingResponses.clear()
                    } else {
                        if (hasToStopCapture)
                            manualPendingExecutingResponses.clear()
                    }
                    if (hasToStopCapture) {
                        if (recognizer?.isRecording() == true
                            && recognizer?.isBackgroundRecognize != true
                        ) {
                            recognizer?.requestEnd()
                        }
                    }
                    val isParallel = ttsParallelMap[currentTtsResourceId] == true
                    val shouldBreakTts =
                        (pairRequestId.startsWith(RequestBuilder.PREFIX_MANUAL)
                            && resourceIdRequestIdMap[currentTtsResourceId]?.startsWith(
                            RequestBuilder.PREFIX_MANUAL
                        ) != true)
                            || (!pairRequestId.startsWith(RequestBuilder.PREFIX_MANUAL)
                            && resourceIdRequestIdMap[currentTtsResourceId]?.startsWith(
                            RequestBuilder.PREFIX_MANUAL
                        ) == true)
                    Log.v(
                        TAG,
                        "isExecuting: $isExecuting, isTtsPlaying: $isTtsPlaying, " +
                            "isParallel: $isParallel, shouldBreakTts: $shouldBreakTts"
                    )
                    if (!isExecuting && !(isTtsPlaying && !isParallel && !shouldBreakTts)) {
                        handler?.post {
                            if (pairRequestId.startsWith(RequestBuilder.PREFIX_MANUAL)) {
                                startExecuting(manualPendingExecutingResponses)
                            } else {
                                startExecuting(normalPendingExecutingResponses)
                            }
                        }
                    } else {
                        Log.v(TAG, "Responses had already pending")
                    }

                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    fun destroy() {
        audioPlayer?.removeListener(audioPlayerListener)
    }
}