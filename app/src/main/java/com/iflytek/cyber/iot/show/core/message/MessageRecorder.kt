package com.iflytek.cyber.iot.show.core.message

import android.content.Context
import com.iflytek.cyber.evs.sdk.agent.Recognizer
import com.iflytek.cyber.iot.show.core.record.GlobalRecorder
import java.io.*


object MessageRecorder : GlobalRecorder.Observer {
    override fun onWakeUp(angle: Int, beam: Int, params: String?) {
        // ignore
    }

    private var isRecording = false

    private var byteArrayOutputStream: BufferedOutputStream? = null

    private var pcmFile: File? = null
    private var wavFile: File? = null

    override fun onAudioData(array: ByteArray, offset: Int, length: Int) {
        if (!isRecording)
            return
        byteArrayOutputStream?.write(array, offset, length)
    }

    fun init(context: Context) {
        val directory = context.externalCacheDir

        pcmFile = File("$directory/${System.currentTimeMillis()}.pcm")
        wavFile = File("$directory/${System.currentTimeMillis()}.wav")
        if (pcmFile?.exists() == true) {
            pcmFile?.delete()
        }
        if (wavFile?.exists() == true) {
            wavFile?.delete()
        }
        try {
            pcmFile?.createNewFile()
            wavFile?.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun createOutputStream() {
        byteArrayOutputStream = BufferedOutputStream(FileOutputStream(pcmFile))
    }

    private fun createRecordFile(context: Context, onCreated: (recordFile: File) -> Unit) {
        val file = File("${context.externalCacheDir}/${System.currentTimeMillis()}.wav")

        if (file.exists())
            file.delete()

        file.createNewFile()

        val fileOutputStream = FileOutputStream(file)

        onCreated.invoke(file)
    }

    fun startRecording() {
        isRecording = true

        Thread {
            createOutputStream()
        }.start()
    }

    fun stopRecording(context: Context, onCreated: (recordFile: File) -> Unit) {
        isRecording = false

        Thread {
            convertWavFile(onCreated)
        }.start()
    }

    // 这里得到可播放的音频文件
    private fun convertWavFile(onCreated: (recordFile: File) -> Unit) {
        val inputStream: FileInputStream?
        val out: FileOutputStream?
        val totalAudioLen: Long
        val totalDataLen: Long
        val longSampleRate: Long = Recognizer.getSampleRateInHz().toLong()
        val channels = 1
        val byteRate = 16 * longSampleRate * channels / 8
        val data = ByteArray(1024)
        try {
            val pcmFile = pcmFile ?: return
            val wavFile = wavFile ?: return
            inputStream = FileInputStream(pcmFile)
            out = FileOutputStream(wavFile)
            totalAudioLen = inputStream.channel.size()
            //由于不包括RIFF和WAV
            totalDataLen = totalAudioLen + 36
            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                longSampleRate, channels, byteRate)
            var size = inputStream.read(data)
            while (size != -1) {
                out.write(data)
                size = inputStream.read(data)
            }
            inputStream.close()
            out.close()

            onCreated.invoke(wavFile)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /* 任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk， FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的， */
    @Throws(IOException::class)
    private fun writeWaveFileHeader(out: FileOutputStream, totalAudioLen: Long, totalDataLen: Long, longSampleRate: Long,
                                    channels: Int, byteRate: Long) {
        val header = ByteArray(44)
        header[0] = 'R'.toByte() // RIFF
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()
        header[4] = (totalDataLen and 0xff).toByte()//数据大小
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.toByte()//WAVE
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()
        //FMT Chunk
        header[12] = 'f'.toByte() // 'fmt '
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()//过渡字节
        //数据大小
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        //编码方式 10H为PCM编码格式
        header[20] = 1 // format = 1
        header[21] = 0
        //通道数
        header[22] = channels.toByte()
        header[23] = 0
        //采样率，每个通道的播放速度
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = (longSampleRate shr 8 and 0xff).toByte()
        header[26] = (longSampleRate shr 16 and 0xff).toByte()
        header[27] = (longSampleRate shr 24 and 0xff).toByte()
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (1 * 16 / 8).toByte()
        header[33] = 0
        //每个样本的数据位数
        header[34] = 16
        header[35] = 0
        //Data chunk
        header[36] = 'd'.toByte()//data
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()
        out.write(header, 0, 44)
    }

}