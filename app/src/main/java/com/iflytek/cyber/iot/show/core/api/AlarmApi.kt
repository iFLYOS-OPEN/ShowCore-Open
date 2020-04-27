package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.model.Alert
import com.iflytek.cyber.iot.show.core.model.AlertBody
import com.iflytek.cyber.iot.show.core.model.Message
import retrofit2.Call
import retrofit2.http.*

interface AlarmApi {

    @GET("v1/alerts")
    fun getAlerts(): Call<ArrayList<Alert>>

    @PUT("v1/alerts/{id}")
    fun updateAlert(@Path("id") id: String, @Body body: AlertBody): Call<Message>

    @POST("v1/alerts")
    fun addNewAlarm(@Body body: AlertBody): Call<Message>

    @DELETE("v1/alerts/{id}")
    fun deleteAlarm(@Path("id") id: String): Call<Message>
}