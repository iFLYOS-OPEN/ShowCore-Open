package com.iflytek.cyber.evs.sdk.agent

import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.model.Constant

/**
 * 拦截器模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/interceptor.html#%E8%87%AA%E5%AE%9A%E4%B9%89%E6%8B%A6%E6%88%AA%E5%99%A8
 */
abstract class Interceptor {
    val version = "1.0"
    var contextJson = JSONObject()
        private set

    companion object {
        const val NAME_CUSTOM = "${Constant.NAMESPACE_INTERCEPTOR}.custom"
        const val NAME_AIUI = "${Constant.NAMESPACE_INTERCEPTOR}.aiui"
    }

    /**
     * response回调。
     */
    abstract fun onResponse(name: String, payload: JSONObject)
}