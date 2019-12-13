package com.iflytek.cyber.evs.sdk.focus

import android.os.Handler
import com.iflytek.cyber.evs.sdk.utils.Log

object VisualFocusManager {
    private const val TAG = "VisualFocusManager"

    const val CHANNEL_OVERLAY = "OVERLAY"
    const val CHANNEL_OVERLAY_TEMPLATE = "OVERLAY_TEMPLATE"
    const val CHANNEL_APP = "APP"

    const val TYPE_STATIC_TEMPLATE = "StaticTemplate"
    const val TYPE_CUSTOM_TEMPLATE = "CustomTemplate"
    const val TYPE_PLAYING_TEMPLATE = "PlayingTemplate"
    const val TYPE_APP_ACTION = "AppAction"

    private val latestForegroundMap = HashMap<String, String>()     // <channel, type>
    private val statusMap = HashMap<String, FocusStatus>()          // <channel, status>
    private var visualFocusObserver: VisualFocusObserver? = null

    private val handler = Handler()

    // 优前级从高到低排列
    internal val sortedChannels = arrayOf(
        CHANNEL_OVERLAY,
        CHANNEL_OVERLAY_TEMPLATE,
        CHANNEL_APP
    )

    init {
        for (channel in sortedChannels) {
            statusMap[channel] = FocusStatus.Idle
        }
    }

    internal fun setFocusObserver(observerAudio: VisualFocusObserver) {
        this.visualFocusObserver = observerAudio
    }

    internal fun removeFocusObserver() {
        this.visualFocusObserver = null
    }

    fun requestActive(activeChannel: String, type: String) {
        if (statusMap[activeChannel] == FocusStatus.Foreground) {
            if (latestForegroundMap[activeChannel] != type) {
                statusMap[activeChannel] = FocusStatus.Idle
                onInternalFocusChanged(activeChannel)

                statusMap[activeChannel] = FocusStatus.Foreground
                latestForegroundMap[activeChannel] = type
                onInternalFocusChanged(activeChannel)
            } else {
                Log.w(
                    TAG,
                    "Channel[$activeChannel] with Type[$type] is active now. Ignore this operation."
                )
            }
        } else {
            when (activeChannel) {
                CHANNEL_APP -> {
                    if (statusMap[CHANNEL_OVERLAY_TEMPLATE] != FocusStatus.Idle) {
                        statusMap[CHANNEL_OVERLAY_TEMPLATE] = FocusStatus.Idle
                        onInternalFocusChanged(CHANNEL_OVERLAY_TEMPLATE)
                    }
                    if (statusMap[CHANNEL_OVERLAY] == FocusStatus.Foreground) {
                        statusMap[activeChannel] = FocusStatus.Background
                        latestForegroundMap[activeChannel] = type
                        onInternalFocusChanged(activeChannel)
                    } else {
                        statusMap[activeChannel] = FocusStatus.Foreground
                        latestForegroundMap[activeChannel] = type
                        onInternalFocusChanged(activeChannel)
                    }
                }
                CHANNEL_OVERLAY_TEMPLATE -> {
                    if (statusMap[CHANNEL_APP] == FocusStatus.Foreground) {
                        statusMap[CHANNEL_APP] = FocusStatus.Background
                        onInternalFocusChanged(CHANNEL_APP)

                        statusMap[activeChannel] = FocusStatus.Foreground
                        latestForegroundMap[activeChannel] = type
                        onInternalFocusChanged(activeChannel)
                    } else if (statusMap[CHANNEL_OVERLAY] == FocusStatus.Foreground) {
                        statusMap[activeChannel] = FocusStatus.Background
                        latestForegroundMap[activeChannel] = type
                        onInternalFocusChanged(activeChannel)
                    }
                }
                CHANNEL_OVERLAY -> {
                    if (statusMap[CHANNEL_APP] == FocusStatus.Foreground) {
                        statusMap[CHANNEL_APP] = FocusStatus.Background
                        onInternalFocusChanged(CHANNEL_APP)
                    } else if (statusMap[CHANNEL_OVERLAY_TEMPLATE] == FocusStatus.Foreground) {
                        statusMap[CHANNEL_OVERLAY_TEMPLATE] = FocusStatus.Background
                        onInternalFocusChanged(CHANNEL_OVERLAY_TEMPLATE)
                    }

                    statusMap[activeChannel] = FocusStatus.Foreground
                    latestForegroundMap[activeChannel] = type
                    onInternalFocusChanged(activeChannel)
                }
            }
        }
    }

    fun getForegroundChannel(): String? {
        statusMap.map {
            if (it.value == FocusStatus.Foreground)
                return it.key
        }
        return null
    }

    fun getForegroundChannelType(): String? {
        val foregroundChannel = getForegroundChannel()
        latestForegroundMap.map {
            if (it.key == foregroundChannel)
                return it.value
        }
        return null
    }

    fun requestAbandon(abandonChannel: String, type: String) {
        if (statusMap[abandonChannel] == FocusStatus.Idle) {
            if (latestForegroundMap[abandonChannel] == type) {
                onInternalFocusChanged(abandonChannel)
                latestForegroundMap.remove(abandonChannel)
            } else {
                Log.w(TAG, "Target type: $type is already abandoned, ignore this operation.")
            }
        } else {
            statusMap[abandonChannel] = FocusStatus.Idle
//            onInternalFocusChanged(abandonChannel)
            latestForegroundMap.remove(abandonChannel)

            when (abandonChannel) {
                CHANNEL_OVERLAY -> {
                    if (statusMap[CHANNEL_OVERLAY_TEMPLATE] == FocusStatus.Background) {
                        statusMap[CHANNEL_OVERLAY_TEMPLATE] = FocusStatus.Foreground
                        onInternalFocusChanged(CHANNEL_OVERLAY_TEMPLATE)
                    } else if (statusMap[CHANNEL_APP] == FocusStatus.Background) {
                        statusMap[CHANNEL_APP] = FocusStatus.Foreground
                        onInternalFocusChanged(CHANNEL_APP)
                    }
                }
                CHANNEL_OVERLAY_TEMPLATE -> {
                    if (statusMap[CHANNEL_APP] == FocusStatus.Background) {
                        statusMap[CHANNEL_APP] = FocusStatus.Foreground
                        onInternalFocusChanged(CHANNEL_APP)
                    }
                }
                CHANNEL_APP -> {
                    // ignore
                }
            }
        }
    }

    private fun onInternalFocusChanged(channel: String) {
        try {
            val type = latestForegroundMap[channel] ?: return
            val status = statusMap[channel] ?: return
            visualFocusObserver?.let { observer ->
                handler.post {
                    observer.onVisualFocusChanged(
                        channel,
                        type,
                        status
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isManageableChannel(channel: String) = sortedChannels.contains(channel)

    interface VisualFocusObserver {
        fun onVisualFocusChanged(channel: String, type: String, status: FocusStatus)
    }
}