/*
 * Copyright (C) 2019 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.iflytek.cyber.iot.show.core.ivw

import java.lang.Exception

/**
 * 唤醒引擎封闭类。
 */
@Suppress("FunctionName")
class IVWEngine(resPath: String, listener: IVWListener, isLogOn: Boolean? = null) {

    private var mResPath: String? = resPath

    private var mListener: IVWListener? = listener

    private var mEngineContextHandle: Long = 0

    val version: String
        get() = jni_get_version(mEngineContextHandle)

    interface IVWListener {
        fun onWakeup(result: String)
    }

    init {
        if (initialed) {
            isLogOn?.let {
                jni_set_log(it)
            }
            mEngineContextHandle = jni_create(resPath, "wakeupCb")
        }
    }

    //    /**
    //     * 设置门限等级。已废弃
    //     *
    //     * @param level
    //     * @return
    //     */
    //    public int setCMLevel(int level) {
    //        return jni_set_cmlevel(mEngineContextHandle, level);
    //    }
    //
    //    /**
    //     * 设置唤醒词门限。已废弃
    //     *
    //     * @param ncm 门限设置，例："0:1250,1:1300"，多个唤醒词用逗号隔开
    //     * @return
    //     */
    //    public int setKeywordNCM(String ncm) {
    //        return jni_set_keywordncm(mEngineContextHandle, ncm);
    //    }

    fun start(): Int {
        if (initialed)
            return jni_start(mEngineContextHandle)
        return -1
    }

    fun writeAudio(buffer: ByteArray, len: Int): Int {
        if (initialed)
            return jni_write(mEngineContextHandle, buffer, len)
        return -1
    }

    fun stop(): Int {
        if (initialed)
            return jni_stop(mEngineContextHandle)
        return -1
    }

    fun destroy() {
        if (initialed) {
            jni_destroy(mEngineContextHandle)
            mEngineContextHandle = INVALID_HANDLE.toLong()
        }
    }

    fun setLogOn(isOn: Boolean) {
        if (initialed)
            jni_set_log(isOn)
    }

    private fun wakeupCb(result: String) {
        mListener?.onWakeup(result)
    }

    private external fun jni_create(resPath: String, wakeupCbName: String): Long

    @Suppress("unused")
    companion object {
        private var initialed = false

        init {
            try {
                System.loadLibrary("ivw-jni")
                initialed = true
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JvmStatic
        private val INVALID_HANDLE = 0

        @JvmStatic
        val CMLEVEL_LOWEST = 0

        @JvmStatic
        val CMLEVEL_LOWER = 1

        @JvmStatic
        val CMLEVEL_LOW = 2

        @JvmStatic
        val CMLEVEL_NORMAL = 3

        @JvmStatic
        val CMLEVEL_HIGH = 4

        @JvmStatic
        val CMLEVEL_HIGHER = 5

        @JvmStatic
        val CMLEVEL_HIGHEST = 6

        @JvmStatic
        private external fun jni_destroy(handle: Long)

        @JvmStatic
        private external fun jni_set_cmlevel(handle: Long, level: Int): Int

        @JvmStatic
        private external fun jni_set_keywordncm(handle: Long, ncm: String): Int

        @JvmStatic
        private external fun jni_start(handle: Long): Int

        @JvmStatic
        private external fun jni_write(handle: Long, buffer: ByteArray, size: Int): Int

        @JvmStatic
        private external fun jni_stop(handle: Long): Int

        @JvmStatic
        private external fun jni_set_log(isOn: Boolean)

        @JvmStatic
        private external fun jni_get_version(handle: Long): String
    }
}
