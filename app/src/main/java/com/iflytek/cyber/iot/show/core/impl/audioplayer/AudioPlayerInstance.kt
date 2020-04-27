package com.iflytek.cyber.iot.show.core.impl.audioplayer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.FileDataSource
import com.iflytek.cyber.evs.sdk.agent.AudioPlayer
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import java.io.File

internal class AudioPlayerInstance(context: Context, private val type: String) {
    companion object {
        const val TYPE_TTS = AudioPlayer.TYPE_TTS
        const val TYPE_PLAYBACK = AudioPlayer.TYPE_PLAYBACK
        const val TYPE_RING = AudioPlayer.TYPE_RING

        const val BACKGROUND_VOLUME = 0.1f
    }

    private val player = ExoPlayerFactory.newSimpleInstance(
        context,
        DefaultRenderersFactory(context),
        DefaultTrackSelector(),
        if (type == TYPE_TTS)
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    100,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .createDefaultLoadControl()
        else
            DefaultLoadControl()
    )
    private val mMediaSourceFactory: MediaSourceFactory

    private val period = Timeline.Period()

    var resourceId: String? = null
    var isStarted: Boolean = false

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(
            when (type) {
                TYPE_RING ->
                    C.CONTENT_TYPE_SONIFICATION
                TYPE_TTS ->
                    C.CONTENT_TYPE_SONIFICATION
                TYPE_PLAYBACK ->
                    C.CONTENT_TYPE_SONIFICATION
                else ->
                    C.CONTENT_TYPE_MUSIC
            }
        )
        .setUsage(
            when (type) {
                TYPE_RING ->
                    C.USAGE_ALARM
                TYPE_TTS ->
                    C.USAGE_NOTIFICATION
                TYPE_PLAYBACK ->
                    C.USAGE_NOTIFICATION
                else ->
                    C.USAGE_MEDIA
            }
        )
        .build()
    private val onVolumeChangedListener = object : EvsSpeaker.OnVolumeChangedListener {
        override fun onVolumeChanged(volume: Int, fromRemote: Boolean) {
            if (isBackground) {
                player.volume = volume / 100f * BACKGROUND_VOLUME
            } else {
                player.volume = volume / 100f
            }
        }
    }
    private var listener: Listener? = null

    var isBackground = false
        internal set

    private val handler = Handler()

    init {
        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playWhenReady && playbackState == Player.STATE_READY) {
                    handler.post(positionUpdateRunnable)
                }
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
        mMediaSourceFactory = MediaSourceFactory(context)
        player.audioAttributes = audioAttributes
//        player.playWhenReady = false

        player.volume = volumePercent()

        if (type == TYPE_PLAYBACK || type == TYPE_TTS)
            EvsSpeaker.get(null).addOnVolumeChangedListener(onVolumeChangedListener)
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
            player.prepare(mediaSource, true, true)
            player.playWhenReady = true
        }
    }

    fun playTtsFile(path: String) {
        handler.post {
            try {
                val uri = Uri.fromFile(File(path))
                val dataSpec = DataSpec(uri)
                val fileDataSource = FileDataSource()
                fileDataSource.open(dataSpec)
                val factory = DataSource.Factory {
                    fileDataSource
                }
                val audioSource = ExtractorMediaSource(
                    fileDataSource.uri,
                    factory,
                    DefaultExtractorsFactory(),
                    null,
                    null
                )
                player.prepare(audioSource, true, false)
                player.playWhenReady = true
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    var volGrowFlag = false

    private fun volumePercent() =
        if (type == TYPE_RING) 1f
        else
            EvsSpeaker.get(null).getCurrentVolume() / 100f

    fun setVolume(volume: Float) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            player.volume = volume * volumePercent()
        } else {
            handler.post {
                player.volume = volume * volumePercent()
            }
        }
    }

    fun getVolume() = volumePercent().let { percent ->
        if (percent == 0f) {
            0f
        } else {
            player.volume / volumePercent()
        }
    }

    fun resume() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            if (player.playbackState == Player.STATE_READY) {
                player.playWhenReady = true
            } else if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
                player.playWhenReady = true
            } else if (player.playbackState == Player.STATE_IDLE) {
                player.seekTo(player.contentPosition)
                player.playWhenReady = true
            }
        } else {
            handler.post {
                if (player.playbackState == Player.STATE_READY) {
                    player.playWhenReady = true
                } else if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                    player.playWhenReady = true
                } else if (player.playbackState == Player.STATE_IDLE) {
                    player.seekTo(player.contentPosition)
                    player.playWhenReady = true
                }
            }
        }
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

    fun getPlaybackState() = player.playbackState

    fun destroy() {
        player.stop(true)
        player.release()

        if (type == TYPE_PLAYBACK || type == TYPE_TTS)
            EvsSpeaker.get(null).removeOnVolumeChangedListener(onVolumeChangedListener)
    }
}