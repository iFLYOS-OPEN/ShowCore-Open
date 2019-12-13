package com.iflytek.cyber.evs.sdk.agent

import androidx.annotation.CallSuper
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.model.Constant
import com.iflytek.cyber.evs.sdk.socket.RequestBuilder
import com.iflytek.cyber.evs.sdk.socket.SocketManager

/**
 * 启动器模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/launcher.html#%E5%90%AF%E5%8A%A8%E5%99%A8
 */
abstract class Launcher {
    val version
        get() = "1.0"

    companion object {
        const val NAME_START_ACTIVITY = "${Constant.NAMESPACE_LAUNCHER}.start_activity"
        const val NAME_BACK = "${Constant.NAMESPACE_LAUNCHER}.back"
        const val NAME_START_ACTIVITY_RESULT =
            "${Constant.NAMESPACE_LAUNCHER}.start_activity_result"
        const val NAME_BACK_RESULT =
            "${Constant.NAMESPACE_LAUNCHER}.back_result"

        const val PAYLOAD_PAGE = "page"
        const val PAYLOAD_RESULT = "result"
        const val PAYLOAD_FAILURE_CODE = "failure_code"
        const val PAYLOAD_FEEDBACK_TEXT = "feedback_text"

        const val PAGE_HOME = "home"
        const val PAGE_SETTINGS = "settings"
        const val PAGE_CONTENTS = "contents"
        const val PAGE_SKILLS = "skills"
        const val PAGE_ALARMS = "alarms"
        const val PAGE_MESSAGES = "messages"
        const val PAGE_NEXT = "next"
        const val PAGE_PREVIOUS = "previous"

        const val RESULT_SUCCEED = "SUCCEED"
        const val RESULT_FAILED = "FAILED"

        const val FAILURE_CODE_NOT_FOUND_PAGE = "NOT_FOUND_PAGE"
        const val FAILURE_CODE_INTERNAL_ERROR = "INTERNAL_ERROR"
    }

    /**
     * 打开跳转 Launcher 内的某个页面
     * @return 是否自定义反馈结果
     */
    abstract fun startActivity(page: String, callback: ExecuteCallback): Boolean

    abstract fun back(callback: ExecuteCallback)

    @CallSuper
    open fun sendStartActivitySucceed(page: String, feedbackText: String? = null) {
        val payload = JSONObject()
        payload[PAYLOAD_RESULT] = RESULT_SUCCEED
        payload[PAYLOAD_PAGE] = page
        feedbackText?.let {
            payload[PAYLOAD_FEEDBACK_TEXT] = feedbackText
        }
        RequestManager.sendRequest(NAME_START_ACTIVITY_RESULT, payload)
    }

    @CallSuper
    open fun sendStartActivityFailed(
        page: String,
        failureCode: String?, feedbackText: String?
    ) {

        val payload = JSONObject()
        payload[PAYLOAD_RESULT] = RESULT_FAILED
        payload[PAYLOAD_PAGE] = page
        failureCode?.let {
            payload[PAYLOAD_FAILURE_CODE] = it
        }
        feedbackText?.let {
            payload[PAYLOAD_FEEDBACK_TEXT] = it
        }
        RequestManager.sendRequest(NAME_START_ACTIVITY_RESULT, payload)
    }

    open fun sendBackSucceed(feedbackText: String? = null) {
        val payload = JSONObject()
        payload[PAYLOAD_RESULT] = RESULT_SUCCEED

        feedbackText?.let {
            payload[PAYLOAD_FEEDBACK_TEXT] = feedbackText
        }
        RequestManager.sendRequest(NAME_BACK_RESULT, payload)
    }

    open fun sendBackFailed(feedbackText: String?) {
        val payload = JSONObject()
        payload[PAYLOAD_RESULT] = RESULT_FAILED
        feedbackText?.let {
            payload[PAYLOAD_FEEDBACK_TEXT] = it
        }
        RequestManager.sendRequest(NAME_BACK_RESULT, payload)
    }

    abstract class ExecuteCallback {
        var result = ""
        var page = ""
        var failureCode: String? = null
        var feedbackText: String? = null

        abstract fun onSuccess(feedbackText: String? = null)
        abstract fun onFailed(failureCode: String?, feedbackText: String?)
    }
}