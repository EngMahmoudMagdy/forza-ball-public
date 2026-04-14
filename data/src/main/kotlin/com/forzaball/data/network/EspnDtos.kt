package com.forzaball.data.network

import com.forzaball.domain.model.Club
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeParseException

@Serializable
data class EspnScoreboardDto(
    val events: List<EspnEventDto>? = null,
)

@Serializable
data class EspnEventDto(
    val id: String? = null,
    val date: String? = null,
    val name: String? = null,
    val season: EspnEventSeasonDto? = null,
    val competitions: List<EspnCompetitionDto>? = null,
)

@Serializable
data class EspnEventSeasonDto(
    val year: Int? = null,
    @SerialName("displayName") val displayName: String? = null,
)

@Serializable
data class EspnTeamScheduleEnvelopeDto(
    val team: EspnScheduleHeaderTeamDto? = null,
    val events: List<EspnEventDto>? = null,
)

@Serializable
data class EspnScheduleHeaderTeamDto(
    val id: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    val logo: String? = null,
)

@Serializable
data class EspnCompetitionDto(
    val id: String? = null,
    val date: String? = null,
    @SerialName("startDate") val startDate: String? = null,
    val status: EspnStatusDto? = null,
    val competitors: List<EspnCompetitorDto>? = null,
)

@Serializable
data class EspnStatusDto(
    val clock: Double? = null,
    @SerialName("displayClock") val displayClock: String? = null,
    val type: EspnStatusTypeDto? = null,
)

@Serializable
data class EspnStatusTypeDto(
    val id: String? = null,
    val name: String? = null,
    val state: String? = null,
    val completed: Boolean? = null,
    @SerialName("shortDetail") val shortDetail: String? = null,
    val detail: String? = null,
)

@Serializable
data class EspnCompetitorDto(
    val id: String? = null,
    @SerialName("homeAway") val homeAway: String? = null,
    val score: String? = null,
    val team: EspnScoreboardTeamDto? = null,
)

@Serializable
data class EspnScoreboardTeamDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    val logo: String? = null,
    val logos: List<EspnLogoDto>? = null,
)

@Serializable
data class EspnNewsEnvelopeDto(
    val articles: List<EspnNewsArticleDto>? = null,
)

@Serializable
data class EspnNewsArticleDto(
    val id: Long? = null,
    val headline: String? = null,
    val description: String? = null,
    val published: String? = null,
    @SerialName("lastModified") val lastModified: String? = null,
    val images: List<EspnNewsImageDto>? = null,
    val categories: List<EspnNewsCategoryDto>? = null,
    val links: EspnArticleLinksDto? = null,
)

@Serializable
data class EspnArticleLinksDto(
    val web: EspnArticleHrefDto? = null,
    val mobile: EspnArticleHrefDto? = null,
)

@Serializable
data class EspnArticleHrefDto(
    val href: String? = null,
)

@Serializable
data class EspnNewsImageDto(
    val url: String? = null,
    val type: String? = null,
)

@Serializable
data class EspnNewsCategoryDto(
    val type: String? = null,
    val team: EspnCategoryTeamDto? = null,
)

@Serializable
data class EspnCategoryTeamDto(
    val id: Int? = null,
)

@Serializable
data class EspnTeamsEnvelopeDto(
    val sports: List<EspnSportDto>? = null,
)

@Serializable
data class EspnSportDto(
    val leagues: List<EspnLeagueTeamsDto>? = null,
)

@Serializable
data class EspnLeagueTeamsDto(
    val slug: String? = null,
    val name: String? = null,
    val teams: List<EspnTeamEntryDto>? = null,
)

@Serializable
data class EspnTeamEntryDto(
    val team: EspnTeamDetailDto? = null,
)

@Serializable
data class EspnTeamDetailDto(
    val id: String? = null,
    val name: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    val logos: List<EspnLogoDto>? = null,
)

@Serializable
data class EspnLogoDto(
    val href: String? = null,
    val rel: List<String>? = null,
)

