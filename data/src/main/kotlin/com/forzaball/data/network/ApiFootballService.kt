package com.forzaball.data.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * [API-Football v3](https://www.api-football.com/documentation-v3) via api-sports.io.
 */
interface ApiFootballService {

    @GET("fixtures")
    suspend fun fixtures(
        @Query("team") team: Int? = null,
        @Query("next") next: Int? = null,
        @Query("last") last: Int? = null,
        @Query("live") live: String? = null,
    ): FixturesEnvelope

    @GET("injuries")
    suspend fun injuries(
        @Query("team") team: Int,
    ): InjuriesEnvelope
}
