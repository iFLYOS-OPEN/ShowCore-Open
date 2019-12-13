package com.iflytek.cyber.evs.sdk.socket

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.agent.*
import com.iflytek.cyber.evs.sdk.model.*
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
internal object RequestBuilder {
    const val PREFIX_REQUEST = "request"
    const val PREFIX_EVENT = "event"
    const val PREFIX_MANUAL = "manual_"

    private const val KEY_VERSION = "version"

    private var token = ""
    private var deviceId = ""

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

    var customIflyosContext: String? = null

    fun init(
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
    }

    fun setDeviceAuthInfo(deviceId: String, token: String) {
        this.deviceId = deviceId
        this.token = token
    }

    fun buildRequestBody(
        name: String,
        payload: JSONObject,
        isManual: Boolean = false
    ): OsRequestBody {
        val requestId = UUID.randomUUID().toString()
        val prefix = if (isManual) PREFIX_MANUAL else ""
        val requestHeader = RequestHeader(name, "$prefix$requestId")

        val device = HeaderDevice(deviceId, null, DevicePlatform(), null)
        val header = OsHeader("Bearer $token", device)

        val context = buildContext()

        return OsRequestBody(header, context, OsRequest(requestHeader, payload))
    }

    fun buildContext(): JSONObject {
        if (!customIflyosContext.isNullOrEmpty()) {
            try {
                return JSON.parseObject(customIflyosContext)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        val context = JSONObject()

        alarm?.let {
            if (!it.isDisabled()) {
                val alarmContext = JSONObject()
                alarmContext[KEY_VERSION] = it.version
                val localAlarms = it.getLocalAlarms()
                if (localAlarms.isNotEmpty()) {
                    val localAlarmsJson = JSONArray()
                    localAlarms.map { item ->
                        val alarmJson = JSONObject()
                        alarmJson[Alarm.KEY_ALARM_ID] = item.alarmId
                        alarmJson[Alarm.KEY_TIMESTAMP] = item.timestamp
                        localAlarmsJson.add(alarmJson)
                    }
                    alarmContext[Alarm.KEY_LOCAL] = localAlarmsJson
                }
                it.getActiveAlarmId()?.let { activeAlarmId ->
                    val activeAlarmContext = JSONObject()
                    activeAlarmContext[Alarm.KEY_ALARM_ID] = activeAlarmId
                    alarmContext[Alarm.KEY_ACTIVE] = activeAlarmContext
                }
                context[Constant.NAMESPACE_ALARM] = alarmContext
            }
        }

        appAction?.let {
            val appActionContext = JSONObject()
            val foreApp = it.getForegroundApp()

            foreApp?.let {
                appActionContext[AppAction.KEY_FOREGROUND_APP] = foreApp.pkgName
                appActionContext[AppAction.KEY_ACTIVITY] = foreApp.curActivity
            }

            val supportedExecute = it.getSupportedExecute()
            if (supportedExecute.isNotEmpty()) {
                val array = JSONArray()
                supportedExecute.map { type ->
                    array.add(type)
                }
                appActionContext[AppAction.KEY_SUPPORTED_EXECUTE] = array
            }

            appActionContext[KEY_VERSION] = it.version
            context[Constant.NAMESPACE_APP_ACTION] = appActionContext
        }

        audioPlayer?.let {
            val audioPlayerContext = JSONObject()
            audioPlayerContext[KEY_VERSION] = it.version
            val playbackContext = JSONObject()
            it.playbackResourceId?.let { resourceId ->
                playbackContext[AudioPlayer.KEY_RESOURCE_ID] = resourceId
            }
            it.playbackOffset.let { offset ->
                if (offset >= 0) {
                    playbackContext[AudioPlayer.KEY_OFFSET] = offset
                }
            }
            playbackContext[AudioPlayer.KEY_STATE] = it.playbackState
            audioPlayerContext[AudioPlayer.KEY_PLAYBACK] = playbackContext

            context[Constant.NAMESPACE_AUDIO_PLAYER] = audioPlayerContext
        }

        interceptor?.let {
            val interceptorContext = it.contextJson
            interceptorContext[KEY_VERSION] = it.version
            context[Constant.NAMESPACE_INTERCEPTOR] = interceptorContext
        }

        launcher?.let {
            val launcherController = JSONObject()
            launcherController[KEY_VERSION] = it.version
            context[Constant.NAMESPACE_LAUNCHER] = launcherController
        }

        playbackController?.let {
            val playbackController = JSONObject()
            playbackController[KEY_VERSION] = it.version
            context[Constant.NAMESPACE_PLAYBACK_CONTROLLER] = playbackController
        }

        recognizer?.let {
            val recognizerContext = JSONObject()
            recognizerContext[KEY_VERSION] = it.version
            context[Constant.NAMESPACE_RECOGNIZER] = recognizerContext
        }

        screen?.let {
            val screenContext = JSONObject()
            screenContext[KEY_VERSION] = it.version
            screenContext[Screen.KEY_STATE] = it.getState()
            screenContext[Screen.KEY_BRIGHTNESS] = it.getBrightness()
            screenContext[Screen.KEY_TYPE] = it.getBrightnessType()
            context[Constant.NAMESPACE_SCREEN] = screenContext
        }

        speaker?.let {
            val speakerContext = JSONObject()
            speakerContext[KEY_VERSION] = it.version
            speakerContext[Speaker.KEY_VOLUME] = it.getCurrentVolume()
            speakerContext[Speaker.KEY_TYPE] = it.getType()
            context[Constant.NAMESPACE_SPEAKER] = speakerContext
        }

        system?.let {
            val systemContext = JSONObject()
            systemContext[KEY_VERSION] = it.version
            systemContext[System.KEY_SOFTWARE_UPDATER] = it.hasSoftwareUpdater
            systemContext[System.KEY_POWER_CONTROLLER] = it.hasPowerController
            systemContext[System.KEY_DEVICE_MODES] = it.hasDeviceModes
            context[Constant.NAMESPACE_SYSTEM] = systemContext
        }

        template?.let {
            val templateContext = JSONObject()
            templateContext[KEY_VERSION] = it.version
            templateContext[Template.KEY_FOCUSED] = it.isFocused()
            it.getFocusTemplateType()?.let { templateType ->
                templateContext[Template.KEY_TEMPLATE_TYPE] = templateType
            }
            context[Constant.NAMESPACE_TEMPLATE] = templateContext
        }

        videoPlayer?.let {
            val videoPlayerContext = JSONObject()
            videoPlayerContext[KEY_VERSION] = it.version
            videoPlayerContext[VideoPlayer.KEY_STATE] = it.state
            it.resourceId?.let { resourceId ->
                videoPlayerContext[VideoPlayer.KEY_RESOURCE_ID] = resourceId
            }
            it.videoOffset.let { offset ->
                if (offset > 0) {
                    videoPlayerContext[VideoPlayer.KEY_OFFSET] = offset
                }
            }
            context[Constant.NAMESPACE_VIDEO_PLAYER] = videoPlayerContext
        }

        wakeWord?.let {
            val wakeWordContext = JSONObject()
            wakeWordContext[KEY_VERSION] = it.version
            context[Constant.NAMESPACE_WAKE_WORD] = wakeWordContext
        }

        return context
    }
}