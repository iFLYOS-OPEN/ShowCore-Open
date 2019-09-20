package com.iflytek.cyber.iot.show.core.impl.videoplayer

import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.SurfaceView
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.iflytek.cyber.evs.sdk.agent.VideoPlayer
import com.iflytek.cyber.evs.sdk.agent.impl.VideoPlayerInstance
import com.iflytek.cyber.evs.sdk.focus.AudioFocusChannel
import com.iflytek.cyber.evs.sdk.focus.AudioFocusManager
import com.iflytek.cyber.evs.sdk.focus.FocusStatus
import java.lang.Thread.sleep
import java.net.URLEncoder

class EvsVideoPlayer : VideoPlayer {

    companion object {
        private const val TAG = "VideoPlayerImpl"
    }

    private var player: VideoPlayerInstance? = null

    private var volGrowFlag = false

    constructor(context: Context) {
        player?.destroy()
        player = VideoPlayerInstance(context)
        player?.setListener(listener)
    }

    fun setManager(manager: AudioFocusManager) {
        videoChannel.setupManager(manager)
    }

    val videoChannel = object : AudioFocusChannel() {
        override fun onFocusChanged(focusStatus: FocusStatus) {
            when (focusStatus) {
                FocusStatus.Background -> player?.pause()
                FocusStatus.Idle -> player?.pause()
                else -> player?.resume()
            }
        }

        override fun getChannelName(): String {
            return AudioFocusManager.CHANNEL_CONTENT
        }

        override fun getType(): String {
            return "Video"
        }
    }

    private val listener = object : VideoPlayerInstance.Listener {
        private var playWhenReady = false

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
                    if (playWhenReady)
                        onCompleted(player.resourceId ?: "")
                }
                Player.STATE_BUFFERING -> {
                    // ignore
                }
                Player.STATE_IDLE -> {
                    if (!playWhenReady) {
                        onStopped(player.resourceId ?: "")
                    }
                }
                Player.STATE_READY -> {
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
            it.resourceId = resourceId

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                var mutableUrl = url
                val regex = Regex(pattern = "[^\\p{ASCII}]")
                val results = regex.findAll(mutableUrl)
                for (result in results) {
                    mutableUrl = mutableUrl.replace(
                        result.value, URLEncoder.encode(result.value, "utf-8")
                    )
                }
                it.play(mutableUrl)
            } else {
                it.play(url)
            }
            onStarted(player.resourceId ?: "")
            videoChannel.requestActive()
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
        videoChannel.requestActive()
        return true
    }

    override fun pause(): Boolean {
        val player = getPlayer()
        player?.pause() ?: run {
            return false
        }
        videoChannel.requestAbandon()
        return true
    }

    override fun stop(): Boolean {
        val player = getPlayer()
        player?.stop() ?: run {
            return false
        }
        videoChannel.requestAbandon()
        return true
    }

    fun realStop() {
        val player = getPlayer()
        player?.stop()
    }

    override fun seekTo(offset: Long): Boolean {
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