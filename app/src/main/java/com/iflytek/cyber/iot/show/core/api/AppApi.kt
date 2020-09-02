package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.BuildConfig
import com.iflytek.cyber.iot.show.core.model.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AppApi {

    @GET("https://${BuildConfig.PREFIX}api.iflyos.cn/showcore/api/v2/app/list?version=v2")
    fun getAppList(@Query("client_version") clientVersion: String): Call<List<TemplateApp>>

    @GET("https://${BuildConfig.PREFIX}api.iflyos.cn/showcore/api/v2/app/index")
    fun getIndex(
        @Query("source") source: String,
        @Query("business") business: String,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Call<ResponseBody>

    @GET("https://${BuildConfig.PREFIX}api.iflyos.cn/showcore/api/v2/app/home")
    fun getHome(@Query("source") source: String): Call<List<TemplateApp2>>

    @GET("https://${BuildConfig.PREFIX}api.iflyos.cn/showcore/api/v2/app/xmly/index")
    fun getXmlyIndex(
        @Query("section") section: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Call<ResponseBody>

    @POST("https://${BuildConfig.PREFIX}api.iflyos.cn/showcore/api/v2/app/show")
    fun getAlbumShow(@Body body: RequestBody): Call<AppShowResult>

    @POST("https://${BuildConfig.PREFIX}api.iflyos.cn/showcore/api/v2/app/xmly/show")
    fun getXmlyShow(@Body body: RequestBody): Call<ResponseBody>

    @POST("https://${BuildConfig.PREFIX}api.iflyos.cn/showcore/api/v2/app/xmly/query")
    fun getXmlyQuery(@Body body: RequestBody): Call<XmlyQueryResponse>

    @POST("https://${BuildConfig.PREFIX}api.iflyos.cn/showcore/api/v2/app/media_player/play")
    fun postPlayMedia(@Body body: RequestBody): Call<ResponseBody>

    @POST("v2/app/media_player/play_album")
    fun postPlayAlbum(@Body body: RequestBody): Call<ResponseBody>

    @GET("v2/app/apptemplate")
    fun getTemplateList(
        @Query("appName") appName: String?,
        @Query("topLevel") topLevel: String? = null,
        @Query("secondLevel") secondLevel: String? = null,
        @Query("thirdLevel") thirdLevel: String? = null,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): Call<TemplateMediaItem>

    @GET("v2/app/albumdetail")
    fun getAlbumList(
        @Query("source") source: String?,
        @Query("business") business: String?,
        @Query("album") album: String?,
        @Query("media_type") mediaType: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): Call<AppShowResult>

    @POST("v2/app/media_player/play_template3")
    fun playTemplate3(@Body body: RequestBody): Call<ResponseBody>
}