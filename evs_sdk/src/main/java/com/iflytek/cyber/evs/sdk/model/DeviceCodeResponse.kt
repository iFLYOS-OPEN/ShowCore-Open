package com.iflytek.cyber.evs.sdk.model

import android.os.Parcelable
import com.alibaba.fastjson.annotation.JSONField
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DeviceCodeResponse(
    @JSONField(name = "verification_uri") val verificationUri: String,
    @JSONField(name = "user_code") val userCode: String,
    val interval: Int,
    @JSONField(name = "expires_in") val expiresIn: Int,
    @JSONField(name = "device_code") val deviceCode: String
) : Parcelable