package com.iflytek.cyber.evs.sdk.agent

import androidx.annotation.CallSuper
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.model.Constant
import com.iflytek.cyber.evs.sdk.utils.Log

/**
 * 音频播放器模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/audio_player.html#%E9%9F%B3%E9%A2%91%E6%92%AD%E6%94%BE%E5%99%A8
 */
abstract class AudioPlayer {
    val version
        get() = "1.1"

    companion object {
        const val NAME_PLAYBACK_PROGRESS_SYNC =
            "${Constant.NAMESPACE_AUDIO_PLAYER}.playback.progress_sync"
        const val NAME_RING_PROGRESS_SYNC = "${Constant.NAMESPACE_AUDIO_PLAYER}.ring.progress_sync"
        const val NAME_TTS_PROGRESS_SYNC = "${Constant.NAMESPACE_AUDIO_PLAYER}.tts.progress_sync"
        const val NAME_TTS_TEXT_IN = "${Constant.NAMESPACE_AUDIO_PLAYER}.tts.text_in"

        const val NAME_AUDIO_OUT = "${Constant.NAMESPACE_AUDIO_PLAYER}.audio_out"

        const val TYPE_PLAYBACK = "PLAYBACK"
        const val TYPE_RING = "RING"
        const val TYPE_TTS = "TTS"

        const val SYNC_TYPE_STARTED = "STARTED"
        const val SYNC_TYPE_FINISHED = "FINISHED"
        const val SYNC_TYPE_FAILED = "FAILED"
        const val SYNC_TYPE_NEARLY_FINISHED = "NEARLY_FINISHED"
        const val SYNC_TYPE_PAUSED = "PAUSED"
        const val SYNC_TYPE_STOPPED = "STOPPED"

        const val CONTROL_PLAY = "PLAY"
        const val CONTROL_PAUSE = "PAUSE"
        const val CONTROL_RESUME = "RESUME"

        const val KEY_TYPE = "type"
        const val KEY_URL = "url"
        const val KEY_CONTROL = "control"
        const val KEY_RESOURCE_ID = "resource_id"
        const val KEY_OFFSET = "offset"
        const val KEY_BEHAVIOR = "behavior"
        const val KEY_FAILURE_CODE = "failure_code"
        const val KEY_PLAYBACK = "playback"
        const val KEY_STATE = "state"
        const val KEY_TEXT = "text"
        const val KEY_METADATA = "metadata"

        const val BEHAVIOR_IMMEDIATELY = "IMMEDIATELY"
        const val BEHAVIOR_UPCOMING = "UPCOMING"
        const val BEHAVIOR_SERIAL = "SERIAL"
        const val BEHAVIOR_PARALLEL = "PARALLEL"

        const val PLAYBACK_STATE_IDLE = "IDLE"
        const val PLAYBACK_STATE_PLAYING = "PLAYING"
        const val PLAYBACK_STATE_PAUSED = "PAUSED"

        const val MEDIA_ERROR_UNKNOWN = "1001"                // 发生了未知错误
        const val MEDIA_ERROR_INVALID_REQUEST =
            "1002"        // 请求无效。可能的情况有：bad request, unauthorized, forbidden, not found等。
        const val MEDIA_ERROR_SERVICE_UNAVAILABLE = "1003"    // 设备端无法获取音频文件
        const val MEDIA_ERROR_INTERNAL_SERVER_ERROR = "1004"  // 服务端接收了请求但未能正确处理
        const val MEDIA_ERROR_INTERNAL_DEVICE_ERROR = "1005"  // 设备端内部错误

    }

    private val listeners = HashSet<MediaStateChangedListener>()

    var playbackResourceId: String? = null
        private set
    var playbackState = PLAYBACK_STATE_IDLE
        private set
    var playbackOffset = Long.MIN_VALUE

    fun addListener(listener: MediaStateChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: MediaStateChangedListener) {
        listeners.remove(listener)
    }

    /**
     * 播放音频。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @param resourceId 资源id，可从response中获取
     * @param url 待播放的音频url
     * @return 是否成功
     */
    abstract fun play(type: String, resourceId: String, url: String): Boolean

    /**
     * 暂停播放。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @return 是否成功
     */
    abstract fun pause(type: String): Boolean

    /**
     * 暂停后恢复播放。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @return 是否成功
     */
    abstract fun resume(type: String): Boolean

    /**
     * 停止播放。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @return 是否成功
     */
    abstract fun stop(type: String): Boolean

    /**
     * 时间进度选择。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @param offset 时间进度，单位：毫秒
     * @return 是否成功
     */
    @CallSuper
    open fun seekTo(type: String, offset: Long): Boolean {
        if (type == TYPE_PLAYBACK)
            playbackOffset = offset
        return false
    }

    /**
     * 获取当前播放时间进度。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @return 时间进度，单位：毫秒
     */
    abstract fun getOffset(type: String): Long

    /**
     * 获取音频文件时长。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @return 时长，单位：毫秒
     */
    abstract fun getDuration(type: String): Long

