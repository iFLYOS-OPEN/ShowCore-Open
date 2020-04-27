package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.model.WakeWordsBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface WakeWordApi {

    @GET("v1/wakewords")
    fun getWakeWords(): Call<WakeWordsBody>

    @PUT("v1/wakeword")
    fun putWakeWord(@Body requestBody: RequestBody): Call<ResponseBody>

    @POST("v1/wakewords/check")
    fun checkWakeWord(@Body requestBody: RequestBody): Call<ResponseBody>

    @POST("v1/wakewords")
    fun postWakeWords(@Body requestBody: RequestBody): Call<ResponseBody>
}