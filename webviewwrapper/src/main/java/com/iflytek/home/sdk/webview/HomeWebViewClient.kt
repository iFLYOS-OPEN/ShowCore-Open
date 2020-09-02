package com.iflytek.home.sdk.webview

import android.net.http.SslError
import android.os.Build
import android.util.Log
import android.webkit.*
import com.iflytek.home.sdk.callback.AuthorizeCallback
import java.io.UnsupportedEncodingException
import java.net.URLDecoder


internal class HomeWebViewClient(private val webViewWrapper: WebViewWrapper, private val authorizeCallback: AuthorizeCallback) : WebViewClient() {
    var tag: String? = null

    companion object {
        private const val toLoadJs = "WebViewJavascriptBridge.js"
    }

    @Suppress("OverridingDeprecatedMember")
    override fun shouldOverrideUrlLoading(view: WebView, originUrl: String): Boolean {
        var url = originUrl
        try {
            url = URLDecoder.decode(url, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        when {
            url.startsWith(BridgeUtil.YY_RETURN_DATA) -> { // 如果是返回数据
                webViewWrapper.handlerReturnData(url)
                return true
            }
            url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA) -> { //
                webViewWrapper.flushMessageQueue()
                return true
            }
            url.startsWith("iflyos://authorize") -> {
                tag?.let {
                    authorizeCallback.onAuthorizeUrlCall(it, url)
                }
                return true
            }
            url.startsWith("close://") -> {
                return true
            }
            else -> @Suppress("DEPRECATION")
            return super.shouldOverrideUrlLoading(view, url)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        handler?.proceed()
    }

    // 增加 shouldOverrideUrlLoading 在 api >= 24 时
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var url = request.url.toString()
            try {
                url = URLDecoder.decode(url, "UTF-8")
            } catch (ex: UnsupportedEncodingException) {
                ex.printStackTrace()
            }

            return when {
                url.startsWith(BridgeUtil.YY_RETURN_DATA) -> { // 如果是返回数据
                    webViewWrapper.handlerReturnData(url)
                    true
                }
                url.startsWith(BridgeUtil.YY_OVERRIDE_SCHEMA) -> { //
                    webViewWrapper.flushMessageQueue()
                    true
                }
                url.startsWith("iflyos://authorize") -> {
                    tag?.let {
                        authorizeCallback.onAuthorizeUrlCall(it, url)
                    }
                    return true
                }
                url.startsWith("close://") -> {
                    return true
                }
                else -> super.shouldOverrideUrlLoading(view, request)
            }
        } else {
            return super.shouldOverrideUrlLoading(view, request)
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        BridgeUtil.webViewLoadLocalJs(view, toLoadJs)

        val startupMessages = webViewWrapper.getStartupMessage()
        startupMessages?.let {
            for (m in startupMessages) {
                webViewWrapper.dispatchMessage(m)
            }
        }
        webViewWrapper.setStartupMessage(null)

        if (view.title != view.url)
            webViewWrapper.onTitleUpdated(view.title)
        webViewWrapper.evaluateJavascript(
                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                ValueCallback { html ->
                    try {
                        val word = "\\u003Cmeta name=\\\"head-color\\\""
                        if (html.contains(word)) {
                            var index = html.indexOf(word)
                            if (index >= 0) {
                                index += word.length
                                val first = html.substring(index)
                                val contentWord = "content=\\\""
                                var contentIndex = first.indexOf(contentWord)
                                if (contentIndex >= 0) {
                                    contentIndex += contentWord.length
                                    val second = first.substring(contentIndex)
                                    val valueWord = "\\\""
                                    val valueIndex = second.indexOf(valueWord)
                                    if (valueIndex >= 0) {
                                        val third = second.substring(0, valueIndex)
                                        webViewWrapper.onHeaderColorUpdated(third)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
    }
}