package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.iflytek.cyber.evs.sdk.player.MediaSourceFactory

class VideoPlayerInstance {
    private lateinit var player: SimpleExoPlayer

    private val mediaSourceFactory: MediaSourceFactory

    private val period = Timeline.Period()

    private var listener: Listener? = null

    private val handler = Handler()

    var resourceId: String? = null
    var isStarted: Boolean = false

    constructor(context: Context) {
        createPlayer(context)
        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                listener?.onPlayerStateChanged(
                    this@VideoPlayerInstance,
                    playWhenReady,
                    playbackState
                )
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                listener?.onPlayerError(this@VideoPlayerInstance, error)
            }
        })
        player.playWhenReady = true
        mediaSourceFactory = MediaSourceFactory(context, "")
    }

    constructor(context: Context, playerView: PlayerView) {
        createPlayer(context)
        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                listener?.onPlayerStateChanged(
                    this@VideoPlayerInstance,
                    playWhenReady,
                    playbackState
                )
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                listener?.onPlayerError(this@VideoPlayerInstance, error)
            }
        })
        player.playWhenReady = true

        playerView.player = player
        mediaSourceFactory = MediaSourceFactory(context, "")
    }

    private fun createPlayer(context: Context) {
        player = ExoPlayerFactory.newSimpleInstance(
            context,
            DefaultRenderersFactory(context),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )
    }

    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            if (player.playbackState == Player.STATE_READY
                || player.playbackState == Player.STATE_BUFFERING
            ) {
                if (player.playWhenReady) {
                    listener?.onPlayerPositionUpdated(this@VideoPlayerInstance, getOffset())

                    handler.postDelayed(this, 500)
                }
            }
        }
    }

    interface Listener {
        fun onPlayerStateChanged(
            player: VideoPlayerInstance,
            playWhenReady: Boolean,
            playbackState: Int
        )

        fun onPlayerError(player: VideoPlayerInstance, error: ExoPlaybackException?)
        fun onPlayerPositionUpdated(player: VideoPlayerInstance, position: Long)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setVideoSurfaceView(surfaceView: SurfaceView) {
        player.setVideoSurfaceView(surfaceView)
    }

    fun play(url: String) {
        handler.post {
            val uri = Uri.parse(url)
            val mediaSource = mediaSourceFactory.createHttpMediaSource(uri)
            player.prepare(mediaSource, true, false)
            player.playWhenReady = true
        }

        handler.post(positionUpdateRunnable)
    }

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
//            if (player.playbackState == Player.STATE_READY) {
            player.playWhenReady = true
//            }
        } else {
//            if (player.playbackState == Player.STATE_READY) {
            handler.post {
                player.playWhenReady = true
            }
//            }
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
            player.stop(true)
        } else {
            handler.post {
                player.playWhenReady = false
                player.stop(true)
            }
        }
    }

    fun seekTo(offset: Long) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            player.seekTo(offset)
        } else {
            player.seekTo(offset)
        }
    }

    fun getOffset(): Long {
        val position = player.currentPosition
        return try {
            position - player.currentTimeline.getPeriod(
                player.currentPeriodIndex, period
            ).positionInWindowMs
        } catch (e: Exception) {
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