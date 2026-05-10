package com.forzaball.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.forzaball.feature.home.paging.FixturesPagingSource
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.favoriteTeamIdsList
import com.forzaball.domain.model.leagueSlugsForEspnContent
import com.forzaball.domain.repository.MatchRepository
import com.forzaball.domain.usecase.ObserveUserPreferencesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class FixturesListViewModel(
    private val matchRepository: MatchRepository,
    private val observeUserPreferences: ObserveUserPreferencesUseCase,
) : ViewModel() {

    val fixturesPaging: Flow<PagingData<Match>> = flow {
        emit(observeUserPreferences().first())
    }.flatMapLatest { prefs ->
                Pager(
                    config = PagingConfig(
                        pageSize = 20,
                        enablePlaceholders = false,
                        prefetchDistance = 40,
                    ),
                    pagingSourceFactory = {
                        FixturesPagingSource(
                            matchRepository = matchRepository,
                            leagues = prefs.leagueSlugsForEspnContent(),
                            teams = prefs.favoriteTeamIdsList(),
                        )
                    },
                ).flow
    }.cachedIn(viewModelScope)
}
