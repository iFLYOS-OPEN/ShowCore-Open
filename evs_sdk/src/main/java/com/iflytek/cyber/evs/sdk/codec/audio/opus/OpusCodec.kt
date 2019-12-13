package com.iflytek.cyber.evs.sdk.codec.audio.opus

import com.iflytek.cyber.evs.sdk.codec.audio.AudioCodec
import com.iflytek.cyber.evs.sdk.utils.Log

/**
 * Opus编解码类，暂时只支持32bit 16000Hz的PCM。
 *
 * 构造函数。
 * @param codec 指定编码还是解码，取值：CODEC_ENCODE，CODEC_DECODE（暂未支持）
 * @param sampleRate 采样率，暂时只支持16000
 * @param bitRate 比特率，取值：500-512000，云端暂时只支持32000
 */
class OpusCodec(codec: Int, sampleRate: Int, bitRate: Int) : AudioCodec(codec) {
    companion object {
        init {
            System.loadLibrary("opus")
        }

        private val TAG = "OpusCodec"
    }

    private val len_20ms_bytes: Int
    private var handle_codec: ByteArray? = null

    private fun setHandles(handle: ByteArray) {
        handle_codec = handle
    }

    init {
        this.sampleRate = sampleRate
        this.quality = bitRate
        len_20ms_bytes = sampleRate / 100 * 2 * 2

        val ret = init_native(codec, sampleRate, bitRate)
        if (ret != 0) {
            Log.e(TAG, "init failed", null)
        }
    }

    private external fun init_native(codec: Int, sampleRate: Int, bitRate: Int): Int

    /**
     * 编码。
     * @param handle 编码器指针
     * @param data 原始音频数据
     * @param dataLen 数据长度
     * @param buffer 编码数据缓存
     * @param bufferLen 缓存长度
     * @param packetSize 编码数据包长度
     * @return 编码后的数据总长度，负值表示出错
     */
    private external fun encode_native(handle: ByteArray?, data: ByteArray, dataLen: Int,
                                       buffer: ByteArray, bufferLen: Int, packetSize: Int): Int

    /**
     * 解码。
     * @param handle 解码器指针
     * @param data 已编码数据
     * @param dataLen 数据长度
     * @param buffer 解码数据缓存
     * @param bufferLen 缓存长度
     * @param packetSize 编码数据包长度
     * @param packetDecSize 数据包解码后的长度
     * @return 解码后的数据总长度，负值表示出错
     */
    private external fun decode_native(handle: ByteArray?, data: ByteArray, dataLen: Int,
                                       buffer: ByteArray, bufferLen: Int,
                                       packetSize: Int, packetDecSize: Int): Int

    private external fun destroy_native(codec: Int, handle: ByteArray?)

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

        if (handle_codec == null) {
            Log.e(TAG, "not inited or destroyed")
            return -1
        }

        val ret = encode_native(handle_codec, data, dataLen, buffer, bufferLen, len_20ms_bytes)
        if (ret < 0) {
            Log.e(TAG, "encode error, ret=$ret")
        }

        return ret
    }

    /**
     * 解码。
     * @param data 编码数据
     * @param dataLen 数据长度
     * @param buffer 解码缓存，必须足够长能容纳解码数据
     * @param bufferLen 缓存长度
     * @return 解码后的数据长度，负值表示出错
     */
    override fun decode(data: ByteArray, dataLen: Int, buffer: ByteArray, bufferLen: Int): Int {
        if (isEncoder()) {
            Log.e(TAG, "encoder cannot be used to decode")
            return -1
        }

        if (handle_codec == null) {
            Log.e(TAG, "not inited or destroyed")
            return -1
        }

        val ret = decode_native(handle_codec, data, dataLen, buffer, bufferLen,
            80, len_20ms_bytes)
        if (ret < 0) {
            Log.e(TAG, "decode error, ret=$ret")
        }

        return ret
    }

    override fun destroy() {
        if (handle_codec != null) {
            destroy_native(codeMode, handle_codec)
            handle_codec = null
        }
    }

    protected fun finalize() {
        destroy()
    }
}