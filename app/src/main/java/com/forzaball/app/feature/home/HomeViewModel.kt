package com.forzaball.app.feature.home

import com.forzaball.app.core.mvi.MviViewModel
import com.forzaball.domain.usecase.ObserveFavoriteClubMatchUseCase
import com.forzaball.domain.usecase.ObserveFavoriteClubsNewsUseCase
import com.forzaball.domain.usecase.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
    private val observeFavoriteClubMatch: ObserveFavoriteClubMatchUseCase,
    private val observeFavoriteClubsNews: ObserveFavoriteClubsNewsUseCase,
) : MviViewModel<HomeIntent, HomeState, HomeEffect>(HomeState()) {

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Load -> loadHome()
            is HomeIntent.Refresh -> loadHome(force = intent.force)
        }
    }

    private suspend fun loadHome(force: Boolean = false) {
        setState { copy(isLoading = true, errorMessage = null) }

        // Observe preferences and then wire the rest based on favorites.
        observeUserPreferences().collectLatest { preferences ->
            val favoriteClubs = preferences.favoriteClubs

            viewModelScope.launch {
                observeFavoriteClubMatch().collectLatest { match ->
                    setState { copy(favoriteClubMatch = match) }
                }
            }

            viewModelScope.launch {
                observeFavoriteClubsNews(favoriteClubs).collectLatest { articles ->
                    setState { copy(isLoading = false, news = articles) }
                }
            }
        }
    }
}

