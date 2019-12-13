package com.iflytek.cyber.evs.sdk.agent

import android.content.Context
import android.os.Handler
import android.os.Message
import androidx.annotation.CallSuper
import androidx.annotation.StringDef
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.EvsError
import com.iflytek.cyber.evs.sdk.EvsService
import com.iflytek.cyber.evs.sdk.RequestCallback
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.evs.sdk.model.Constant
import com.iflytek.cyber.evs.sdk.socket.RequestBuilder
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.evs.sdk.socket.SocketManager
import java.util.concurrent.TimeUnit

/**
 * 系统模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/system.html#%E7%B3%BB%E7%BB%9F%E7%9B%B8%E5%85%B3
 */
abstract class System {
    val version = "1.2"

    companion object {
        const val NAME_PING = "${Constant.NAMESPACE_SYSTEM}.ping"
        const val NAME_FACTORY_RESET = "${Constant.NAMESPACE_SYSTEM}.factory_reset"
        const val NAME_ERROR = "${Constant.NAMESPACE_SYSTEM}.error"
        const val NAME_EXCEPTION = "${Constant.NAMESPACE_SYSTEM}.exception"
        const val NAME_STATE_SYNC = "${Constant.NAMESPACE_SYSTEM}.state_sync"
        const val NAME_REVOKE_AUTHORIZATION = "${Constant.NAMESPACE_SYSTEM}.revoke_authorization"

        const val NAME_CHECK_SOFTWARE_UPDATE = "${Constant.NAMESPACE_SYSTEM}.check_software_update"
        const val NAME_UPDATE_SOFTWARE = "${Constant.NAMESPACE_SYSTEM}.update_software"
        const val NAME_POWER_OFF = "${Constant.NAMESPACE_SYSTEM}.power_off"
        const val NAME_UPDATE_DEVICE_MODES = "${Constant.NAMESPACE_SYSTEM}.update_device_modes"
        const val NAME_CHECK_SOFTWARE_UPDATE_RESULT =
            "${Constant.NAMESPACE_SYSTEM}.check_software_update_result"
        const val NAME_SOFTWARE_UPDATE_STATE_SYNC =
            "${Constant.NAMESPACE_SYSTEM}.software_update_state_sync"

        const val KEY_SOFTWARE_UPDATER = "software_updater"
        const val KEY_POWER_CONTROLLER = "power_controller"
        const val KEY_DEVICE_MODES = "device_modes"

        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_TYPE = "type"
        const val KEY_CODE = "code"
        const val KEY_MESSAGE = "message"

        const val PAYLOAD_CODE = "code"
        const val PAYLOAD_STATE = "state"
        const val PAYLOAD_RESULT = "result"
        const val PAYLOAD_NEED_UPDATE = "need_update"
        const val PAYLOAD_VERSION_NAME = "version_name"
        const val PAYLOAD_UPDATE_DESCRIPTION = "update_description"
        const val PAYLOAD_ERROR_TYPE = "error_type"
        const val PAYLOAD_ERROR_MESSAGE = "error_message"
        const val PAYLOAD_KID = "kid"
        const val PAYLOAD_TIMESTAMP = "timestamp"

        const val RESULT_SUCCEED = "SUCCEED"
        const val RESULT_FAILED = "FAILED"
        const val STATE_FINISHED = "FINISHED"
        const val STATE_FAILED = "FAILED"
        const val STATE_STARTED = "STARTED"

        const val ERROR_TYPE_CHECK_ERROR = "CHECK_ERROR"
        const val ERROR_TYPE_DOWNLOAD_ERROR = "DOWNLOAD_ERROR"
        const val ERROR_TYPE_INSTALL_ERROR = "INSTALL_ERROR"
    }

    @StringDef(ERROR_TYPE_CHECK_ERROR, ERROR_TYPE_DOWNLOAD_ERROR, ERROR_TYPE_INSTALL_ERROR)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ErrorType

    private val countStateTimeHandler = CountStateTimeHandler(this)

    @CallSuper
    open fun sendStateSync() {
        RequestManager.sendRequest(NAME_STATE_SYNC, JSONObject(), object : RequestCallback {
            override fun onResult(result: Result) {
                if (result.isSuccessful)
                    countStateTimeHandler.postNextStateSync()
            }
        })
    }

    abstract fun onDeviceModeChanged(kid: Boolean)

    /**
     * 服务端请求设备执行检查更新
     * 检查结果通过 [sendCheckSoftwareUpdateSucceed] 和 [sendCheckSoftwareUpdateFailed]
     */
    abstract fun checkSoftWareUpdate()

