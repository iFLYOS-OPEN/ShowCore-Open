package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.model.SkillDetail
import com.iflytek.cyber.iot.show.core.model.SkillSection
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SkillApi {

    @GET("v1/skill_sections")
    fun getSkillSections(): Call<ArrayList<SkillSection>>

    @GET("v1/skills/{id}")
    fun getSkillDetail(@Path("id") id: String): Call<SkillDetail>

    @POST("https://${BuildConfig.PREFIX}api.iflyos.cn/external/flow/device/clear_session")
    fun clearSession(): Call<ResponseBody>
}