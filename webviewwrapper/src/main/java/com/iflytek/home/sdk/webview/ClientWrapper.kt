package com.iflytek.home.sdk.webview

import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.KeyEvent
import android.view.View
import android.webkit.*

internal object ClientWrapper {
    fun wrapWebChromeClient(clients: Array<WebChromeClient?>): WebChromeClient {
        return object : WebChromeClient() {
            override fun onCloseWindow(window: WebView?) {
                clients.map {
                    it?.onCloseWindow(window)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onConsoleMessage(consoleMessage) ?: false)
                }
                return returnValue
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                            ?: false)
                }
                return returnValue
            }

            override fun onGeolocationPermissionsHidePrompt() {
                clients.map {
                    it?.onGeolocationPermissionsHidePrompt()
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                clients.map {
                    it?.onGeolocationPermissionsShowPrompt(origin, callback)
                }
            }

            override fun onHideCustomView() {
                clients.map {
                    it?.onHideCustomView()
                }
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onJsAlert(view, url, message, result)
                            ?: false)
                }
                return returnValue
            }

            override fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onJsBeforeUnload(view, url, message, result)
                            ?: false)
                }
                return returnValue
            }

            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onJsConfirm(view, url, message, result)
                            ?: false)
                }
                return returnValue
            }

            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onJsPrompt(view, url, message, defaultValue, result)
                            ?: false)
                }
                return returnValue
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                clients.map {
                    it?.onPermissionRequest(request)
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                clients.map {
                    it?.onPermissionRequestCanceled(request)
                }
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                clients.map {
                    it?.onProgressChanged(view, newProgress)
                }
            }

            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                clients.map {
                    it?.onReceivedIcon(view, icon)
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                clients.map {
                    it?.onReceivedTitle(view, title)
                }
            }

            override fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {
                clients.map {
                    it?.onReceivedTouchIconUrl(view, url, precomposed)
                }
            }

            override fun onRequestFocus(view: WebView?) {
                clients.map {
                    it?.onRequestFocus(view)
                }
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                clients.map {
                    it?.onShowCustomView(view, callback)
                }
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onShowFileChooser(webView, filePathCallback, fileChooserParams)
                            ?: false)
                }
                return returnValue
            }

            override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                clients.map {
                    it?.onConsoleMessage(message, lineNumber, sourceID)
                }
            }

            override fun onExceededDatabaseQuota(url: String?, databaseIdentifier: String?, quota: Long, estimatedDatabaseSize: Long, totalQuota: Long, quotaUpdater: WebStorage.QuotaUpdater?) {
                clients.map {
                    it?.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater)
                }
            }

            override fun onJsTimeout(): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onJsTimeout()
                            ?: false)
                }
                return returnValue
            }

            override fun onReachedMaxAppCacheSize(requiredStorage: Long, quota: Long, quotaUpdater: WebStorage.QuotaUpdater?) {
                clients.map {
                    it?.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
                }
            }

            override fun onShowCustomView(view: View?, requestedOrientation: Int, callback: CustomViewCallback?) {
                clients.map {
                    it?.onShowCustomView(view, requestedOrientation, callback)
                }
            }

            override fun getDefaultVideoPoster(): Bitmap? {
                clients.map { client ->
                    client?.defaultVideoPoster?.let {
                        return it
                    }
                }
                return super.getDefaultVideoPoster()
            }

            override fun getVideoLoadingProgressView(): View? {
                clients.map { client ->
                    client?.videoLoadingProgressView?.let {
                        return it
                    }
                }
                return super.getVideoLoadingProgressView()
            }

            override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
                clients.map { client ->
                    client?.getVisitedHistory(callback)?.let {
                        return it
                    }
                }
                super.getVisitedHistory(callback)
            }
        }
    }

    fun wrapWebViewClient(clients: Array<WebViewClient?>): WebViewClient {
        return object : WebViewClient() {
            override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
                try {
                    clients.map {
                        it?.onFormResubmission(view, dontResend, resend)
                    }
                } catch (ignore: Exception) {
                    // Message may be used, just ignore it.
                }
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                clients.map {
                    it?.onLoadResource(view, url)
                }
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                if (Build.VERSION.SDK_INT >= 23)
                    clients.map {
                        it?.onPageCommitVisible(view, url)
                    }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                clients.map {
                    it?.onPageFinished(view, url)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                clients.map {
                    it?.onPageStarted(view, url, favicon)
                }
            }

            override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
                clients.map {
                    it?.onReceivedClientCertRequest(view, request)
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (Build.VERSION.SDK_INT >= 23)
                    clients.map {
                        it?.onReceivedError(view, request, error)
                    }
            }

            override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
                clients.map {
                    it?.onReceivedHttpAuthRequest(view, handler, host, realm)
                }
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                if (Build.VERSION.SDK_INT >= 23)
                    clients.map {
                        it?.onReceivedHttpError(view, request, errorResponse)
                    }
            }

            override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?, args: String?) {
                clients.map {
                    it?.onReceivedLoginRequest(view, realm, account, args)
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                clients.map {
                    it?.onReceivedSslError(view, handler, error)
                }
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                if (Build.VERSION.SDK_INT < 26)
                    return super.onRenderProcessGone(view, detail)
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.onRenderProcessGone(view, detail)
                            ?: false)
                }
                return returnValue
            }

            override fun onSafeBrowsingHit(view: WebView?, request: WebResourceRequest?, threatType: Int, callback: SafeBrowsingResponse?) {
                if (Build.VERSION.SDK_INT >= 27)
                    clients.map {
                        it?.onSafeBrowsingHit(view, request, threatType, callback)
                    }
            }

            override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
                clients.map {
                    it?.onScaleChanged(view, oldScale, newScale)
                }
            }

            override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
                clients.map {
                    it?.onUnhandledKeyEvent(view, event)
                }
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                clients.map {
                    it?.onReceivedError(view, errorCode, description, failingUrl)
                }
            }

            override fun onTooManyRedirects(view: WebView?, cancelMsg: Message?, continueMsg: Message?) {
                clients.map {
                    it?.onTooManyRedirects(view, cancelMsg, continueMsg)
                }
            }

            override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.shouldOverrideKeyEvent(view, event)
                            ?: false)
                }
                return returnValue
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                clients.map { client ->
                    client?.shouldInterceptRequest(view, request)?.let { return it }
                }
                return null
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (Build.VERSION.SDK_INT < 24)
                    return super.shouldOverrideUrlLoading(view, request)
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.shouldOverrideUrlLoading(view, request)
                            ?: false)
                }
                return returnValue
            }

            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
                clients.map { client ->
                    client?.shouldInterceptRequest(view, url)?.let {
                        return it
                    }
                }
                return null
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                var returnValue = false
                clients.map {
                    returnValue = returnValue || (it?.shouldOverrideUrlLoading(view, url)
                            ?: false)
                }
                return returnValue
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                clients.map {
                    it?.doUpdateVisitedHistory(view, url, isReload)
                }
            }

        }
    }
}