package com.iflytek.cyber.iot.show.core.impl.speaker

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.iflytek.cyber.evs.sdk.agent.Speaker
import java.lang.ref.SoftReference
import kotlin.math.ceil
import kotlin.math.roundToInt

class EvsSpeaker private constructor(context: Context) : Speaker() {
    companion object {
        private const val TAG = "EvsSpeaker"
        private var speaker: EvsSpeaker? = null

        fun get(context: Context?): EvsSpeaker {
            speaker?.let {
                return it
            } ?: run {
                val newSpeaker = EvsSpeaker(context!!)
                this.speaker = newSpeaker
                return newSpeaker
            }
        }
    }

    private var currentVolume = 0
    private val contextRef = SoftReference(context)

    private var muteCount = 0

    var isAudioFocusGain = false
    var isVisualFocusGain = false
    val isFocusGain: Boolean
        get() = isAudioFocusGain || isVisualFocusGain

    private var cacheVolume = -1


    private val listeners = HashSet<OnVolumeChangedListener>()

    init {
        (context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val min =
                if (Build.VERSION.SDK_INT > 29)
                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                else
                    0
            val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            currentVolume = ceil((volume - min) * 100f / (max - min)).roundToInt()
        }
    }

    fun refreshNativeAudioFocus(context: Context) {
        if (isFocusGain) {
            requestNativeAudioFocus(context)
        } else {
            abandonNativeAudioFocus(context)
        }
    }

    private fun setMusicVolume(context: Context?, volume: Int) {
        if (isFocusGain) {
            cacheVolume = volume
        } else {
            val am = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
        }
    }

    private fun requestNativeAudioFocus(context: Context) {
        Log.v(TAG, "requestNativeAudioFocus")
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        Log.v(TAG, "setMute")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!am.isStreamMute(AudioManager.STREAM_MUSIC))
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
        } else {
            am.setStreamMute(AudioManager.STREAM_MUSIC, true)

            muteCount++
        }
    }

    private fun abandonNativeAudioFocus(context: Context) {
        Log.v(TAG, "abandonNativeAudioFocus")
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        if (cacheVolume == -1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.v(TAG, "setUnmute")
                if (am.isStreamMute(AudioManager.STREAM_MUSIC))
                    am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            } else {
                while (muteCount > 0) {
                    Log.v(TAG, "setUnmute")
                    muteCount--
                    am.setStreamMute(AudioManager.STREAM_MUSIC, false)
                }
            }
        } else {
            Log.v(TAG, "set volume to $cacheVolume")
            am.setStreamVolume(AudioManager.STREAM_MUSIC, cacheVolume, 0)

            cacheVolume = -1
        }
    }

    fun updateCurrentVolume() {
        if (isFocusGain)
            return
        (contextRef.get()?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)?.let { audioManager ->
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val min =
                if (Build.VERSION.SDK_INT > 29)
                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                else
                    0
            val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            currentVolume = ((volume - min) * 100f / (max - min)).roundToInt()

            listeners.map {
                try {
                    it.onVolumeChanged(currentVolume, false)
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    fun addOnVolumeChangedListener(listener: OnVolumeChangedListener) {
        listeners.add(listener)
    }

    fun removeOnVolumeChangedListener(listener: OnVolumeChangedListener) {
        listeners.remove(listener)
    }

    override fun getCurrentVolume(): Int {
        return currentVolume
    }

    fun raiseVolumeLocally(fakeRemote: Boolean = false): Boolean {
        (contextRef.get()?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
            ?.let { audioManager ->
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    0
                )

                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val min =
                    if (Build.VERSION.SDK_INT > 29)
                        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                    else
                        0
                val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                currentVolume = ((volume - min) * 100f / (max - min)).roundToInt()

                listeners.map {
                    try {
                        it.onVolumeChanged(currentVolume, fakeRemote)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }

                return true
            }
        return false
    }

    fun lowerVolumeLocally(fakeRemote: Boolean = false): Boolean {
        (contextRef.get()?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
            ?.let { audioManager ->
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    0
                )

                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val min =
                    if (Build.VERSION.SDK_INT > 29)
                        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                    else
                        0
                val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

                currentVolume = ((volume - min) * 100f / (max - min)).roundToInt()

                listeners.map {
                    try {
                        it.onVolumeChanged(currentVolume, fakeRemote)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }

                return true
            }
        return false
    }

    fun setVolumeLocally(volume: Int): Boolean {
        (contextRef.get()?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
            ?.let { audioManager ->
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val min =
                    if (Build.VERSION.SDK_INT > 29)
                        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                    else
                        0
                val realVolume = (ceil(volume * (max - min) / 100f + min)).roundToInt()
                setMusicVolume(contextRef.get(), realVolume)
                currentVolume = volume

                listeners.map {
                    try {
                        it.onVolumeChanged(currentVolume, false)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }

                return true
            }
        return false
    }

    override fun setVolume(volume: Int): Boolean {
        (contextRef.get()?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
            ?.let { audioManager ->
                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val min =
                    if (Build.VERSION.SDK_INT > 29)
                        audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                    else
                        0
                val realVolume = (ceil(volume * (max - min) / 100f + min)).roundToInt()
                setMusicVolume(contextRef.get(), realVolume)
                currentVolume = volume

                listeners.map {
                    try {
                        it.onVolumeChanged(currentVolume, true)
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }

                return true
            }
        return false
    }

    interface OnVolumeChangedListener {
        fun onVolumeChanged(volume: Int, fromRemote: Boolean)
    }
}