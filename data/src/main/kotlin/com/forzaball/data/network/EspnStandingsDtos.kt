package com.forzaball.data.network

import com.forzaball.domain.model.TeamStandingSnapshot
import kotlinx.serialization.Serializable

@Serializable
data class EspnStandingsApiRootDto(
    val name: String? = null,
    val children: List<EspnStandingsChildGroupDto>? = null,
)

@Serializable
data class EspnStandingsChildGroupDto(
    val standings: EspnStandingsTableDto? = null,
)

@Serializable
data class EspnStandingsTableDto(
    val entries: List<EspnStandingsEntryDto>? = null,
)

@Serializable
data class EspnStandingsEntryDto(
    val team: EspnStandingsTeamRefDto? = null,
    val stats: List<EspnStandingStatDto>? = null,
)

@Serializable
data class EspnStandingsTeamRefDto(
    val id: String? = null,
    val displayName: String? = null,
)

@Serializable
data class EspnStandingStatDto(
    val name: String? = null,
    val type: String? = null,
    val summary: String? = null,
    val displayValue: String? = null,
    val value: Double? = null,
)

private fun List<EspnStandingStatDto>.statInt(vararg keys: String): Int {
    val lower = keys.map { it.lowercase() }
    val hit = firstOrNull { s ->
        val n = s.name?.lowercase() ?: return@firstOrNull false
        n in lower
    } ?: return 0
    return hit.displayValue?.toIntOrNull()
        ?: hit.value?.toInt()
        ?: 0
}

private fun List<EspnStandingStatDto>.statString(vararg keys: String): String? {
    val lower = keys.map { it.lowercase() }
    val hit = firstOrNull { s ->
        val n = s.name?.lowercase() ?: return@firstOrNull false
        n in lower
    } ?: return null
    return hit.displayValue?.takeIf { it.isNotBlank() }
}

internal fun EspnStandingsApiRootDto.findEntryForTeam(teamId: String): EspnStandingsEntryDto? {
    val tid = teamId.trim()
    if (tid.isEmpty()) return null
    for (ch in children.orEmpty()) {
        val found = ch.standings?.entries.orEmpty().firstOrNull { it.team?.id == tid }
        if (found != null) return found
    }
    return null
}

internal fun EspnStandingsEntryDto.toSnapshot(leagueSlug: String, leagueDisplayName: String): TeamStandingSnapshot {
    val stats = stats.orEmpty()
    val teamName = team?.displayName.orEmpty()
    return TeamStandingSnapshot(
        leagueSlug = leagueSlug,
        leagueDisplayName = leagueDisplayName,
        teamName = teamName,
        position = stats.statInt("rank"),
        played = stats.statInt("gamesplayed", "gamesPlayed"),
        wins = stats.statInt("wins"),
        draws = stats.statInt("ties", "draws"),
        losses = stats.statInt("losses"),
        points = stats.statInt("points"),
        goalDifferenceDisplay = stats.statString("pointdifferential", "pointDifferential"),
        recordSummary = stats.firstOrNull { it.type?.equals("total", ignoreCase = true) == true }?.summary
            ?: stats.firstOrNull { it.name?.equals("overall", ignoreCase = true) == true }?.displayValue,
    )
}
