package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.iflytek.cyber.iot.show.core.R

class WifiConnectFailedFragment : BaseFragment() {
    companion object {
        fun newInstance(errorMessage: String? = null): WifiConnectFailedFragment {
            val fragment = WifiConnectFailedFragment()
            val arguments = Bundle()
            arguments.putString("error", errorMessage)
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
    }

}