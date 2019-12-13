package com.iflytek.cyber.evs.sdk.codec.audio

/**
 * 音频编码抽象类。
 */
abstract class AudioCodec(val codeMode: Int) {
    companion object {
        const val CODEC_ENCODE = 1
        const val CODEC_DECODE = 2
    }

    protected var sampleRate: Int = 16000
    protected var mode: Int = 0
    protected var quality: Int = 0

    var formatName: String = ""

    fun isEncoder(): Boolean {
        return codeMode == CODEC_ENCODE
    }

    fun isDecoder(): Boolean {
        return codeMode == CODEC_DECODE
    }

    /**
     * 编码。
     * @param data 原始音频
     * @param dataLen 音频长度，必须为20ms音频长度（对应16000采样的音频640字节）的整数倍
     * @param buffer 编码缓存区
     * @param bufferLen 缓存区长度
     * @return 编码后的长度，负值表示出错
     */
    abstract fun encode(data: ByteArray, dataLen: Int, buffer: ByteArray, bufferLen: Int): Int

    abstract fun decode(data: ByteArray, dataLen: Int, buffer: ByteArray, bufferLen: Int): Int

    abstract fun destroy()
}