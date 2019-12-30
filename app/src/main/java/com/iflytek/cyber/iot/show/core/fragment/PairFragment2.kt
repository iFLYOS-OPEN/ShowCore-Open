package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.PowerManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.evs.sdk.model.AuthResponse
import com.iflytek.cyber.evs.sdk.model.DeviceCodeResponse
import com.iflytek.cyber.iot.show.core.*
import com.iflytek.cyber.iot.show.core.model.ActionConstant
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.utils.DeviceUtils
import me.yokeyword.fragmentation.ISupportFragment
import okhttp3.internal.http2.ConnectionShutdownException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

class PairFragment2 : BaseFragment() {
    private var ivQrCode: ImageView? = null
    private var qrCodeProgress: ProgressBar? = null
    private var tvErrorText: TextView? = null
    private var pairWakeLock: PowerManager.WakeLock? = null

    private var retryCount = 0
    private var backCount = 0

    private var currentRequestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enableWakeUp = Intent(context, EngineService::class.java)
        enableWakeUp.action = EngineService.ACTION_SET_WAKE_UP_ENABLED
        enableWakeUp.putExtra(EngineService.EXTRA_ENABLED, false)
        context?.startService(enableWakeUp)

        AuthDelegate.setAuthUrl("https://auth.iflyos.cn")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pair_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivQrCode = view.findViewById(R.id.qrcode)
        qrCodeProgress = view.findViewById(R.id.qrcode_progress)
        tvErrorText = view.findViewById(R.id.tv_error)

        // 是否第一次打开
        val fromSetup = findFragment(WifiSettingsFragment::class.java) != null

        // 是否从设置中进入，如果之前已经配置过，解除绑定后会直接进入二维码界面而不会经过其他界面
        val fromSettings = findFragment(AccountFragment::class.java) != null

        if (fromSetup) {
            view.findViewById<View>(R.id.top_with_back)?.isVisible = false
            view.findViewById<View>(R.id.top_without_back)?.isVisible = true
            view.findViewById<View>(R.id.bottom)?.isVisible = true
        } else {
            if (fromSettings) {
                view.findViewById<View>(R.id.top_with_back)?.isVisible = true
                view.findViewById<View>(R.id.bottom)?.isVisible = false
                view.findViewById<View>(R.id.top_without_back)?.isVisible = false
            } else {
                view.findViewById<View>(R.id.top_with_back)?.isVisible = false
                view.findViewById<View>(R.id.top_without_back)?.isVisible = true
                view.findViewById<View>(R.id.bottom)?.isVisible = true
            }
        }

        view.findViewById<TextView>(R.id.previous)?.let {
            it.setOnClickListener {
                if (findFragment(WifiSettingsFragment::class.java) != null) {
                    pop()
                } else {
                    start(WifiSettingsFragment.newInstance(), ISupportFragment.SINGLETOP)
                }
            }
            it.text = if (fromSetup) {
                getString(R.string.previous_step)
            } else {
                getString(R.string.setup_network)
            }
        }

        view.findViewById<View>(R.id.error_field).setOnClickListener {
            it.visibility = View.GONE
            qrCodeProgress?.visibility = View.VISIBLE

            requestQrCode()
        }
        view.findViewById<View>(R.id.back)?.setOnClickListener {
            if (backCount != 0)
                return@setOnClickListener
            backCount++
            pop()
        }

        retryCount = 0

