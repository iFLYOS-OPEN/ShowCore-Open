package com.iflytek.cyber.evs.sdk.agent

import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.annotation.CallSuper
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.RequestCallback
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.codec.audio.AudioCodec
import com.iflytek.cyber.evs.sdk.codec.audio.opus.OpusCodec
import com.iflytek.cyber.evs.sdk.codec.audio.speex.SpeexCodec
import com.iflytek.cyber.evs.sdk.focus.AudioFocusManager
import com.iflytek.cyber.evs.sdk.model.Constant
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.evs.sdk.socket.SocketManager
import com.iflytek.cyber.evs.sdk.utils.Log
import java.lang.System
import java.util.Timer
import java.util.TimerTask

/**
 * 语音识别模块。详细介绍见https://doc.iflyos.cn/device/evs/reference/recognizer.html#%E8%AF%AD%E9%9F%B3%E8%AF%86%E5%88%AB%E5%99%A8
 */
abstract class Recognizer {
    val version: String
        get() {
            return if (isSupportBackgroundRecognize())
                "1.1"
            else
                "1.0"
        }

    companion object {
        private const val TAG = "Recognizer"

        const val NAME_TEXT_IN = "${Constant.NAMESPACE_RECOGNIZER}.text_in"
        const val NAME_AUDIO_IN = "${Constant.NAMESPACE_RECOGNIZER}.audio_in"
        const val NAME_EXPECT_REPLY = "${Constant.NAMESPACE_RECOGNIZER}.expect_reply"
        const val NAME_STOP_CAPTURE = "${Constant.NAMESPACE_RECOGNIZER}.stop_capture"
        const val NAME_INTERMEDIATE_TEXT = "${Constant.NAMESPACE_RECOGNIZER}.intermediate_text"

        internal const val KEY_QUERY = "query"
        internal const val KEY_TEXT = "text"
        internal const val KEY_PROFILE = "profile"
        internal const val KEY_FORMAT = "format"
        internal const val KEY_REPLY_KEY = "reply_key"
        internal const val KEY_ENABLE_VAD = "enable_vad"
        internal const val KEY_WAKE_UP = "iflyos_wake_up"
        internal const val KEY_BACKGROUND_RECOGNIZE = "background_recognize"
        internal const val KEY_TIMEOUT = "timeout"

        /**
         * 获取EVS支持的SampleRate，当前只支持16000Hz。
         */
        fun getSampleRateInHz() = 16000

        /**
         * 获取EVS支持的AudioFormat，当前只支持16bit PCM编码。
         */
        fun getAudioFormatEncoding() = AudioFormat.ENCODING_PCM_16BIT

        /**
         * 获取EVS支持的通道数，当前只支持单通道。
         */
        fun getAudioChannel() = AudioFormat.CHANNEL_IN_MONO
    }

    private var recorderThread: RecorderThread? = null

    @Suppress("MemberVisibilityCanBePrivate")
    var profile = Profile.CloseTalk
    @Suppress("MemberVisibilityCanBePrivate")
    var isSupportOpus = false
    @Suppress("MemberVisibilityCanBePrivate")
    var isLocalVad = false
    var isPreventExpectReply = false

    // 音频编码格式
    var audioCodecFormat = AudioCodecFormat.AUDIO_L16_RATE_16000_CHANNELS_1

