package com.iflytek.cyber.evs.sdk.utils

import com.iflytek.cyber.evs.sdk.BuildConfig

internal object Log {
    var isDebug = BuildConfig.DEBUG

    private const val MAX_LENGTH = 600

    fun w(tag: String, log: String, throwable: Throwable? = null) {
        if (throwable != null)
            android.util.Log.w(tag, log, throwable)
        else {
            var index = 0
            while (index < log.length) {
                val sub = if (index + MAX_LENGTH < log.length)
                    log.substring(index, index + MAX_LENGTH)
                else
                    log.substring(index)
                android.util.Log.w(tag, "${if (index != 0) "    " else ""}$sub")
                index += MAX_LENGTH
            }
        }
    }

    fun d(tag: String, log: String, throwable: Throwable? = null) {
        if (!isDebug) return
        if (throwable != null)
            android.util.Log.d(tag, log, throwable)
        else {
            var index = 0
            while (index < log.length) {
                val sub = if (index + MAX_LENGTH < log.length)
                    log.substring(index, index + MAX_LENGTH)
                else
                    log.substring(index)
                android.util.Log.d(tag, "${if (index != 0) "    " else ""}$sub")
                index += MAX_LENGTH
            }
        }
    }

    fun i(tag: String, log: String, throwable: Throwable? = null) {
        if (!isDebug) return
        if (throwable != null)
            android.util.Log.i(tag, log, throwable)
        else {
            var index = 0
            while (index < log.length) {
                val sub = if (index + MAX_LENGTH < log.length)
                    log.substring(index, index + MAX_LENGTH)
                else
                    log.substring(index)
                android.util.Log.i(tag, "${if (index != 0) "    " else ""}$sub")
                index += MAX_LENGTH
            }
        }
    }

    fun e(tag: String, log: String, throwable: Throwable? = null) {
        if (throwable != null)
            android.util.Log.e(tag, log, throwable)
        else {
            var index = 0
            while (index < log.length) {
                val sub = if (index + MAX_LENGTH < log.length)
                    log.substring(index, index + MAX_LENGTH)
                else
                    log.substring(index)
                android.util.Log.e(tag, "${if (index != 0) "    " else ""}$sub")
                index += MAX_LENGTH
            }
        }
    }

    fun v(tag: String, log: String, throwable: Throwable? = null) {
        if (!isDebug) return
        if (throwable != null)
            android.util.Log.v(tag, log, throwable)
        else {
            var index = 0
            while (index < log.length) {
                val sub = if (index + MAX_LENGTH < log.length)
                    log.substring(index, index + MAX_LENGTH)
                else
                    log.substring(index)
                android.util.Log.v(tag, "${if (index != 0) "    " else ""}$sub")
                index += MAX_LENGTH
            }
        }
    }
}