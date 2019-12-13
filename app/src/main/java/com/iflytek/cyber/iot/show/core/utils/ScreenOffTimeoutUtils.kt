package com.iflytek.cyber.iot.show.core.utils

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.os.UserHandle
import android.provider.Settings
import java.util.concurrent.TimeUnit

object ScreenOffTimeoutUtils {

    @Suppress("unused")
    private const val TAG = "ScreenOffTimeoutUtils"

    private const val SCREENSAVER_ACTIVATE_ON_DOCK = "screensaver_activate_on_dock"
    private const val SCREENSAVER_ACTIVATE_ON_SLEEP = "screensaver_activate_on_sleep"
    private const val SCREENSAVER_DEFAULT_COMPONENT = "screensaver_default_component"
    private const val SCREENSAVER_COMPONENTS = "screensaver_components"
    private const val SCREENSAVER_ENABLED = "screensaver_enabled"

    val ONE_MIN_TIMEOUT = TimeUnit.MINUTES.toMillis(1).toInt()
    val TWO_MIN_TIMEOUT = TimeUnit.MINUTES.toMillis(2).toInt()
    val FIVE_MIN_TIMEOUT = TimeUnit.MINUTES.toMillis(5).toInt()
    val TEN_MIN_TIMEOUT = TimeUnit.MINUTES.toMillis(10).toInt()
    val DEFAULT_TIMEOUT = TWO_MIN_TIMEOUT
    @Suppress("MemberVisibilityCanBePrivate")
    const val NEVER_TIMEOUT = Int.MAX_VALUE

    fun getTimeout(context: Context): Int {
        val contentResolver = context.contentResolver
        return Settings.System.getInt(
            contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            DEFAULT_TIMEOUT
        )
    }

    fun setTimeout(context: Context, timeout: Int) {
        val contentResolver = context.contentResolver
        Settings.System.putInt(
            contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            timeout
        )
    }

    private fun setActiveDream(context: Context, dream: ComponentName) {
        try {
            val clz = Settings.Secure::class.java
            val putStringForUser = clz.getDeclaredMethod(
                "putStringForUser",
                ContentResolver::class.java,
                String::class.java,
                String::class.java,
                Int::class.java
            )
            putStringForUser.invoke(
                null,
                context.contentResolver,
                SCREENSAVER_COMPONENTS,
                dream.flattenToString(),
                getCurrentUserId()
            )
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    fun dreamNow(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            val time = SystemClock.uptimeMillis()
            val clz = PowerManager::class.java
            val userActivity =
                clz.getDeclaredMethod("userActivity", Long::class.java, Boolean::class.java)
            userActivity.isAccessible = true
            userActivity.invoke(pm, time, false)

            val nap = clz.getDeclaredMethod("nap", Long::class.java)
            nap.isAccessible = true
            nap.invoke(pm, time)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun getCurrentUserId(): Int {
        try {
            val clz = UserHandle::class.java

            val userOwner = clz.getDeclaredField("USER_OWNER")
            userOwner.isAccessible = true
            return userOwner.get(null) as Int
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return -1
    }

    fun init(context: Context) {
        // only charging
        setBoolean(context, SCREENSAVER_ENABLED, true)
        setBoolean(context, SCREENSAVER_ACTIVATE_ON_DOCK, false)
        setBoolean(context, SCREENSAVER_ACTIVATE_ON_SLEEP, true)

        setActiveDream(
            context,
            ComponentName("com.android.deskclock", "com.android.deskclock.Screensaver")
        )
    }

    private fun setBoolean(context: Context, key: String, value: Boolean) {
        Settings.Secure.putInt(context.contentResolver, key, if (value) 1 else 0)
    }
}