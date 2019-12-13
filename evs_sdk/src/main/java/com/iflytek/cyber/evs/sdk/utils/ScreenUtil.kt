package com.iflytek.cyber.evs.sdk.utils

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import java.lang.Exception

object ScreenUtil {

    /**
     * 判断屏幕是否处于点亮状态。
     * @param context 上下文对象
     * @return 是否点亮
     */
    fun isScreenOn(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.state == Display.STATE_ON
        } else {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isScreenOn
        }
    }

    /**
     * 锁屏。
     * @param context 上下文对象
     * @param componentName 被授予DEVICE_ADMIN的component
     * @return 是否成功
     */
    fun lockScreen(context: Context, componentName: ComponentName): Boolean {
        val policyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (policyManager.isAdminActive(componentName)) {
            policyManager.lockNow()
            return true
        }
        return false
    }

    /**
     * 解锁屏幕。
     * @param context 上下文对象
     * @return 是否成功
     */
    fun unlockScreen(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = powerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                ":bright")
        wl.acquire(1000)
        wl.release()
        return true
    }

    /**
     * 获取屏幕亮度（在某些机型如小米6上可能有问题，因为SCREEN_BRIGHTNESS的最大值不是255）。
     * @param context 上下文对象
     * @return 亮度值（0-100）
     */
    fun getBrightness(context: Context): Long {
        try {
            val brightness: Long = Settings.System.getInt(context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS).toLong()

            return (brightness / 255.0 * 100).toLong()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0
    }

    /**
     * 设置屏幕亮度（在某些机型如小米6上可能有问题，因为SCREEN_BRIGHTNESS的最大值不是255）。
     * @param context 上下文对象
     * @param brightness 亮度值（0-100）
     * @return 是否成功
     */
    @RequiresPermission(Manifest.permission.WRITE_SETTINGS)
    fun setBrightness(context: Context, brightness: Long): Boolean {
        val resolver = context.contentResolver
        val uri = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS)

        val result = Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS,
            (brightness * 255.0 / 100.0).toInt())
        if (result)
            resolver.notifyChange(uri, null)

        return result
    }
}