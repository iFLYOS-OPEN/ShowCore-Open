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

    fun initHandler(handler: Handler?, service: EvsService) {
        this.handler = handler
        this.service = service
    }

    fun sendRequest(name: String, payload: JSONObject, requestCallback: RequestCallback? = null) {
        handler?.post {
            val requestBody = RequestBuilder.buildRequestBody(name, payload)

            val requestId = requestBody.request.header.requestId
            if (requestId.startsWith(RequestBuilder.PREFIX_REQUEST))
                ResponseProcessor.updateCurrentRequestId(requestId)

            val result = SocketManager.send(requestBody.toString())

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