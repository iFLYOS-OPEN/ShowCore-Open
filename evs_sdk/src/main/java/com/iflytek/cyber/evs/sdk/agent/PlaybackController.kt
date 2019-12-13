package com.iflytek.cyber.evs.sdk.agent

import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.RequestCallback
import com.iflytek.cyber.evs.sdk.RequestManager

/**
 * 播放控制模块，详细介绍见https://doc.iflyos.cn/device/evs/reference/playback_controller.html#%E6%92%AD%E6%94%BE%E6%8E%A7%E5%88%B6
 */
abstract class PlaybackController {
    val version = "1.0"

    companion object {
        const val NAME_CONTROL_COMMAND = "playback_controller.control_command"

        const val KEY_TYPE = "type"
    }

    /**
     * 发送控制命令。
     * @param command 命令
     */
    fun sendCommand(command: Command, requestCallback: RequestCallback? = null) {
        val payload = JSONObject()
        when (command) {
            Command.Exit -> payload[KEY_TYPE] = "EXIT"
            Command.Next -> payload[KEY_TYPE] = "NEXT"
            Command.Pause -> payload[KEY_TYPE] = "PAUSE"
            Command.Previous -> payload[KEY_TYPE] = "PREVIOUS"
            Command.Resume -> payload[KEY_TYPE] = "RESUME"
        }
        RequestManager.sendRequest(NAME_CONTROL_COMMAND, payload, requestCallback, true)
    }

    /**
     * 控制命令。
     */
    enum class Command {
        Exit,       // 恢复
        Next,       // 下一个
        Pause,      // 暂停
        Previous,   // 上一个
        Resume,     // 恢复
    }
}