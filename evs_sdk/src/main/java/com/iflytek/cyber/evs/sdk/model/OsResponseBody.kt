package com.iflytek.cyber.evs.sdk.model

import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.annotation.JSONField

class OsResponseBody(
    @JSONField(name = "iflyos_meta") val meta: OsMeta,
    @JSONField(name = "iflyos_responses") val responses: List<OsResponse>
) {
    companion object {
        fun fromJSONObject(json: JSONObject): OsResponseBody {
            val meta = OsMeta.fromJSONObject(json.getJSONObject("iflyos_meta"))
            val respJsonArray = json.getJSONArray("iflyos_responses")

            val respList: MutableList<OsResponse> = ArrayList()
            for (resp in respJsonArray) {
                respList.add(OsResponse.fromJSONObject(resp as JSONObject))
            }

            return OsResponseBody(meta, respList)
        }
    }
}

class OsMeta(
    @JSONField(name = "trace_id") val traceId: String,
    @JSONField(name = "request_id") val requestId: String?,
    @JSONField(name = "is_last") val isLast: Boolean
) {
    companion object {
        fun fromJSONObject(json: JSONObject): OsMeta {
            val traceId = json.getString("trace_id")
            val requestId = json.getString("request_id")
            val isLast = json.getBoolean("is_last")

            return OsMeta(traceId, requestId, isLast)
        }
    }
}

class OsResponse(
    @JSONField(name = "header") val header: ResponseHeader,
    @JSONField(name = "payload") val payload: JSONObject) {
    companion object {
        fun fromJSONObject(json: JSONObject): OsResponse {
            val headJson = json.getJSONObject("header")
            val payloadJson = json.getJSONObject("payload")

            return OsResponse(ResponseHeader.fromJSONObject(headJson), payloadJson)
        }
    }
}

class ResponseHeader(
    @JSONField(name = "name") val name: String) {
    companion object {
        fun fromJSONObject(json: JSONObject): ResponseHeader {
            val name = json.getString("name")

            return ResponseHeader(name)
        }
    }
}