package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.model.DeskRecommend
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DeskApi {
    companion object {
        const val MODEL_ADULT = 1
        const val MODEL_CHILD = 0
    }

    @GET("https://api.iflyos.cn/showcore/api/v2/desk")
    fun getRecommend(@Query("model") model: Int): Call<List<DeskRecommend>>

    @GET("v2/desk/recommend")
    fun refreshCard(@Query("card") cardId: Int): Call<DeskRecommend>
}