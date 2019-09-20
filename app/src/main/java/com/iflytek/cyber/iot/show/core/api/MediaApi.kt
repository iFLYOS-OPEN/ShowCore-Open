package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.model.*
import retrofit2.Call
import retrofit2.http.*


interface MediaApi {

    @GET("media_player/playlist")
    fun getPlayList(@Query("media_type") mediaType: String): Call<PlayList>

    @POST("media_player/play")
    fun playMusic(@Body music: MusicBody): Call<String>

    @GET("media_sections")
    fun getMediaSections(): Call<ArrayList<Group>>

    @GET("media_sections/{id}")
    fun getMediaSection(@Path("id") id: String): Call<Group>

    @GET("media/{id}")
    fun getSongList(@Path("id") id: String, @Query("page") page: Int,
                    @Query("limit") limit: Int): Call<SongList>
}