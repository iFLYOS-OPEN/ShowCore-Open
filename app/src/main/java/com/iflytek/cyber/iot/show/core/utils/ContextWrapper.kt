package com.iflytek.cyber.iot.show.core.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserHandle
import java.lang.reflect.InvocationTargetException


object ContextWrapper {

    fun startServiceAsUser(context: Context, intent: Intent, userHandle: String) {
        try {
            val clz = context::class.java
            val method =
                clz.getMethod("startServiceAsUser", Intent::class.java, UserHandle::class.java)
            method.isAccessible = true
            method.invoke(context, intent, newUserHandle(userHandle))
        } catch (t: Throwable) {
            if (t is InvocationTargetException) {
                context.startService(intent)
            } else {
                t.printStackTrace()
            }
        }
    }

    fun startActivityAsUser(activity: Activity, intent: Intent, userHandle: String) {
        try {
            val clz = activity::class.java
            val method =
                clz.getMethod("startActivityAsUser", Intent::class.java, UserHandle::class.java)
            method.isAccessible = true
            method.invoke(activity, intent, newUserHandle(userHandle))
        } catch (t: Throwable) {
            if (t is InvocationTargetException) {
                activity.startActivity(intent)
            } else {
                t.printStackTrace()
            }
        }
    }

    fun getBroadcastAsUser(
        context: Context?,
        requestCode: Int,
        intent: Intent,
        flags: Int,
        userHandle: String
    ): PendingIntent? {
        try {
            val clz = PendingIntent::class.java
            val method =
                clz.getMethod(
                    "getBroadcastAsUser",
                    Context::class.java,
                    Int::class.java,
                    Intent::class.java,
                    Int::class.java,
                    UserHandle::class.java
                )
            method.isAccessible = true
            return method.invoke(
                null,
                context,
                requestCode,
                intent,
                flags,
                newUserHandle(userHandle)
            ) as? PendingIntent
        } catch (t: Throwable) {
            if (t is InvocationTargetException) {
                return PendingIntent.getBroadcast(context, requestCode, intent, flags)
            } else {
                t.printStackTrace()
            }
        }
        return null
    }

    fun startForegroundServiceAsUser(context: Context, intent: Intent, userHandle: String) {
        try {
            val clz = context::class.java
            val method =
                clz.getMethod(
                    "startForegroundServiceAsUser",
                    Intent::class.java,
                    UserHandle::class.java
                )
            method.isAccessible = true
            method.invoke(context, intent, newUserHandle(userHandle))
        } catch (t: Throwable) {
            if (t is InvocationTargetException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                }
            } else {
                t.printStackTrace()
            }
        }
    }

    private fun newUserHandle(userHandle: String): UserHandle? {
        try {
            val clz = UserHandle::class.java
            val all = clz.getDeclaredField(userHandle)
            all.isAccessible = true
            return all.get(null) as UserHandle
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return null
    }

}