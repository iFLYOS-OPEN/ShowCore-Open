package com.iflytek.home.sdk.callback

import android.webkit.WebChromeClient
import android.webkit.WebViewClient

/**
 * 绑定接口回调
 */
abstract class IFlyHomeCallback {
    /**
     * 请求在新页面中打开标记为 tag 的网页
     */
    abstract fun openNewPage(tag: String, params: String? = null)

    /**
     * 请求关闭页面
     */
    abstract fun closePage()

    /**
     * 请求更新顶栏颜色，以便 UI 配色统一
     */
    open fun updateHeaderColor(color: String) {}

    /**
     * 请求更新顶栏标题，以便正确显示标题
     */
    open fun updateTitle(title: String) {}

    /**
     * 回调自定义的 WebChromeClient
     */
    open fun getWebChromeClient(): WebChromeClient? = null

    /**
     * 回调自定义的 WebViewClient
     */
    open fun getWebViewClient(): WebViewClient? = null
}