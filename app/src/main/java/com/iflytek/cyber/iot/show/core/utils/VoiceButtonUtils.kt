package com.iflytek.cyber.iot.show.core.utils

object VoiceButtonUtils : ConfigUtils.OnConfigChangedListener {

    var isPairing = false
        set(value) {
            field = value
            requestRefresh()
        }
    var isWelcoming = false
        set(value) {
            field = value
            requestRefresh()
        }
    var isMicrophoneEnabled = false
        set(value) {
            field = value
            requestRefresh()
        }
    var lastTouchTime = 0L
        set(value) {
            field = value
            onVoiceButtonVisibleCallback?.onScreenTouched()
        }
    var isBackgroundRecognizing = false
        set(value) {
            field = value
            requestRefresh()
        }


    var onVoiceButtonVisibleCallback: OnVoiceButtonVisibleCallback? = null

    fun requestRefresh() {
        val isVoiceButtonEnabled =
            ConfigUtils.getBoolean(ConfigUtils.KEY_VOICE_BUTTON_ENABLED, true)
        val isSetupCompleted = ConfigUtils.getBoolean(ConfigUtils.KEY_SETUP_COMPLETED, false)

        if (!isPairing && isVoiceButtonEnabled && isSetupCompleted
            && !isWelcoming && !isBackgroundRecognizing && isMicrophoneEnabled
        ) {
            onVoiceButtonVisibleCallback?.onShow()
        } else {
            onVoiceButtonVisibleCallback?.onDisappear()
        }
    }

    override fun onConfigChanged(key: String, value: Any?) {
        if (key == ConfigUtils.KEY_VOICE_BUTTON_ENABLED) {
            requestRefresh()
        }
    }

    interface OnVoiceButtonVisibleCallback {
        fun onShow()
        fun onDisappear()
        fun onScreenTouched()
    }
}