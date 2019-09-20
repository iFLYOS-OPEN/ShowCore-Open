package com.iflytek.cyber.evs.sdk.socket

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Result(
    val code: Int,
    val message: String?
) : Parcelable {
    companion object {
        const val CODE_SUCCEED = 0
        const val CODE_DISCONNECTED = -1
        const val CODE_UNINITIALIZED = -2
    }

    val isSuccessful: Boolean
        get() = code == CODE_SUCCEED
}