package com.iflytek.cyber.evs.sdk.agent

import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.model.Constant

abstract class WakeWord {
    companion object {
        const val NAME_SET_WAKE_WORD = "${Constant.NAMESPACE_WAKE_WORD}.set_wakeword"
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
    }

    val version = "1.0"

    abstract fun setWakeWord(wakeWordId: String?, wakeWord: String?, url: String?)

    fun sendSetWakeWordSucceed(wakeWordId: String, wakeWord: String) {
        val jsonObject = JSONObject()
        jsonObject[PAYLOAD_RESULT] = RESULT_SUCCEED
        jsonObject[PAYLOAD_WAKE_WORD_ID] = wakeWordId
        jsonObject[PAYLOAD_WAKE_WORD] = wakeWord
        RequestManager.sendRequest(NAME_SET_WAKE_WORD_RESULT, jsonObject)
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
}