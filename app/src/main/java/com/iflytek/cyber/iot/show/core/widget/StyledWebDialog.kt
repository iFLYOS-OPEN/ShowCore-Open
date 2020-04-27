package com.iflytek.cyber.iot.show.core.widget

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.ScreenUtils
import com.iflytek.cyber.iot.show.core.utils.dp2Px
import com.iflytek.home.sdk.IFlyHome
import com.iflytek.home.sdk.callback.IFlyHomeCallback

class StyledWebDialog : DialogFragment() {

    private var webView: WebView? = null
    private var loading: LottieAnimationView? = null
    private var errorContainer: LinearLayout? = null
    private var retry: Button? = null

    private var onWebPageCallback: OnWebPageCallback? = null

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

    private var receivedError = false

    fun setOnWebPageCallback(onWebPageCallback: OnWebPageCallback) {
        this.onWebPageCallback = onWebPageCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return inflater.inflate(R.layout.layout_styled_web_dialog, container, false)
    }

    override fun onStart() {
        super.onStart()

        val height = ScreenUtils.getHeight(context) - 64.dp2Px()

        val width = context?.resources?.getDimensionPixelSize(R.dimen.dp_250) ?: return

        dialog?.let { dialog ->
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val close = view.findViewById<ImageView>(R.id.close)
        close.setOnClickListener {
            dismissAllowingStateLoss()
        }

        webView = view.findViewById(R.id.web_view)
        loading = view.findViewById(R.id.loading)
        errorContainer = view.findViewById(R.id.error_container)
        retry = view.findViewById(R.id.retry)
        retry?.setOnClickListener {
            receivedError = false
            errorContainer?.isVisible = false
            loading?.isVisible = true
            loading?.playAnimation()
            webView?.reload()
        }

        loading?.isVisible = true
        loading?.playAnimation()

        webView?.let {
            IFlyHome.register(it, object : IFlyHomeCallback() {
                override fun openNewPage(tag: String, params: String?) {
                    onWebPageCallback?.openNewPage(tag, params)
                }

                override fun closePage() {
                    onWebPageCallback?.onClosePage()
                }

                override fun updateTitle(title: String) {
                }

                override fun getWebViewClient(): WebViewClient? {
                    return object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            loading?.isVisible = true
                            loading?.playAnimation()
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            loading?.isVisible = false
                            loading?.pauseAnimation()
                            if (!receivedError) {
                                webView?.isVisible = true
                            } else {
                                errorContainer?.isVisible = true
                                webView?.isVisible = false
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            receivedError = true
                            loading?.isVisible = false
                            loading?.pauseAnimation()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                handleWebError(error?.errorCode)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            receivedError = true
                            loading?.isVisible = false
                            loading?.pauseAnimation()
                            handleWebError(errorCode)
                        }
                    }
                }
            }, null)
        }

        arguments?.getString("url")?.let { url ->
            webView?.loadUrl(url)
        }
    }

    private fun handleWebError(errorCode: Int?) {
        for (code in errorCodes) {
            if (errorCode == code) {
                Log.e("WebDialog", "code: " + code)
                webView?.isVisible = false
                errorContainer?.isVisible = true
                return
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.run {
            IFlyHome.unregister(this)
        }
        webView?.destroy()
    }

    interface OnWebPageCallback {

        fun onClosePage()

        fun openNewPage(tag: String, params: String?)
    }
}