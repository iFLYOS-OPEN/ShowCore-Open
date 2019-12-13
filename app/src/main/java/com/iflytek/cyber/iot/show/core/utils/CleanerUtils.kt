package com.iflytek.cyber.iot.show.core.utils


import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

object CleanerUtils {
    private const val TAG = "CleanerUtils"

    fun clearRecordDebugFiles() {
        TerminalUtils.execute("rm /sdcard/iflytek_test_audio.pcm")
        TerminalUtils.execute("rm /sdcard/recordtest.log")
    }

    fun clearCache(context: Context) {
        // clear data/data/package/cache
        val internalCache = context.cacheDir
        internalCache.deleteRecursively()

        // clear sdcard/Android/data/package/cache
        val externalCache = context.externalCacheDir
        externalCache?.listFiles()?.map {
            if (it.isFile)
                it.delete()
        }
    }

    fun clearWakeWordCache(context: Context) {
        // clear sdcard/Android/data/package/cache/wakeword
        val externalCache = context.externalCacheDir
        val wakeWordDirectory = File("$externalCache/wakeword")
        Log.d(
            TAG,
            "exits ${wakeWordDirectory.exists()}, isDirectory ${wakeWordDirectory.isDirectory}"
        )
        if (wakeWordDirectory.exists() && wakeWordDirectory.isDirectory) {
            wakeWordDirectory.deleteRecursively()
        }
    }

    private fun externalMemoryAvailable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    fun getSdcardFreeSpace(): Long {
        return if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            availableBlocks * blockSize
        } else {
            -1
        }
    }

    fun getSdcardFullSpace(): Long {
        return if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            totalBlocks * blockSize
        } else {
            -1
        }
    }
}
