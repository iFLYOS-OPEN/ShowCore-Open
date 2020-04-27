@file:Suppress("FunctionName")

package com.iflytek.cyber.iot.show.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.RecoverySystem
import android.provider.Settings
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.EngineService
import com.iflytek.cyber.iot.show.core.FloatingService
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import java.io.File

object DeviceUtils {

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getClientId(): String {
        val savedClientId = ConfigUtils.getString(ConfigUtils.KEY_CLIENT_ID, null)
        return if (savedClientId.isNullOrEmpty()) {
            BuildConfig.CLIENT_ID
        } else {
            savedClientId
        }
    }

    fun getIvwVersion(): String? {
        return "7.0"
    }

    fun getSystemVersionName(): String {
        val systemVersion = BuildConfig.VERSION_NAME
        if (BuildConfig.DEBUG)
            return "$systemVersion(${BuildConfig.VERSION_CODE})"
        return systemVersion
    }

    fun getSystemVersion(): Int {
        return BuildConfig.VERSION_CODE
    }

    fun lockScreen(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val screenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            pm.isInteractive
        } else {
            pm.isScreenOn
        }
        if (screenOn && Process.myUid() == Process.SYSTEM_UID) {
            // 点亮屏幕
            TerminalUtils.execute("input keyevent POWER")
        }
    }

    fun unlockScreen(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
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
    }

    fun getBatteryLevel(context: Context?): Int {
        context?.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )?.let { intent ->
            //            val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
//
//            // How are we charging?
//            val chargePlug: Int = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
//            val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
//            val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
//
//            val isCharging: Boolean =
//                (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
//                    && chargePlug == BatteryManager.BATTERY_PLUGGED_USB

            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            return level * 100 / scale
        }
        return 0
    }

    fun doFactoryReset(context: Context) {
        Thread {
            try {
                RecoverySystem.rebootWipeUserData(context)
            } catch (t: Throwable) {
                if (t is SecurityException) {
                    val intent = Intent(context, FloatingService::class.java)
                    intent.action = FloatingService.ACTION_SHOW_NOTIFICATION
                    intent.putExtra(FloatingService.EXTRA_MESSAGE, "恢复出厂操作无法获得权限")
                    intent.putExtra(
                        FloatingService.EXTRA_POSITIVE_BUTTON_TEXT,
                        context.getString(R.string.i_got_it)
                    )
                    context.startService(intent)

                    ConfigUtils.removeAll()

                    val revokeAuth = Intent(context, EngineService::class.java)
                    revokeAuth.action = EngineService.ACTION_AUTH_REVOKED
                    context.startService(revokeAuth)

                    // init overlay
                    val overlay = Intent(context, FloatingService::class.java)
                    overlay.action = FloatingService.ACTION_INIT_CONFIG
                    context.startService(overlay)

                    // 重置音量和亮度
                    EvsSpeaker.get(context).setVolumeLocally(50)
                    BrightnessUtils.setBrightness(context, 50)

                    // 删除自定义唤醒词文件
                    val externalCache = context.externalCacheDir
                    val customWakeResFile = File("$externalCache/wake_up_res_custom.bin")
                    if (customWakeResFile.exists()) {
                        customWakeResFile.delete()
                    }
                } else {
                    t.printStackTrace()
                }
            }
        }.start()
    }
}