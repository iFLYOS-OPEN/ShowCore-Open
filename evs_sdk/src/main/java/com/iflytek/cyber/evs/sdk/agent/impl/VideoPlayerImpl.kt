package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.SurfaceView
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.iflytek.cyber.evs.sdk.agent.VideoPlayer
import java.lang.Thread.sleep

class VideoPlayerImpl : VideoPlayer {
    companion object {
        private const val TAG = "VideoPlayerImpl"
    }

    private var player: VideoPlayerInstance? = null

    var volGrowFlag = false

    constructor(context: Context) {
        player?.destroy()
        player = VideoPlayerInstance(context)
        player?.setListener(listener)
    }

    constructor(context: Context, playerView: PlayerView) {
        initPlayer(context, playerView)
    }

    private val listener = object : VideoPlayerInstance.Listener {
        private var playWhenReady = false
        private var lastPlayState = -1

        fun initState() {
            playWhenReady = false
            lastPlayState = -1
        }

        override fun onPlayerStateChanged(
            player: VideoPlayerInstance,
            playWhenReady: Boolean,
            playbackState: Int
        ) {
            Log.d(TAG, "onPlayerStateChanged($playWhenReady, $playbackState)")
            val isPlayingChanged = this.playWhenReady != playWhenReady
            this.playWhenReady = playWhenReady

            when (playbackState) {
                Player.STATE_ENDED -> {
                    player.isStarted = false
                    if (playWhenReady)
                        onCompleted(player.resourceId ?: "")
                }
                Player.STATE_BUFFERING -> {
                    // ignore
                }
                Player.STATE_IDLE -> {
                    player.isStarted = false
                    if (!playWhenReady) {
                        onStopped(player.resourceId ?: "")
                    }
                }
                Player.STATE_READY -> {
                    if (lastPlayState == Player.STATE_BUFFERING) {
                        if (playWhenReady) {
                            if (!player.isStarted) {
                                player.isStarted = true
                                onStarted(player.resourceId ?: "")
                            } else {
                                onResumed(player.resourceId ?: "")
                            }
                        }
                    } else {
                        if (isPlayingChanged) {
                            if (playWhenReady) {
                                onResumed(player.resourceId ?: "")
                            } else {
                                onPaused(player.resourceId ?: "")
                            }
                        }
                    }
                }
            }

            lastPlayState = playbackState
        }

        override fun onPlayerPositionUpdated(player: VideoPlayerInstance, position: Long) {
            onPositionUpdated(player.resourceId ?: "", position)
        }

        override fun onPlayerError(player: VideoPlayerInstance, error: ExoPlaybackException?) {
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
            onError(player.resourceId ?: "", errorCode)
        }
    }

    private fun initPlayer(context: Context, playerView: PlayerView) {
        player?.destroy()
        player = VideoPlayerInstance(context, playerView)
        player?.setListener(listener)
    }

    private fun getPlayer(): VideoPlayerInstance? {
        return player
    }

    fun setVideoSurfaceView(surfaceView: SurfaceView) {
        getPlayer()?.setVideoSurfaceView(surfaceView)
    }

    override fun play(resourceId: String, url: String): Boolean {
        Log.d(TAG, "try to play $url on video player")
        val player = getPlayer()
        player?.let {
            listener.initState()

            it.resourceId = resourceId
            it.isStarted = false
            it.play(url)
            return true
        } ?: run {
            return false
        }
    }

    override fun resume(): Boolean {
        val player = getPlayer()
        player?.resume() ?: run {
            return false
        }
        return true
    }

    override fun pause(): Boolean {
        val player = getPlayer()
        player?.pause() ?: run {
            return false
        }
        return true
    }

    override fun stop(): Boolean {
        val player = getPlayer()
        player?.stop() ?: run {
            return false
        }
        return true
    }

    override fun exit(): Boolean {
        val player = getPlayer()
        player?.stop() ?: run {
            return false
        }
        return true
    }

    override fun seekTo(offset: Long): Boolean {
        super.seekTo(offset)
        val player = getPlayer()
        player?.let {
            it.seekTo(offset)
            return true
        } ?: run {
            return false
        }
    }

    override fun getOffset(): Long {
        return getPlayer()?.getOffset() ?: 0
    }

    override fun getDuration(): Long {
        return getPlayer()?.getDuration() ?: 0
    }

    override fun moveToBackground(): Boolean {
        getPlayer()?.run {
            synchronized(volGrowFlag) {
                volGrowFlag = false
                val targetVolume = .1f
                Handler(getLooper()).post {
                    setVolume(targetVolume)
                }
            }
            return true
        } ?: run {
            return false
        }
    }

    override fun moveToForegroundIfAvailable(): Boolean {
        getPlayer()?.run {
            synchronized(volGrowFlag) {
                volGrowFlag = true
            }

            val volume = getVolume()
            val targetVolume = 1f

            Thread {
                var nextVolume = volume
                val step = 0.05f
                while (volGrowFlag && nextVolume != targetVolume) {
                    nextVolume =
                        if (nextVolume + step > targetVolume) targetVolume else nextVolume + step
                    try {
                        sleep(30)
                    } catch (_: Exception) {
                        //ignore
                    }

                    synchronized(volGrowFlag) {
                        if (volGrowFlag) {
                            Handler(getLooper()).post {
                                setVolume(nextVolume)
                            }
                        } else {
                            return@Thread
                        }
                    }
                }
            }.start()

            return true
        } ?: run {
            return false
        }
    }
}