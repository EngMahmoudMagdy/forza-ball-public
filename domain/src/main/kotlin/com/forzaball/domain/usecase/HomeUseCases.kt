package com.forzaball.domain.usecase

import com.forzaball.domain.model.HomeMatchContent
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.UserPreferences
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
)

class LoadHomeContentUseCase(
    private val matchRepository: MatchRepository,
    private val newsRepository: NewsRepository,
) {
    suspend operator fun invoke(clubIds: List<String>): HomeScreenContent {
        val matches = matchRepository.loadFavoriteHighlightAndLive(clubIds)
        val news = newsRepository.loadNewsForClubs(clubIds)
        return HomeScreenContent(matches = matches, news = news)
    }
}
