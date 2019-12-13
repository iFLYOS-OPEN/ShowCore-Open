package com.iflytek.cyber.iot.show.core.utils

import android.graphics.BitmapFactory
import android.widget.ImageView

class FrameAnimation(
    private val iv: ImageView,
    private val frameRes: IntArray,
    private val duration: Long,
    isRepeat: Boolean
) {

    private var mIsRepeat: Boolean = isRepeat

    /**
     * 每帧动画的播放间隔数组
     */
    private var mDurations: IntArray? = null

    /**
     * 下一遍动画播放的延迟时间
     */
    private var mDelay: Int = 0

    private var mLastFrame: Int = 0

    private var mNext: Boolean = false

    var isPause: Boolean = false
        private set

    private var mCurrentSelect: Int = 0

    private var mCurrentFrame: Int = 0

    private val options: BitmapFactory.Options = BitmapFactory.Options()

    init {
        this.mLastFrame = frameRes.size - 1

        options.inMutable = true
        options.inBitmap = BitmapFactory.decodeResource(iv.resources, frameRes[0], options)
    }

    fun startAnimation() {
        play(0)
    }

    fun stopAnimation() {
        isPause = true
    }

    private fun play(index: Int) {
        val runnable = FrameRunnable()
        runnable.currentFrame = index
        iv.postDelayed(runnable, duration)
    }

    inner class FrameRunnable : Runnable {
        var currentFrame = 0

        override fun run() {
            if (isPause) {
                mCurrentFrame = currentFrame
                return
            }
//            iv.setImageResource(frameRes[currentFrame])
            val bitmap = BitmapFactory.decodeResource(iv.resources, frameRes[currentFrame], options)
            iv.setImageBitmap(bitmap)
            if (currentFrame == mLastFrame) {
                if (mIsRepeat) {
                    currentFrame = 0
                    play(0)
                }
            } else {
                currentFrame++
                play(currentFrame)
            }
        }

    }

    companion object {

        private val SELECTED_A = 1

        private val SELECTED_B = 2

        private val SELECTED_C = 3

        private val SELECTED_D = 4
    }

}