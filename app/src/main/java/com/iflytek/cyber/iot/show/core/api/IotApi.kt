package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.model.*
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface IotApi {

    @GET("https://home.iflyos.cn/web/smart_home/user/iots")
    fun getIots(): Call<ArrayList<Brand>>

    @POST("v2/iot/user/device")
    fun getDeviceDetail(@Body body: RequestBody): Call<Device>

    @GET("https://home.iflyos.cn/web/smart_home/iots/{iot_id}/auth_url")
    fun getAuthUrl(@Path("iot_id") iotId: Int): Call<AuthUrl>

    @GET("v2/iot/user/devices")
    fun getDeviceList(): Call<DeviceList>

    @GET("v2/iot/oauth/polling")
    fun getAuthState(@Query("state") state: String): Call<DeviceAuthState>

    @PUT("https://home.iflyos.cn/web/smart_home/user/sync_all_devices")
    fun syncDevice(): Call<Message>

    @DELETE("https://home.iflyos.cn/web/smart_home/iot/brands/{iot_id}/token_infos")
    fun unbindDevice(@Path("iot_id") iotId: Int): Call<Message>
}