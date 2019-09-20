package com.iflytek.cyber.iot.show.core.message

import android.content.Context
import android.util.Log
import com.iflytek.cyber.evs.sdk.auth.AuthDelegate
import okhttp3.*
import okio.ByteString

class MessageSocket private constructor() {
    companion object {
        private const val TAG = "MessageSocket"

        private var instance: MessageSocket? = null

        fun get(): MessageSocket {
            instance?.let {
                return it
            } ?: run {
                val newInstance = MessageSocket()
                this.instance = newInstance
                return newInstance
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder().build()
    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)

            Log.d(TAG, "Socket connected")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)

            Log.d(TAG, "Socket onMessage: $bytes")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            Log.d(TAG, "Socket onMessage: $text")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)

            Log.d(TAG, "Socket disconnected")
        }
    }

    fun connect(context: Context) {
        val authResponse = AuthDelegate.getAuthResponseFromPref(context)

        val request = Request.Builder()
            .url("wss://staging-api.iflyos.cn/web/push?command=connect&token=${authResponse?.accessToken}&deviceId=42&fromType=message")
            .build()
        okHttpClient.newWebSocket(request, webSocketListener)
    }
}