        post {
            requestQrCode()
        }
    }

    private fun requestQrCode() {
        val context = view?.context ?: return
        val requestId = UUID.randomUUID().toString()
        AuthDelegate.requestDeviceCode(context,
            BuildConfig.CLIENT_ID,
            DeviceUtils.getDeviceId(context),
            object : AuthDelegate.ResponseCallback<DeviceCodeResponse> {
                override fun onResponse(response: DeviceCodeResponse) {
                    if (currentRequestId != requestId)
                        return
                    retryCount = 0
                    val authUrl = "${response.verificationUri}?user_code=${response.userCode}"
                    ivQrCode?.let { imageView ->

                        createQRBitmap(authUrl, imageView.width, imageView.height)?.let { bitmap ->
                            imageView.post {
                                imageView.setImageBitmap(bitmap)
                                qrCodeProgress?.visibility = View.GONE
                            }
                        }
                    }
                }

                override fun onError(httpCode: Int?, errorBody: String?, throwable: Throwable?) {
                    if (currentRequestId != requestId)
                        return
                    if (retryCount < 5) {
                        try {
                            Thread.sleep(1000)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        requestQrCode()
                        retryCount++
                    } else {
                        throwable?.let {
                            if (it is UnknownHostException
                                || it is SSLHandshakeException
                                || it is ConnectionShutdownException
                            ) {
                                showError(getString(R.string.qr_code_client_network_error_message))
                            }
                            it.printStackTrace()
                        } ?: run {
                            if (httpCode != 200) {
                                showError(getString(R.string.qr_code_server_error_message))
                            }
                        }
                    }
                }
            },
            object : AuthDelegate.AuthResponseCallback {
                override fun onAuthSuccess(authResponse: AuthResponse) {
                    if (currentRequestId != requestId)
                        return
                    ConfigUtils.putBoolean(ConfigUtils.KEY_SETUP_COMPLETED, true)
                    if (findFragment(WifiSettingsFragment::class.java) != null) {
                        (activity as? EvsLauncherActivity)?.showWelcome()
                        popTo(WifiSettingsFragment::class.java, true, Runnable {
                            (getSupportActivity() as? BaseActivity)?.loadRootFragment(
                                R.id.fragment_container,
                                MainFragment2()
                            )
                        })
                    } else {
                        ContentStorage.get().savePlayInfo(null)
                        ContentStorage.get().isMusicPlaying = false
                        val stopAudioPlayer = Intent(context, EngineService::class.java)
                        stopAudioPlayer.action = EngineService.ACTION_REQUEST_STOP_AUDIO_PLAYER
                        context.startService(stopAudioPlayer)

                        if (findFragment(MainFragment2::class.java) != null) {
                            popTo(MainFragment2::class.java, false)
                        } else {
                            val activity = activity as? EvsLauncherActivity
                            activity?.showWelcome()
                            popTo(PairFragment2::class.java, true, Runnable {
                                activity?.loadRootFragment(
                                    R.id.fragment_container,
                                    MainFragment2.newInstance()
                                )
                            })
                        }
                    }
                    val requestConnect = Intent()
                    requestConnect.action = ActionConstant.ACTION_CLIENT_AUTH_REFRESHED
                    context.sendBroadcast(requestConnect)
                }

                override fun onAuthFailed(errorBody: String?, throwable: Throwable?) {
                    if (currentRequestId != requestId)
                        return
                    throwable?.let {
                        if (it is UnknownHostException) {
                            showError(getString(R.string.qr_code_client_error_message))
                        } else if (it is SocketTimeoutException) {
                            showError(getString(R.string.qr_code_server_timeout_message))
                        }
                        it.printStackTrace()
                    } ?: run {
                        when (errorBody) {
                            AuthDelegate.ERROR_EXPIRED_TOKEN -> {
                                // 轮询超时
                                showError(getString(R.string.qr_code_expired_error_message))
                            }
                            AuthDelegate.ERROR_ACCESS_DENIED -> {
                                // 用户拒绝了授权
                                showError(getString(R.string.qr_code_expired_error_message))
                            }
                            else -> {
                                showError(getString(R.string.qr_code_expired_error_message))
                            }
                        }
                    }
                }
            })
        currentRequestId = requestId
    }

    private fun showError(errorText: String) {
        post {
            tvErrorText?.text = errorText
            ivQrCode?.setImageDrawable(null)
            qrCodeProgress?.visibility = View.GONE
            view?.findViewById<View>(R.id.error_field)?.visibility = View.VISIBLE
        }
    }

    private fun createQRBitmap(content: String, width: Int, height: Int): Bitmap? {
        val context = context ?: return null
        try {
            val colorBlack = ActivityCompat.getColor(context, android.R.color.black)
            val coloWhite = ActivityCompat.getColor(context, android.R.color.transparent)

            // 设置二维码相关配置,生成BitMatrix(位矩阵)对象
            val hints = Hashtable<EncodeHintType, String>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8" // 字符转码格式设置
            hints[EncodeHintType.ERROR_CORRECTION] = "H" // 容错级别设置
            hints[EncodeHintType.MARGIN] = "4" // 空白边距设置

            val bitMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            // 创建像素数组,并根据BitMatrix(位矩阵)对象为数组元素赋颜色值
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * width + x] = colorBlack // 黑色色块像素设置
                    } else {
                        pixels[y * width + x] = coloWhite // 白色色块像素设置
                    }
                }
            }

            // 创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,之后返回Bitmap对象
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onSupportVisible() {
        super.onSupportVisible()

        if (context?.let { AuthDelegate.getAuthResponseFromPref(it) == null } == true)
            acquireWakeLock()
    }

    override fun onSupportInvisible() {
        super.onSupportInvisible()

        releaseWakeLock()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        pairWakeLock?.acquire() ?: run {
            val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
            val flag = PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_BRIGHT_WAKE_LOCK
            val wakeLock = powerManager.newWakeLock(flag, "iflytek:pair")

            wakeLock.acquire()
            pairWakeLock = wakeLock
        }
    }

    private fun releaseWakeLock() {
        pairWakeLock?.release()
        pairWakeLock = null

        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        val flag = PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_BRIGHT_WAKE_LOCK
        val wakeLock = powerManager.newWakeLock(flag, "iflytek:pair")

        wakeLock.acquire(TimeUnit.SECONDS.toMillis(10)) // 尝试保持十秒
    }

    override fun onDestroy() {
        super.onDestroy()

        AuthDelegate.cancelPolling()

        val enableWakeUp = Intent(context, EngineService::class.java)
        enableWakeUp.action = EngineService.ACTION_SET_WAKE_UP_ENABLED
        enableWakeUp.putExtra(EngineService.EXTRA_ENABLED, true)
        context?.startService(enableWakeUp)
    }

    override fun onBackPressedSupport(): Boolean {
        // 是否第一次打开
        val fromSetup = findFragment(WifiSettingsFragment::class.java) != null

        // 是否从设置中进入，如果之前已经配置过，解除绑定后会直接进入二维码界面而不会经过其他界面
        val fromSettings = findFragment(AccountFragment::class.java) != null

        return !(fromSetup || fromSettings)
    }
}