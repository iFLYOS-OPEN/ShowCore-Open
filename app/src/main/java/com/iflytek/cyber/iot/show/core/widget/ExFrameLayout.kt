package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.os.postDelayed

class ExFrameLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs, 0) {

    private val timeHandler = Handler()

    private var onCloseListener: OnCloseListener? = null

    fun setOnCloseListener(onCloseListener: OnCloseListener) {
        this.onCloseListener = onCloseListener
    }

    init {
        timeHandler.postDelayed(5 * 60 * 1000) {
            timeHandler.removeCallbacksAndMessages(null)
            onCloseListener?.onClose()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        timeHandler.removeCallbacksAndMessages(null)
        timeHandler.postDelayed(5 * 60 * 1000) {
            timeHandler.removeCallbacksAndMessages(null)
            onCloseListener?.onClose()
        }
        return super.onInterceptTouchEvent(ev)
    }

    interface OnCloseListener {
        fun onClose()
    }
}
