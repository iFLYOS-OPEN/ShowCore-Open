package com.iflytek.cyber.evs.sdk

import com.iflytek.cyber.evs.sdk.socket.Result

interface RequestCallback {
    fun onResult(result: Result)
}