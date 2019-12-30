package com.iflytek.cyber.iot.show.core.impl.videoplayer

import android.util.Log
import com.iflytek.cyber.evs.sdk.agent.VideoPlayer
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import com.kk.taurus.playerbase.AVPlayer
import com.kk.taurus.playerbase.entity.DataSource
import com.kk.taurus.playerbase.event.EventKey
import com.kk.taurus.playerbase.event.OnErrorEventListener
import com.kk.taurus.playerbase.event.OnPlayerEventListener
import com.kk.taurus.playerbase.receiver.OnReceiverEventListener
import com.kk.taurus.playerbase.render.IRender
import com.kk.taurus.playerbase.render.RenderTextureView
import com.kk.taurus.playerbase.widget.SuperContainer

class EvsVideoPlayerInstance {
    companion object {
        private const val TAG = "EvsVideoPlayerInstance"

        const val VOLUME_BACKGROUND = .1f
    }

    private val basePlayer = AVPlayer()

    private var listener: Listener? = null

    var resourceId: String? = null

    private var cacheVolume = 1f

    private var superContainer: SuperContainer? = null
    private var render: IRender? = null
    private var renderHolder: IRender.IRenderHolder? = null
    private val playerEventListener = OnPlayerEventListener { eventCode, bundle ->
        if (eventCode != OnPlayerEventListener.PLAYER_EVENT_ON_TIMER_UPDATE)
            Log.d(TAG, "OnPlayerEventListener event: $eventCode, bundle: $bundle")
        when (eventCode) {
            OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED -> {
                bundle?.let { args ->
                    val width = args.getInt(EventKey.INT_ARG1)
                    val height = args.getInt(EventKey.INT_ARG2)

                    render?.updateVideoSize(width, height)
                }

                bindRenderHolder(renderHolder)
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE -> {
                bundle?.let { args ->
                    val width = args.getInt(EventKey.INT_ARG1)
                    val height = args.getInt(EventKey.INT_ARG2)

                    val videoSarNum = bundle.getInt(EventKey.INT_ARG3)
                    val videoSarDen = bundle.getInt(EventKey.INT_ARG4)

                    render?.updateVideoSize(width, height)
                    render?.setVideoSampleAspectRatio(videoSarNum, videoSarDen)
                }
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_STATUS_CHANGE -> {
                bundle?.let { args ->
                    val status = args.getInt(EventKey.INT_DATA)
                    listener?.onPlayerStateChanged(this, status)
                }
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_TIMER_UPDATE -> {
                bundle?.let { args ->
                    val position = args.getInt(EventKey.INT_ARG1)

                    if (basePlayer.isPlaying)
                        listener?.onPlayerPositionUpdated(this, position.toLong())
                }
            }
        }
    }
    private val errorEventListener = OnErrorEventListener { eventCode, bundle ->
        when (eventCode) {
            OnErrorEventListener.ERROR_EVENT_COMMON,
            OnErrorEventListener.ERROR_EVENT_MALFORMED,
            OnErrorEventListener.ERROR_EVENT_IO,
            OnErrorEventListener.ERROR_EVENT_TIMED_OUT,
            OnErrorEventListener.ERROR_EVENT_DATA_PROVIDER_ERROR,
            OnErrorEventListener.ERROR_EVENT_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK,
            OnErrorEventListener.ERROR_EVENT_UNSUPPORTED -> {
                stop()
                listener?.onPlayerError(this, VideoPlayer.MEDIA_ERROR_INVALID_REQUEST, null)
            }
            OnErrorEventListener.ERROR_EVENT_SERVER_DIED -> {
                listener?.onPlayerError(this, VideoPlayer.MEDIA_ERROR_SERVICE_UNAVAILABLE, null)
            }
            OnErrorEventListener.ERROR_EVENT_UNKNOWN -> {
                listener?.onPlayerError(this, VideoPlayer.MEDIA_ERROR_UNKNOWN, null)
            }

        }
    }
    private val receiverEventListener = OnReceiverEventListener { eventCode, bundle ->
        Log.d(TAG, "OnReceiverEventListener event: $eventCode, bundle: $bundle")
    }
    private val renderCallback = object : IRender.IRenderCallback {
        override fun onSurfaceChanged(
            renderHolder: IRender.IRenderHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {

        }

        override fun onSurfaceCreated(
            renderHolder: IRender.IRenderHolder?,
            width: Int,
            height: Int
        ) {
            Log.d(TAG, "onSurfaceCreated: width = $width, height = $height")
            this@EvsVideoPlayerInstance.renderHolder = renderHolder
            bindRenderHolder(renderHolder)
        }

        override fun onSurfaceDestroy(renderHolder: IRender.IRenderHolder?) {
            Log.d(TAG, "onSurfaceDestroy...")
            this@EvsVideoPlayerInstance.renderHolder = null
        }

    }
    private val onVolumeChangedListener = object : EvsSpeaker.OnVolumeChangedListener {
        override fun onVolumeChanged(volume: Int, fromRemote: Boolean) {
            if (isAudioBackground) {
                basePlayer.setVolume(
                    VOLUME_BACKGROUND * volume / 100,
                    VOLUME_BACKGROUND * volume / 100
                )
            } else {
                basePlayer.setVolume(
                    volume / 100f,
                    volume / 100f
                )
            }
        }
    }
    var isAudioBackground = false
        internal set

    init {
        basePlayer.setOnPlayerEventListener(playerEventListener)
        basePlayer.setOnErrorEventListener(errorEventListener)

        EvsSpeaker.get(null).addOnVolumeChangedListener(onVolumeChangedListener)
    }

    interface Listener {
        fun onPlayerStateChanged(
            player: EvsVideoPlayerInstance,
            playbackState: Int
        )

        fun onPlayerError(player: EvsVideoPlayerInstance, errorCode: String, errorMessage: String?)
        fun onPlayerPositionUpdated(player: EvsVideoPlayerInstance, position: Long)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setSuperContainer(superContainer: SuperContainer?) {
        basePlayer.setSurface(null)

        superContainer?.let {
            val render = RenderTextureView(superContainer.context)
            render.isTakeOverSurfaceTexture = true

            render.setRenderCallback(renderCallback)

            it.setRenderView(render.renderView)

            it.setOnReceiverEventListener(receiverEventListener)
        }

        this.superContainer = superContainer
    }

    private fun bindRenderHolder(renderHolder: IRender.IRenderHolder?) {
        renderHolder?.bindPlayer(basePlayer)
    }

    fun play(url: String) {
        val dataSource = DataSource(url)
        basePlayer.setDataSource(dataSource)
        basePlayer.start()
    }

    fun setVolume(volume: Float) {
        cacheVolume = volume
        basePlayer.setVolume(volume, volume)
    }

    fun getVolume(): Float {
        return cacheVolume
    }

    fun resume() {
        basePlayer.resume()
    }

    fun pause() {
        basePlayer.pause()
    }

    fun stop() {
        basePlayer.stop()
    }

    fun seekTo(offset: Long) {
        basePlayer.seekTo(offset.toInt())
    }

    fun getOffset(): Long {
        return basePlayer.currentPosition.toLong()
    }

    fun getDuration(): Long {
        return basePlayer.duration.toLong()
    }

    fun destroy() {
        basePlayer.stop()
        basePlayer.destroy()

        EvsSpeaker.get(null).removeOnVolumeChangedListener(onVolumeChangedListener)
    }

    fun getPlaybackState(): Int {
        return basePlayer.state
    }
}