    /**
     * 转移到前台播放。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @return 是否成功
     */
    abstract fun moveToForegroundIfAvailable(type: String): Boolean

    /**
     * 转移到后台播放。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @return 是否成功
     */
    abstract fun moveToBackground(type: String): Boolean

    /**
     * 发送合成文本，调用云端合成。
     * @param text 待合成文本
     */
    @CallSuper
    open fun sendTtsText(text: String) {
        val payload = JSONObject()
        payload[KEY_TEXT] = text

        RequestManager.sendRequest(NAME_TTS_TEXT_IN, payload)
    }

    /**
     * 接收到TTS音频对应的文本。
     * @param text 文本内容（其中可能带有类似 `[di4]` 之类的标记符）
     */
    open fun onTtsText(text: String) {}

    /**
     * 开始播放回调。
     * @param type 音频类型
     * @param resourceId 资源id
     */
    fun onStarted(type: String, resourceId: String) {
        Log.d("AudioPlayer", "onStarted($type, $resourceId)")
        if (type == TYPE_PLAYBACK) {
            playbackState = PLAYBACK_STATE_PLAYING
            playbackResourceId = resourceId

            Log.d("state_test", "onStarted")
        }
        listeners.map {
            try {
                it.onStarted(this, type, resourceId)
            } catch (_: Exception) {

            }
        }
    }

    /**
     * 恢复播放回调。
     * @param type 音频类型
     * @param resourceId 资源id
     */
    fun onResumed(type: String, resourceId: String) {
        Log.d("AudioPlayer", "onResumed($type, $resourceId)")
        if (type == TYPE_PLAYBACK) {
            playbackState = PLAYBACK_STATE_PLAYING

            Log.d("state_test", "onResumed")
        }
        listeners.map {
            try {
                it.onResumed(this, type, resourceId)
            } catch (_: Exception) {

            }
        }
    }

    /**
     * 暂停播放回调。
     * @param type 音频类型
     * @param resourceId 资源id
     */
    fun onPaused(type: String, resourceId: String) {
        Log.d("AudioPlayer", "onPaused($type, $resourceId)")
        if (type == TYPE_PLAYBACK) {
            playbackState = PLAYBACK_STATE_PAUSED
        }
        listeners.map {
            try {
                it.onPaused(this, type, resourceId)
            } catch (_: Exception) {

            }
        }
    }

    /**
     * 停止播放回调。
     * @param type 音频类型
     * @param resourceId 资源id
     */
    fun onStopped(type: String, resourceId: String) {
        Log.d("AudioPlayer", "onStopped($type, $resourceId)")
        if (type == TYPE_PLAYBACK) {
            playbackState = PLAYBACK_STATE_PAUSED
        }
        listeners.map {
            try {
                it.onStopped(this, type, resourceId)
            } catch (_: Exception) {

            }
        }
    }

    /**
     * 播放完成回调。
     * @param type 音频类型
     * @param resourceId 资源id
     */
    fun onCompleted(type: String, resourceId: String) {
        Log.d("AudioPlayer", "onCompleted($type, $resourceId)")
        if (type == TYPE_PLAYBACK) {
            playbackState = PLAYBACK_STATE_PAUSED
        }
        listeners.map {
            try {
                it.onCompleted(this, type, resourceId)
            } catch (_: Exception) {

            }
        }
    }

    /**
     * 播放位置更新回调。
     * @param type 音频类型
     * @param resourceId 资源id
     * @param position 当前播放位置，单位：毫秒
     */
    fun onPositionUpdated(type: String, resourceId: String, position: Long) {
        if (type == TYPE_PLAYBACK) {
            playbackOffset = position
        }

        listeners.map {
            try {
                it.onPositionUpdated(this, type, resourceId, position)
            } catch (_: Exception) {

            }
        }
    }

    /**
     * 出错回调。
     * @param type 音频类型，取值：TYPE_PLAYBACK，TYPE_RING，TYPE_TTS
     * @param resourceId 资源id
     * @param errorCode 错误码
     */
    fun onError(type: String, resourceId: String, errorCode: String) {
        Log.d("AudioPlayer", "onError($type, $resourceId, $errorCode)")
        if (type == TYPE_PLAYBACK)
            playbackState = PLAYBACK_STATE_PAUSED
        listeners.map {
            try {
                it.onError(this, type, resourceId, errorCode)
            } catch (_: Exception) {

            }
        }
    }

    fun onPressPlayOrPause() {

    }

    fun onPressPreious() {

    }

    fun onPressNext() {

    }

    interface MediaStateChangedListener {
        fun onStarted(player: AudioPlayer, type: String, resourceId: String)
        fun onResumed(player: AudioPlayer, type: String, resourceId: String)
        fun onPaused(player: AudioPlayer, type: String, resourceId: String)
        fun onStopped(player: AudioPlayer, type: String, resourceId: String)
        fun onCompleted(player: AudioPlayer, type: String, resourceId: String)
        fun onPositionUpdated(player: AudioPlayer, type: String, resourceId: String, position: Long)
        fun onError(player: AudioPlayer, type: String, resourceId: String, errorCode: String)
    }
}