package com.iflytek.cyber.evs.sdk.socket

import android.util.Log
import com.iflytek.cyber.evs.sdk.EvsError
import com.iflytek.cyber.evs.sdk.model.Constant
import okhttp3.*
import okio.ByteString
import java.io.EOFException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory
import javax.net.ssl.*

/**
 * WebSocket realized by okhttp3.
 */
internal class OkHttpWebSocket : SocketManager.EvsWebSocket() {
    private val TAG = "OkHttpWebSocket"

    private var socket: WebSocket? = null

    companion object {
        private const val NORMAL_CLOSE_CODE = 1000
        private const val CLOSE_NO_STATUS_CODE = 1005

        private const val MESSAGE_NORMAL_CLOSE = "Normal close"
        private const val MESSAGE_REMOTE_CLOSE = "Remote close"
    }

    override fun connect(serverUrl: String?, deviceId: String, token: String) {
        val url = Constant.getWebSocketUrl(serverUrl, deviceId, token)
        Log.d(TAG, "connect evs with url {$url}")

        socket?.cancel()
        socket = null

        val request = Request.Builder().url(url).build()
        val listener = InnerListener()

        if (url.startsWith("wss")) {
            OkHttpClient.Builder()
                .sslSocketFactory(createSSLSocketFactory(),
                    object : X509TrustManager {
                        override fun checkClientTrusted(
                            chain: Array<out X509Certificate>?,
                            authType: String?
                        ) {

                        }

                        override fun checkServerTrusted(
                            chain: Array<out X509Certificate>?,
                            authType: String?
                        ) {

                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate> {
                            return emptyArray()
                        }

                    })
                .connectTimeout(3, TimeUnit.SECONDS).build()
                .newWebSocket(request, listener)
        } else {
            OkHttpClient.Builder()
                .socketFactory(SocketFactory.getDefault())
                .connectTimeout(3, TimeUnit.SECONDS).build()
                .newWebSocket(request, listener)
        }

        this.listener = listener
    }

    private fun createSSLSocketFactory(): SSLSocketFactory {
        val context = SSLContext.getInstance("TLSv1.2")
        context.init(null, null, SecureRandom())

        return context.socketFactory
    }

    override fun send(message: String): Result {
        return if (socket != null) {
            Log.d(TAG, "socket send: $message")

            socket?.send(message)
            onSend(message)
            Result(Result.CODE_SUCCEED, null)
        } else {
            Log.e(TAG, "send $message failed, socket is null.")
            onSendFailed(EvsError.Code.ERROR_CLIENT_DISCONNECTED, null, message)
            Result(Result.CODE_DISCONNECTED, null)
        }
    }

    override fun send(message: ByteArray): Result {
        return if (socket != null) {
            socket?.send(ByteString.of(message, 0, message.size))
            onSend(message)
            Result(Result.CODE_SUCCEED, null)
        } else {
            Log.e(TAG, "send failed, socket is null.")
            onSendFailed(EvsError.Code.ERROR_CLIENT_DISCONNECTED, null, message)
            Result(Result.CODE_DISCONNECTED, null)
        }
    }

    override fun send(message: ByteBuffer): Result {
        return if (socket != null) {
            socket?.send(ByteString.of(message))
            onSend(message)
            Result(Result.CODE_SUCCEED, null)
        } else {
            Log.e(TAG, "send failed, socket is null.")
            onSendFailed(EvsError.Code.ERROR_CLIENT_DISCONNECTED, null, message)
            Result(Result.CODE_DISCONNECTED, null)
        }
    }

    override fun disconnect() {
        socket?.close(NORMAL_CLOSE_CODE, MESSAGE_NORMAL_CLOSE)
        socket = null
    }

    private var listener: WebSocketListener? = null

    inner class InnerListener : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)

            Log.d(TAG, "onOpen: {code: ${response.code()}, message: ${response.message()}}")

            socket = webSocket

            onConnected()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            Log.d(TAG, "onMessage: $text")

            onMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)

            Log.w(TAG, "onClosing, code=$code, reason=$reason")

            onDisconnected(code, reason, true)

            if (code == EvsError.Code.ERROR_SERVER_DISCONNECTED) {
                disconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)

            Log.w(
                TAG,
                "onFailure, response: $response, code=${response?.code()}, reason=${t.message}"
            )

            t.printStackTrace()

            if (response == null) {
                when (t) {
                    is UnknownHostException -> {
                        onConnectFailed(t)
                    }
                    is SSLException -> {
                        onDisconnected(EvsError.Code.ERROR_CLIENT_DISCONNECTED, null, false)

                        disconnect()
                    }
                    is EOFException -> {
                        onDisconnected(EvsError.Code.ERROR_CLIENT_DISCONNECTED, null, false)

                        disconnect()
                    }
                    is SocketException -> {
                        onDisconnected(EvsError.Code.ERROR_SERVER_DISCONNECTED, null, false)

                        disconnect()
                    }
                    is SocketTimeoutException -> {
                        onDisconnected(EvsError.Code.ERROR_SOCKET_TIMEOUT, null, false)

                        disconnect()
                    }
                }
            } else {
                val code = response.code()
                onDisconnected(code, t.message, true)

                disconnect()
            }
        }
    }

}