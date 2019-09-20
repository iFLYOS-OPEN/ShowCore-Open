package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import android.provider.Settings
import com.iflytek.cyber.evs.sdk.agent.Screen
import com.iflytek.cyber.evs.sdk.utils.ScreenUtil

class ScreenImpl(private val context: Context) : Screen() {

    override fun getState(): String {
        return if (ScreenUtil.isScreenOn(context)) STATE_ON else STATE_OFF
    }

    override fun setState(state: String): Boolean {
        return true
    }

    override fun getBrightness(): Long {
        return ScreenUtil.getBrightness(context)
    }

    override fun setBrightness(brightness: Long): Boolean {
        return true
    }
}