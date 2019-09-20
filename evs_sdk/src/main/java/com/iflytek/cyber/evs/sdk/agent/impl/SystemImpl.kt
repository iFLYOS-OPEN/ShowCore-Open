package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.EvsService
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.agent.System

class SystemImpl(private val context: Context) : System() {
    override fun checkSoftWareUpdate() {

    }

    override fun onPing(payload: JSONObject) {

    }

    override fun onError(payload: JSONObject) {
        super.onError(payload)
    }

    override fun updateSoftware() {
    }

    override fun onDeviceModeChanged(kid: Boolean) {
    }

    override fun onPowerOff(payload: JSONObject) {

    }

    override fun onUpdateDeviceModes(payload: JSONObject) {

    }

}