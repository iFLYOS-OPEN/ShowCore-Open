package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout

class InterceptFrameLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    var onInterceptTouchListener: OnTouchListener? = null
    var onInterceptKeyEventListener: OnKeyListener? = null

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return onInterceptTouchListener?.onTouch(this, ev) ?: super.onInterceptTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return onInterceptKeyEventListener?.onKey(this, event.keyCode, event)
            ?:  super.dispatchKeyEvent(event)
    }
}