package com.forzaball.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.forzaball.app.feature.home.paging.NewsPagingSource
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.repository.NewsRepository
import com.forzaball.domain.usecase.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class NewsListViewModel(
    private val newsRepository: NewsRepository,
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
) : ViewModel() {

    val newsPaging: Flow<PagingData<NewsArticle>> = flow {
        emit(observeUserPreferences().first())
    }.flatMapLatest { prefs ->
                Pager(
                    config = PagingConfig(
                        pageSize = 20,
                        enablePlaceholders = false,
                        prefetchDistance = 40,
                    ),
                    pagingSourceFactory = {
                        NewsPagingSource(
                            newsRepository = newsRepository,
                            domesticLeagueSlug = prefs.favoriteTeamLeagueSlug,
                        )
                    },
                ).flow
    }.cachedIn(viewModelScope)
}
