package com.iflytek.cyber.evs.sdk.focus

import android.os.Handler
import com.iflytek.cyber.evs.sdk.utils.Log

object AudioFocusManager {
    const val CHANNEL_OUTPUT = "OUTPUT"
    const val CHANNEL_DIAL = "DIAL"
    const val CHANNEL_INPUT = "INPUT"
    const val CHANNEL_ALARM = "ALARM"
    const val CHANNEL_CONTENT = "CONTENT"

    private const val TAG = "AudioFocusManager"

    internal const val TYPE_SOUND = "Sound" // 提示音
    internal const val TYPE_TTS = "Tts"
    internal const val TYPE_RECOGNIZE = "Recognize" // 识别
    internal const val TYPE_RECOGNIZE_V = "RecognizeV" // 评测模式
    internal const val TYPE_RING = "Ring"
    internal const val TYPE_ALARM = "Alarm"
    internal const val TYPE_PLAYBACK = "Playback"
    internal const val TYPE_VIDEO = "Video" // 视频播放器
    internal const val TYPE_EXTERNAL_AUDIO = "ExternalAudio" // 外部音频播放器
    internal const val TYPE_EXTERNAL_VIDEO = "ExternalVideo" // 外部视频播放器

    private val latestForegroundMap = HashMap<String, String>() // <channel, type>
    private val statusMap = HashMap<String, FocusStatus>()      // <channel, status>
    private var audioFocusObserver: AudioFocusObserver? = null

    private val handler = Handler()

    internal val sortedChannels = arrayOf(
        CHANNEL_OUTPUT,
        CHANNEL_DIAL,
        CHANNEL_INPUT,
        CHANNEL_ALARM,
        CHANNEL_CONTENT
    )

    private val priorityMap = HashMap<String, Int>()

    init {
        priorityMap[CHANNEL_OUTPUT] = 10
        priorityMap[CHANNEL_DIAL] = 20
        priorityMap[CHANNEL_INPUT] = 30
        priorityMap[CHANNEL_ALARM] = 40
        priorityMap[CHANNEL_CONTENT] = 50
    }

    internal fun setFocusObserver(observerAudio: AudioFocusObserver) {
        this.audioFocusObserver = observerAudio
    }

    internal fun removeFocusObserver() {
        this.audioFocusObserver = null
    }

    private fun findForegroundChannel(): String? {
        for (channel in sortedChannels) {
            if (statusMap[channel] == FocusStatus.Foreground) {
                return channel
            }
        }

        return null
    }

    private fun findBackgroundChannel(): String? {
        for (channel in sortedChannels) {
            if (statusMap[channel] == FocusStatus.Background) {
                return channel
            }
        }

        return null
    }

    private fun comparePriority(channel1: String, channel2: String): Int {
        return (priorityMap[channel1]!! - priorityMap[channel2]!!)
    }