    abstract fun updateSoftware()

    open fun onFactoryReset() {
    }

    fun sendCheckSoftwareUpdateSucceed(
        needUpdate: Boolean,
        versionName: String? = null,
        updateDescription: String? = null
    ) {
        val payload = JSONObject()
        payload[PAYLOAD_RESULT] = RESULT_SUCCEED
        payload[PAYLOAD_NEED_UPDATE] = needUpdate
        versionName?.let {
            payload[PAYLOAD_VERSION_NAME] = it
        }
        updateDescription?.let {
            payload[PAYLOAD_UPDATE_DESCRIPTION] = it
        }
        RequestManager.sendRequest(NAME_CHECK_SOFTWARE_UPDATE_RESULT, payload)
    }

    fun sendCheckSoftwareUpdateFailed() {
        val payload = JSONObject()
        payload[PAYLOAD_RESULT] = RESULT_FAILED
        RequestManager.sendRequest(NAME_CHECK_SOFTWARE_UPDATE_RESULT, payload)
    }

    fun sendUpdateSoftwareStarted(versionName: String, updateDescription: String?) {
        val payload = JSONObject()
        payload[PAYLOAD_STATE] = STATE_STARTED
        payload[PAYLOAD_VERSION_NAME] = versionName
        updateDescription?.let {
            payload[PAYLOAD_UPDATE_DESCRIPTION] = it
        }
        RequestManager.sendRequest(NAME_SOFTWARE_UPDATE_STATE_SYNC, payload)
    }

    fun sendUpdateSoftwareFinished(versionName: String, updateDescription: String?) {
        val payload = JSONObject()
        payload[PAYLOAD_STATE] = STATE_FINISHED
        payload[PAYLOAD_VERSION_NAME] = versionName
        updateDescription?.let {
            payload[PAYLOAD_UPDATE_DESCRIPTION] = it
        }
        RequestManager.sendRequest(NAME_SOFTWARE_UPDATE_STATE_SYNC, payload)
    }

    fun sendUpdateSoftwareFailed(@ErrorType type: String, message: String? = null) {
        val payload = JSONObject()
        payload[PAYLOAD_STATE] = STATE_FAILED
        payload[PAYLOAD_ERROR_TYPE] = type
        message?.let {
            payload[PAYLOAD_ERROR_MESSAGE] = it
        }
        RequestManager.sendRequest(NAME_SOFTWARE_UPDATE_STATE_SYNC, payload)
    }

    var hasSoftwareUpdater = false
    var hasPowerController = false
    var hasDeviceModes = false

    /**
     * 收到云端的ping消息。
     */
    abstract fun onPing(timestamp: Long)

    /**
     * 收到云端返回的错误。
     */
    @CallSuper
    open fun onError(payload: JSONObject) {
        payload[PAYLOAD_CODE]?.let { code ->
            if (code !is Int) {
                return
            }
            when (code) {
                EvsError.Code.ERROR_AUTH_FAILED -> {
                    SocketManager.onConnectFailed(EvsError.AuthorizationExpiredException())
                }
            }
        }
    }

    /**
     * 收到云端关机消息。
     */
    abstract fun onPowerOff(payload: JSONObject)

    /**
     * 更新设备模式。
     */
    abstract fun onUpdateDeviceModes(payload: JSONObject)

    /**
     * 云端撤回授权。
     */
    @CallSuper
    open fun revokeAuth(context: Context?) {
        context ?: return

        AuthDelegate.removeAuthResponseFromPref(context)

        SocketManager.disconnect()
    }

    @CallSuper
    open fun sendException(type: String, code: String, message: String) {
        val payload = JSONObject()
        payload[KEY_TYPE] = type
        payload[KEY_CODE] = code
        payload[KEY_MESSAGE] = message

        RequestManager.sendRequest(NAME_EXCEPTION, payload)
    }

    private class CountStateTimeHandler(private val system: System) : Handler() {
        private val duration = TimeUnit.MINUTES.toMillis(15)

        private var time = java.lang.System.currentTimeMillis()

        fun postNextStateSync() {
            val newTime = java.lang.System.currentTimeMillis()
            val msg = Message.obtain()
            msg.what = 1
            msg.obj = newTime
            sendMessageDelayed(msg, duration)

            time = newTime
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            when (msg?.what) {
                1 -> {
                    val msgTime = msg.obj as? Long
                    if (msgTime == time) {
                        system.sendStateSync()
                    }
                }
            }
        }
    }
}