package com.iflytek.cyber.iot.show.core.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import com.iflytek.cyber.iot.show.core.task.SleepWorker

class ExFrameLayout(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs, 0) {

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        SleepWorker.get(context).doTouchWork(context)
        return super.onInterceptTouchEvent(ev)
    }
}