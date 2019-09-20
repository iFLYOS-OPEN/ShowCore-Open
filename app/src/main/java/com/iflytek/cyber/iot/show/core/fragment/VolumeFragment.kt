package com.iflytek.cyber.iot.show.core.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.SelfBroadcastReceiver
import com.iflytek.cyber.iot.show.core.impl.prompt.PromptManager
import com.iflytek.cyber.iot.show.core.impl.speaker.EvsSpeaker
import com.iflytek.cyber.iot.show.core.widget.BoxedHorizontal
import kotlin.math.abs

class VolumeFragment : BaseFragment() {
    companion object {
        private const val TAG = "VolumeFragment"
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"

        private const val VOLUME_0 = 0f
        private const val VOLUME_1 = 0.34444f
        private const val VOLUME_2 = 0.66667f
        private const val VOLUME_3 = 1f
    }

    private var mediaVolumeAnimationView: LottieAnimationView? = null

    private var mediaVolumeSlider: BoxedHorizontal? = null

    private var mediaVolumeAnimator: Animator? = null
    private var mediaAnimatingVolumeTo = 0f

    private val volumeChangeReceiver = object : SelfBroadcastReceiver(VOLUME_CHANGED_ACTION) {
        override fun onReceiveAction(action: String, intent: Intent) {
            when (action) {
                VOLUME_CHANGED_ACTION -> {
                    mediaVolumeSlider?.let { slider ->
                        val type = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1)
                        if (type == AudioManager.STREAM_MUSIC) {
                            if (!slider.isPressed) {
                                updateMediaVolume()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        volumeChangeReceiver.register(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_volume, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.back).setOnClickListener {
            pop()
        }

        mediaVolumeAnimationView = view.findViewById(R.id.media_volume_icon)

        mediaVolumeSlider = view.findViewById(R.id.media_volume_slider)

        mediaVolumeSlider?.setOnBoxedPointsChangeListener(
            object : BoxedHorizontal.OnValuesChangeListener {
                override fun onPointsChanged(
                    boxedPoints: BoxedHorizontal,
                    points: Int,
                    fromTouch: Boolean
                ) {
                    if (!fromTouch)
                        return
                    val speaker = EvsSpeaker.get(context)
                    speaker.setVolumeLocally(points)

                    val volume = speaker.getCurrentVolume()
                    mediaVolumeAnimationView?.let { icon ->
                        val current = icon.progress
                        when {
                            volume == 0 -> {
                                animateMediaVolumeTo(current, VOLUME_0)
                            }
                            volume in 1..32 -> {
                                animateMediaVolumeTo(current, VOLUME_1)
                            }
                            volume in 33..66 -> {
                                animateMediaVolumeTo(current, VOLUME_2)
                            }
                            volume >= 67 -> {
                                animateMediaVolumeTo(current, VOLUME_3)
                            }
                        }
                    }
                }

                override fun onStartTrackingTouch(boxedPoints: BoxedHorizontal) {
                }

                override fun onStopTrackingTouch(boxedPoints: BoxedHorizontal) {
                    PromptManager.play(PromptManager.VOLUME)
                }
            })

        post {
            mediaVolumeSlider?.let { slider ->
                EvsSpeaker.get(context).updateCurrentVolume()
                if (!slider.isPressed) {
                    updateMediaVolume()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeChangeReceiver.unregister(context)
    }

    private fun updateMediaVolume() {
        val volume = EvsSpeaker.get(context).getCurrentVolume()
        mediaVolumeSlider?.let { slider ->
            slider.value = volume
        }
        mediaVolumeAnimationView?.let { icon ->
            val current = icon.progress
            when {
                volume == 0 -> {
                    animateMediaVolumeTo(current, VOLUME_0)
                }
                volume in 1..32 -> {
                    animateMediaVolumeTo(current, VOLUME_1)
                }
                volume in 33..66 -> {
                    animateMediaVolumeTo(current, VOLUME_2)
                }
                volume >= 67 -> {
                    animateMediaVolumeTo(current, VOLUME_3)
                }
            }
        }
    }

    private fun animateMediaVolumeTo(from: Float, progress: Float) {
        if (from == progress)
            return
        if (mediaVolumeAnimator?.isStarted == true) {
            if (mediaAnimatingVolumeTo == progress) {
                // ignore
            } else {
                mediaVolumeAnimator?.cancel()
            }
        } else {
            val animator = ValueAnimator.ofFloat(from, progress)
            mediaAnimatingVolumeTo = progress
            animator.addUpdateListener {
                val value = it.animatedValue as Float
                mediaVolumeAnimationView?.progress = value
            }
            animator.duration = (500 * abs(from - progress)).toLong()
            animator.start()
            mediaVolumeAnimator = animator
        }
    }
}