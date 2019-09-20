package com.iflytek.cyber.evs.sdk

/**
 * 错误码集合
 */
@Suppress("unused")
object EvsError {
    object Code {
        const val ERROR_UNKNOWN = -1

        // from service side
        const val ERROR_WRONG_PARAMS = 400
        const val ERROR_AUTH_FAILED = 401
        const val ERROR_NO_PERMISSION = 403
        const val ERROR_SERVER_INTERNAL = 500
        const val ERROR_SERVICE_UNAVAILABLE = 503

        const val ERROR_NETWORK_UNREACHABLE = 998
        const val ERROR_CLIENT_DISCONNECTED = 999

        // from WebSocket protocol, part of https://tools.ietf.org/html/rfc6455#section-7.4
        const val ERROR_SERVER_DISCONNECTED = 1005
        const val ERROR_INVALID_SOCKET = 1011
        const val ERROR_SOCKET_EXCEPTION = 1012
        const val ERROR_SOCKET_TIMEOUT = 1013
    }

    /**
     * 授权过期异常
     */
    class AuthorizationExpiredException : IllegalArgumentException()
}