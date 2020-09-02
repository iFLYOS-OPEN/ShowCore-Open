package com.iflytek.home.sdk.webview


internal interface WebViewJavascriptBridge {
    fun send(data: String)
    fun send(data: String, responseCallback: CallBackFunction)
}
