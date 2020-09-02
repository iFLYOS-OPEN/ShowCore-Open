package com.iflytek.home.sdk.webview

internal class DefaultHandler : BridgeHandler {
    companion object {
        private const val TAG = "DefaultHandler"
    }

    override fun handler(data: String?, function: CallBackFunction) {
        function.onCallBack("DefaultHandler response data")
    }
}
