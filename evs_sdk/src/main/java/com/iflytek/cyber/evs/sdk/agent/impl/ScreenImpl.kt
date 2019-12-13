package com.iflytek.cyber.evs.sdk.agent.impl

import android.annotation.SuppressLint
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

    @SuppressLint("MissingPermission")
    override fun setBrightness(brightness: Long): Boolean {
        try {
            ScreenUtil.setBrightness(context, brightness * 255 / 100)
            return true
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return false
    }
}