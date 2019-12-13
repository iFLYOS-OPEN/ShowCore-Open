package com.iflytek.cyber.evs.sdk.model

internal object Constant {
    const val WEB_SOCKET_URL = "wss://ivs.iflyos.cn/embedded/v1?device_id=%s&token=%s"

    fun getWebSocketUrl(serverUrl: String?, deviceId: String, token: String): String {
        if (!serverUrl.isNullOrEmpty()) {
            return String.format("$serverUrl?device_id=%s&token=%s", deviceId, token)
        }

        return String.format(WEB_SOCKET_URL, deviceId, token)
    }

    const val NAMESPACE_ALARM = "alarm"
    const val NAMESPACE_APP_ACTION = "app_action"
    const val NAMESPACE_AUDIO_PLAYER = "audio_player"
    const val NAMESPACE_INTERCEPTOR = "interceptor"
    const val NAMESPACE_LAUNCHER = "launcher"
    const val NAMESPACE_PLAYBACK_CONTROLLER = "playback_controller"
    const val NAMESPACE_RECOGNIZER = "recognizer"
    const val NAMESPACE_SPEAKER = "speaker"
    const val NAMESPACE_SYSTEM = "system"
    const val NAMESPACE_SCREEN = "screen"
    const val NAMESPACE_TEMPLATE = "template"
    const val NAMESPACE_VIDEO_PLAYER = "video_player"
    const val NAMESPACE_WAKE_WORD = "wakeword"
}