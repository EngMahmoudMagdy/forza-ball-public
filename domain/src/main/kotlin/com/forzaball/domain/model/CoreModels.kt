package com.forzaball.domain.model

data class League(
    val id: String,
    val name: String,
    val countryCode: String,
)

data class Club(
    val id: String,
    val name: String,
    val leagueId: String,
    val crestUrl: String?,
)

data class Match(
    val id: String,
    val homeClub: Club,
    val awayClub: Club,
    val startTimeMillis: Long,
    val isLive: Boolean,
    val homeScore: Int? = null,
    val awayScore: Int? = null,
    /** Short status from API-Football (e.g. NS, FT, 1H). */
    val statusShort: String? = null,
    /** Live minute when available. */
    val minuteElapsed: Int? = null,
    val leagueName: String? = null,
    /** From ESPN when the fixture is final / post-match. */
    val isCompleted: Boolean = false,
)

/** Highlight card + horizontal live list for the home screen. */
data class HomeMatchContent(
    val highlightMatch: Match?,
    val liveMatches: List<Match>,
)

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val imageUrl: String?,
    val publishedAtMillis: Long,
    val leagueId: String?,
    val clubIds: List<String>,
    /** ESPN story URL (web). */
    val articleUrl: String? = null,
)

/** Next scheduled / live fixture for a favorite team from the team schedule API. */
data class TeamNextMatch(
    val teamId: String,
    val teamDisplayName: String,
    val teamCrestUrl: String?,
    val nextMatch: Match?,
)

data class UserPreferences(
    val countryCode: String?,
    val favoriteLeagues: List<String>,
    val favoriteClubs: List<String>,
    val nickname: String? = null,
    val profilePhotoUrl: String? = null,
)

