package com.iflytek.cyber.iot.show.core.utils

import android.app.Instrumentation
import android.view.KeyEvent

object NavigationUtils {
    fun clickBack(
        onSuccess: (() -> Unit)? = null,
        onFailed: ((e: Exception) -> Unit)? = null
    ) {
        Thread {
            try {
                val instrumentation = Instrumentation()
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                onSuccess?.invoke()
            } catch (e: Exception) {
                e.printStackTrace()
                onFailed?.invoke(e)
            }
        }.start()
    }
}