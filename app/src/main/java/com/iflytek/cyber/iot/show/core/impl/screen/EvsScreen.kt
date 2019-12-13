package com.iflytek.cyber.iot.show.core.impl.screen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.iflytek.cyber.evs.sdk.agent.Screen
import com.iflytek.cyber.iot.show.core.utils.BrightnessUtils
import com.iflytek.cyber.iot.show.core.utils.ScreenOffTimeoutUtils
import com.iflytek.cyber.iot.show.core.utils.TerminalUtils
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
            val pm = context
                .getSystemService(Context.POWER_SERVICE) as PowerManager
            val screenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                pm.isInteractive
            } else {
                pm.isScreenOn
            }
            if (!screenOn) {
                // 点亮屏幕
                @SuppressLint("InvalidWakeLockTag")
                val wl = pm.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                    "bright"
                )
                wl.acquire(10000)
                wl.release()
            }
        } else if (state == STATE_OFF) {
            val batteryStatus: Intent? =
                IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    context.registerReceiver(null, ifilter)
                }
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val isCharging =
                (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
                    && chargePlug == BatteryManager.BATTERY_PLUGGED_USB
            if (isCharging) {
                ScreenOffTimeoutUtils.dreamNow(context)
            } else {
                TerminalUtils.execute("input keyevent POWER")
            }
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