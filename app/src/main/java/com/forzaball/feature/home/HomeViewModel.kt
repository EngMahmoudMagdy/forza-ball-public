package com.forzaball.feature.home

import androidx.lifecycle.viewModelScope
import com.forzaball.core.mvi.MviViewModel
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.usecase.LoadHomeContentUseCase
import com.forzaball.domain.usecase.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class HomeViewModel(
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
    private val loadHomeContent: LoadHomeContentUseCase,
) : MviViewModel<HomeIntent, HomeState, HomeEffect>(HomeState()) {

    init {
        viewModelScope.launch {
            observeUserPreferences().collectLatest { preferences ->
                Timber.tag("HomePage").d(
                    "preferences update league=%s teamId=%s",
                    preferences.favoriteTeamLeagueSlug,
                    preferences.favoriteTeamId,
                )
                setState { copy(userPreferences = preferences) }
                loadContent(preferences)
            }
        }
    }

    override suspend fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Load -> Unit
            is HomeIntent.Refresh -> {
                Timber.tag("HomePage").d("user refresh")
                val prefs = observeUserPreferences().first()
                loadContent(prefs)
            }
        }
    }

    private suspend fun loadContent(preferences: UserPreferences) {
        Timber.tag("HomePage").d("loadContent start")
        setState { copy(isLoading = true, errorMessage = null) }
        runCatching {
            loadHomeContent(preferences)
        }.onSuccess { content ->
            Timber.tag("HomePage").d(
                "loadContent success news=%d nextMatch=%s",
                content.news.size,
                content.favoriteTeamNextMatch?.nextMatch?.id,
            )
            setState {
                copy(
                    isLoading = false,
                    favoriteClubMatch = content.matches.highlightMatch,
                    liveMatches = content.matches.liveMatches,
                    favoriteTeamNextMatch = content.favoriteTeamNextMatch,
                    news = content.news,
                    errorMessage = null,
                )
            }
        }.onFailure { e ->
            Timber.tag("HomePage").e(e, "loadContent failed")
            setState {
                copy(
                    isLoading = false,
                    errorMessage = e.message,
                )
            }
        }
    }
}
