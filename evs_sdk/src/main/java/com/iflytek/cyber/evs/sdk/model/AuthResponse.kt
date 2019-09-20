package com.iflytek.cyber.evs.sdk.model

import android.os.Parcelable
import com.alibaba.fastjson.annotation.JSONField
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthResponse(
    @JSONField(name = "access_token") val accessToken: String,
    @JSONField(name = "created_at") val createdAt: Long,
    @JSONField(name = "expires_in") val expiresIn: Long,
    @JSONField(name = "refresh_token") val refreshToken: String,
    @JSONField(name = "token_type") val tokenType: String
) : Parcelable