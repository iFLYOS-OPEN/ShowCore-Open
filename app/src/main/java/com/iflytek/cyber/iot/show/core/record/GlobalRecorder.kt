package com.iflytek.cyber.iot.show.core.record

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.util.Log
import com.iflytek.cyber.evs.sdk.agent.Recognizer

object GlobalRecorder {
    private const val TAG = "GlobalRecorder"

    private var mAudioRecord: AudioRecord? = null
    var isRecording = false
        private set
    private val observerSet = HashSet<Observer>()
    private var mRecordThread: RecordThread? = null
    private val minBufferSize = AudioRecord.getMinBufferSize(
            Recognizer.getSampleRateInHz(),
            Recognizer.getAudioChannel(),
            Recognizer.getAudioFormatEncoding()
    )

    fun init() {
        mAudioRecord = createAudioRecord()
    }

    private fun getAudioSource() = MediaRecorder.AudioSource.DEFAULT

    private fun createAudioRecord() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AudioRecord.Builder()
                .setAudioFormat(
                        AudioFormat.Builder()
                                .setEncoding(Recognizer.getAudioFormatEncoding())
                                .setSampleRate(Recognizer.getSampleRateInHz())
                                .setChannelMask(Recognizer.getAudioChannel())
                                .build()
                )
                .setAudioSource(getAudioSource())
                .setBufferSizeInBytes(minBufferSize)
                .build()
    } else {
        AudioRecord(
                getAudioSource(),
                Recognizer.getSampleRateInHz(),
                Recognizer.getAudioChannel(),
                Recognizer.getAudioFormatEncoding(),
                minBufferSize
        )
    }

    fun startRecording() {
        isRecording = true

        if (mAudioRecord?.state == AudioRecord.STATE_INITIALIZED)
            mAudioRecord?.startRecording()

        try {
            mRecordThread?.interrupt()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mRecordThread = RecordThread()
        mRecordThread?.start()
    }

    fun stopRecording() {
        isRecording = false

        Handler().post {
            if (mAudioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                mAudioRecord?.stop()
        }
    }

    fun registerObserver(observer: Observer) {
        observerSet.add(observer)
    }

    fun unregisterObserver(observer: Observer) {
        observerSet.remove(observer)
    }

    private fun publishAudioData(array: ByteArray, offset: Int, length: Int) {
        try {
            for (observer in observerSet) {
                try {
                    observer.onAudioData(array, offset, length)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        } catch (e: ConcurrentModificationException) {
            // ignore possible ConcurrentModificationException
        }

    }

    private fun isReady(): Boolean {
        return mAudioRecord != null && mAudioRecord?.state != AudioRecord.STATE_UNINITIALIZED
    }


    private class RecordThread : Thread() {
        override fun run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)

            try {
                val tempBuffer = ByteArray(minBufferSize)
                while (isRecording) {
                    if (isReady()) {
                        val bytesRecord = mAudioRecord?.read(tempBuffer, 0, minBufferSize) ?: -1

                        publishAudioData(tempBuffer, 0, bytesRecord)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }


    interface Observer {
        fun onAudioData(array: ByteArray, offset: Int, length: Int)
    }
}