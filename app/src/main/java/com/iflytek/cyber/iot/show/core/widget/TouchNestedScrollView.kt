package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

class TouchNestedScrollView : NestedScrollView {
    interface OnCustomTouchListener {
        fun onTouchEvent(ev: MotionEvent?)
    }

    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)

    private var customTouchListener: OnCustomTouchListener? = null

    fun setOnCustomTouchListener(listener: OnCustomTouchListener?) {
        customTouchListener = listener
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        customTouchListener?.onTouchEvent(ev)

        return super.dispatchTouchEvent(ev)
    }
}