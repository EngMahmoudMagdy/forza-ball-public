package com.forzaball.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * [ESPN Site API](https://site.api.espn.com) — public soccer endpoints (no API key).
 */
interface EspnApiService {

    @GET("{league}/scoreboard")
    suspend fun scoreboard(
        @Path("league", encoded = true) league: String,
    ): EspnScoreboardDto

    @GET("{league}/news")
    suspend fun news(
        @Path("league", encoded = true) league: String,
    ): EspnNewsEnvelopeDto

    @GET("{league}/teams")
    suspend fun teams(
        @Path("league", encoded = true) league: String,
    ): EspnTeamsEnvelopeDto

    /** Upcoming fixtures for a team ([fixtures.json] sample). */
    @GET("all/teams/{teamId}/schedule")
    suspend fun teamSchedule(
        @Path("teamId") teamId: String,
        @Query("fixture") fixture: Boolean? = true,
    ): EspnTeamScheduleEnvelopeDto

    /** Schedule within a competition (e.g. domestic league or UEFA Champions League). */
    @GET("{league}/teams/{teamId}/schedule")
    suspend fun teamScheduleInLeague(
        @Path("league", encoded = true) league: String,
        @Path("teamId") teamId: String,
        @Query("fixture") fixture: Boolean? = true,
    ): EspnTeamScheduleEnvelopeDto
}