    // 为channel下某type请求活动焦点
    internal fun requestActive(activeChannel: String, type: String) {
        val tid = Thread.currentThread().id
        Log.d(TAG, "requestActive tid=$tid, activeChannel=$activeChannel, type=$type")

        // 若请求的channel处于非前景状态或者type对不上，才进行处理
        if (statusMap[activeChannel] != FocusStatus.Foreground ||
            type != latestForegroundMap[activeChannel]
        ) {
            if (statusMap[activeChannel] == FocusStatus.Background &&
                type != latestForegroundMap[activeChannel]
            ) {
                // 之前的type变成idle
                statusMap[activeChannel] = FocusStatus.Idle
                onInternalFocusChanged(activeChannel)
            }
            val foreChannel = findForegroundChannel()
            if (foreChannel == null) {
                statusMap[activeChannel] = FocusStatus.Foreground
            } else {
                if (activeChannel == foreChannel) {
                    // activeChannel正处于前景
                    if (latestForegroundMap[foreChannel] != type) {
                        statusMap[foreChannel] = FocusStatus.Idle
                        onInternalFocusChanged(foreChannel)

                        statusMap[activeChannel] = FocusStatus.Foreground
                    }
                } else {
                    when (foreChannel) {
                        CHANNEL_OUTPUT -> {
                            when (activeChannel) {
                                CHANNEL_DIAL,
                                CHANNEL_INPUT,
                                CHANNEL_ALARM -> {
                                    statusMap[foreChannel] = FocusStatus.Idle
                                    onInternalFocusChanged(foreChannel)

                                    statusMap[activeChannel] = FocusStatus.Foreground
                                }
                                CHANNEL_CONTENT -> {
                                    statusMap[activeChannel] = FocusStatus.Background
                                }
                            }
                        }
                        CHANNEL_DIAL -> {
                            // 通话通道在活跃，其他通道都不能抢焦点
                            statusMap[activeChannel] = FocusStatus.Idle
                        }
                        CHANNEL_INPUT -> {
                            when (activeChannel) {
                                CHANNEL_ALARM -> {
                                    statusMap[foreChannel] = FocusStatus.Idle
                                    onInternalFocusChanged(foreChannel)

                                    statusMap[activeChannel] = FocusStatus.Foreground
                                }
                            }
                        }
                        CHANNEL_ALARM -> {
                            if (activeChannel == CHANNEL_OUTPUT || activeChannel == CHANNEL_INPUT) {
                                statusMap[foreChannel] = FocusStatus.Idle
                                onInternalFocusChanged(foreChannel)

                                statusMap[activeChannel] = FocusStatus.Foreground
                            } else {
                                statusMap[activeChannel] = FocusStatus.Background
                            }
                        }
                        CHANNEL_CONTENT -> {
                            if (activeChannel == CHANNEL_CONTENT) {
                                if (latestForegroundMap[foreChannel] != type) {
                                    statusMap[foreChannel] = FocusStatus.Idle
                                    onInternalFocusChanged(foreChannel)
                                }
                            } else {
                                statusMap[foreChannel] = FocusStatus.Background
                                onInternalFocusChanged(foreChannel)
                            }

                            statusMap[activeChannel] = FocusStatus.Foreground
                        }
                    }
                }

            }

            // 保证在可置为背景的前景通道通知完后，才通知想要激活的通道
            latestForegroundMap[activeChannel] = type
            onInternalFocusChanged(activeChannel)
        }
    }

    fun isManageableChannel(channel: String) = sortedChannels.contains(channel)

    // 释放channel下某type的焦点
    internal fun requestAbandon(abandonChannel: String, @Suppress("UNUSED_PARAMETER") type: String) {
        val tid = Thread.currentThread().id
        Log.d(TAG, "tid=$tid, abandonChannel=$abandonChannel, type=$type")

        when {
            latestForegroundMap[abandonChannel] != type ->
                Log.w(TAG, "Target type: $type is already abandoned, ignore this operation.")
            statusMap[abandonChannel] != FocusStatus.Idle -> {
                // 将状态置为丢失焦点
                statusMap[abandonChannel] = FocusStatus.Idle
//                onInternalFocusChanged(abandonChannel)
                latestForegroundMap.remove(abandonChannel)
                for (channel in sortedChannels) {
                    if (statusMap[channel] == FocusStatus.Background) {
                        // 将某个背景的 channel 置为前景
                        statusMap[channel] = FocusStatus.Foreground
                        onInternalFocusChanged(channel)
                        return
                    }
                }
            }
        }

    }

    private fun onInternalFocusChanged(channel: String) {
        try {
            val type = latestForegroundMap[channel] ?: return
            val status = statusMap[channel] ?: return

            audioFocusObserver?.onAudioFocusChanged(channel, type, status)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface AudioFocusObserver {
        fun onAudioFocusChanged(channel: String, type: String, status: FocusStatus)
    }

}