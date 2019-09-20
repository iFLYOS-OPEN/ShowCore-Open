package com.iflytek.cyber.evs.sdk.agent.impl

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRecord.getMinBufferSize
import android.os.Build
import com.iflytek.cyber.evs.sdk.agent.Recognizer

class RecognizerImpl : Recognizer() {

    private var recorder: AudioRecord? = null

    init {

    }

    private fun createAudioRecord() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AudioRecord.Builder()
                .setAudioFormat(
                        AudioFormat.Builder()
                                .setEncoding(getAudioFormatEncoding())
                                .setSampleRate(getSampleRateInHz())
                                .setChannelMask(getAudioChannel())
                                .build()
                )
                .setAudioSource(getAudioSource())
                .setBufferSizeInBytes(
                        getMinBufferSize(
                                getSampleRateInHz(),
                                getAudioChannel(),
                                getAudioFormatEncoding()
                        )
                )
                .build()
    } else {
        AudioRecord(
                getAudioSource(),
                getSampleRateInHz(),
                getAudioChannel(),
                getAudioFormatEncoding(),
                getMinBufferSize(
                        getSampleRateInHz(),
                        getAudioChannel(),
                        getAudioFormatEncoding()
                )
        )
    }

    override fun readBytes(byteArray: ByteArray, length: Int): Int {
        if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return recorder?.read(byteArray, 0, length) ?: -1
        }
        return -1
    }

    override fun startRecording() {
        if (recorder == null) {
            recorder = createAudioRecord()
        }
        recorder?.startRecording()
    }

    override fun stopRecording() {
        recorder?.stop()
        recorder = null
    }

    override fun isRecording(): Boolean {
        return recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            recorder?.release()
        } catch (e: Exception) {

        }
    }

}