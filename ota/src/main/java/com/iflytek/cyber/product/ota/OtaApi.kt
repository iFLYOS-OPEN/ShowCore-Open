package com.iflytek.cyber.product.ota

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface OtaApi {
    @GET("/ota/client/packages")
    @Deprecated("Won't be used in future versions", replaceWith = ReplaceWith("getClientCheckPackages"))
    fun getPackages(): Call<List<PackageEntity>>

    @PUT("/ota/client/packages")
    @Deprecated("Won't be used in future versions")
    fun putPackages(@Body report: ReportEntity): Call<Void>

    @GET("/ota/client/check/packages")
    fun getClientCheckPackages(): Call<List<PackageEntityNew>>
}