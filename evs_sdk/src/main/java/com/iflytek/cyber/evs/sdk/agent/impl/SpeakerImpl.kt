package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.iflytek.cyber.evs.sdk.agent.Speaker

class SpeakerImpl(private val context: Context) : Speaker() {
    override fun getCurrentVolume(): Int {
        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val min =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                else 0

            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            return (1.0 * (current - min) / (max - min) * 100.0).toInt()
        }
        return 0
    }

    override fun setVolume(volume: Int): Boolean {
        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val min =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                else 0

            val realVolume = (1.0 * volume / 100.0 * (max - min) + min).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, realVolume, AudioManager.FLAG_SHOW_UI)

            return true
        }
        return false
    }

}