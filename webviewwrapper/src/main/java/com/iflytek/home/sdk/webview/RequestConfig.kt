package com.iflytek.home.sdk.webview

internal class RequestConfig {
    var type: String? = null
    var url: String? = null
    var sendToken: Boolean? = null
    var data: String? = null
    var function: CallBackFunction? = null

    companion object {
        const val TYPE_LIUSHENG = "liu_sheng"
        const val TYPE_URL = "url"
    }
}