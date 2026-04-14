package com.forzaball.domain.repository

import com.forzaball.domain.model.Club

/** Fetches team rosters for an ESPN soccer league slug (e.g. [eng.1]). */
interface SoccerTeamsRepository {
    suspend fun teamsForLeague(leagueSlug: String): List<Club>
}
