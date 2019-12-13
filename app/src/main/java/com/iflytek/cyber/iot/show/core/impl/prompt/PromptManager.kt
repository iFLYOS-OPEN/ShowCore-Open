package com.iflytek.cyber.iot.show.core.impl.prompt


import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.SparseArray
import android.util.SparseIntArray
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.RawResourceDataSource
import com.iflytek.cyber.evs.sdk.focus.AudioFocusChannel
import com.iflytek.cyber.evs.sdk.focus.AudioFocusManager
import com.iflytek.cyber.evs.sdk.focus.FocusStatus
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.audioplayer.MediaSourceFactory
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import com.iflytek.cyber.iot.show.core.utils.ToneManager
import java.lang.Math.random
import java.lang.ref.SoftReference
import kotlin.math.roundToInt

/**
 * 用于播放各种情景下的提示文案
 */
object PromptManager {
    const val NETWORK_LOST = 7
    const val VOLUME = 23
    const val WAKE_1 = 24
    const val WAKE_2 = 25
    const val WAKE_3 = 26
    const val WAKE_4 = 27
    const val WAKE_5 = 28
    const val WAKE_6 = 29
    const val WAKE_7 = 30
    const val WAKE_8 = 31
    const val WAKE_9 = 32
    const val POWER_ON = 33
    const val TOKEN_EXPIRED = 34
    const val NETWORK_WAIT_RETRY = 35

    private const val TAG = "PromptManager"

    private val mUriMap = SparseArray<Uri>()

    private val mSoundIdMap = SparseIntArray()

    private var mPlayer: SimpleExoPlayer? = null

    private var contextRef: SoftReference<Context>? = null

    private var currentSound = -1

    private var mMediaSourceFactory: MediaSourceFactory? = null

    private val withoutFocusArray = arrayOf(
        VOLUME, WAKE_1, WAKE_2, WAKE_3,
        WAKE_4, WAKE_5, WAKE_6, WAKE_7, WAKE_8, WAKE_9, POWER_ON
    )

    val promptAudioChannel = object : AudioFocusChannel() {
        override fun onFocusChanged(focusStatus: FocusStatus) {
            if (focusStatus != FocusStatus.Foreground) {
                if (!isTone(currentSound))
                    stop()
            }
        }

        override fun getChannelName(): String {
            return AudioFocusManager.CHANNEL_OUTPUT
        }

        override fun getType(): String {
            return "TTS"
        }
    }
    private val onVolumeChangeListener = object : EvsSpeaker.OnVolumeChangedListener {
        override fun onVolumeChanged(volume: Int, fromRemote: Boolean) {
            mPlayer?.volume = volume / 100f
        }
    }

    init {
        mSoundIdMap.put(VOLUME, R.raw.tone_volume)
        mSoundIdMap.put(WAKE_1, R.raw.tone_wake_1)
        mSoundIdMap.put(WAKE_2, R.raw.tone_wake_2)
        mSoundIdMap.put(WAKE_3, R.raw.tone_wake_3)
        mSoundIdMap.put(WAKE_4, R.raw.tone_wake_4)
        mSoundIdMap.put(WAKE_5, R.raw.tone_wake_5)
        mSoundIdMap.put(WAKE_6, R.raw.tone_wake_6)
        mSoundIdMap.put(WAKE_7, R.raw.tone_wake_7)
        mSoundIdMap.put(WAKE_8, R.raw.tone_wake_8)
        mSoundIdMap.put(WAKE_9, R.raw.tone_wake_9)
        mSoundIdMap.put(POWER_ON, R.raw.tone_power_on)
        mSoundIdMap.put(TOKEN_EXPIRED, R.raw.tts_token_expired)
        mSoundIdMap.put(NETWORK_LOST, R.raw.tts_network_lost)
        mSoundIdMap.put(NETWORK_WAIT_RETRY, R.raw.tts_network_wait_retry)
    }

    fun init(context: Context) {
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        val selectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(selectionFactory)

        mMediaSourceFactory = MediaSourceFactory(context)

        mPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)

