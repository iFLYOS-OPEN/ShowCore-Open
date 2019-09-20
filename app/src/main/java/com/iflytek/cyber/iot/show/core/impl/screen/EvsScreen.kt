package com.iflytek.cyber.iot.show.core.impl.screen

import android.content.Context
import android.provider.Settings
import com.iflytek.cyber.evs.sdk.agent.Screen
import com.iflytek.cyber.iot.show.core.task.SleepWorker
import com.iflytek.cyber.iot.show.core.utils.BrightnessUtils
import java.lang.ref.SoftReference

class EvsScreen private constructor(context: Context) : Screen() {
    private val contextRef = SoftReference(context)
    private val callbacks = HashSet<BrightnessChangedCallback>()

    private var state = STATE_ON

    companion object {
        private var instance: EvsScreen? = null

        fun get(context: Context?): EvsScreen {
            instance?.let {
                return it
            } ?: run {
                val instance = EvsScreen(context!!)
                this.instance = instance
                return instance
            }
        }
    }

    fun registerBrightnessChangedCallback(callback: BrightnessChangedCallback) {
        callbacks.add(callback)
    }

    fun unregisterBrightnessChangedCallback(callback: BrightnessChangedCallback) {
        callbacks.remove(callback)
    }

    override fun setState(state: String): Boolean {
        val context = contextRef.get() ?: return false

        this.state = state

        if (state == STATE_ON) {
            SleepWorker.get(context).hideSleepView(context)
        } else if (state == STATE_OFF) {
            SleepWorker.get(context).showSleepView(context)
        }
        return true
    }

    override fun setBrightness(brightness: Long): Boolean {
        val context = contextRef.get() ?: return false

        BrightnessUtils.setBrightness(context, brightness.toInt())

        callbacks.map {
            try {
                it.onBrightnessChanged(brightness.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return true
    }

    override fun getBrightness(): Long {
        return BrightnessUtils.getBrightness(contextRef.get()).toLong()
    }

    override fun getState(): String {
        return state
    }

    interface BrightnessChangedCallback {
        fun onBrightnessChanged(brightness: Int)
    }
}