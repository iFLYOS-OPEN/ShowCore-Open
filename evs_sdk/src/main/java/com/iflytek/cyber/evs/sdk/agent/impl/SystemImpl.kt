package com.iflytek.cyber.evs.sdk.agent.impl

import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.agent.System

class SystemImpl : System() {
    override fun checkSoftWareUpdate() {

    }

    override fun onPing(timestamp: Long) {

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