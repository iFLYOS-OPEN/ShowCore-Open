package com.iflytek.cyber.evs.sdk.agent

import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.RequestCallback
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.model.Constant

abstract class WakeWord {
    companion object {
        const val NAME_SET_WAKE_WORD = "${Constant.NAMESPACE_WAKE_WORD}.set_wakeword"
        const val NAME_RESET_WAKE_WORD = "${Constant.NAMESPACE_WAKE_WORD}.reset_wakeword"
        const val NAME_SET_WAKE_WORD_RESULT = "${Constant.NAMESPACE_WAKE_WORD}.set_wakeword_result"

        const val PAYLOAD_WAKE_WORD_ID = "wakeword_id"
        const val PAYLOAD_WAKE_WORD = "wakeword"
        const val PAYLOAD_URL = "url"
        const val PAYLOAD_RESULT = "result"
        const val PAYLOAD_ERROR_TYPE = "error_type"

        const val RESULT_SUCCEED = "SUCCEED"
        const val RESULT_FAILED = "FAILED"
        const val ERROR_DOWNLOAD_ERROR = "DOWNLOAD_ERROR"
        const val ERROR_CAE_ERROR = "CAE_ERROR"
        const val ERROR_USER_DENY = "USER_DENY"

        const val KEY_ENGINE = "ent"
        const val KEY_ENGINE_NAME = "name"
        const val KEY_ENGINE_VERSION = "version"
    }

    val version = "1.1"

    abstract fun setWakeWord(wakeWordId: String?, wakeWord: String?, url: String?)

    abstract fun resetWakeWord(wakeWordId: String?)

    open fun getWakeEngine(): Engine? {
        return null
    }

    fun sendSetWakeWordSucceed(wakeWordId: String, wakeWord: String?) {
        val jsonObject = JSONObject()
        jsonObject[PAYLOAD_RESULT] = RESULT_SUCCEED
        jsonObject[PAYLOAD_WAKE_WORD_ID] = wakeWordId
        jsonObject[PAYLOAD_WAKE_WORD] = wakeWord
        RequestManager.sendRequest(NAME_SET_WAKE_WORD_RESULT, jsonObject)
    }

    fun sendSetWakeWordSucceed(wakeWordId: String?, wakeWord: String?, resultCallback: RequestCallback) {
        val jsonObject = JSONObject()
        jsonObject[PAYLOAD_RESULT] = RESULT_SUCCEED
        jsonObject[PAYLOAD_WAKE_WORD_ID] = wakeWordId
        jsonObject[PAYLOAD_WAKE_WORD] = wakeWord
        RequestManager.sendRequest(NAME_SET_WAKE_WORD_RESULT, jsonObject, resultCallback)
    }

    /**
     * @param errorType one of { [ERROR_DOWNLOAD_ERROR], [ERROR_CAE_ERROR] }
     */
    fun sendSetWakeWordFailed(wakeWordId: String, wakeWord: String, errorType: String) {
        val jsonObject = JSONObject()
        jsonObject[PAYLOAD_RESULT] = RESULT_FAILED
        jsonObject[PAYLOAD_WAKE_WORD_ID] = wakeWordId
        jsonObject[PAYLOAD_WAKE_WORD] = wakeWord
        jsonObject[PAYLOAD_ERROR_TYPE] = errorType
        RequestManager.sendRequest(NAME_SET_WAKE_WORD_RESULT, jsonObject)
    }

    data class Engine(
        val name: String?,
        val version: String?
    )
}