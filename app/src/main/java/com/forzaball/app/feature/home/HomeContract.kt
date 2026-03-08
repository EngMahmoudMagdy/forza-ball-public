package com.forzaball.app.feature.home

import com.forzaball.app.core.mvi.UiEffect
import com.forzaball.app.core.mvi.UiIntent
import com.forzaball.app.core.mvi.UiState
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle

sealed class HomeIntent : UiIntent {
    data object Load : HomeIntent()
    data class Refresh(val force: Boolean = false) : HomeIntent()
}

data class HomeState(
    val isLoading: Boolean = false,
    val favoriteClubMatch: Match? = null,
    val news: List<NewsArticle> = emptyList(),
    val errorMessage: String? = null,
) : UiState

sealed class HomeEffect : UiEffect {
    data class ShowMessage(val message: String) : HomeEffect()
}

