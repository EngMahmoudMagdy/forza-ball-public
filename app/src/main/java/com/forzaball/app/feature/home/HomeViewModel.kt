package com.forzaball.app.feature.home

import androidx.lifecycle.viewModelScope
import com.forzaball.app.core.mvi.MviViewModel
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.usecase.LoadHomeContentUseCase
import com.forzaball.domain.usecase.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
    private val loadHomeContent: LoadHomeContentUseCase,
) : MviViewModel<HomeIntent, HomeState, HomeEffect>(HomeState()) {

    init {
        viewModelScope.launch {
            observeUserPreferences().collectLatest { preferences ->
                setState { copy(userPreferences = preferences) }
                loadContent(preferences)
            }
        }
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Load -> Unit
            is HomeIntent.Refresh -> {
                val prefs = observeUserPreferences().first()
                loadContent(prefs)
            }
        }
    }

    private suspend fun loadContent(preferences: UserPreferences) {
        setState { copy(isLoading = true, errorMessage = null) }
        runCatching {
            loadHomeContent(preferences)
        }.onSuccess { content ->
            setState {
                copy(
                    isLoading = false,
                    favoriteClubMatch = content.matches.highlightMatch,
                    liveMatches = content.matches.liveMatches,
                    teamNextMatches = content.teamNextMatches,
                    news = content.news,
                    errorMessage = null,
                )
            }
        }.onFailure { e ->
            setState {
                copy(
                    isLoading = false,
                    errorMessage = e.message,
                )
            }
        }
    }
}
