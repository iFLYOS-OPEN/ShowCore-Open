package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.utils.WifiUtils

class WifiConnectFailedFragment : BaseFragment() {
    companion object {
        fun newInstance(
            errorMessage: String? = null,
            ssid: String? = null,
            cancelText: String? = null,
            retryText: String? = null
        ): WifiConnectFailedFragment {
            val fragment = WifiConnectFailedFragment()
            val arguments = Bundle()
            arguments.putString("error", errorMessage)
            arguments.putString("ssid", ssid)
            arguments.putString("cancelText", cancelText)
            arguments.putString("retryText", retryText)
            fragment.arguments = arguments
            return fragment
        }
    }

    private var backCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wifi_connect_failed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cancelText = arguments?.getString("cancelText")
        val retryText = arguments?.getString("retryText")
        val ssid = arguments?.getString("ssid")

        if (cancelText.isNullOrEmpty() && retryText.isNullOrEmpty()) {
            view.findViewById<View>(R.id.retry).setOnClickListener {
                if (backCount != 0)
                    return@setOnClickListener
                backCount++
                pop()
            }
            view.findViewById<View>(R.id.cancel).setOnClickListener {
                popTo(WifiSettingsFragment::class.java, false)
            }

            arguments?.getString("error")?.let { errorMessage ->
                val tvMessage = view.findViewById<TextView>(R.id.error_text)
                tvMessage?.text = errorMessage
            }
        } else if (!cancelText.isNullOrEmpty() && !retryText.isNullOrEmpty()) {
            val retryTextView = view.findViewById<TextView>(R.id.retry)
            retryTextView.text = retryText
            retryTextView.setOnClickListener {
                if (!ssid.isNullOrEmpty()) {
                    WifiUtils.forget(context, ssid)
                }
                popTo(WifiSettingsFragment::class.java, false)
            }
            val cancelTextView = view.findViewById<TextView>(R.id.cancel)
            cancelTextView.text = cancelText
            cancelTextView.setOnClickListener {
                popTo(WifiSettingsFragment::class.java, false)
            }

            arguments?.getString("error")?.let { errorMessage ->
                val tvMessage = view.findViewById<TextView>(R.id.error)
                tvMessage?.text = errorMessage
            }
        }
    }

}