package com.iflytek.cyber.evs.sdk.socket

import java.nio.ByteBuffer

internal object SocketManager {
    private var webSocket: EvsWebSocket? = null

    init {
        webSocket = OkHttpWebSocket()
    }

    fun connect(serverUrl: String?, deviceId: String, token: String) {
        webSocket?.connect(serverUrl, deviceId, token)
    }

    fun send(message: String): Result {
        return webSocket?.send(message) ?: Result(Result.CODE_UNINITIALIZED, null)
    }

    fun send(message: ByteArray): Result {
        return webSocket?.send(message) ?: Result(Result.CODE_UNINITIALIZED, null)
    }

    fun send(message: ByteBuffer): Result {
        return webSocket?.send(message) ?: Result(Result.CODE_UNINITIALIZED, null)
    }

    fun disconnect() {
        webSocket?.disconnect()
    }

    fun addListener(listener: SocketListener) {
        webSocket?.addListener(listener)
    }

    fun removeListener(listener: SocketListener) {
        webSocket?.removeListener(listener)
    }

    fun onConnectFailed(t: Throwable?) {
        webSocket?.onConnectFailed(t)
    }

    abstract class SocketListener {
        open fun onConnected() {}
        open fun onConnectFailed(t: Throwable?) {}
        open fun onDisconnected(code: Int, reason: String?, remote: Boolean) {}
        open fun onMessage(message: String) {}
        open fun onSend(message: Any) {}
        @Deprecated("")
        open fun onSendFailed(code: Int, reason: String?) {}
        open fun onSendFailed(code: Int, reason: String?, sendFailedMessage:Any) {}
    }

    abstract class EvsWebSocket {
        private val onMessageListeners = HashSet<SocketListener>()

        abstract fun connect(serverUrl: String?, deviceId: String, token: String)
        abstract fun send(message: String): Result
        abstract fun send(message: ByteArray): Result
        abstract fun send(message: ByteBuffer): Result
        abstract fun disconnect()

        open fun onConnected() {
            synchronized(onMessageListeners) {
                onMessageListeners.map {
                    Thread {
                        try {
                            it.onConnected()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        }

        open fun onConnectFailed(t: Throwable?) {
            synchronized(onMessageListeners) {
                onMessageListeners.map {
                    Thread {
                        try {
                            it.onConnectFailed(t)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        }

        open fun onDisconnected(code: Int, reason: String?, remote: Boolean) {
            synchronized(onMessageListeners) {
                onMessageListeners.map {
                    Thread {
                        try {
                            it.onDisconnected(code, reason, remote)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        }

        open fun onMessage(message: String) {
            synchronized(onMessageListeners) {
                onMessageListeners.map {
                    Thread {
                        try {
                            it.onMessage(message)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        }

        open fun onSend(message: Any) {
            synchronized(onMessageListeners) {
                onMessageListeners.map {
                    Thread {
                        try {
                            it.onSend(message)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        }

        @Deprecated("")
        open fun onSendFailed(code: Int, reason: String?) {
            synchronized(onMessageListeners) {
                onMessageListeners.map {
                    Thread {
                        try {
                            it.onSendFailed(code, reason)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        }

        open fun onSendFailed(code: Int, reason: String?, sendFailedMessage: Any) {
            synchronized(onMessageListeners) {
                onMessageListeners.map {
                    Thread {
                        try {
                            it.onSendFailed(code, reason, sendFailedMessage)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                }
            }
        }

        open fun addListener(listener: SocketListener) {
            synchronized(onMessageListeners) {
                onMessageListeners.add(listener)
            }
        }

        open fun removeListener(listener: SocketListener) {
            synchronized(onMessageListeners) {
                onMessageListeners.remove(listener)
            }
        }
    }
}