        mPlayer?.audioAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_SONIFICATION)
            .setUsage(C.USAGE_ASSISTANCE_SONIFICATION)
            .build()

        mPlayer?.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED || !playWhenReady) {
                    if (currentSound !in withoutFocusArray) {
                        promptAudioChannel.requestAbandon()
                    }
                    currentSound = -1
                    onEnd?.invoke()
                }
            }
        })

        contextRef = SoftReference(context)

        EvsSpeaker.get(context).let { evsSpeaker ->
            evsSpeaker.addOnVolumeChangedListener(onVolumeChangeListener)
            mPlayer?.volume = evsSpeaker.getCurrentVolume() / 100f
        }
    }

    fun setupAudioFocusManager(manager: AudioFocusManager) {
        promptAudioChannel.setupManager(manager)
    }

    private var onEnd: (() -> Unit)? = null

    fun play(id: Int, onEnd: (() -> Unit)? = null) {
        //this.onEnd?.invoke()
        this.onEnd = onEnd

        play(id)
    }

    fun playUrl(url: String) {
        mPlayer?.let { player ->
            Handler(player.applicationLooper).post {
                player.stop()

                val uri = Uri.parse(url)
                val mediaSource = mMediaSourceFactory?.createHttpMediaSource(uri)
                player.prepare(mediaSource, true, false)
                player.playWhenReady = true

                promptAudioChannel.requestActive()
            }
        }
    }

    private fun play(id: Int) {
        mPlayer?.let { player ->
            Handler(player.applicationLooper).post {
                player.stop()
                mSoundIdMap[id].let {
                    val rawResourceDataSource = RawResourceDataSource(contextRef?.get())
                    val factory = DataSource.Factory { rawResourceDataSource }
                    var uri = mUriMap[it]
                    if (uri == null) {
                        val dataSpec = DataSpec(RawResourceDataSource.buildRawResourceUri(it))
                        rawResourceDataSource.open(dataSpec)
                        rawResourceDataSource.uri?.let { newUri ->
                            mUriMap.put(id, newUri)
                            uri = newUri
                        }
                    }
                    val mediaSource = ExtractorMediaSource.Factory(factory)
                        .createMediaSource(uri)
                    player.prepare(mediaSource)
                    player.playWhenReady = true

                    currentSound = id

                    if (id !in withoutFocusArray) {
                        promptAudioChannel.requestActive()
                    }
                }
            }
        }
    }

    private fun isTone(soundId: Int): Boolean {
        return arrayOf(
            WAKE_1, WAKE_2, WAKE_3,
            WAKE_4, WAKE_5, WAKE_6,
            WAKE_7, WAKE_8, WAKE_9,
            VOLUME
        ).contains(soundId)
    }

    fun playWakeSound(onEnd: (() -> Unit)? = null) {
        // full
//        val array = arrayOf(
//            WAKE_1, WAKE_2, WAKE_3,
//            WAKE_4, WAKE_5, WAKE_6,
//            WAKE_7, WAKE_8, WAKE_9
//        )
        // simple
        val array = arrayOf(WAKE_1, WAKE_2)
        play(array[(random() * (array.size - 1)).roundToInt()], onEnd)
//        val context = contextRef?.get() ?: return
//        ToneManager[context].play(ToneManager.TONE_WAKE, 0.3f)
    }

    fun isPlaying(): Boolean {
        val playWhenReady = mPlayer?.playWhenReady == true
        val playbackState = mPlayer?.playbackState
        return playWhenReady && (playbackState == Player.STATE_BUFFERING ||
            playbackState == Player.STATE_READY)
    }

    fun setVolume(volume: Byte) {
        mPlayer?.let { player ->
            Handler(player.applicationLooper).post {
                mPlayer?.volume = 1f * volume / 100
            }
        }
    }

    fun destroy() {
        mPlayer?.release()

        EvsSpeaker.get(contextRef?.get()).addOnVolumeChangedListener(onVolumeChangeListener)

        currentSound = -1
    }

    fun stop() {
        mPlayer?.let { player ->
            Handler(player.applicationLooper).post {
                mPlayer?.playWhenReady = false
            }
        }

        if (!isTone(currentSound))
            promptAudioChannel.requestAbandon()

        currentSound = -1
    }
}
