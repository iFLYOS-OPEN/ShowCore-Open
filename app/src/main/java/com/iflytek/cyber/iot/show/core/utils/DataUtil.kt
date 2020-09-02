package com.iflytek.cyber.iot.show.core.utils

import android.util.Base64

object DataUtil {
    fun encodeBase64(data: ByteArray): String {
        val encData = Base64.encode(data, Base64.DEFAULT)
        return String(encData)
    }

    fun decodeBase64(base64: String): ByteArray {
        return Base64.decode(base64, Base64.CRLF)
    }
}