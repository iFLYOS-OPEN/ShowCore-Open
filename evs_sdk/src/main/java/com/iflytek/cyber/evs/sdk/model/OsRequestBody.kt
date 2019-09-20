package com.iflytek.cyber.evs.sdk.model

import android.os.Build
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.annotation.JSONField

class OsRequestBody(
    @JSONField(name = "iflyos_header") val header: OsHeader,
    @JSONField(name = "iflyos_context") val context: JSONObject,
    @JSONField(name = "iflyos_request") val request: OsRequest
) {
    override fun toString(): String {
        val json = JSONObject();
        json["iflyos_header"] = header.toJSONObject()
        json["iflyos_context"] = context
        json["iflyos_request"] = request.toJSONObject()

        return JSON.toJSONString(json)
    }
}

class OsHeader(val authorization: String, val device: HeaderDevice) {
    fun toJSONObject(): JSONObject {
        val json = JSONObject()
        json["authorization"] = authorization
        json["device"] = device.toJSONObject()

        return json
    }
}

class HeaderDevice(
    @JSONField(name = "device_id") val deviceId: String,
    val location: DeviceLocation?,
    val platform: DevicePlatform,
    val flags: DeviceFlags?
) {
    fun toJSONObject(): JSONObject {
        val json = JSONObject()
        json["device_id"] = deviceId
        location?.let {
            json["location"] = it.toJSONObject()
        }
        json["platform"] = platform.toJSONObject()

        /* 最新协议中去掉flags
        flags?.let {
            json["flags"] = it.toJSONObject()
        }*/

        return json
    }
}

class DeviceLocation(val latitude: Double, val longitude: Double) {
    fun toJSONObject(): JSONObject {
        val json = JSONObject()
        json["latitude"] = latitude
        json["longitude"] = longitude

        return json
    }
}

class DevicePlatform(val name: String = "android", val version: String = Build.VERSION.RELEASE) {
    fun toJSONObject(): JSONObject {
        val json = JSONObject()
        json["name"] = name
        json["version"] = version

        return json
    }
}

class DeviceFlags(val kid: Boolean, @JSONField(name = "full_duplex") val fullDuplex: Boolean) {
    fun toJSONObject(): JSONObject {
        val json = JSONObject()
        json["kid"] = kid
        json["full_duplex"] = fullDuplex

        return json
    }
}

class OsRequest(val header: RequestHeader, val payload: JSONObject) {
    fun toJSONObject(): JSONObject {
        val json = JSONObject()
        json["header"] = header.toJSONObject()
        json["payload"] = payload

        return json
    }
}

class RequestHeader(val name: String, @JSONField(name = "request_id") val requestId: String) {
    fun toJSONObject(): JSONObject {
        val json = JSONObject()
        json["name"] = name
        json["request_id"] = requestId

        return json
    }
}