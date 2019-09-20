package com.iflytek.cyber.iot.show.core.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.iflytek.cyber.iot.show.core.R

class WifiConnectFailedFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wifi_connect_failed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.retry).setOnClickListener {
            pop()
        }
        view.findViewById<View>(R.id.cancel).setOnClickListener {
            popTo(WifiSettingsFragment::class.java, false)
        }
    }

}