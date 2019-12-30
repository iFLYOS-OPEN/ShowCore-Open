package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R

class WebViewFragment : BaseFragment() {
    private var webView: WebView? = null
    private var tvTitle: TextView? = null
    private var ivRefresh: ImageView? = null
    private var loadingView: LottieAnimationView? = null

    private var isLoading = false

    private var backCount = 0

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

        webView?.settings?.let { settings ->
            settings.javaScriptEnabled = true
        }
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)

                tvTitle?.text = title
            }
        }
        webView?.webViewClient = object : WebViewClient() {
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

                isLoading = false

                loadingView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
                    ivRefresh?.animate()?.alpha(1f)?.setDuration(200)?.start()
                }?.start()
            }
        }

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
            webView?.loadUrl(url)
        }
        arguments?.getBoolean("hide_title")?.let { hideTitle ->
            tvTitle?.isVisible = !hideTitle
        }
    }
}