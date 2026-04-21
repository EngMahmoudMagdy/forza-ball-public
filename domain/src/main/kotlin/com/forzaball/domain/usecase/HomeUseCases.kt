package com.forzaball.domain.usecase

import com.forzaball.domain.model.HomeMatchContent
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.TeamNextMatch
import com.forzaball.domain.diagnostics.HomeLoadTracer
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.model.favoriteTeamIdsList
import com.forzaball.domain.model.leagueSlugsForEspnContent
import com.forzaball.domain.repository.MatchRepository
import com.forzaball.domain.repository.NewsRepository
import com.forzaball.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserPreferencesUseCase(
    private val preferencesRepository: PreferencesRepository,
) {
    operator fun invoke(): Flow<UserPreferences> = preferencesRepository.observeUserPreferences()
}

data class HomeScreenContent(
    val matches: HomeMatchContent,
    val news: List<NewsArticle>,
    val favoriteTeamNextMatch: TeamNextMatch?,
)

class LoadHomeContentUseCase(
    private val matchRepository: MatchRepository,
    private val newsRepository: NewsRepository,
    private val tracer: HomeLoadTracer = HomeLoadTracer { },
) {
    suspend operator fun invoke(preferences: UserPreferences): HomeScreenContent {
        val scoreboardLeagues = preferences.leagueSlugsForEspnContent()
        val teams = preferences.favoriteTeamIdsList()
        val homeScheduleLeagues = listOfNotNull(
            preferences.favoriteTeamLeagueSlug?.trim()?.takeIf { it.isNotEmpty() },
        )
        tracer(
            "LoadHomeContent start favoriteLeague=${preferences.favoriteTeamLeagueSlug} " +
                "favoriteTeamId=${preferences.favoriteTeamId} scoreboardLeagues=$scoreboardLeagues " +
                "teams=$teams scheduleLeagueSlugs=$homeScheduleLeagues",
        )
        tracer("API 1/3 loadFavoriteHighlightAndLive (ESPN scoreboards + team schedule merges)…")
        val matches = matchRepository.loadFavoriteHighlightAndLive(
            favoriteLeagueSlugs = scoreboardLeagues,
            favoriteTeamIds = teams,
            scheduleLeagueSlugs = homeScheduleLeagues.takeIf { it.isNotEmpty() },
        )
        tracer(
            "API 1/3 done highlightMatchId=${matches.highlightMatch?.id} liveMatches=${matches.liveMatches.size}",
        )
        tracer("API 2/3 loadNewsForDomesticLeague slug=${preferences.favoriteTeamLeagueSlug} maxArticles=200…")
        val news = newsRepository.loadNewsForDomesticLeague(
            preferences.favoriteTeamLeagueSlug,
            maxArticles = 200,
        )
        tracer("API 2/3 done articles=${news.size}")
        tracer(
            "API 3/3 loadNextMatchForFavoriteTeam league=${preferences.favoriteTeamLeagueSlug} " +
                "teamId=${preferences.favoriteTeamId}…",
        )
        val teamNext = matchRepository.loadNextMatchForFavoriteTeam(
            domesticLeagueSlug = preferences.favoriteTeamLeagueSlug,
            teamId = preferences.favoriteTeamId,
            fallbackTeamDisplayName = preferences.favoriteTeamName,
            includeChampionsLeagueSchedule = false,
        )
        tracer(
            "API 3/3 done nextGameId=${teamNext?.nextMatch?.id} teamLabel=${teamNext?.teamDisplayName}",
        )
        tracer("LoadHomeContent complete")
        return HomeScreenContent(matches = matches, news = news, favoriteTeamNextMatch = teamNext)
    }
}
