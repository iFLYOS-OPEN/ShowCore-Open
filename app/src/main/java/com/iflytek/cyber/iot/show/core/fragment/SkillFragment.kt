package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.webkit.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.launcher.EvsLauncher
import com.iflytek.cyber.iot.show.core.model.Skill
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.widget.StyledAlertDialog
import com.iflytek.home.sdk.IFlyHome
import com.iflytek.home.sdk.callback.IFlyHomeCallback

class SkillFragment : BaseFragment() {
    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TIMEOUT = "timeout"

        private const val SESSION_TIMEOUT = 15 * 60 * 1000

        fun newInstance(
            url: String?,
            timeout: Int
        ): SkillFragment {
            return SkillFragment().apply {
                arguments = bundleOf(
                    Pair(EXTRA_URL, url),
                    Pair(EXTRA_TIMEOUT, timeout)
                )
            }
        }
    }

    private var webView: WebView? = null
    private var loadingView: LottieAnimationView? = null

    private var timer: CountDownTimer? = null

    private var shouldShowVoice = false
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_skill, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webview)
        loadingView = view.findViewById(R.id.loading)

        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.domStorageEnabled = true
        webView?.settings?.allowFileAccess = true

        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url =
                    request?.url?.toString() ?: return super.shouldOverrideUrlLoading(view, request)
                return shouldOverrideUrlLoading(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url.isNullOrEmpty())
                    return super.shouldOverrideUrlLoading(view, url)
                return if (url == "close://now") {
                    pop()
                    true
                } else {
                    super.shouldOverrideUrlLoading(view, url)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false

                loadingView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
                }?.start()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoading = true
            }
        }

        webView?.addJavascriptInterface(SkillInterface(), "androidApi")

        arguments?.getString("url")?.let { url ->
            webView?.loadUrl(url)
        }

        val timeout = arguments?.getInt(EXTRA_TIMEOUT, -1)
        if (timeout != null && timeout > 0) {
            startCount(timeout * 1000)
        }

        val isMicrophoneEnabled =
            ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_WAKEUP_ENABLED, true)
        shouldShowVoice = isMicrophoneEnabled

        launcher?.let {
            val intent = Intent(it, FloatingService::class.java).apply {
                action = FloatingService.ACTION_HIDE_VOICE_BUTTON
            }
            it.startService(intent)
        }
    }

    private fun startCount(timeout: Int) {
        timer?.cancel()
        timer = object : CountDownTimer(timeout.toLong(), 1000) {
            override fun onFinish() {
                if (!isAdded || context == null) {
                    return
                }
                pop()
            }

            override fun onTick(millisUntilFinished: Long) {
            }
        }
        timer?.start()
    }

    fun setSemanticData(semanticData: String) {
        //webView?.loadUrl("javascript:handleSemanticData('$semanticData')")
        startCount(SESSION_TIMEOUT)
        webView?.evaluateJavascript(
            "javascript:handleSemanticData($semanticData)"
        ) {
            Log.d("WebViewFragment", "receive value: $it")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EvsLauncher.get().clearInternalAppId()
        webView?.destroy()
        if (shouldShowVoice) {
            launcher?.let {
                val intent = Intent(it, FloatingService::class.java).apply {
                    action = FloatingService.ACTION_SHOW_VOICE_BUTTON
                }
                it.startService(intent)
            }
        }
    }

    inner class SkillInterface {

        @JavascriptInterface
        fun audio_in() {
            launcher?.let {
                val intent = Intent(it, EngineService::class.java)
                intent.action = EngineService.ACTION_SEND_AUDIO_IN
                it.startService(intent)
            }
        }
    }
}