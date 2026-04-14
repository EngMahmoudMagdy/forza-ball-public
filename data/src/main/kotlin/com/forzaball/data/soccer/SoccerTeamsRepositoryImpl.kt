package com.forzaball.data.soccer

import com.forzaball.data.network.EspnApiService
import com.forzaball.data.network.toClubs
import com.forzaball.domain.model.Club
import com.forzaball.domain.repository.SoccerTeamsRepository

class SoccerTeamsRepositoryImpl(
    private val espn: EspnApiService,
) : SoccerTeamsRepository {

    override suspend fun teamsForLeague(leagueSlug: String): List<Club> {
        val envelope = espn.teams(leagueSlug)
        return envelope.toClubs(leagueSlug)
    }
}
