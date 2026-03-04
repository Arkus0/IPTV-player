package com.neutraltv.player.data.xtream

import retrofit2.http.GET
import retrofit2.http.Url

interface XtreamApiService {

    @GET
    suspend fun authenticate(@Url url: String): XtreamAuthResponse

    @GET
    suspend fun getLiveCategories(@Url url: String): List<XtreamCategory>

    @GET
    suspend fun getLiveStreams(@Url url: String): List<XtreamLiveStream>

    @GET
    suspend fun getVodCategories(@Url url: String): List<XtreamCategory>

    @GET
    suspend fun getVodStreams(@Url url: String): List<XtreamVodStream>

    @GET
    suspend fun getSeriesCategories(@Url url: String): List<XtreamCategory>

    @GET
    suspend fun getSeries(@Url url: String): List<XtreamSeriesItem>

    @GET
    suspend fun getSeriesInfo(@Url url: String): XtreamSeriesInfo
}
