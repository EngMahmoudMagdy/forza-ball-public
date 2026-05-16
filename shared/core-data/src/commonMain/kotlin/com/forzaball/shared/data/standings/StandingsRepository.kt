package com.forzaball.shared.data.standings

import com.forzaball.shared.domain.model.ScoreContext
import com.forzaball.shared.domain.model.StandingSnapshot
import com.forzaball.shared.domain.model.normalizedLeagueSlug
import com.forzaball.shared.domain.model.normalizedTeamId
import com.forzaball.shared.platform.PlatformLogger

interface SharedStandingsRepository {
    suspend fun getFavoriteTeamStanding(context: ScoreContext): StandingSnapshot?
    suspend fun getTeamStanding(leagueSlug: String, teamId: String): StandingSnapshot?
}

class SharedStandingsRepositoryImpl(
    private val api: EspnStandingsApi = EspnStandingsApi(),
) : SharedStandingsRepository {
    override suspend fun getFavoriteTeamStanding(context: ScoreContext): StandingSnapshot? {
        val leagueSlug = context.normalizedLeagueSlug() ?: return null
        val teamId = context.normalizedTeamId() ?: return null
        return getTeamStanding(leagueSlug = leagueSlug, teamId = teamId)
    }

    override suspend fun getTeamStanding(leagueSlug: String, teamId: String): StandingSnapshot? {
        val slug = leagueSlug.trim().takeIf { it.isNotEmpty() } ?: return null
        val tid = teamId.trim().takeIf { it.isNotEmpty() } ?: return null
        return runCatching {
            val dto = api.standings(slug)
            val entry = dto.findEntryForTeam(tid) ?: return@runCatching null
            val leagueLabel = dto.name?.takeIf { it.isNotBlank() } ?: slug
            entry.toSnapshot(slug, leagueLabel)
        }.onFailure { error ->
            PlatformLogger.w("SharedStandingsRepository", "Failed standings for $slug / $tid", error)
        }.getOrNull()
    }
}

private fun List<EspnStandingStatDto>.statInt(vararg keys: String): Int {
    val lower = keys.map { it.lowercase() }
    val hit = firstOrNull { s ->
        val n = s.name?.lowercase() ?: return@firstOrNull false
        n in lower
    } ?: return 0
    return hit.displayValue?.toIntOrNull() ?: hit.value?.toInt() ?: 0
}

private fun List<EspnStandingStatDto>.statString(vararg keys: String): String? {
    val lower = keys.map { it.lowercase() }
    val hit = firstOrNull { s ->
        val n = s.name?.lowercase() ?: return@firstOrNull false
        n in lower
    } ?: return null
    return hit.displayValue?.takeIf { it.isNotBlank() }
}

private fun EspnStandingsApiRootDto.findEntryForTeam(teamId: String): EspnStandingsEntryDto? {
    val tid = teamId.trim()
    if (tid.isEmpty()) return null
    for (ch in children.orEmpty()) {
        val found = ch.standings?.entries.orEmpty().firstOrNull { it.team?.id == tid }
        if (found != null) return found
    }
    return null
}

private fun EspnStandingsEntryDto.toSnapshot(leagueSlug: String, leagueDisplayName: String): StandingSnapshot {
    val statsSafe = stats.orEmpty()
    return StandingSnapshot(
        leagueSlug = leagueSlug,
        leagueDisplayName = leagueDisplayName,
        teamId = team?.id.orEmpty(),
        teamName = team?.displayName.orEmpty(),
        position = statsSafe.statInt("rank"),
        played = statsSafe.statInt("gamesplayed", "gamesPlayed"),
        wins = statsSafe.statInt("wins"),
        draws = statsSafe.statInt("ties", "draws"),
        losses = statsSafe.statInt("losses"),
        points = statsSafe.statInt("points"),
        goalDifferenceDisplay = statsSafe.statString("pointdifferential", "pointDifferential"),
        recordSummary = statsSafe.firstOrNull { it.type?.equals("total", ignoreCase = true) == true }?.summary
            ?: statsSafe.firstOrNull { it.name?.equals("overall", ignoreCase = true) == true }?.displayValue,
    )
}
