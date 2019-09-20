package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.model.Banner
import retrofit2.Call
import retrofit2.http.GET

interface BannerApi {

    @GET("banners")
    fun getBanners(): Call<List<Banner>>
}