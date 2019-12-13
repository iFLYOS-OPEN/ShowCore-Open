package com.iflytek.cyber.iot.show.core.impl.playback

import com.iflytek.cyber.evs.sdk.agent.PlaybackController

class EvsPlaybackController private constructor() : PlaybackController() {
    companion object {
        private var instance: EvsPlaybackController? = null
        fun get(): EvsPlaybackController {
            instance?.let {
                return it
            } ?: run {
                val controller = EvsPlaybackController()
                instance = controller
                return controller
            }
        }
    }
}