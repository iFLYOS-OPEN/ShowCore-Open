package com.iflytek.cyber.evs.sdk.agent.impl

import android.content.Context
import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.iflytek.cyber.evs.sdk.player.MediaSourceFactory
import com.iflytek.cyber.evs.sdk.utils.Log

class AlarmPlayerInstance(context: Context) {

    private val player = ExoPlayerFactory.newSimpleInstance(
        context,
        DefaultRenderersFactory(context),
        DefaultTrackSelector(),
        DefaultLoadControl()
    )
    private val mMediaSourceFactory: MediaSourceFactory

    private val type = "Alarm"
    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_SONIFICATION)
        .setUsage(C.USAGE_ALARM)
        .build()

    private var listener: OnAlarmStateChangeListener? = null

    private var onStartSent = false

    init {
        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                Log.d(type, "onPlayerStateChanged($playWhenReady, $playbackState)")
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        if (!playWhenReady) {
                            listener?.onStopped()
                        }
                    }
                    Player.STATE_BUFFERING -> {

                    }
                    Player.STATE_READY -> {
                        if (!onStartSent) {
                            onStartSent = true
                            if (playWhenReady)
                                listener?.onStarted()
                        }
                    }
                    Player.STATE_ENDED -> {
                        listener?.onStopped()
                    }
                }
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                playLocalAlarm()
            }
        })
        mMediaSourceFactory = MediaSourceFactory(context, type)
        player.audioAttributes = audioAttributes
        player.playWhenReady = false
    }

    fun setOnAlarmStateChangeListener(listener: OnAlarmStateChangeListener) {
        this.listener = listener
    }

    fun removeOnAlarmStateChangeListener() {
        listener = null
    }

    fun play(url: String) {
        onStartSent = false
        Handler(player.applicationLooper).post {
            val uri = Uri.parse(url)
            val mediaSource = mMediaSourceFactory.createHttpMediaSource(uri)
            player.playWhenReady = true
            player.prepare(mediaSource, true, false)
        }
    }

    fun playLocalAlarm() {
        // todo
    }

    fun stop() {
        Handler(player.applicationLooper).post {
            player.playWhenReady = false
            player.stop()
        }
    }

    interface OnAlarmStateChangeListener {
        fun onStarted()
        fun onStopped()
    }
}