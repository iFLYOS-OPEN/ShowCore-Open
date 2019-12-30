package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.model.Weather
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("/showcore/api/v1/weather")
    fun getWeather(@Query("location") location: String): Call<Weather>
}