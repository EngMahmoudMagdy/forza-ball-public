package com.forzaball.domain.usecase

import com.forzaball.domain.model.Match
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

class ObserveFavoriteClubMatchUseCase(
    private val matchRepository: MatchRepository,
) {
    operator fun invoke(): Flow<Match?> = matchRepository.observeNextOrLiveMatchForFavoriteClub()
}

class ObserveFavoriteClubsNewsUseCase(
    private val newsRepository: NewsRepository,
) {
    operator fun invoke(clubIds: List<String>): Flow<List<NewsArticle>> =
        newsRepository.observeClubNews(clubIds)
}

