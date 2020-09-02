package com.iflytek.home.sdk

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import com.iflytek.home.sdk.callback.AuthorizeCallback
import com.iflytek.home.sdk.callback.IFlyHomeCallback
import com.iflytek.home.sdk.webview.BridgeHandler
import com.iflytek.home.sdk.webview.CallBackFunction
import com.iflytek.home.sdk.webview.RequestConfig
import com.iflytek.home.sdk.webview.WebViewWrapper
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

object IFlyHome {

    private val webViewMap = HashMap<WebView, WebViewWrapper>()
    private val webViewTagMap = HashMap<String, WebView>()

    private val tagConfigMap = HashMap<String, RequestConfig>()
    private val tagCallbackMap = HashMap<String, IFlyHomeCallback>()

    /**
     * 内容账号页面，显示与 IFLYOS 账号绑定的第三方账号
     */
    const val ACCOUNTS = "accounts"
    /**
     * 我的闹钟页面，提供绑定设备上的闹钟查看和设置功能
     */
    const val CLOCKS = "clocks"
    /**
     * 被控设备页面，显示可被操作的被控智能家居设备
     */
    const val CONTROLLED_DEVICES = "controlled-devices"
    /**
     * 语音技能相关页面，查看设备可用的技能及介绍
     */
    const val SKILLS = "skills"
    /**
     * 个性音库页面，控制可设置到设备上的发音人音库，实验性功能
     */
    const val PERSONAL_SOUNDS = "personal-sounds"
    /**
     * 指定设备自定义设备唤醒词，需指定 deviceId 参数
     */
    const val WAKEUP_WORDS = "wakeup-words"
    /**
     * 指定设备设置定时休眠，需指定 deviceId 参数
     */
    const val TIME_TO_SLEEP = "time-to-sleep"
    /**
     * 指定设备设置 TTS 发音人，需指定 deviceId 参数
     */
    const val SPEAKER = "informant"
    /**
     * 指定设备设置蓝牙开关，需指定 deviceId 参数
     */
    const val BLUETOOTH = "bluetooth"
    /**
     * 指定设备检查更新，需指定 deviceId 参数
     */
    const val CHECK_UPDATE = "check-update"
    /**
     * 红外控制页，仅适用于 iFLYOS 合作设备
     */
    const val INFRARED = "infrared"
    /**
     * 指定设备的对话页，显示当前设备发生的交互内容，需指定 deviceId 参数
     */
    const val DIALOGUE = "dialogue"

    /**
     * 留言板
     */
    const val MESSAGE = "leave-msgs"

    /**
     * 训练计划
     */
    const val TRAINING_PLAN = "training-plan"

    /**
     * 聊天设置
     */
    const val COMMUNICATION_SETTING = "modes"

    const val DEVICE_ZONE = "update-device-zone"

    /**
     * 注册 WebView
     * @param webView 将要使用的 WebView
     * @param callback SDK 网页传递的回调
     * @return 返回一个 tag，与 sdk 通过 WebView 自动加载部分 URL 相关
     */
    fun register(webView: WebView, callback: IFlyHomeCallback): String {
        return register(webView, callback, null)
    }

    /**
     * 注册 WebView
     * @param webView 将要使用的 WebView
     * @param callback SDK 网页传递的回调
     * @param tag 传入 openNewPage 回调的 tag，则立即打开 openNewPage 要打开的页面
     * @return 返回一个 webViewTag，与 sdk 通过 WebView 自动加载部分 URL 相关
     */
    fun register(webView: WebView, callback: IFlyHomeCallback, tag: String?): String {
        val wrapper = WebViewWrapper(webView, callback, object : AuthorizeCallback {
            override fun onAuthorizeUrlCall(tag: String, url: String) {
                authorize(tag, url)
            }
        })
        val webViewTag = generateTag()
        webViewTagMap[webViewTag] = webView
        webViewMap[webView] = wrapper
        wrapper.registerHandler("openNewPage", object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction) {
                val tag0 = generateTag()
                val request = RequestConfig()
                val json = JSONObject(data)
                request.type = RequestConfig.TYPE_URL
                request.url = json.optString("url")
                request.sendToken = json.optBoolean("sendToken")
                tagConfigMap[tag0] = request
                var params: JSONObject? = null
                if (json.has("noBack")) {
                    params = JSONObject()
                    params.put("noBack", json.optInt("noBack"))
                }
                callback.openNewPage(tag0, params?.toString())
            }
        })
        wrapper.registerHandler("closePage", object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction) {
                callback.closePage()
            }
        })
        wrapper.registerHandler("loginFailed", object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction) {
                wrapper.getTag()?.let {
                    // ignore
                }
            }
        })
        wrapper.registerHandler("startLiuShengTrain", object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction) {
                // ignore
            }
        })
        wrapper.registerHandler("requestPermission", object : BridgeHandler {
            override fun handler(data: String?, function: CallBackFunction) {
                if (Build.VERSION.SDK_INT >= 23 &&
                    webView.context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    webView.context.let {
                        if (it is Activity) {
                            it.requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1000)
                        }
                    }
                }
            }
        })
        if (!tag.isNullOrEmpty()) {
            val requestConfig = tagConfigMap[tag]
            if (requestConfig != null) {
                tagConfigMap.remove(tag)
                when (requestConfig.type) {
                    RequestConfig.TYPE_LIUSHENG -> {
                        // ignore
                    }
                    RequestConfig.TYPE_URL -> {
                        webView.loadUrl(requestConfig.url)
                    }
                }
            }
            tagCallbackMap[tag] = callback
            wrapper.setTag(tag)
        }
        return webViewTag
    }

    private fun generateTag(): String {
        return UUID.randomUUID().toString()
    }

    private fun authorize(tag: String, redirectUrl: String) {
        // ignore
    }

    fun unregister(webView: WebView) {
        val webViewWrapper = webViewMap[webView]
        webViewWrapper?.getTag()?.let {
            tagConfigMap.remove(it)
            tagCallbackMap.remove(it)

            webViewMap.remove(webView)
        }
    }

    /**
     * 通知 WebView 界面可见，应在 Activity 或 Fragment 的 onResume 中调用
     * @param webViewTag 注册 WebView 时返回的 webViewTag
     */
    fun resumeWebView(webViewTag: String) {
        val webView = webViewTagMap[webViewTag]
        webView?.evaluateJavascript("window.webview_bridge('webview_appear')") { }
    }

    /**
     * 通知 WebView 界面不可见，应在 Activity 或 Fragment 的 onPause 中调用
     * @param webViewTag 注册 WebView 时返回的 webViewTag
     */
    fun pauseWebView(webViewTag: String) {
        val webView = webViewTagMap[webViewTag]
        webView?.evaluateJavascript("window.webview_bridge('webview_disappear')") { }
    }
}

