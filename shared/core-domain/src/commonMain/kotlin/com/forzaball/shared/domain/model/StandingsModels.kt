package com.forzaball.shared.domain.model

data class StandingSnapshot(
    val leagueSlug: String,
    val leagueDisplayName: String,
    val teamId: String,
    val teamName: String,
    val position: Int,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val points: Int,
    val goalDifferenceDisplay: String?,
    val recordSummary: String?,
)

data class ScoreContext(
    val favoriteTeamLeagueSlug: String?,
    val favoriteTeamId: String?,
)

fun ScoreContext.normalizedLeagueSlug(): String? = favoriteTeamLeagueSlug?.trim()?.takeIf { it.isNotEmpty() }

fun ScoreContext.normalizedTeamId(): String? = favoriteTeamId?.trim()?.takeIf { it.isNotEmpty() }
