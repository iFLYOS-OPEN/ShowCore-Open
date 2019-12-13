package com.iflytek.cyber.evs.sdk

import android.os.Handler
import com.alibaba.fastjson.JSONObject
import com.iflytek.cyber.evs.sdk.socket.RequestBuilder
import com.iflytek.cyber.evs.sdk.socket.Result
import com.iflytek.cyber.evs.sdk.socket.SocketManager
import com.iflytek.cyber.evs.sdk.utils.Log
import java.nio.ByteBuffer

internal object RequestManager {
    private var handler: Handler? = null
    private var service: EvsService? = null

    const val MESSAGE_END = "__END__"
    const val MESSAGE_CANCEL = "__CANCEL__"

    var currentManualRequestId: String? = null
        private set

    fun initHandler(handler: Handler?, service: EvsService) {
        this.handler = handler
        this.service = service
    }

    fun sendRequest(
        name: String,
        payload: JSONObject,
        requestCallback: RequestCallback? = null,
        isManual: Boolean = false
    ) {
        handler?.post {
            val requestBody = RequestBuilder.buildRequestBody(name, payload, isManual)

            if (isManual) {
                currentManualRequestId = requestBody.request.header.requestId

                ResponseProcessor.clearPendingManualExecuting()
            }

//            val requestId = requestBody.request.header.requestId
//            if (requestId.startsWith(RequestBuilder.PREFIX_REQUEST))
//                ResponseProcessor.updateCurrentRequestId(requestId)

            val result = SocketManager.send(requestBody.toString())

            requestCallback?.onResult(result)
        } ?: run {
            requestCallback?.onResult(Result(Result.CODE_UNINITIALIZED, null))
            throw IllegalStateException("Must init handler for RequestManager at first.")
        }
    }

    fun sendRawString(
        raw: String,
        requestCallback: RequestCallback? = null
    ) {
        handler?.post {
            if (raw == MESSAGE_CANCEL)
                currentManualRequestId = null

            val result = SocketManager.send(raw)

            requestCallback?.onResult(result)
        } ?: run {
            requestCallback?.onResult(Result(Result.CODE_UNINITIALIZED, null))
            throw IllegalStateException("Must init handler for RequestManager at first.")
        }
    }

    fun sendBinary(byteArray: ByteArray, requestCallback: RequestCallback? = null) {
        handler?.post {
            val result = SocketManager.send(byteArray)
            Log.v("RequestManager", "sendBinary result($result)")
            requestCallback?.onResult(result)
        } ?: run {
            requestCallback?.onResult(Result(Result.CODE_UNINITIALIZED, null))
            throw IllegalStateException("Must init handler for RequestManager at first.")
        }
    }

    fun sendBinary(byteBuffer: ByteBuffer, requestCallback: RequestCallback? = null) {
        handler?.post {
            val result = SocketManager.send(byteBuffer)
            requestCallback?.onResult(result)
        } ?: run {
            requestCallback?.onResult(Result(Result.CODE_UNINITIALIZED, null))
            throw IllegalStateException("Must init handler for RequestManager at first.")
        }
    }
}