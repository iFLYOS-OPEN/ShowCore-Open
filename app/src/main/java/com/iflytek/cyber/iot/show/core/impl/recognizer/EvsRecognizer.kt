package com.iflytek.cyber.iot.show.core.impl.recognizer

import android.content.Context
import com.iflytek.cyber.evs.sdk.agent.Recognizer
import com.iflytek.cyber.iot.show.core.record.GlobalRecorder
import com.iflytek.cyber.iot.show.core.utils.ConfigUtils
import okio.BufferedSink
import okio.BufferedSource
import okio.Pipe
import okio.buffer
import java.io.InterruptedIOException
import java.lang.ref.SoftReference

class EvsRecognizer(context: Context) : Recognizer(), GlobalRecorder.Observer {
    private var sink: BufferedSink? = null
    private var source: BufferedSource? = null
    private var isCapturing = false
    private val contextRef = SoftReference(context)

    init {
        GlobalRecorder.registerObserver(this)

        profile =
            when (ConfigUtils.getString(ConfigUtils.KEY_RECOGNIZER_PROFILE, null)) {
                Profile.CloseTalk.toString() -> {
                    Profile.CloseTalk
                }
                else -> {
                    Profile.FarField
                }
            }

        val pipe = Pipe(2 * 1024 * 1024)
        sink = pipe.sink.buffer()
        source = pipe.source.buffer()
    }


    override fun onAudioData(array: ByteArray, offset: Int, length: Int) {
        if (length > 0 && isCapturing)
            sink?.write(array, offset, length)
    }

    override fun onWakeUp(angle: Int, beam: Int, params: String?) {
        // ignore
    }

    override fun readBytes(byteArray: ByteArray, length: Int): Int {
        try {
            val data = source?.readByteArray(length.toLong())
            if (data?.isNotEmpty() == true) {
                for (i in data.indices) {
                    byteArray[i] = data[i]
                }
                return data.size
            }
        } catch (e: InterruptedIOException) {
            // ignore
        }
        return -1
    }

    override fun startRecording() {
        isCapturing = true
    }

    override fun stopRecording() {
        isCapturing = false
    }

    override fun isRecording(): Boolean {
        return isCapturing
    }

    override fun isSupportBackgroundRecognize(): Boolean {
        return ConfigUtils.getBoolean(ConfigUtils.KEY_BACKGROUND_RECOGNIZE, false)
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            sink?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            sink = null
            source = null
        }
    }
}