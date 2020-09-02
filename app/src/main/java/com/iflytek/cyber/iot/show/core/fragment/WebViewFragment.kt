package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.webkit.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.launcher.EvsLauncher
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.iflytek.home.sdk.IFlyHome
import com.iflytek.home.sdk.callback.IFlyHomeCallback

class WebViewFragment : BaseFragment() {
    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TAG = "tag"

        private const val NAME_NOT_RESOLVED = "net::ERR_NAME_NOT_RESOLVED"
    }

    private var webView: WebView? = null
    private var tvTitle: TextView? = null
    private var ivRefresh: ImageView? = null
    private var loadingView: LottieAnimationView? = null
    private var retryView: View? = null
    private var retry: View? = null

    private var webViewTag: String? = null
    private var isLoading = false
    private var receivedError: Boolean = false

    private var backCount = 0

    private var errorCodes = intArrayOf(
        WebViewClient.ERROR_AUTHENTICATION,
        WebViewClient.ERROR_AUTHENTICATION, WebViewClient.ERROR_BAD_URL,
        WebViewClient.ERROR_CONNECT, WebViewClient.ERROR_FAILED_SSL_HANDSHAKE,
        WebViewClient.ERROR_FILE, WebViewClient.ERROR_FILE_NOT_FOUND,
        WebViewClient.ERROR_HOST_LOOKUP, WebViewClient.ERROR_IO,
        WebViewClient.ERROR_PROXY_AUTHENTICATION, WebViewClient.ERROR_UNSAFE_RESOURCE,
        WebViewClient.ERROR_REDIRECT_LOOP, WebViewClient.ERROR_TIMEOUT,
        WebViewClient.ERROR_TOO_MANY_REQUESTS, WebViewClient.ERROR_UNKNOWN,
        WebViewClient.ERROR_UNSAFE_RESOURCE, WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME,
        WebViewClient.ERROR_UNSUPPORTED_SCHEME
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_webview, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webview)
        tvTitle = view.findViewById(R.id.web_title)
        loadingView = view.findViewById(R.id.loading)
        ivRefresh = view.findViewById(R.id.refresh)
        retryView = view.findViewById(R.id.retry_view)
        retry = view.findViewById(R.id.retry)
        retry?.setOnClickListener {
            receivedError = false
            retryView?.isVisible = false
            loadingView?.isVisible = true
            loadingView?.let { animationView ->
                animationView.repeatCount = Animation.INFINITE
                animationView.playAnimation()
                animationView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            webView?.reload()
        }

        val tag = arguments?.getString(EXTRA_TAG)

        webView?.let { webView ->
            webViewTag = IFlyHome.register(webView, object : IFlyHomeCallback() {
                override fun openNewPage(tag: String, params: String?) {
                    val fragment = WebViewFragment()
                    val fragmentArguments = Bundle()
                    fragmentArguments.putString(EXTRA_TAG, tag)
                    fragment.arguments = fragmentArguments
                    start(fragment)
                }

                override fun closePage() {
                    pop()
                }

                override fun updateTitle(title: String) {
                    if (title != webView.url)
                        tvTitle?.text = title
                }

                override fun getWebChromeClient(): WebChromeClient? {
                    return object : WebChromeClient() {
                        override fun onJsConfirm(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            result: JsResult?
                        ): Boolean {
                            val fragmentManager = fragmentManager ?: return false
                            StyledAlertDialog.Builder()
                                .setMessage(message ?: "")
                                .setPositiveButton(getString(R.string.ensure),
                                    View.OnClickListener {
                                        result?.confirm()
                                    })
                                .setNegativeButton(getString(R.string.cancel),
                                    View.OnClickListener {
                                        result?.cancel()
                                    })
                                .show(fragmentManager)
                            return true
                        }
                    }
                }

                override fun getWebViewClient(): WebViewClient? {
                    return object : WebViewClient() {

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            url: String?
                        ): Boolean {
                            Log.e("webview", "url: " + url)
                            if (url?.startsWith("sinanews") == true) {
                                return true
                            }
                            return false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            Log.e("webview", "url: " + request?.url)
                            if (request?.url?.toString()?.startsWith("sinanews") == true) {
                                return true
                            }
                            return false
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)

                            isLoading = true

                            ivRefresh?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
                                loadingView?.let { animationView ->
                                    animationView.repeatCount = Animation.INFINITE
                                    animationView.playAnimation()
                                    animationView.animate()
                                        .alpha(1f)
                                        .setDuration(200)
                                        .start()
                                }
                            }?.start()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            if (receivedError) {
                                retryView?.isVisible = true
                            } else {
                                retryView?.isVisible = false
                                webView.isVisible = true
                                isLoading = false

                                loadingView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
                                    ivRefresh?.animate()?.alpha(1f)?.setDuration(200)?.start()
                                }?.start()
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (Build.VERSION.SDK_INT < 23)
                                return

                            Log.d(
                                "SDKWebView",
                                "error: " + error?.description + "  error code: " + error?.errorCode
                            )
                            if (error != null && error.description != NAME_NOT_RESOLVED) {
                                handleWebError(error.errorCode)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            Log.d("SDKWebView", "error: " + description + "  error code: " + errorCode)
                            if (!description.equals(NAME_NOT_RESOLVED)) {
                                handleWebError(errorCode)
                            }
                        }
                    }
                }
            }, tag)
        }

        webView?.addJavascriptInterface(PlatformInterface(), "plat")

        ivRefresh?.setOnClickListener {
            if (!isLoading)
                webView?.reload()
        }

        view.findViewById<View>(R.id.back)?.setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        arguments?.getString("url")?.let { url ->
            if (tag.isNullOrEmpty())
                webView?.loadUrl(url)
        }
        arguments?.getBoolean("hide_title")?.let { hideTitle ->
            tvTitle?.isVisible = !hideTitle
        }
    }

    private fun handleWebError(errorCode: Int) {
        for (code in errorCodes) {
            if (errorCode == code) {
                webView?.isVisible = false
                receivedError = true
                break
            }
        }
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()
        webViewTag?.let { webViewTag ->
            IFlyHome.pauseWebView(webViewTag)
        }
    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        webViewTag?.let { webViewTag ->
            IFlyHome.resumeWebView(webViewTag)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.run {
            IFlyHome.unregister(this)
        }
        webView?.destroy()
        EvsLauncher.get().clearInternalAppId()
    }

    class PlatformInterface {
        @JavascriptInterface
        fun getName() = "eva"
    }
}