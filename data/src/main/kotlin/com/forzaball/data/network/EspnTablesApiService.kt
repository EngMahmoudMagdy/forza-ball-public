package com.forzaball.data.network

import retrofit2.http.GET
import retrofit2.http.Path

/** ESPN `apis/v2/sports/soccer` tables (different base path than site v2 scoreboard). */
interface EspnTablesApiService {

    @GET("{league}/standings")
    suspend fun standings(
        @Path("league", encoded = true) league: String,
    ): EspnStandingsApiRootDto
}
