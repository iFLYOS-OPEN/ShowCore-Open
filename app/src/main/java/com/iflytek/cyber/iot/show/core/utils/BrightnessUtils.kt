package com.iflytek.cyber.iot.show.core.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log

object BrightnessUtils {
    private const val MAX_BRIGHTNESS = 255
    private const val MIN_BRIGHTNESS = 25

    fun hasPermission(context: Context?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
    }

    /**
     * Android 的亮度范围为 0~255，这里会转为 0~100
     */
    fun getBrightness(context: Context?): Int {
        val contentResolver = context?.contentResolver ?: return -1
        val realBrightness =  Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        return (realBrightness - MIN_BRIGHTNESS) * 100 / (MAX_BRIGHTNESS - MIN_BRIGHTNESS)
    }

    fun getBrightnessMode(context: Context?): Int {
        val contentResolver = context?.contentResolver ?: return -1

        return Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
    }

    /**
     * @param brightness Android 的亮度范围为 0~255，这里会转为 0~100
     */
    fun setBrightness(context: Context?, brightness: Int) {
        Log.d("setBrightness", brightness.toString())
        if (hasPermission(context)) {
            val target = brightness * (MAX_BRIGHTNESS - MIN_BRIGHTNESS) / 100 + MIN_BRIGHTNESS

            val contentResolver = context?.contentResolver ?: return
            try {
                if (Settings.System.getInt(
                        contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE
                    ) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                ) {
                    Settings.System.putInt(
                        contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                }
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }

            try {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, target
                )
            } catch (e: Settings.SettingNotFoundException) {
                e.printStackTrace()
            }
        }
    }
}