package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.model.ChatConfigData
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface DeviceApi {

    @GET("device")
    fun get(): Call<ResponseBody>

    @PUT("device")
    fun put(@Body requestBody: RequestBody): Call<ResponseBody>

    @GET("chat_config")
    fun getChatConfig(): Call<ChatConfigData>

    @PUT("chat_config")
    fun putChatConfig(@Body params: RequestBody): Call<ResponseBody>

    @POST("device/restore_factory")
    fun postRestoreFactory(): Call<ResponseBody>
}