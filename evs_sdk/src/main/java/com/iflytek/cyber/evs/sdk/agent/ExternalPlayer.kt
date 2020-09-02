package com.iflytek.cyber.evs.sdk.agent

import androidx.annotation.CallSuper
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.RequestCallback
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.focus.AudioFocusManager
import com.iflytek.cyber.evs.sdk.model.Constant

abstract class ExternalPlayer {
    val version
        get() = "1.0"

    companion object {
        const val NAME_COMMAND = "${Constant.NAMESPACE_EXTERNAL_PLAYER}.command"
        const val NAME_COMMAND_RESULT = "${Constant.NAMESPACE_EXTERNAL_PLAYER}.command_result"

        const val KEY_SOURCE_ID = "source_id"
        const val KEY_COMMAND = "command"
        const val KEY_VALUE = "value"
        const val KEY_LIST = "list"
        const val KEY_URL = "url"
        const val KEY_ID = "id"
        const val KEY_OFFSET = "offset"
        const val KEY_EXTERNAL_PLAYER_ACTIVE = "external_player_active"
        const val KEY_TYPE = "type"
        const val KEY_RESULT = "result"
        const val KEY_FAILURE_CODE = "failure_code"
        const val KEY_FEEDBACK_TEXT = "feedback_text"

        const val TYPE_AUDIO = "AUDIO"
        const val TYPE_VIDEO = "VIDEO"
        const val RESULT_SUCCEED = "SUCCEED"
        const val RESULT_FAILED = "FAILED"
        const val COMMAND_PLAY = "PLAY"
        const val COMMAND_NEXT = "NEXT"
        const val COMMAND_PREVIOUS = "PREVIOUS"
        const val COMMAND_RESUME = "RESUME"
        const val COMMAND_PAUSE = "PAUSE"
        const val COMMAND_EXIT = "EXIT"
        const val COMMAND_VOLUME_UP = "VOLUME_UP"
        const val COMMAND_VOLUME_DOWN = "VOLUME_DOWN"
        const val COMMAND_FAST_FORWARD = "FAST_FORWARD"
        const val COMMAND_FAST_BACKWARD = "FAST_BACKWARD"
        const val COMMAND_SET_OFFSET = "SET_OFFSET"
        const val CODE_COMMAND_UNSUPPORTED = "COMMAND_UNSUPPORTED"
        const val CODE_NO_EXTERNAL_PLAYER = "NO_EXTERNAL_PLAYER"
        const val CODE_INTERNAL_ERROR = "INTERNAL_ERROR"
    }

    enum class PlayerState {
        IDLE,
        PLAYING,
        PAUSED,
    }

    var playerState: PlayerState = PlayerState.IDLE
        protected set

    var offset: Int = 0
        protected set

    var playerType: String = TYPE_AUDIO
    var sourceId: String = "abc123"
    var isActive: Boolean = false

    /**
     * 执行云端下发的命令。
     */
    fun execCommand(payload: JSONObject) {
        val command = payload.getString(KEY_COMMAND)

        when (command) {
            COMMAND_PLAY -> {
                val value = payload.getJSONObject(KEY_VALUE)
                val list = value.getJSONArray(KEY_LIST)

                play(list.toJSONString())
            }
            COMMAND_NEXT -> {
                next()
            }
            COMMAND_PREVIOUS -> {
                previous()
            }
            COMMAND_RESUME -> {
                resume()
            }
            COMMAND_PAUSE -> {
                pause()
            }
            COMMAND_EXIT -> {
                exit()
            }
            COMMAND_VOLUME_UP -> {
                volumeUp()
            }
            COMMAND_VOLUME_DOWN -> {
                volumeDown()
            }
            COMMAND_FAST_FORWARD -> {
                val value = payload.getJSONObject(KEY_VALUE)
                val tempOffset = value.getIntValue(KEY_OFFSET)

                fastForward(tempOffset)
            }
            COMMAND_FAST_BACKWARD -> {
                val value = payload.getJSONObject(KEY_VALUE)
                val tempOffset = value.getIntValue(KEY_OFFSET)

                fastBackward(tempOffset)
            }
            COMMAND_SET_OFFSET -> {
                val value = payload.getJSONObject(KEY_VALUE)
                val tempOffset = value.getIntValue(KEY_OFFSET)

                seekTo(tempOffset)
            }
        }
    }

    fun requestFocus() {
        when (playerType) {
            TYPE_AUDIO -> {
                AudioFocusManager.requestActive(
                    AudioFocusManager.CHANNEL_CONTENT,
                    AudioFocusManager.TYPE_EXTERNAL_AUDIO
                )
            }
            TYPE_VIDEO -> {
                AudioFocusManager.requestActive(
                    AudioFocusManager.CHANNEL_CONTENT,
                    AudioFocusManager.TYPE_EXTERNAL_VIDEO
                )
            }
        }
    }

    fun abandonFocus() {
        when (playerType) {
            TYPE_AUDIO -> {
                AudioFocusManager.requestAbandon(
                    AudioFocusManager.CHANNEL_CONTENT,
                    AudioFocusManager.TYPE_EXTERNAL_AUDIO
                )
            }
            TYPE_VIDEO -> {
                AudioFocusManager.requestAbandon(
                    AudioFocusManager.CHANNEL_CONTENT,
                    AudioFocusManager.TYPE_EXTERNAL_VIDEO
                )
            }
        }
    }

    /**
     * 播放一个列表。
     *
     * @list 列表，格式示例：
     *
     *  [
     *   {
     *   "url": "",
     *   "id": ""
     *   },
     *   {...}
     *   ]
     */
    @CallSuper
    open fun play(list: String) {
        requestFocus()
    }

    @CallSuper
    open fun next() {
        requestFocus()
    }

    @CallSuper
    open fun previous() {
        requestFocus()
    }

    @CallSuper
    open fun pause() {
        abandonFocus()
    }

    @CallSuper
    open fun resume() {
        requestFocus()
    }

    @CallSuper
    open fun exit() {
        abandonFocus()
    }

    abstract fun volumeUp()

    abstract fun volumeDown()

    abstract fun fastForward(offset: Int)

    abstract fun fastBackward(offset: Int)

    abstract fun seekTo(offset: Int)

    /**
     * 向服务端回复命令的执行结果。
     *
     * @param isSuccess 是否成功
     * @param failureCode 失败代码，取值：CODE_COMMAND_UNSUPPORTED，CODE_NO_EXTERNAL_PLAYER，CODE_INTERNAL_ERROR
     * @param feedbackText 提示用户的TTS，没有该字段或字段取值""，代表不提示
     */
    fun replyCommandResult(isSuccess: Boolean, failureCode: String? = null, feedbackText: String? = null,
                           requestCallback: RequestCallback? = null) {
        val payload = JSONObject()

        payload[KEY_RESULT] = if (isSuccess) RESULT_SUCCEED else RESULT_FAILED
        payload[KEY_SOURCE_ID] = sourceId;

        if (!failureCode.isNullOrEmpty()) {
            payload[KEY_FAILURE_CODE] = failureCode
        }

        if (!feedbackText.isNullOrEmpty()) {
            payload[KEY_FEEDBACK_TEXT] = feedbackText
        }

        RequestManager.sendRequest(NAME_COMMAND_RESULT, payload, requestCallback, false)
    }

    abstract fun moveToBackground(): Boolean

    abstract fun moveToForegroundIfAvailable(): Boolean
}