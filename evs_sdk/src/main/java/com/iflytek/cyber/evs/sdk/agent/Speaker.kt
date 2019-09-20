package com.iflytek.cyber.evs.sdk.agent

/**
 * 扬声器控制模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/speaker.html#%E6%89%AC%E5%A3%B0%E5%99%A8%E6%8E%A7%E5%88%B6
 */
abstract class Speaker {
    val version = "1.0"

    companion object {
        const val NAME_SET_VOLUME = "speaker.set_volume"

        const val KEY_TYPE = "type"
        const val KEY_VOLUME = "volume"
    }

    /**
     * 返回音量类型，当前只支持百分比。
     */
    fun getType() = "percent"

    /**
     * 获取当前音量。
     * @return 音量值（0-100）
     */
    abstract fun getCurrentVolume(): Int

    /**
     * 设置音量。
     * @param volume 音量值（0-100）
     * @return 是否设置成功
     */
    abstract fun setVolume(volume: Int): Boolean
}