    var isBackgroundRecognize = false
        set(value) {
            try {
                callback?.onBackgroundRecognizeStateChanged(value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            field = value
        }


    private var callback: RecognizerCallback? = null

    private var audioCodec: AudioCodec? = null

    /**
     * 读取录音数据，SDK调用[startRecording]后就会开始尝试调用此函数读取录音数据。
     * @param byteArray 音频数据
     * @param length 一次读取的长度
     * @return 实际读取的长度
     */
    abstract fun readBytes(byteArray: ByteArray, length: Int): Int

    /**
     * 外部写入音频。
     * @param byteArray 音频数据
     * @param length 一次写入的长度（当需要压缩编码时，必须为640的整数倍）
     */
    fun write(byteArray: ByteArray, length: Int) {
        if (isWrittenByYourself() && length > 0) {
            val audioCodec = getAudioCodec()

            if (audioCodec != null) {
                val encBuffer = ByteArray(length)
                val encLen = audioCodec.encode(byteArray, length, encBuffer, length)

                if (encLen > 0) {
                    RequestManager.sendBinary(encBuffer.copyOf(encLen))
                } else {
                    Log.e(TAG, "encoded error, ret=$encLen")
                }
            } else {
                RequestManager.sendBinary(byteArray.copyOf(length))
            }
        }
    }

    /**
     * 开始录音。
     */
    abstract fun startRecording()

    /**
     * 停止录音。
     */
    abstract fun stopRecording()

    /**
     * 识别文本实时返回回调。
     * @param text 识别文本
     */
    @CallSuper
    open fun onIntermediateText(text: String) {
        callback?.onIntermediateText(text)
    }

    /**
     * 设置识别回调。
     */
    @Suppress("unused")
    fun setRecognizerCallback(callback: RecognizerCallback) {
        this.callback = callback
    }

    /**
     * 移除识别回调。
     */
    @Suppress("unused")
    fun removeRecognizerCallback() {
        this.callback = null
    }

    open fun isSupportBackgroundRecognize(): Boolean {
        return false
    }

    /**
     * 获取音频输入源，覆盖以返回自定义的输入源。
     */
    open fun getAudioSource(): Int {
        return MediaRecorder.AudioSource.DEFAULT
    }

    /**
     * 销毁识别模块。
     */
    open fun onDestroy() {
        audioCodec?.destroy()
        audioCodec = null
    }

    /**
     * 取消语音识别，将不会有结果返回。
     */
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun requestCancel() {
        if (isRecording()) {
            stopRecording()

            isBackgroundRecognize = false

            recorderThread?.let {
                RequestManager.sendRawString(RequestManager.MESSAGE_CANCEL)
                it.stopNow()
            }
            recorderThread = null
        }

        AudioFocusManager.requestAbandon(
            AudioFocusManager.CHANNEL_INPUT,
            AudioFocusManager.TYPE_RECOGNIZE
        )
    }

    open fun isWrittenByYourself() = false

    /**
     * 结束语音输入。
     */
    @Suppress("MemberVisibilityCanBePrivate", "unused")
    fun requestEnd() {
        if (isRecording()) {
            stopRecording()

            isBackgroundRecognize = false

            recorderThread?.let {
                RequestManager.sendRawString(RequestManager.MESSAGE_END)
                it.stopNow()
            }
            recorderThread = null
        }

        if (!isBackgroundRecognize) {
            Handler(Looper.getMainLooper()).post {
                AudioFocusManager.requestAbandon(
                    AudioFocusManager.CHANNEL_INPUT,
                    AudioFocusManager.TYPE_RECOGNIZE
                )
            }
        }

        isBackgroundRecognize = false
    }

    private fun generatePayload(replyKey: String? = null): JSONObject {
        val payload = JSONObject()
        if (profile == Profile.CloseTalk) {
            payload[KEY_PROFILE] = "CLOSE_TALK"
        } else {
            payload[KEY_PROFILE] = "FAR_FIELD"
        }

//        payload[KEY_FORMAT] = "AUDIO_L16_RATE_16000_CHANNELS_1"

        when (audioCodecFormat) {
            AudioCodecFormat.AUDIO_L16_RATE_16000_CHANNELS_1 -> {
                payload[KEY_FORMAT] = "AUDIO_L16_RATE_16000_CHANNELS_1"
            }
            AudioCodecFormat.SPEEX_WB_QUALITY_9 -> {
                payload[KEY_FORMAT] = "SPEEX_WB_QUALITY_9"
            }
            AudioCodecFormat.OPUS -> {
                payload[KEY_FORMAT] = "OPUS"
            }
        }

        if (!replyKey.isNullOrEmpty()) {
            payload[KEY_REPLY_KEY] = replyKey
        }
        payload[KEY_ENABLE_VAD] = !isLocalVad

        return payload
    }

    /**
     * 云端追问，期待回复。
     * @param replyKey 关联追问上下文的key
     * @param requestCallback 回调接口
     */
    fun expectReply(
        replyKey: String,
        requestCallback: RequestCallback? = null
    ) {
        if (isRecording()) {
            requestCancel()
        }

        if (!isPreventExpectReply) {
            val payload = generatePayload(replyKey)

            RequestManager.sendRequest(NAME_AUDIO_IN, payload, object : RequestCallback {
                override fun onResult(result: Result) {
                    if (result.isSuccessful) {
                        recorderThread?.stopNow()

                        recorderThread = RecorderThread(this@Recognizer, true)
                        recorderThread?.start()

                        startRecording()

                        if (!isBackgroundRecognize) {
                            AudioFocusManager.requestActive(
                                AudioFocusManager.CHANNEL_INPUT,
                                AudioFocusManager.TYPE_RECOGNIZE
                            )
                        }
                    }

                    requestCallback?.onResult(result)
                }
            }, true)
        }
    }

    /**
     * 开始发送音频。
     * @param replyKey 关联追问上下文的key
     * @param wakeUpData 唤醒结果
     * @param requestCallback 结果回调
     */
    fun sendAudioIn(
        replyKey: String?,
        wakeUpData: String? = null,
        requestCallback: RequestCallback? = null
    ) {
        if (isRecording()) {
            requestCancel()
        }

        val payload = generatePayload()

        if (!replyKey.isNullOrEmpty()) {
            payload[KEY_REPLY_KEY] = replyKey
        }

        wakeUpData?.let {
            val json = JSON.parseObject(it)
            payload[KEY_WAKE_UP] = json
        }

        RequestManager.sendRequest(NAME_AUDIO_IN, payload, object : RequestCallback {
            override fun onResult(result: Result) {
                Log.d("Recognizer", "sendAudioIn result: $result")
                if (result.isSuccessful) {
                    recorderThread?.stopNow()

                    recorderThread = RecorderThread(this@Recognizer, false)
                    recorderThread?.start()

                    startRecording()

                    AudioFocusManager.requestActive(
                        AudioFocusManager.CHANNEL_INPUT,
                        AudioFocusManager.TYPE_RECOGNIZE
                    )
                }

                requestCallback?.onResult(result)
            }

        }, true)
    }

    /**
     * 发送文本进行语义理解。
     * @param query 文本内容
     * @param replyKey 关联追问上下文的key
     * @param requestCallback 结果回调
     */
    fun sendTextIn(
        query: String,
        replyKey: String? = null,
        requestCallback: RequestCallback? = null
    ) {
        if (query.isNotEmpty()) {
            val payload = JSONObject()
            payload[KEY_QUERY] = query

            if (!replyKey.isNullOrEmpty()) {
                payload[KEY_REPLY_KEY] = replyKey
            }

            RequestManager.sendRequest(NAME_TEXT_IN, payload, requestCallback, true)
        }
    }

    /**
     * 停止采集音频。
     */
    fun stopCapture() {
        if (isRecording())
            requestEnd()
    }

    /**
     * 是否正在录音。
     */
    open fun isRecording(): Boolean {
        return null != recorderThread
    }

    fun setCodecFormat(format: AudioCodecFormat): Boolean {
        if (isRecording()) {
            return false
        }

        if (audioCodecFormat != format) {
            audioCodec?.destroy()
            audioCodec = null
        }
        audioCodecFormat = format

        return true
    }

    fun getAudioCodec(): AudioCodec? {
        if (audioCodec == null) {
            when (audioCodecFormat) {
                AudioCodecFormat.SPEEX_WB_QUALITY_9 -> {
                    audioCodec = SpeexCodec(
                        AudioCodec.CODEC_ENCODE, getSampleRateInHz(),
                        SpeexCodec.MODE_WB, 9
                    )
                    audioCodec?.formatName = "SPEEX_WB_QUALITY_9"
                }
                AudioCodecFormat.OPUS -> {
                    audioCodec = OpusCodec(AudioCodec.CODEC_ENCODE, getSampleRateInHz(), 32000)
                    audioCodec?.formatName = "OPUS"
                }
                else -> {
                }
            }
        }
        return audioCodec
    }

    private class RecorderThread(val recognizer: Recognizer, isExpectReply: Boolean) : Thread() {
        private var needProcess = true

        init {
            recognizer.callback?.onRecognizeStarted(isExpectReply)
        }

        override fun run() {
            super.run()

            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

            Log.v(TAG, "start new Recorder($id)")
            Log.d(TAG, "AudioCodec, format=${recognizer.audioCodecFormat}")

            val audioCodec = recognizer.getAudioCodec()
            val array = ByteArray(1280)

//            val audioDec = OpusCodec(AudioCodec.CODEC_DECODE, getSampleRateInHz(), 32000)

            while (needProcess) {
                try {
                    if (!recognizer.isWrittenByYourself()) {
                        val readSize = recognizer.readBytes(array, array.size)
                        if (readSize > 0) {
                            if (audioCodec != null) {
                                val encArray = ByteArray(readSize)
                                val encLen = audioCodec.encode(array, readSize, encArray, readSize)

                                if (encLen > 0) {
//                                    val decLen = audioDec.decode(encArray, encLen, array, array.size)
//                                    if (decLen > 0) {
//                                        RequestManager.sendBinary(array.copyOf(decLen))
//                                    }
//
//                                    Log.d(TAG, "encLen=$encLen, decLen=$decLen")

                                    RequestManager.sendBinary(encArray.copyOf(encLen))
                                } else {
                                    Log.e(TAG, "encoded error, ret=$encLen")
                                }
                            } else {
                                RequestManager.sendBinary(array.copyOf(readSize))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

//            audioDec.destroy()

            Log.v(TAG, "Recorder($id) had end")
        }

        fun stopNow() {
            needProcess = false

            recognizer.callback?.onRecognizeStopped()

            interrupt()
        }
    }

    /**
     * 识别回调。
     */
    interface RecognizerCallback {
        fun onRecognizeStarted(isExpectReply: Boolean)
        fun onRecognizeStopped()
        fun onIntermediateText(text: String)
        fun onBackgroundRecognizeStateChanged(isBackgroundRecognize: Boolean)
    }

    /**
     * 交互类型。
     */
    enum class Profile(val value: String) {
        CloseTalk("CLOSE_TALK"),      // 近场
        FarField("FAR_FIELD")        // 远场
    }

    /**
     * 音频编码格式。
     */
    enum class AudioCodecFormat {
        AUDIO_L16_RATE_16000_CHANNELS_1,        // 原始音频，不压缩
        SPEEX_WB_QUALITY_9,                     // speex双字节编码质量为9，1280字节PCM压缩到212字节
        OPUS                                    // opus 32000码率，固定编码，1280字节PCM压缩到160字节
    }
}