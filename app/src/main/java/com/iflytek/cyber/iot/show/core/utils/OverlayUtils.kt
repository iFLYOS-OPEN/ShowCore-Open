package com.iflytek.cyber.iot.show.core.utils

import android.app.AppOpsManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi


object OverlayUtils {
    private const val TAG = "OverlayUtils"

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            hasPermissionBelowMarshmallow(context)
        }
    }

    fun hasPermissionOnActivityResult(context: Context): Boolean {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            return hasPermissionForO(context)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            hasPermissionBelowMarshmallow(context)
        }
    }

    fun hasPermissionAwait(context: Context, callback: (result: Boolean) -> Unit) {
        callback.invoke(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                hasPermissionBelowMarshmallow(context)
            }
        )
    }

    fun hasPermissionOnActivityResultAwait(
        context: Context, callback: (result: Boolean) -> Unit) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val handler = Handler()
                Thread {
                    try {
                        Thread.sleep(500)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    handler.post {
                        callback.invoke(Settings.canDrawOverlays(context))
                    }
                }.start()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                callback.invoke(Settings.canDrawOverlays(context))
            else ->
                callback.invoke(hasPermissionBelowMarshmallow(context))
        }
    }

    /**
     * 6.0以下判断是否有权限
     * 理论上6.0以上才需处理权限，但有的国内rom在6.0以下就添加了权限
     * 其实此方式也可以用于判断6.0以上版本，只不过有更简单的canDrawOverlays代替
     */
    private fun hasPermissionBelowMarshmallow(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val dispatchMethod = AppOpsManager::class.java.getMethod("checkOp", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
            //AppOpsManager.OP_SYSTEM_ALERT_WINDOW = 24
            AppOpsManager.MODE_ALLOWED == dispatchMethod.invoke(
                manager, 24, Binder.getCallingUid(), context.applicationContext.packageName) as Int
        } catch (e: Exception) {
            false
        }
    }


    /**
     * 用于判断8.0时是否有权限，仅用于OnActivityResult
     * 针对8.0官方bug:在用户授予权限后Settings.canDrawOverlays或checkOp方法判断仍然返回false
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun hasPermissionForO(context: Context): Boolean {
        try {
            val mgr = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val viewToAdd = View(context)
            val params = WindowManager.LayoutParams(0, 0,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT)
            viewToAdd.layoutParams = params
            mgr.addView(viewToAdd, params)
            mgr.removeView(viewToAdd)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "hasPermissionForO e:$e")
        }

        return false
    }
}