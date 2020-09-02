package com.iflytek.home.sdk.webview

internal interface BridgeHandler {
    fun handler(data: String?, function: CallBackFunction)
}
