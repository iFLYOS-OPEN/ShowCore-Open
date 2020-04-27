package com.iflytek.cyber.iot.show.core.template

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.R

class CustomTemplateView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CustomTemplateView"
    }

    private val webView: WebView = WebView(context)
    private val loadingAnimation = LottieAnimationView(context)

    var onClickBackListener: OnClickListener? = null
    var overrideUrlLoadingCallback: OverrideUrlLoadingCallback? = null

    init {
        addView(webView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val loadingSize = resources.getDimensionPixelSize(R.dimen.dp_40)
        val loadingLp = LayoutParams(loadingSize, loadingSize)
        loadingLp.gravity = Gravity.CENTER
        addView(loadingAnimation, loadingLp)
        loadingAnimation.setAnimation(R.raw.animation_loading_l)
        loadingAnimation.repeatMode = LottieDrawable.RESTART
        loadingAnimation.repeatCount = LottieDrawable.INFINITE
        loadingAnimation.post {
            loadingAnimation.playAnimation()
        }

        webView.settings.apply {
            javaScriptEnabled = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url =
                    request?.url?.toString() ?: return super.shouldOverrideUrlLoading(view, request)
                return shouldOverrideUrlLoading(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                Log.d(TAG, "WebView load $url")
                if (url.isNullOrEmpty())
                    return super.shouldOverrideUrlLoading(view, url)
                return when {
                    url == "close://now" -> {
                        onClickBackListener?.onClick(this@CustomTemplateView)
                        true
                    }
                    url.startsWith("showcore://") -> {
                        try {
                            val uri = Uri.parse(url)
                            when {
                                uri.host == "text_in" -> {
                                    if (uri.queryParameterNames.contains("query")) {
                                        val query = uri.getQueryParameter("query")
                                        val intent = Intent(context, EngineService::class.java)
                                        intent.action = EngineService.ACTION_SEND_TEXT_IN
                                        intent.putExtra(EngineService.EXTRA_QUERY, query)
                                        context.startService(intent)
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            Toast.makeText(context, "不支持的 url", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> overrideUrlLoadingCallback?.shouldOverrideUrlLoading(view, url)
                        ?: super.shouldOverrideUrlLoading(view, url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (loadingAnimation.isVisible && loadingAnimation.alpha == 1f)
                    loadingAnimation.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction {
                            loadingAnimation.isVisible = false
                        }
                        .start()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
        }
    }

    fun loadHtmlText(html: String) {
        webView.loadData(html, "text/html; charset=UTF-8",null)
    }

    class Builder(private val context: Context) {
        private var html: String? = null
        private var onClickListener: OnClickListener? = null
        private var overrideUrlLoadingCallback: OverrideUrlLoadingCallback? = null

        fun onClickBackListener(onClickListener: OnClickListener?): Builder {
            this.onClickListener = onClickListener
            return this
        }

        fun overrideUrlLoadingCallback(overrideUrlLoadingCallback: OverrideUrlLoadingCallback?): Builder {
            this.overrideUrlLoadingCallback = overrideUrlLoadingCallback
            return this
        }

        fun setHtml(html: String): Builder {
            this.html = html
            return this
        }

        fun build(): CustomTemplateView {
            val view = CustomTemplateView(context)
            html?.let {
                view.loadHtmlText(it)
            }
            view.onClickBackListener = onClickListener
            view.overrideUrlLoadingCallback = overrideUrlLoadingCallback
            return view
        }
    }

    fun destroy() {
        webView.destroy()
    }

    interface OverrideUrlLoadingCallback {
        fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean
    }
}