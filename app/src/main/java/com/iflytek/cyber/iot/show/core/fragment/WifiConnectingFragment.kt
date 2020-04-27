package com.iflytek.cyber.iot.show.core.fragment

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.utils.ConnectivityUtils
import com.iflytek.cyber.iot.show.core.utils.WifiUtils
import java.util.*

class WifiConnectingFragment(private val ssid: String? = null) : BaseFragment() {
    companion object {
        private const val TAG = "WifiConnectingFragment"

        const val NETWORK_RETRY_COUNT = 10
    }

    private val countHandler = CountDownHandler()
    private val connectionReceiver = object : SelfBroadcastReceiver(
        WifiManager.SUPPLICANT_STATE_CHANGED_ACTION,
        ConnectivityManager.CONNECTIVITY_ACTION
    ) {

        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                WifiManager.SUPPLICANT_STATE_CHANGED_ACTION -> {
                    val error = intent.getIntExtra(
                        WifiManager.EXTRA_SUPPLICANT_ERROR, -1
                    )

                    if (error == WifiManager.ERROR_AUTHENTICATING) {
                        Log.e(TAG, "Wi-Fi authenticate failed")
                        handleWifiConfigFailed()
                    }
                }
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val network = intent.getParcelableExtra<NetworkInfo>(
                        ConnectivityManager.EXTRA_NETWORK_INFO
                    )
                    val detailed = network.detailedState

                    if (detailed == NetworkInfo.DetailedState.CONNECTED) {
                        Log.d(TAG, "Wi-Fi connected")
                        val checkId = UUID.randomUUID()
                        this@WifiConnectingFragment.checkId = checkId
                        handleWifiConfigSucceed(checkId)
                    }
                }
            }
        }
    }
    private var networkCallback: Any? = null
    private var retryCount = 0
    private var isChecking = false
    private var checkId: UUID? = null

    fun handleWifiConfigFailed() {
        WifiUtils.forget(context, ssid)

        startWithPop(WifiConnectFailedFragment.newInstance("您的网络异常，请确定网络可用后重试"))

        countHandler.stopCount()
    }

    fun handleWifiConfigSucceed(checkId: UUID) {
        isChecking = true
        Log.d(TAG, "start checking network")
        ConnectivityUtils.checkIvsAvailable({
            isChecking = false
            retryCount = 0

            if (findFragment(MainFragment2::class.java) == null) {
                startWithPopTo(PairFragment2(), WifiSettingsFragment::class.java, false)
            } else {
                popTo(WifiSettingsFragment::class.java, false)
            }
        }) { exception, _ ->
            exception?.printStackTrace()
            if (this@WifiConnectingFragment.checkId == checkId) {
                if (retryCount < NETWORK_RETRY_COUNT) {
                    try {
                        Thread.sleep(2000)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    handleWifiConfigSucceed(checkId)
                } else {
                    isChecking = false
                    startWithPop(
                        WifiConnectFailedFragment.newInstance(
                            "网络可能不可用，是否选择其他网络",
                            ssid,
                            "忽略",
                            "重新选择"
                        )
                    )
                    countHandler.stopCount()
                }
                retryCount++
            }
        }

        countHandler.stopCount()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network?) {
                    super.onAvailable(network)

                    val checkId = UUID.randomUUID()
                    this@WifiConnectingFragment.checkId = checkId
                    handleWifiConfigSucceed(checkId)
                }

                override fun onLost(network: Network?) {
                    super.onLost(network)

                    handleWifiConfigFailed()
                }
            }

            val request = NetworkRequest.Builder().build()

            (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.registerNetworkCallback(request, networkCallback)

            this.networkCallback = networkCallback
        } else {
            connectionReceiver.register(context)
        }

        if (!WifiUtils.getConnectedSsid(context).isNullOrEmpty()) {
            val checkId = UUID.randomUUID()
            this@WifiConnectingFragment.checkId = checkId
            handleWifiConfigSucceed(checkId)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wifi_connecting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loadingAnimation: LottieAnimationView = view.findViewById(R.id.loading)

        val tvConnecting: TextView = view.findViewById(R.id.connecting)
        ssid?.let { ssid ->
            tvConnecting.text = String.format("正在连接到 %s", ssid)
        }

        loadingAnimation.repeatCount = Animation.INFINITE
        loadingAnimation.playAnimation()

        countHandler.startCount(Runnable {
            if (WifiUtils.getConnectedSsid(context) != ssid) {
                WifiUtils.forget(context, ssid)

                startWithPop(WifiConnectFailedFragment())
            }
        })

        retryCount = 0
    }

    override fun onBackPressedSupport(): Boolean {
        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (networkCallback as? ConnectivityManager.NetworkCallback)?.let { networkCallback ->
                (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                    ?.unregisterNetworkCallback(networkCallback)
            }
        } else {
            connectionReceiver.unregister(context)
        }
    }

    private class CountDownHandler : Handler() {
        private var startTime = 0
        private var runnable: Runnable? = null

        fun startCount(runnable: Runnable) {
            val message = Message.obtain()
            message.what = 0
            message.arg1 = System.currentTimeMillis().toInt()

            startTime = message.arg1
            this.runnable = runnable

            sendMessageDelayed(message, 20 * 1000)
        }

        fun stopCount() {
            removeCallbacksAndMessages(null)
            startTime = 0
            runnable = null
        }

        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg?.what == 0) {
                if (msg.arg1 == startTime) {
                    runnable?.run()
                }
            }
        }
    }
}