internal fun EspnTeamsEnvelopeDto.toClubs(leagueSlug: String): List<Club> {
    val entries = sports.orEmpty()
        .flatMap { it.leagues.orEmpty() }
        .filter { it.slug == leagueSlug }
        .flatMap { it.teams.orEmpty() }
    return entries.mapNotNull { entry ->
        val t = entry.team ?: return@mapNotNull null
        val id = t.id ?: return@mapNotNull null
        val crest = t.logos.orEmpty().firstOrNull { logo ->
            logo.rel.orEmpty().contains("default")
        }?.href ?: t.logos?.firstOrNull()?.href
        Club(
            id = id,
            name = t.displayName ?: t.name.orEmpty(),
            leagueId = leagueSlug,
            crestUrl = crest,
        )
    }
}

private fun EspnScoreboardTeamDto.crestUrl(): String? {
    logo?.takeIf { it.isNotBlank() }?.let { return it }
    val rels = logos.orEmpty()
    return rels.firstOrNull { it.rel.orEmpty().contains("default") }?.href
        ?: rels.firstOrNull()?.href
}

internal fun EspnCompetitionDto.toMatch(
    eventId: String?,
    leagueSlug: String,
    leagueDisplayName: String?,
): Match? {
    val comps = competitors.orEmpty()
    val home = comps.firstOrNull { it.homeAway == "home" } ?: return null
    val away = comps.firstOrNull { it.homeAway == "away" } ?: return null
    val homeTeam = home.team ?: return null
    val awayTeam = away.team ?: return null
    val homeId = homeTeam.id ?: home.id ?: return null
    val awayId = awayTeam.id ?: away.id ?: return null
    val state = status?.type?.state
    val isLive = state == "in"
    val completed = status?.type?.completed == true || state == "post"
    val startIso = startDate ?: date ?: return null
    val startMillis = parseEspnInstant(startIso) ?: return null
    val statusShort = status?.type?.shortDetail ?: status?.type?.name
    val minute = parseMinute(status?.displayClock)
    return Match(
        id = id ?: eventId.orEmpty(),
        homeClub = Club(
            id = homeId,
            name = homeTeam.displayName ?: homeTeam.name.orEmpty(),
            leagueId = leagueSlug,
            crestUrl = homeTeam.crestUrl(),
        ),
        awayClub = Club(
            id = awayId,
            name = awayTeam.displayName ?: awayTeam.name.orEmpty(),
            leagueId = leagueSlug,
            crestUrl = awayTeam.crestUrl(),
        ),
        startTimeMillis = startMillis,
        isLive = isLive,
        homeScore = home.score?.toIntOrNull(),
        awayScore = away.score?.toIntOrNull(),
        statusShort = statusShort,
        minuteElapsed = minute,
        leagueName = leagueDisplayName ?: leagueSlug,
        isCompleted = completed,
    )
}

private fun parseMinute(display: String?): Int? {
    if (display.isNullOrBlank()) return null
    val digits = Regex("(\\d+)").find(display)?.groupValues?.getOrNull(1) ?: return null
    return digits.toIntOrNull()
}

private fun parseEspnInstant(iso: String): Long? = try {
    Instant.parse(iso).toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}

internal fun EspnEventDto.toMatchFromEvent(): Match? {
    val comp = competitions?.firstOrNull() ?: return null
    return comp.toMatch(id, "schedule", season?.displayName)
}

internal fun EspnNewsArticleDto.toNewsArticle(leagueSlug: String): NewsArticle? {
    val idStr = id?.toString() ?: return null
    val title = headline ?: return null
    val summary = description.orEmpty()
    val imageUrl = images.orEmpty().firstOrNull { !it.url.isNullOrBlank() }?.url
    val publishedIso = published ?: lastModified ?: return null
    val millis = parseEspnInstant(publishedIso) ?: System.currentTimeMillis()
    val teamIds = categories.orEmpty()
        .filter { it.type == "team" }
        .mapNotNull { it.team?.id?.toString() }
    val webUrl = links?.web?.href?.takeIf { it.isNotBlank() }
        ?: links?.mobile?.href?.takeIf { it.isNotBlank() }
    return NewsArticle(
        id = "espn-$leagueSlug-$idStr",
        title = title,
        summary = summary,
        imageUrl = imageUrl,
        publishedAtMillis = millis,
        leagueId = leagueSlug,
        clubIds = teamIds,
        articleUrl = webUrl,
    )
}
