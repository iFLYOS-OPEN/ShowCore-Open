package com.iflytek.cyber.iot.show.core.fragment

import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.gson.JsonParser
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import com.iflytek.cyber.iot.show.core.utils.WifiUtils
import okhttp3.OkHttpClient
import okhttp3.Request

class SettingsFragment2 : BaseFragment() {
    @Suppress("DEPRECATION")
    private val connectionReceiver = object : SelfBroadcastReceiver(
        ConnectivityManager.CONNECTIVITY_ACTION
    ) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val network = intent.getParcelableExtra<NetworkInfo>(
                        ConnectivityManager.EXTRA_NETWORK_INFO
                    )
                    val detailed = network.detailedState

                    val tvSsid = view?.findViewById<TextView>(R.id.ssid)
                    if (detailed == NetworkInfo.DetailedState.CONNECTED) {
                        WifiUtils.getConnectedSsid(context)?.let { ssid ->
                            if (ssid.isNotEmpty()) {
                                tvSsid?.text = ssid
                            } else {
                                tvSsid?.text = ""
                            }
                        } ?: run {
                            tvSsid?.text = ""
                        }
                    } else {
                        tvSsid?.text = ""
                    }
                }
            }
        }
    }
    private var networkCallback: Any? = null // 不声明 NetworkCallback 的类，否则 L 以下会找不到类

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            connectionReceiver.register(context)
        } else {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network?) {
                    super.onAvailable(network)

                    post {
                        val tvSsid = view?.findViewById<TextView>(R.id.ssid)
                        WifiUtils.getConnectedSsid(context)?.let { ssid ->
                            if (ssid.isNotEmpty()) {
                                tvSsid?.text = ssid
                            } else {
                                tvSsid?.text = ""
                            }
                        } ?: run {
                            tvSsid?.text = ""
                        }
                    }
                }

                override fun onLost(network: Network?) {
                    super.onLost(network)

                    post {
                        val tvSsid = view?.findViewById<TextView>(R.id.ssid)
                        tvSsid?.text = ""
                    }
                }
            }
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)

            this.networkCallback = networkCallback
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back)?.setOnClickListener {
            pop()
        }
        view.findViewById<View>(R.id.wifi)?.setOnClickListener {
            start(WifiSettingsFragment())
        }
        view.findViewById<View>(R.id.about)?.setOnClickListener {
            start(AboutFragment2())
        }
        view.findViewById<View>(R.id.screen_and_brightness)?.setOnClickListener {
            start(ScreenBrightnessFragment())
        }
        view.findViewById<View>(R.id.account)?.setOnClickListener {
            if (!ConfigUtils.getString(ConfigUtils.KEY_CACHE_USER_INFO, null).isNullOrEmpty())
                start(AccountFragment())
        }
        view.findViewById<View>(R.id.volume)?.setOnClickListener {
            start(VolumeFragment())
        }
        view.findViewById<View>(R.id.microphone)?.setOnClickListener {
            start(MicrophoneFragment())
        }
        view.findViewById<View>(R.id.check_update)?.setOnClickListener {
            start(CheckUpdateFragment())
        }

        if (!ConfigUtils.getString(ConfigUtils.KEY_CACHE_USER_INFO, null).isNullOrEmpty()) {
            updateFromPref()
        }
        requestUserAccount()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            connectionReceiver.unregister(context)
        } else {
            (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.let { connectivityManager ->
                    val networkCallback =
                        (this.networkCallback as? ConnectivityManager.NetworkCallback)
                            ?: return
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
        }
    }

    private fun updateFromPref() {
        val userInfo = ConfigUtils.getString(ConfigUtils.KEY_CACHE_USER_INFO, null)

        val tvAccount = view?.findViewById<TextView>(R.id.tv_account)

        try {
            val userInfoJson = JsonParser().parse(userInfo).asJsonObject

            val phone = userInfoJson.get("phone").asString

            tvAccount?.text = phone
        } catch (e: Exception) {
            e.printStackTrace()

            tvAccount?.setText(R.string.my_account)
        }
    }

    private fun requestUserAccount() {
        Thread {
            try {
                val context = context ?: return@Thread

                val client = OkHttpClient.Builder()
                    .build()

                val authResponse = AuthDelegate.getAuthResponseFromPref(context)

                val request = Request.Builder()
                    .url("https://staging-api.iflyos.cn/showcore/api/v1/profile")
                    .header("Authorization", "Bearer ${authResponse?.accessToken}")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    ConfigUtils.putString(
                        ConfigUtils.KEY_CACHE_USER_INFO,
                        response.body()?.string()
                    )

                    post {
                        updateFromPref()
                    }
                } else {

                }
            } catch (e: Exception) {

            }
        }.start()
    }

    override fun onResume() {
        super.onResume()

        val tvSsid: TextView? = view?.findViewById(R.id.ssid)

        WifiUtils.getConnectedSsid(context)?.let { ssid ->
            if (ssid.isNotEmpty()) {
                tvSsid?.text = ssid
            } else {
                tvSsid?.text = ""
            }
        } ?: run {
            tvSsid?.text = ""
        }
    }
}