package com.iflytek.cyber.iot.show.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.iflytek.cyber.iot.show.core.fragment.MainFragment2
import com.iflytek.cyber.iot.show.core.task.SleepWorker
import java.util.*

class TouchAccessibility : AccessibilityService() {

    override fun onInterrupt() {
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (SleepWorker.get(this).topFragment is MainFragment2) {
            return
        }
        SleepWorker.get(this).doTouchWork(this)
    }
}