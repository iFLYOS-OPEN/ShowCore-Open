package com.iflytek.cyber.iot.show.core.fragment

import android.net.wifi.ScanResult
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.KeyboardUtils
import com.iflytek.cyber.iot.show.core.utils.WifiUtils

class InputNetworkFragment(private val scanResult: ScanResult? = null) : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_input_network, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etSsid: EditText = view.findViewById(R.id.ssid)
        val etPassword: EditText = view.findViewById(R.id.password)

        etPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                scanResult?.let {
                    view.findViewById<View>(R.id.connect).isEnabled = (s?.length ?: 0) >= 8
                }
            }

        })

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        view.findViewById<View>(R.id.connect).setOnClickListener {
            val password = etPassword.text.toString()
            KeyboardUtils.closeKeyboard(etPassword)
            scanResult?.let { scanResult ->
                if (password.length < 8) {

                } else {
                    WifiUtils.connect(it.context, scanResult.SSID, password)
                    start(WifiConnectingFragment(scanResult.SSID))
                }
            } ?: run {
                if (password.isEmpty())
                    WifiUtils.connect(it.context, etSsid.text.toString())
                else
                    WifiUtils.connect(it.context, etSsid.text.toString(), password)
                start(WifiConnectingFragment(etSsid.text.toString()))
            }
        }

        val titleView: TextView = view.findViewById(R.id.title)
        scanResult?.let {
            view.findViewById<View>(R.id.connect).isEnabled = false
            view.findViewById<View>(R.id.ssid_container).visibility = View.GONE
            titleView.text = "连接到 ${it.SSID}"
        } ?: run {
            view.findViewById<View>(R.id.ssid_container).visibility = View.VISIBLE
            titleView.text = "手动添加"
        }
    }

    override fun onDestroyView() {
        KeyboardUtils.closeKeyboard(view)
        super.onDestroyView()
    }
}