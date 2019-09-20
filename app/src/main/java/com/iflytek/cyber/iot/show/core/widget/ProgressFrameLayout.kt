package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.abs

class ProgressFrameLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private var touchY = 0f
    private var viewHeight = 0
    private var firstTouchY =0f

    private var maxProgress = 100
    private var progressRatio = 0f
    private var currentProgress = 0f

    private var onTouchProgressChangeListener: OnTouchProgressChangeListener? = null

    fun setOnTouchProgressChangeListener(onTouchProgressChangeListener: OnTouchProgressChangeListener) {
        this.onTouchProgressChangeListener = onTouchProgressChangeListener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewHeight = h
        progressRatio = maxProgress.toFloat() / viewHeight.toFloat() / 100f
    }

    fun setProgress(progress: Float) {
        currentProgress = progress
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchY = event.y
                firstTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val moveY = event.y
                val distance = touchY - moveY
                val progress: Float
                progress = if (distance < 0) {
                    -1f
                } else {
                    1f
                }
                if (abs(distance) > 1) {
                    currentProgress += progress
                    if (currentProgress > maxProgress) {
                        currentProgress = maxProgress.toFloat()
                    } else if (currentProgress < 0) {
                        currentProgress = 0f
                    }
                    onTouchProgressChangeListener?.onTouchProgressChanged(currentProgress.toInt())
                    touchY = moveY
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (abs(firstTouchY - event.y) > 0) {
                    onTouchProgressChangeListener?.onStopTouch()
                } else {
                    onTouchProgressChangeListener?.onClick()
                }
            }
        }
        return true
    }

    interface OnTouchProgressChangeListener {
        fun onTouchProgressChanged(progress: Int)
        fun onStopTouch()
        fun onClick()
    }
}