package com.iflytek.cyber.iot.show.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.iflytek.cyber.iot.show.core.utils.VoiceButtonUtils

class TouchAccessibility : AccessibilityService() {
    companion object {
        var isMainFragment = false
        var isBodyTemplate = false
        val isIgnoreTouchEvent: Boolean
            get() = isMainFragment || isBodyTemplate
    }

    override fun onInterrupt() {
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (isIgnoreTouchEvent) {
            return
        }
        VoiceButtonUtils.lastTouchTime = System.currentTimeMillis()
//        SleepWorker.get(this).doTouchWork(this)
    }
}