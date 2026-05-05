package com.forzaball.data.standings

import com.forzaball.domain.model.TeamStandingSnapshot
import com.forzaball.domain.repository.StandingsRepository
import com.forzaball.shared.data.standings.SharedStandingsRepository

class KmpStandingsRepositoryAdapter(
    private val sharedRepository: SharedStandingsRepository,
) : StandingsRepository {
    override suspend fun getTeamStanding(leagueSlug: String, teamId: String): TeamStandingSnapshot? {
        val snapshot = sharedRepository.getTeamStanding(leagueSlug = leagueSlug, teamId = teamId) ?: return null
        return TeamStandingSnapshot(
            leagueSlug = snapshot.leagueSlug,
            leagueDisplayName = snapshot.leagueDisplayName,
            teamName = snapshot.teamName,
            position = snapshot.position,
            played = snapshot.played,
            wins = snapshot.wins,
            draws = snapshot.draws,
            losses = snapshot.losses,
            points = snapshot.points,
            goalDifferenceDisplay = snapshot.goalDifferenceDisplay,
            recordSummary = snapshot.recordSummary,
        )
    }
}
