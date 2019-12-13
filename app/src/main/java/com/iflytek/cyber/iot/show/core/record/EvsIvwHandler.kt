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

package com.iflytek.cyber.iot.show.core.record

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.ivw.IVWEngine
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class EvsIvwHandler(context: Context, private val listener: IvwHandlerListener?) {
    private var mHandlerThread: HandlerThread? = null
    private var mWakeUpHandler: WakeUpHandler? = null

    private val mWakeUpEnable: Boolean = true
    private val mWakeResPath: String

    private val mObj = Any()

    private var mIvwEngine: IVWEngine? = null
    private var mIvwListener =
        IVWEngine.IVWListener { angle, channel, power, CMScore, beam, param1, param2 ->
            Log.v(TAG, param1)
            listener?.onWakeUp(angle.toInt(), beam.toInt(), param1)
        }

    init {
        val externalCache = context.externalCacheDir
        val customWakeResFile = File("$externalCache/wake_up_res_custom.bin") // 自定义唤醒词
        if (!customWakeResFile.exists()) {
            val wakeUpResFile = File("$externalCache/wake_up_res.bin")
            wakeUpResFile.createNewFile()

            val inputStream = context.assets.open("wakeup/lan2-xiao3-fei1.bin")
            val outputStream = FileOutputStream(wakeUpResFile)
            val buffer = ByteArray(1024)
            var byteCount = inputStream.read(buffer)
            while (byteCount != -1) {
                outputStream.write(buffer, 0, byteCount)
                byteCount = inputStream.read(buffer)
            }
            outputStream.flush()
            inputStream.close()
            outputStream.close()

            mWakeResPath = wakeUpResFile.path
        } else {
            mWakeResPath = customWakeResFile.path
        }

        // if wakeup enable, then new some object
        if (isWakeUpEnable()) {
            Thread {
                mIvwEngine = IVWEngine.createInstance(mWakeResPath, mIvwListener)
            }.start()

            mHandlerThread = HandlerThread("IVW_THREAD", Thread.MAX_PRIORITY)
            mHandlerThread?.let {
                it.start()
                mWakeUpHandler = WakeUpHandler(it.looper)
            }
        }
    }

    /**
     * Check wakeup enable
     */
    fun isWakeUpEnable(): Boolean {
        return mWakeUpEnable
            && !TextUtils.isEmpty(mWakeResPath)
            && File(mWakeResPath).exists()
    }

    /**
     * Write audio to ivw engine
     */
    fun write(audio: ByteArray?, len: Int) {
        synchronized(mObj) {
            mWakeUpHandler?.post {
                audio?.let {
                    mIvwEngine?.writeAudio(it, len)
                }
            }
        }
    }

    private fun stopIvw() {
        mIvwEngine?.destroy()
    }

    fun release() {
        stopIvw()
        mIvwEngine?.destroy()
    }

    inner class WakeUpHandler(lopper: Looper) : Handler(lopper)

    interface IvwHandlerListener {
        fun onWakeUp(angle: Int, beam: Int, params: String?)
    }

    companion object {
        const val TAG = "EvsIvwHandler"
    }
}