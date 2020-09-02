package com.iflytek.home.sdk.webview

import android.annotation.SuppressLint
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import com.iflytek.home.sdk.BuildConfig
import com.iflytek.home.sdk.callback.AuthorizeCallback
import com.iflytek.home.sdk.callback.IFlyHomeCallback
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set


@SuppressLint("SetJavaScriptEnabled")
internal class WebViewWrapper(
    val webView: WebView,
    private val callback: IFlyHomeCallback,
    authorizeCallback: AuthorizeCallback
) {

    companion object {
        private const val URL_MAX_CHARACTER_NUM = 2097152
    }

    private var responseCallbacks: MutableMap<String, CallBackFunction> = HashMap()
    private var startupMessages: MutableList<Message>? = ArrayList()
    private var messageHandlers: MutableMap<String, BridgeHandler> = HashMap()
    private var defaultHandler: BridgeHandler = DefaultHandler()
    private val webViewClient = HomeWebViewClient(this, authorizeCallback)

    var isPreventShowTitle = false

    private var tag: String? = null

    init {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.textZoom = 100
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView.settings.userAgentString =
            "${WebSettings.getDefaultUserAgent(webView.context)} iFLYOS-sdk-Android/" +
                    BuildConfig.VERSION_NAME
        tag.let {
            webViewClient.tag = it
        }
        webView.webViewClient =
            ClientWrapper.wrapWebViewClient(arrayOf(webViewClient, callback.getWebViewClient()))
        webView.webChromeClient =
            ClientWrapper.wrapWebChromeClient(arrayOf(object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (title != view?.url)
                        onTitleUpdated(title ?: "")
                }
            }, callback.getWebChromeClient()))
        webView.addJavascriptInterface(DefaultJavascriptInterface(), "iflytek")
    }

    fun getTag() = tag

    fun setTag(tag: String) {
        this.tag = tag
        webViewClient.tag = tag
    }

    /**
     *
     * @param handler
     * default handler,handle messages send by js without assigned handler name,
     * if js message has handler name, it will be handled by named handlers registered by native
     */
    fun setDefaultHandler(handler: BridgeHandler) {
        this.defaultHandler = handler
    }

    private var uniqueId = 0

    fun callHandler(
        handlerName: String,
        data: String? = null,
        responseCallback: CallBackFunction? = null
    ) {
        val m = Message()
        if (!TextUtils.isEmpty(data)) {
            m.data = data
        }
        if (responseCallback != null) {
            val callbackStr = String.format(
                BridgeUtil.CALLBACK_ID_FORMAT,
                "${++uniqueId}${BridgeUtil.UNDERLINE_STR}${SystemClock.currentThreadTimeMillis()}"
            )
            responseCallbacks[callbackStr] = responseCallback
            m.callbackId = callbackStr
        }
        if (!TextUtils.isEmpty(handlerName)) {
            m.handlerName = handlerName
        }
        queueMessage(m)
    }

    fun flushMessageQueue() {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            webView.loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA)
            val callback = object : CallBackFunction {
                override fun onCallBack(data: String) {
                    val list: MutableList<Message>?
                    try {
                        list = Message.toArrayList(data).toMutableList()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return
                    }

                    if (list.isEmpty()) {
                        return
                    }
                    for (i in list.indices) {
                        val m = list[i]
                        val responseId = m.responseId
                        // 是否是response  CallBackFunction
                        if (!TextUtils.isEmpty(responseId)) {
                            val function = responseCallbacks[responseId]
                            val responseData = m.responseData ?: ""
                            function?.onCallBack(responseData)
                            responseCallbacks.remove(responseId)
                        } else {
                            var responseFunction: CallBackFunction?
                            // if had callbackId 如果有回调Id
                            val callbackId = m.callbackId
                            if (!TextUtils.isEmpty(callbackId)) {
                                responseFunction = object : CallBackFunction {
                                    override fun onCallBack(data: String) {
                                        val responseMsg = Message()
                                        responseMsg.responseId = callbackId
                                        responseMsg.responseData = data
                                        queueMessage(responseMsg)
                                    }
                                }
                            } else {
                                responseFunction = object : CallBackFunction {
                                    override fun onCallBack(data: String) {
                                    }
                                }
                            }
                            // BridgeHandler执行
                            val handler = if (!TextUtils.isEmpty(m.handlerName)) {
                                messageHandlers[m.handlerName]
                            } else {
                                defaultHandler
                            }
                            handler?.handler(m.data, responseFunction)
                        }
                    }
                }

            }
            // deserializeMessage 反序列化消息
            responseCallbacks[BridgeUtil.parseFunctionName(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA)] =
                callback
        }
    }

    fun handlerReturnData(url: String) {
        val functionName = BridgeUtil.getFunctionFromReturnUrl(url)
        val f = responseCallbacks[functionName]
        val data = BridgeUtil.getDataFromReturnUrl(url) ?: ""
        if (f != null) {
            f.onCallBack(data)
            responseCallbacks.remove(functionName)
            return
        }
    }

    /**
     * messageList 不为空则添加到消息集合否则分发消息
     * @param m Message
     */
    private fun queueMessage(m: Message) {
        if (startupMessages != null) {
            startupMessages?.add(m)
        } else {
            dispatchMessage(m)
        }
    }

    fun getStartupMessage(): List<Message>? {
        return startupMessages
    }

    fun dispatchMessage(message: Message) {
        var messageJson = message.toJson() ?: ""
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replace("(\\\\)([^utrn])".toRegex(), "\\\\\\\\$1$2")
        messageJson = messageJson.replace("(?<=[^\\\\])(\")".toRegex(), "\\\\\"")
        messageJson = messageJson.replace("(?<=[^\\\\])(\')".toRegex(), "\\\\\'")
        messageJson = messageJson.replace("%7B", URLEncoder.encode("%7B"))
        messageJson = messageJson.replace("%7D", URLEncoder.encode("%7D"))
        messageJson = messageJson.replace("%22", URLEncoder.encode("%22"))
        val javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson)
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            //webView.loadUrl(javascriptCommand)
            if (javascriptCommand.length >= URL_MAX_CHARACTER_NUM) {
                webView.evaluateJavascript(javascriptCommand, null)
            } else {
                webView.loadUrl(javascriptCommand)
            }
        } else {
            throw IllegalStateException("Not in Android main thread.")
        }
    }

    fun setStartupMessage(startupMessages: MutableList<Message>?) {
        this.startupMessages = startupMessages
    }


    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     * @param handlerName handlerName
     * @param handler BridgeHandler
     */
    fun registerHandler(handlerName: String, handler: BridgeHandler?) {
        if (handler != null) {
            // 添加至 Map<String, BridgeHandler>
            messageHandlers[handlerName] = handler
        }
    }

    /**
     * unregister handler
     *
     * @param handlerName
     */
    fun unregisterHandler(handlerName: String?) {
        if (handlerName != null) {
            messageHandlers.remove(handlerName)
        }
    }

    fun onTitleUpdated(title: String) {
        if (!isPreventShowTitle)
            callback.updateTitle(title)
    }

    fun onHeaderColorUpdated(color: String) {
        callback.updateHeaderColor(color)
    }

    fun evaluateJavascript(script: String, resultCallback: ValueCallback<String>) {
        webView.evaluateJavascript(script, resultCallback)
    }
}