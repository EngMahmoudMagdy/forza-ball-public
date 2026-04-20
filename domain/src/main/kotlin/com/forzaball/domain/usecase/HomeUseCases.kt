package com.forzaball.domain.usecase

import com.forzaball.domain.model.HomeMatchContent
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.TeamNextMatch
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
) {
    suspend operator fun invoke(preferences: UserPreferences): HomeScreenContent {
        val leagues = preferences.leagueSlugsForEspnContent()
        val teams = preferences.favoriteTeamIdsList()
        val matches = matchRepository.loadFavoriteHighlightAndLive(leagues, teams)
        val news = newsRepository.loadNewsForPreferences(
            leagues,
            teams,
            maxArticles = 40,
        )
        val teamNext = matchRepository.loadNextMatchForFavoriteTeam(
            preferences.favoriteTeamLeagueSlug,
            preferences.favoriteTeamId,
            preferences.favoriteTeamName,
        )
        return HomeScreenContent(matches = matches, news = news, favoriteTeamNextMatch = teamNext)
    }
}
