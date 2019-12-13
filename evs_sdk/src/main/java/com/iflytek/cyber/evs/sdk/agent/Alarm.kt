package com.iflytek.cyber.evs.sdk.agent

import androidx.annotation.CallSuper
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.annotation.JSONField
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.focus.AudioFocusManager
import com.iflytek.cyber.evs.sdk.model.Constant

/**
 * 本地闹铃模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/alarm.html#%E6%9C%AC%E5%9C%B0%E9%97%B9%E9%92%9F
 */
abstract class Alarm {
    val version = "1.1"

    companion object {
        const val NAME_SET_ALARM = "${Constant.NAMESPACE_ALARM}.set_alarm"
        const val NAME_DELETE_ALARM = "${Constant.NAMESPACE_ALARM}.delete_alarm"
        const val NAME_STATE_SYNC = "${Constant.NAMESPACE_ALARM}.state_sync"

        const val KEY_ALARM_ID = "alarm_id"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_URL = "url"
        const val KEY_ACTIVE = "active"
        const val KEY_LOCAL = "local"
        const val KEY_TYPE = "type"

        /**
         * 如果不需要闹钟能力，override 返回此函数即可。
         */
        fun disable(): Alarm {
            return object : Alarm() {
                override fun getLocalAlarms(): List<Item> {
                    return emptyList()
                }

                override fun getActiveAlarmId(): String? {
                    return null
                }

                override fun setAlarm(alarm: Item) {

                }

                override fun deleteAlarm(alarmId: String) {

                }

                override fun stop() {

                }

                override fun destroy() {

                }

                override fun isDisabled(): Boolean {
                    return true
                }
            }
        }
    }

    private var listeners = HashSet<AlarmStateChangedListener>()

    private var alarmUpdateListeners = HashSet<OnAlarmUpdatedListener>()

    internal var onAlarmUpdatedListener = object : OnAlarmUpdatedListener {
        override fun onAlarmUpdated() {
            alarmUpdateListeners.map {
                try {
                    it.onAlarmUpdated()
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    /**
     * 获取本地闹钟。
     * @return 闹钟列表
     */
    abstract fun getLocalAlarms(): List<Item>

    /**
     * 获取活动（正在响铃）闹钟的id。
     * @return 闹钟id，为null则无活动闹钟
     */
    abstract fun getActiveAlarmId(): String?

    /**
     * 设置闹钟。
     * @param alarm 闹钟对象
     */
    @CallSuper
    open fun setAlarm(alarm: Item) {
        onAlarmUpdated()
    }

    /**
     * 删除闹钟。
     * @param alarmId 闹钟id
     */
    @CallSuper
    open fun deleteAlarm(alarmId: String) {
        onAlarmUpdated()
    }

    /**
     * 停止闹钟。
     */
    abstract fun stop()

    open fun isDisabled(): Boolean {
        return false
    }

    /**
     * 销毁模块。
     */
    abstract fun destroy()

    @Suppress("unused")
    fun addListener(listener: AlarmStateChangedListener) {
        listeners.add(listener)
    }

    @Suppress("unused")
    fun removeListener(listener: AlarmStateChangedListener) {
        listeners.remove(listener)
    }

    fun addOnAlarmUpdatedListener(onAlarmUpdatedListener: OnAlarmUpdatedListener) {
        alarmUpdateListeners.add(onAlarmUpdatedListener)
    }

    fun removeOnAlarmUpdatedListener(onAlarmUpdatedListener: OnAlarmUpdatedListener) {
        alarmUpdateListeners.remove(onAlarmUpdatedListener)
    }

    fun onAlarmStarted(alarmId: String) {
        val payload = JSONObject()
        payload[KEY_TYPE] = "STARTED"
        payload[KEY_ALARM_ID] = alarmId
        RequestManager.sendRequest(NAME_STATE_SYNC, payload)

        AudioFocusManager.requestActive(
            AudioFocusManager.CHANNEL_ALARM,
            AudioFocusManager.TYPE_ALARM
        )

        onAlarmStateChanged(alarmId, AlarmState.Started)
    }

    fun onAlarmStopped(alarmId: String) {
        val payload = JSONObject()
        payload[KEY_TYPE] = "STOPPED"
        payload[KEY_ALARM_ID] = alarmId
        RequestManager.sendRequest(NAME_STATE_SYNC, payload)

        AudioFocusManager.requestAbandon(
            AudioFocusManager.CHANNEL_ALARM,
            AudioFocusManager.TYPE_ALARM
        )

        onAlarmStateChanged(alarmId, AlarmState.Stopped)
    }

    fun onAlarmUpdated() {
        onAlarmUpdatedListener?.onAlarmUpdated()
    }

    open fun onAlarmStateChanged(alarmId: String, state: AlarmState) {
        listeners.map {
            try {
                it.onAlarmStateChanged(alarmId, state)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 闹钟实体类。
     * @param alarmId 闹铃id
     * @param timestamp 响铃时间（从1970-01-01 00:00:00到现在的时间），单位：秒
     * @param url 待播放的铃声url
     */
    data class Item(
        @JSONField(name = KEY_ALARM_ID) val alarmId: String,
        val timestamp: Long,
        val url: String
    )

    interface AlarmStateChangedListener {
        fun onAlarmStateChanged(alarmId: String, state: AlarmState)
    }

    enum class AlarmState {
        Started,
        Stopped,
    }

    interface OnAlarmUpdatedListener {
        /**
         * set alarm or delete alarm, notify user
         */
        fun onAlarmUpdated()
    }
}