package com.iflytek.cyber.iot.show.core.impl.launcher

import android.content.Context
import android.content.Intent
import com.iflytek.cyber.evs.sdk.agent.Launcher
import com.iflytek.cyber.iot.show.core.EvsLauncherActivity
import com.iflytek.cyber.iot.show.core.utils.NavigationUtils
import java.lang.Exception
import java.lang.ref.SoftReference

class EvsLauncher private constructor() : Launcher() {
    companion object {
        private var instance: EvsLauncher? = null

        fun get(): EvsLauncher {
            instance?.let {
                return it
            } ?: run {
                val launcher = EvsLauncher()
                instance = launcher
                return launcher
            }
        }
    }

    private var contextRef: SoftReference<Context>? = null

    var pageScrollCallback: PageScrollCallback? = null

    fun init(context: Context) {
        contextRef = SoftReference(context)
    }

    override fun startActivity(page: String, callback: ExecuteCallback): Boolean {
        try {
            if (page == PAGE_NEXT || page == PAGE_PREVIOUS) {
                val result =
                    when (page) {
                        PAGE_NEXT -> pageScrollCallback?.onScrollToNext()
                        PAGE_PREVIOUS -> pageScrollCallback?.onScrollToPrevious()
                        else -> null
                    }
                if (result?.isSuccessful == true) {
                    callback.onSuccess(result.feedback)
                } else {
                    callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, result?.feedback)
                }
            } else {
                contextRef?.get()?.let { context ->
                    val intent = Intent(context, EvsLauncherActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    intent.action = EvsLauncherActivity.ACTION_LAUNCHER_CONTROL
                    intent.putExtra(EvsLauncherActivity.EXTRA_PAGE, page)
                    context.startActivity(intent)
                    if (page == PAGE_ALARMS) {
                        callback.onSuccess("")
                    } else {
                        callback.onSuccess()
                    }
                } ?: run {
                    callback.onFailed(FAILURE_CODE_NOT_FOUND_PAGE, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, null)
        }
        return true
    }

    override fun back(callback: ExecuteCallback) {
        NavigationUtils.clickBack({
            callback.onSuccess()
        }, {
            callback.onFailed(FAILURE_CODE_INTERNAL_ERROR, null)
        })
    }

    interface PageScrollCallback {
        fun onScrollToPrevious(): ScrollResult
        fun onScrollToNext(): ScrollResult
    }

    data class ScrollResult(
        val isSuccessful: Boolean,
        val feedback: String? = null
    )
}