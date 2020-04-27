package com.iflytek.cyber.iot.show.core.impl.interceptor

import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.agent.Interceptor

class EvsInterceptor private constructor() : Interceptor() {
    companion object {
        private var interceptor: EvsInterceptor? = null

        fun get(): EvsInterceptor {
            interceptor?.let {
                return it
            }
            val newInterceptor = EvsInterceptor()
            interceptor = newInterceptor
            return newInterceptor
        }
    }

    private var transferSemanticCallback: ((payload: JSONObject) -> Unit)? = null
    private var customCallback: ((payload: JSONObject) -> Unit)? = null

    fun transferSemantic(callback: (payload: JSONObject) -> Unit): EvsInterceptor {
        transferSemanticCallback = callback
        return this
    }

    fun custom(callback: (payload: JSONObject) -> Unit): EvsInterceptor {
        customCallback = callback
        return this
    }

    override fun onResponse(name: String, payload: JSONObject) {
        when (name) {
            NAME_AIUI -> {
            }
            NAME_CUSTOM -> {
                customCallback?.invoke(payload)
            }
            NAME_TRANSFER_SEMANTIC -> {
                transferSemanticCallback?.invoke(payload)
            }
        }
    }
}