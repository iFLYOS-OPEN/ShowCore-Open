package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.iflytek.cyber.evs.sdk.player.MediaSourceFactory
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.evs.sdk.utils.Log

internal class AudioPlayerInstance(context: Context, private val type: String) {
    companion object {
        const val TYPE_TTS = AudioPlayer.TYPE_TTS
        const val TYPE_PLAYBACK = AudioPlayer.TYPE_PLAYBACK
        const val TYPE_RING = AudioPlayer.TYPE_RING
    }

    private val player = ExoPlayerFactory.newSimpleInstance(
        context,
        DefaultRenderersFactory(context),
        DefaultTrackSelector(),
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000,
                DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                100,
                DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .createDefaultLoadControl()
    )
    private val mMediaSourceFactory: MediaSourceFactory

    private val streamType = when (type) {
        TYPE_TTS, TYPE_PLAYBACK -> AudioManager.STREAM_MUSIC
        TYPE_RING -> AudioManager.STREAM_ALARM
        else -> AudioManager.STREAM_MUSIC
    }
    private val period = Timeline.Period()

    var resourceId: String? = null
    var isStarted: Boolean = false

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(
            when (streamType) {
                AudioManager.STREAM_ALARM ->
                    C.CONTENT_TYPE_SONIFICATION
                AudioManager.STREAM_MUSIC ->
                    C.CONTENT_TYPE_MUSIC
                else ->
                    C.CONTENT_TYPE_MUSIC
            }
        )
        .setUsage(
            when (streamType) {
                AudioManager.STREAM_ALARM ->
                    C.USAGE_ALARM
                AudioManager.STREAM_MUSIC ->
                    C.USAGE_MEDIA
                else ->
                    C.USAGE_MEDIA
            }
        )
        .build()
    private var listener: Listener? = null

    private val handler = Handler()

    init {
        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                listener?.onPlayerStateChanged(
                    this@AudioPlayerInstance,
                    type,
                    playWhenReady,
                    playbackState
                )
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                listener?.onPlayerError(this@AudioPlayerInstance, type, error)
            }
        })
        mMediaSourceFactory = MediaSourceFactory(context, type)
        player.audioAttributes = audioAttributes
        player.playWhenReady = false
    }

    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            if (player.playbackState == Player.STATE_READY
                || player.playbackState == Player.STATE_BUFFERING
            ) {
                if (player.playWhenReady) {
                    listener?.onPlayerPositionUpdated(this@AudioPlayerInstance, type, getOffset())

                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    internal interface Listener {
        fun onPlayerStateChanged(
            player: AudioPlayerInstance,
            type: String,
            playWhenReady: Boolean,
            playbackState: Int
        )

        fun onPlayerError(player: AudioPlayerInstance, type: String, error: ExoPlaybackException?)
        fun onPlayerPositionUpdated(player: AudioPlayerInstance, type: String, position: Long)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun getListener(): Listener? {
        return this.listener
    }

    fun play(url: String) {
        handler.post {
            val uri = Uri.parse(url)
            val mediaSource = mMediaSourceFactory.createHttpMediaSource(uri)
            player.playWhenReady = true
            player.prepare(mediaSource, true, false)
        }

        handler.post(positionUpdateRunnable)
    }

    var volGrowFlag = false

    fun setVolume(volume: Float) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            player.volume = volume
        } else {
            handler.post {
                player.volume = volume
            }
        }
    }

    fun getVolume() = player.volume

    fun resume() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (player.playbackState == Player.STATE_READY) {
                player.playWhenReady = true
            }
        } else {
            handler.post {
                if (player.playbackState == Player.STATE_READY) {
                    player.playWhenReady = true
                }
            }
        }

        handler.post(positionUpdateRunnable)
    }

    fun pause() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            player.playWhenReady = false
        } else {
            handler.post {
                player.playWhenReady = false
            }
        }
    }

    fun stop() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            player.playWhenReady = false
            //player.stop(true)
        } else {
            handler.post {
                player.playWhenReady = false
                //player.stop(true)
            }
        }
    }

    fun seekTo(offset: Long) {
        if (Looper.myLooper() == getLooper()) {
            player.seekTo(offset)
        } else {
            handler.post {
                player.seekTo(offset)
            }
        }
    }

    fun getOffset(): Long {
        val position = player.currentPosition
        val realPosition = try {
            position - player.currentTimeline.getPeriod(
                player.currentPeriodIndex, period
            ).positionInWindowMs
        } catch (e: Exception) {
            position
        }
        return if (realPosition == -1L) {
            0
        } else {
            position
        }
    }

    fun getDuration(): Long {
        val duration = player.duration
        return if (duration != C.TIME_UNSET) duration else 0
    }

    fun getLooper(): Looper = player.applicationLooper

    fun destroy() {
        player.stop(true)
        player.release()
    }
}