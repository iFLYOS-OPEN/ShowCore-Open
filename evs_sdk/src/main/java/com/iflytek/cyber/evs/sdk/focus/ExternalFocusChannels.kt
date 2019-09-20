package com.iflytek.cyber.evs.sdk.focus

abstract class AudioFocusChannel {
    private var manager: AudioFocusManager? = null

    abstract fun onFocusChanged(focusStatus: FocusStatus)
    abstract fun getChannelName(): String
    abstract fun getType(): String

    fun setupManager(manager: AudioFocusManager) {
        this.manager = manager
    }

    fun requestActive() {
        manager?.requestActive(getChannelName(), getExternalType()) ?: run {
            throw IllegalStateException(
                "You should override getExternalAudioFocusChannels in EvsService, and add this channel to return value"
            )
        }
    }

    fun requestAbandon() {
        manager?.requestAbandon(getChannelName(), getExternalType()) ?: run {
            throw IllegalStateException(
                "You should override getExternalAudioFocusChannels in EvsService, and add this channel to return value"
            )
        }
    }

    fun getExternalType() = "External.${getType()}"

    fun getCurrentStatus(): FocusStatus {
        return FocusStatus.Idle
    }
}

abstract class VisualFocusChannel {
    private var manager: VisualFocusManager? = null

    abstract fun onFocusChanged(focusStatus: FocusStatus)
    abstract fun getChannelName(): String
    abstract fun getType(): String

    fun setupManager(manager: VisualFocusManager) {
        this.manager = manager
    }

    fun requestActive() {
        manager?.requestActive(getChannelName(), getExternalType()) ?: run {
            throw IllegalStateException(
                "You should override getExternalVisualFocusChannels in EvsService, and add this channel to return value"
            )
        }
    }

    fun requestAbandon() {
        manager?.requestAbandon(getChannelName(), getExternalType()) ?: run {
            throw IllegalStateException(
                "You should override getExternalVisualFocusChannels in EvsService, and add this channel to return value"
            )
        }
    }

    fun getExternalType() = "External.${getType()}"

    fun getCurrentStatus(): FocusStatus {
        return FocusStatus.Idle
    }
}