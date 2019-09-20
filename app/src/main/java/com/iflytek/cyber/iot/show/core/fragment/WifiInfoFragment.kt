package com.iflytek.cyber.iot.show.core.fragment

import android.net.wifi.ScanResult
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.WifiUtils

class WifiInfoFragment(private val scanResult: ScanResult? = null) : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wifi_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvSsid: TextView = view.findViewById(R.id.ssid)
        val tvIp: TextView = view.findViewById(R.id.ip_address)
        val tvMac: TextView = view.findViewById(R.id.wifi_mac)

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        view.findViewById<View>(R.id.forget).setOnClickListener {
            WifiUtils.forget(it.context, scanResult?.SSID)

            pop()
        }

        tvSsid.text = scanResult?.SSID

        if (scanResult?.SSID == WifiUtils.getConnectedSsid(context)) {
            tvIp.text = WifiUtils.getIPAddress(view.context)
            tvMac.text = WifiUtils.getMacAddress(view.context)

            view.findViewById<View>(R.id.ip_container).visibility = View.VISIBLE
            view.findViewById<View>(R.id.mac_container).visibility = View.VISIBLE
        } else {
            view.findViewById<View>(R.id.ip_container).visibility = View.GONE
            view.findViewById<View>(R.id.mac_container).visibility = View.GONE
        }
    }
}