package com.iflytek.cyber.iot.show.core.api

import com.iflytek.cyber.iot.show.core.model.*
import retrofit2.Call
import retrofit2.http.*


interface MediaApi {

    @GET("v1/media_player/playlist")
    fun getPlayList(@Query("media_type") mediaType: String): Call<PlayList>

    @POST("v1/media_player/play")
    fun playMusic(@Body music: MusicBody): Call<String>

    @GET("v1/media_sections")
    fun getMediaSections(): Call<ArrayList<Group>>

    @GET("v1/media_sections/{id}")
    fun getMediaSection(@Path("id") id: String): Call<Group>

    @GET("v1/media/{id}")
    fun getSongList(
        @Path("id") id: String, @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Call<SongList>

    @GET("v1/search_banner")
    fun loadRecommendTag(@Query("device_type") deviceType: String): Call<RecommendTag>

    @POST("v1/search")
    fun search(@Body body: SearchBody): Call<ArrayList<SearchResult>>

    @POST("v1/media_player/aiui_bs_play")
    fun playSearchMedia(@Body body: PlayBody): Call<String>

    @GET("v2/collection/tags")
    fun getTags(): Call<CollectionTag>

    @GET("v2/collection")
    fun getCollection(@Query("tag_id") tagId: Int): Call<CollectionV3>

    @POST("v2/player/play_collections")
    fun playSingleCollection(@Body playSingleBody: PlaySingleBody): Call<PlayResult>

    @GET("v2/collection/album")
    fun getAlbumList(
        @Query("album_id") albumId: String,
        @Query("source_type") sourceType: String?,
        @Query("business") business: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Call<SongList>

    @POST("v2/player/play_album")
    fun playAlbum(@Body body: PlayAlbumBody): Call<PlayResult>
}