package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.alibaba.fastjson.JSONObject
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.Util
import com.iflytek.cyber.evs.sdk.RequestManager
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.utils.Log
import java.lang.Exception
import java.lang.Thread.sleep

class AudioPlayerImpl(context: Context) : AudioPlayer() {
    companion object {
        private const val TAG = "AudioPlayerImpl"
    }

    private var playbackPlayer: AudioPlayerInstance? = null
    private var ttsPlayer: AudioPlayerInstance? = null
    private var ringPlayer: AudioPlayerInstance? = null

    private var currentResourceMediaType = C.TYPE_OTHER

    private inner class ImplListener : AudioPlayerInstance.Listener {
        override fun onPlayerPositionUpdated(
            player: AudioPlayerInstance,
            type: String,
            position: Long
        ) {
            onPositionUpdated(type, player.resourceId ?: "", position)
        }

        private var playWhenReady = false
        private var lastPlayState = -1

        override fun onPlayerStateChanged(
            player: AudioPlayerInstance,
            type: String,
            playWhenReady: Boolean,
            playbackState: Int
        ) {
            Log.d(TAG, "onPlayerStateChanged($type, $playWhenReady, $playbackState)")
            val isPlayingChanged = this.playWhenReady != playWhenReady
            this.playWhenReady = playWhenReady

            when (playbackState) {
                Player.STATE_ENDED -> {
                    player.isStarted = false
                    if (playWhenReady)
                        onCompleted(type, player.resourceId ?: "")
                }
                Player.STATE_BUFFERING -> {
//                    if (player.isStarted && lastPlayState == Player.STATE_READY) {
//                        onPaused(type, player.resourceId ?: "")
//                    }
                }
                Player.STATE_IDLE -> {
                    player.isStarted = false
                    if (!playWhenReady) {
                        onStopped(type, player.resourceId ?: "")
                    }
                }
                Player.STATE_READY -> {
                    if (lastPlayState == Player.STATE_BUFFERING) {
                        if (playWhenReady) {
                            if (!player.isStarted) {
                                player.isStarted = true
                                onStarted(type, player.resourceId ?: "")
                            } else {
                                onResumed(type, player.resourceId ?: "")
                            }
                        }
                    } else if (lastPlayState == Player.STATE_READY) {
                        if (isPlayingChanged) {
                            if (playWhenReady) {
                                onResumed(type, player.resourceId ?: "")
                            } else {
                                onPaused(type, player.resourceId ?: "")
                            }
                        }
                    }
                }
            }

            lastPlayState = playbackState
        }

        override fun onPlayerError(
            player: AudioPlayerInstance,
            type: String,
            error: ExoPlaybackException?
        ) {
            val errorCode: String = when (error?.type) {
                ExoPlaybackException.TYPE_UNEXPECTED -> {
                    MEDIA_ERROR_UNKNOWN
                }
                ExoPlaybackException.TYPE_SOURCE -> {
                    MEDIA_ERROR_INVALID_REQUEST
                }
                ExoPlaybackException.TYPE_REMOTE -> {
                    MEDIA_ERROR_SERVICE_UNAVAILABLE
                }
                ExoPlaybackException.TYPE_RENDERER -> {
                    MEDIA_ERROR_INTERNAL_SERVER_ERROR
                }
                ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
                    MEDIA_ERROR_INTERNAL_DEVICE_ERROR
                }
                else -> {
                    MEDIA_ERROR_UNKNOWN
                }
            }
            onError(type, player.resourceId ?: "", errorCode)
        }
    }

    private fun initPlayers(context: Context) {
        playbackPlayer?.destroy()
        ttsPlayer?.destroy()
        ringPlayer?.destroy()

        playbackPlayer = AudioPlayerInstance(context, TYPE_PLAYBACK)
        ttsPlayer = AudioPlayerInstance(context, TYPE_TTS)
        ringPlayer = AudioPlayerInstance(context, TYPE_RING)

        playbackPlayer?.setListener(ImplListener())
        ttsPlayer?.setListener(ImplListener())
        ringPlayer?.setListener(ImplListener())
    }

    init {
        initPlayers(context)
    }

    fun getCurrentResourceMediaPlayer(): Int {
        return currentResourceMediaType
    }

    private fun getPlayer(type: String?): AudioPlayerInstance? {
        return when (type) {
            TYPE_TTS -> ttsPlayer
            TYPE_PLAYBACK -> playbackPlayer
            TYPE_RING -> ringPlayer
            else -> null
        }
    }

    override fun play(type: String, resourceId: String, url: String): Boolean {
        Log.d(TAG, "try to play $url on $type player")
        val player = getPlayer(type)
        if (type == TYPE_PLAYBACK) {
            val uri = Uri.parse(url)
            currentResourceMediaType = Util.inferContentType(uri.lastPathSegment)
        }
        player?.let {
            it.resourceId = resourceId
            it.play(url)
            it.isStarted = false
            onStarted(type, resourceId)
            onPositionUpdated(type, resourceId, 0)
            return true
        } ?: run {
            return false
        }
    }

    override fun resume(type: String): Boolean {
        val player = getPlayer(type)
        player?.resume() ?: run {
            return false
        }
        return true
    }

    override fun pause(type: String): Boolean {
        val player = getPlayer(type)
        player?.pause()?.let {
            onPaused(type, player.resourceId ?: "")
        } ?: run {
            return false
        }
        return true
    }

    override fun stop(type: String): Boolean {
        val player = getPlayer(type)
        player?.stop()?.let {
            onStopped(type, player.resourceId ?: "")
        } ?: run {
            return false
        }
        return true
    }

    override fun seekTo(type: String, offset: Long): Boolean {
        super.seekTo(type, offset)
        val player = getPlayer(type)
        player?.let {
            it.seekTo(offset)
            return true
        } ?: run {
            return false
        }
    }

    override fun getOffset(type: String): Long {
        return getPlayer(type)?.getOffset() ?: 0
    }

    override fun getDuration(type: String): Long {
        return getPlayer(type)?.getDuration() ?: 0
    }

    override fun moveToForegroundIfAvailable(type: String): Boolean {
        getPlayer(type)?.run {
            val targetVolume = 1f

            Handler(getLooper()).post {
                setVolume(targetVolume)
            }

            return true
        } ?: run {
            return false
        }
    }

    override fun moveToBackground(type: String): Boolean {
        getPlayer(type)?.run {
            val targetVolume = .1f

            Handler(getLooper()).post {
                setVolume(targetVolume)
            }
            return true
        } ?: run {
            return false
        }
    }


}
