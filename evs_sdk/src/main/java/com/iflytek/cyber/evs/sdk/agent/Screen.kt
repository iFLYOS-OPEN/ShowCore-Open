package com.iflytek.cyber.evs.sdk.agent

import com.iflytek.cyber.evs.sdk.model.Constant

/**
 * 屏幕控制模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/screen.html#%E5%B1%8F%E5%B9%95%E6%8E%A7%E5%88%B6
 */
abstract class Screen {
    val version = "1.2"

    companion object {
        const val NAME_SET_STATE = "${Constant.NAMESPACE_SCREEN}.set_state"
        const val NAME_SET_BRIGHTNESS = "${Constant.NAMESPACE_SCREEN}.set_brightness"

        const val KEY_TYPE = "type"
        const val KEY_STATE = "state"
        const val KEY_BRIGHTNESS = "brightness"

        const val STATE_ON = "ON"
        const val STATE_OFF = "OFF"

        const val TYPE_PERCENT = "percent"
    }

    /**
     * 获取亮度值类型，暂时只支持百分比。
     */
    open fun getBrightnessType() = TYPE_PERCENT

    /**
     * 设置屏幕状态。
     * @param state 状态：ON（打开）、OFF（熄屏）
     * @return 是否设置成功
     */
    abstract fun setState(state: String): Boolean

    /**
     * 获取屏幕状态。
     * @return 状态：ON（打开）、OFF（熄屏）
     */
    abstract fun getState(): String

    /**
     * 设置屏幕亮度。
     * @param brightness 亮度值（0-100）
     * @return 是否设置成功
     */
    abstract fun setBrightness(brightness: Long): Boolean

    /**
     * 获取屏幕亮度。
     * @return 亮度值（0-100）
     */
    abstract fun getBrightness(): Long
}