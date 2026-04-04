package com.forzaball.data.network

import com.forzaball.domain.model.Club
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FixturesEnvelope(
    val response: List<FixtureDto>? = null,
)

@Serializable
data class FixtureDto(
    val fixture: FixtureInfoDto,
    val league: LeagueInfoDto,
    val teams: TeamsDto,
    val goals: GoalsDto? = null,
)

@Serializable
data class FixtureInfoDto(
    val id: Long,
    val timestamp: Long,
    val status: StatusDto,
)

@Serializable
data class StatusDto(
    val short: String? = null,
    @SerialName("long") val longName: String? = null,
    val elapsed: Int? = null,
)

@Serializable
data class LeagueInfoDto(
    val id: Int? = null,
    val name: String? = null,
)

@Serializable
data class TeamsDto(
    val home: TeamDto,
    val away: TeamDto,
)

@Serializable
data class TeamDto(
    val id: Int,
    val name: String,
    val logo: String? = null,
)

@Serializable
data class GoalsDto(
    val home: Int? = null,
    val away: Int? = null,
)

@Serializable
data class InjuriesEnvelope(
    val response: List<InjuryDto>? = null,
)

@Serializable
data class InjuryDto(
    val player: PlayerDto,
    val team: TeamDto? = null,
    val league: LeagueInfoDto? = null,
    val fixture: InjuryFixtureDto? = null,
    val type: String? = null,
)

@Serializable
data class PlayerDto(
    val id: Int,
    val name: String,
    val photo: String? = null,
)

@Serializable
data class InjuryFixtureDto(
    val id: Long? = null,
    val timestamp: Long? = null,
)

private val liveStatusCodes = setOf(
    "1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE",
    "Q1", "Q2", "Q3", "Q4",
)

internal fun FixtureDto.toMatch(): Match {
    val leagueId = league.id?.toString().orEmpty()
    val short = fixture.status.short
    val isLive = short != null && short in liveStatusCodes
    return Match(
        id = fixture.id.toString(),
        homeClub = teams.home.toClub(leagueId),
        awayClub = teams.away.toClub(leagueId),
        startTimeMillis = fixture.timestamp * 1000L,
        isLive = isLive,
        homeScore = goals?.home,
        awayScore = goals?.away,
        statusShort = short,
        minuteElapsed = fixture.status.elapsed,
        leagueName = league.name,
    )
}

private fun TeamDto.toClub(leagueId: String): Club = Club(
    id = id.toString(),
    name = name,
    leagueId = leagueId,
    crestUrl = logo,
)

internal fun FixtureDto.toResultNewsArticle(): NewsArticle {
    val home = teams.home
    val away = teams.away
    val homeGoals = goals?.home
    val awayGoals = goals?.away
    val title = when {
        homeGoals != null && awayGoals != null ->
            "${home.name} $homeGoals – $awayGoals ${away.name}"
        else -> "${home.name} vs ${away.name}"
    }
    val summary = listOfNotNull(
        league.name,
        fixture.status.longName ?: fixture.status.short,
    ).joinToString(" · ")
    return NewsArticle(
        id = "fixture-result-${fixture.id}",
        title = title,
        summary = summary,
        imageUrl = home.logo,
        publishedAtMillis = fixture.timestamp * 1000L,
        leagueId = league.id?.toString(),
        clubIds = listOf(home.id.toString(), away.id.toString()),
    )
}

internal fun InjuryDto.toNewsArticle(): NewsArticle {
    val ts = fixture?.timestamp ?: 0L
    return NewsArticle(
        id = "injury-${player.id}-${fixture?.id ?: team?.id ?: 0}",
        title = "${player.name} — ${type ?: "Update"}",
        summary = team?.name.orEmpty(),
        imageUrl = player.photo,
        publishedAtMillis = if (ts > 0) ts * 1000L else System.currentTimeMillis(),
        leagueId = league?.id?.toString(),
        clubIds = listOfNotNull(team?.id?.toString()),
    )
}
