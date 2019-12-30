package com.iflytek.cyber.iot.show.core.impl.videoplayer

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.exoplayer2.Player
import com.iflytek.cyber.evs.sdk.agent.VideoPlayer
import com.kk.taurus.playerbase.player.IPlayer
import com.kk.taurus.playerbase.widget.SuperContainer
import java.net.URLEncoder

class EvsVideoPlayer private constructor(context: Context) : VideoPlayer() {

    companion object {
        private const val TAG = "EvsVideoPlayer"

        private var instance: EvsVideoPlayer? = null

        fun get(context: Context?): EvsVideoPlayer {
            instance?.let {
                return it
            } ?: run {
                val player = EvsVideoPlayer(context!!)
                instance = player
                return player
            }
        }
    }

    private var player: EvsVideoPlayerInstance? = null

    private var volGrowFlag = false

    var exitCallback: ExitCallback? = null

    private val listener = object : EvsVideoPlayerInstance.Listener {

        override fun onPlayerStateChanged(
            player: EvsVideoPlayerInstance,
            playbackState: Int
        ) {
            Log.d(TAG, "onPlayerStateChanged($playbackState)")

            when (playbackState) {
                IPlayer.STATE_END, IPlayer.STATE_PLAYBACK_COMPLETE -> {
                    onCompleted(player.resourceId ?: "")
                }
                Player.STATE_BUFFERING -> {
                    // ignore
                }
                IPlayer.STATE_IDLE, IPlayer.STATE_STOPPED -> {
                    onStopped(player.resourceId ?: "")
                }
                IPlayer.STATE_STARTED -> {
                    if (player.getOffset() > 0) {
                        onResumed(player.resourceId ?: "")
                    } else {
                        onStarted(player.resourceId ?: "")
                    }
                }
                IPlayer.STATE_PAUSED -> {
                    onPaused(player.resourceId ?: "")
                }
            }
        }

        override fun onPlayerPositionUpdated(player: EvsVideoPlayerInstance, position: Long) {
            onPositionUpdated(player.resourceId ?: "", position)
        }

        override fun onPlayerError(
            player: EvsVideoPlayerInstance,
            errorCode: String,
            errorMessage: String?
        ) {
            onError(player.resourceId ?: "", errorCode)
        }
    }

    init {
        player?.destroy()
        player = EvsVideoPlayerInstance()
        player?.setListener(listener)
    }

    private fun getPlayer(): EvsVideoPlayerInstance? {
        return player
    }

    fun setSuperContainer(superContainer: SuperContainer?) {
        getPlayer()?.setSuperContainer(superContainer)
    }

    override fun play(resourceId: String, url: String): Boolean {
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
            onStarted(resourceId)
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
        getPlayer()?.let { player ->
            player.stop()
            exitCallback?.onRequestExit()
        } ?: run {
            return false
        }
        return true
    }

    fun realStop() {
        val player = getPlayer()
        player?.stop()
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
            isAudioBackground = true

            synchronized(volGrowFlag) {
                volGrowFlag = false

                setVolume(EvsVideoPlayerInstance.VOLUME_BACKGROUND)
            }
            return true
        } ?: run {
            return false
        }
    }

    override fun moveToForegroundIfAvailable(): Boolean {
        getPlayer()?.run {
            isAudioBackground = false

            synchronized(volGrowFlag) {
                volGrowFlag = true
            }

            setVolume(1f)

            return true
        } ?: run {
            return false
        }
    }

    fun getPlaybackState() : Int? {
        return player?.getPlaybackState()
    }

    interface ExitCallback {
        fun onRequestExit()
    }
}