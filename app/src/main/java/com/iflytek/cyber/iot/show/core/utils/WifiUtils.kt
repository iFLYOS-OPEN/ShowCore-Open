package com.iflytek.cyber.iot.show.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.lang.Exception
import java.lang.StringBuilder
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

object WifiUtils {
    private const val TAG = "WifiUtils"

    fun isEncrypted(scan: ScanResult): Boolean {
        // [ESS]
        // [WPA2-PSK-CCMP][ESS]
        // [WPA2-PSK-CCMP][WPS][ESS]
        // [WPA-PSK-CCMP+TKIP][WPA2-PSK-CCMP+TKIP][WPS][ESS]
        return scan.capabilities.contains("WPA")
    }

    fun connect(context: Context, ssid: String, password: String? = null): Boolean {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val config =
            if (password.isNullOrEmpty())
                buildWifiConfig(ssid)
            else
                buildWifiConfig(ssid, password)
        val networkId = wm.addNetwork(config)

        return connect(context, networkId)
    }

    fun connect(context: Context, networkId: Int): Boolean {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wm.disconnect()) {
            Log.d(TAG, "Disconnect failed")
            return false
        }

        if (!wm.enableNetwork(networkId, true)) {
            Log.d(TAG, "Enable failed")
            return false
        }

        if (!wm.reconnect()) {
            Log.d(TAG, "Reconnect failed")
            return false
        }

        return true
    }

    fun forget(context: Context?, ssid: String?): Boolean {
        (context?.applicationContext?.getSystemService(Context.WIFI_SERVICE)
            as? WifiManager)?.let { wm ->
            val iterator = wm.configuredNetworks.iterator()
            while (iterator.hasNext()) {
                val configuration = iterator.next()
                val configuredSsid =
                    if (configuration.SSID.isNullOrEmpty())
                        ""
                    else
                        configuration.SSID.substring(1, configuration.SSID.length - 1)
                if (configuredSsid == ssid) {
                    iterator.remove()
                    wm.removeNetwork(configuration.networkId)
                    return true
                }
            }
            return false
        }
        return false
    }

    private fun buildWifiConfig(ssid: String): WifiConfiguration {
        val config = WifiConfiguration()
        config.SSID = "\"" + ssid + "\""
        config.status = WifiConfiguration.Status.ENABLED
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        return config
    }

    private fun buildWifiConfig(ssid: String, password: String): WifiConfiguration {
        val config = WifiConfiguration()
        config.SSID = "\"" + ssid + "\""
        config.status = WifiConfiguration.Status.ENABLED
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP)
        @Suppress("DEPRECATION")
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP)
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
        config.preSharedKey = "\"" + password + "\""
        return config
    }

    @SuppressLint("HardwareIds")
    fun getMacAddress(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wifiManager.connectionInfo.macAddress
        } else {
            try {
                val en = NetworkInterface.getNetworkInterfaces()
                while (en.hasMoreElements()) {
                    val intf = en.nextElement()
                    if (intf.name == "wlan0") {
                        val buf = StringBuilder()
                        intf.hardwareAddress.map {
                            buf.append(String.format("%02X:", it))
                        }
                        buf.deleteCharAt(buf.length - 1)

                        return buf.toString()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }

    fun getIPAddress(context: Context): String? {
        val info = (context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
        if (info != null && info.isConnected) {
            if (info.type == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces()
                    val en = NetworkInterface.getNetworkInterfaces()
                    while (en.hasMoreElements()) {
                        val intf = en.nextElement()
                        val enumIpAddr = intf.inetAddresses
                        while (enumIpAddr.hasMoreElements()) {
                            val inetAddress = enumIpAddr.nextElement()
                            if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                return inetAddress.getHostAddress()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            } else if (info.type == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                return intIP2StringIP(wifiInfo.ipAddress)//得到IPV4地址
            }
        } else {
            //当前无网络连接,请在设置中打开网络
        }
        return null
    }

    fun getConnectedSsid(context: Context?): String? {
        (context?.applicationContext?.getSystemService(Context.WIFI_SERVICE)
            as? WifiManager)?.let {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val configuredNetworks = it.configuredNetworks
                if (configuredNetworks?.isNotEmpty() == true)
                    for (config in configuredNetworks) {
                        val ssid = config.SSID.substring(1, config.SSID.length - 1)

                        if (config.status == WifiConfiguration.Status.CURRENT) {
                            println("ssid: $ssid")
                            return ssid
                        }
                    }
            } else {
                val connManager = context.applicationContext
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                if (networkInfo.isConnected) {
                    val wifiInfo = it.connectionInfo
                    val ssid = wifiInfo.ssid
                    println("ssid: $ssid")
                    return ssid.substring(1, ssid.length - 1)
                }
            }
        }
        return null
    }

    /**
     * 将得到的int类型的IP转换为String类型
     *
     * @param ip
     * @return
     */
    fun intIP2StringIP(ip: Int): String {
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${((ip shr 16) and 0xFF)}.${(ip shr 24 and 0xFF)}"
    }

    fun isWifiEnabled(context: Context?): Boolean {
        return (context?.applicationContext?.getSystemService(Context.WIFI_SERVICE)
            as? WifiManager)?.isWifiEnabled == true
    }
}