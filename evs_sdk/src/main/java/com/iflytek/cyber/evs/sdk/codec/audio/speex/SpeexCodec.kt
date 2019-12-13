package com.iflytek.cyber.evs.sdk.codec.audio.speex

import com.iflytek.cyber.evs.sdk.codec.audio.AudioCodec
import com.iflytek.cyber.evs.sdk.utils.Log

/**
 * Speex编解码类。
 *
 * 构造函数。
 * @param codec 指定编码还是解码，取值：CODEC_ENCODE，CODEC_DECODE
 * @param sampleRate 采样率
 * @param quality 质量，取值：1-10
 */
class SpeexCodec(codec: Int, sampleRate: Int, mode: Int, quality: Int): AudioCodec(codec) {
    companion object {
        const val TAG = "SpeexCodec"

        const val MODE_NB = 1   // 8bit编码
        const val MODE_WB = 2   // 16bit编码
    }

    init {
        System.loadLibrary("speex")

        this.sampleRate = sampleRate
        this.mode = mode
        this.quality = quality

        val ret = init_native(codeMode, sampleRate, mode, quality)
        if (ret != 0) {
            Log.d(TAG, "init failed")
        }
    }

    private var handle_state: ByteArray? = null
    private var handle_speex_bits: ByteArray? = null

    private fun setHandles(state: ByteArray?, bits: ByteArray?) {
        handle_state = state
        handle_speex_bits = bits
    }

    /**
     * 初始化本地对象。
     * @param codec 编码还是解码器
     * @param sampleRate 采样率
     * @param mode 模式，取值：MODE_NB，MODE_WB
     * @param quality 质量，取值：1-10
     * @return 成功返回0，失败返回-1
     */
    private external fun init_native(codec: Int, sampleRate: Int, mode: Int, quality: Int): Int

    /**
     * 编码。
     * @param state native指针
     * @param speex_bits native指针
     * @param data 原始数据
     * @param dataLen 原始数据长度
     * @param buffer 编码缓存
     * @param bufferLen 缓存长度
     * @return 编码数据长度，-1表示失败
     */
    private external fun encode_native(state: ByteArray?, speex_bits: ByteArray?, data: ByteArray, dataLen: Int,
                            buffer: ByteArray, bufferLen: Int): Int

    /**
     * 解码。
     * @param state native指针
     * @param speex_bits native指针
     * @param data 已编码数据
     * @param dataLen 数据长度
     * @param buffer 解码缓存
     * @param bufferLen 缓存长度
     * @param mode 模式，取值：MODE_NB，MODE_WB
     * @param quality 压缩质量
     * @return 解码数据长度，-1表示失败
     */
    private external fun decode_native(state: ByteArray?, speex_bits: ByteArray?, data: ByteArray, dataLen: Int,
                            buffer: ByteArray, bufferLen: Int, mode: Int, quality: Int): Int

    /**
     * 销毁本地对象。
     * @param codec 编码还是解码器
     * @param handle_state state指针
     * @param handle_speex_bits speex_bits指针
     */
    private external fun destroy_native(codec: Int, handle_state: ByteArray?, handle_speex_bits: ByteArray?)

    /**
     * 编码。
     * @param data 原始音频
     * @param dataLen 音频长度，必须为20ms音频长度的整数倍
     * @param buffer 编码缓存区
     * @param bufferLen 缓存区长度
     * @return 编码后的长度，负值表示出错
     */
    override fun encode(data: ByteArray, dataLen: Int, buffer: ByteArray, bufferLen: Int): Int {
        if (isDecoder()) {
            Log.e(TAG, "decoder cannot be used to encode")
            return -1
        }

        if (handle_state == null) {
            Log.e(TAG, "not inited or destroyed")
            return -1
        }

        val ret = encode_native(handle_state, handle_speex_bits, data, dataLen, buffer, bufferLen)
        if (ret < 0) {
            Log.e(TAG, "encode error, ret=$ret")
        }

        return ret
    }

    /**
     * 解码。
     * @param data speex音频
     * @param dataLen 音频长度
     * @param buffer 解码缓存区
     * @param bufferLen 缓存区长度
     * @return 解后的长度，负值表示出错
     */
    override fun decode(data: ByteArray, dataLen: Int, buffer: ByteArray, bufferLen: Int): Int {
        if (isEncoder()) {
            Log.e(TAG, "encoder cannot be used to decode")
            return -1
        }

        if (handle_state == null) {
            Log.e(TAG, "not inited or destroyed")
            return -1
        }

        val ret = decode_native(handle_state, handle_speex_bits, data, dataLen, buffer, bufferLen, mode, quality)
        if (ret < 0) {
            Log.e(TAG, "decode error, ret=$ret")
        }

        return ret
    }

    override fun destroy() {
        if (handle_state != null) {
            destroy_native(codeMode, handle_state, handle_speex_bits)
            handle_state = null
            handle_speex_bits = null
        }
    }

    protected fun finalize() {
        destroy()
    }
}