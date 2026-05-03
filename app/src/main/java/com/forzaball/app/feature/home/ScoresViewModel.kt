package com.forzaball.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.TeamStandingSnapshot
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.model.favoriteTeamIdsList
import com.forzaball.domain.model.leagueSlugsForEspnContent
import com.forzaball.domain.model.shouldShowUclScores
import com.forzaball.domain.repository.MatchRepository
import com.forzaball.domain.repository.PreferencesRepository
import com.forzaball.domain.repository.StandingsRepository
import com.forzaball.shared.domain.model.ScoreContext
import com.forzaball.shared.domain.model.normalizedLeagueSlug
import com.forzaball.shared.domain.model.normalizedTeamId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScoresUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val domesticStanding: TeamStandingSnapshot? = null,
    val uclStanding: TeamStandingSnapshot? = null,
    val showUclSection: Boolean = false,
    val fixtures: List<Match> = emptyList(),
)

class ScoresViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val standingsRepository: StandingsRepository,
    private val matchRepository: MatchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ScoresUiState())
    val state: StateFlow<ScoresUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.observeUserPreferences().collectLatest { prefs ->
                loadForPreferences(prefs)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadForPreferences(preferencesRepository.observeUserPreferences().first())
        }
    }

    private suspend fun loadForPreferences(prefs: UserPreferences) {
        val scoreContext = ScoreContext(
            favoriteTeamLeagueSlug = prefs.favoriteTeamLeagueSlug,
            favoriteTeamId = prefs.favoriteTeamId,
        )
        val league = scoreContext.normalizedLeagueSlug()
        val teamId = scoreContext.normalizedTeamId()
        if (league == null || teamId == null) {
            _state.value = ScoresUiState()
            return
        }
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        runCatching {
            val showUcl = prefs.shouldShowUclScores()
            val leagues = prefs.leagueSlugsForEspnContent()
            val teams = prefs.favoriteTeamIdsList()
            coroutineScope {
                val domestic = async { standingsRepository.getTeamStanding(league, teamId) }
                val ucl = async {
                    if (showUcl) standingsRepository.getTeamStanding("uefa.champions", teamId) else null
                }
                val fixtures = async { matchRepository.loadMergedFixtures(leagues, teams) }
                ScoresUiState(
                    isLoading = false,
                    domesticStanding = domestic.await(),
                    uclStanding = ucl.await(),
                    showUclSection = showUcl,
                    fixtures = fixtures.await(),
                )
            }
        }.onSuccess { next ->
            _state.value = next
        }.onFailure { e ->
            _state.update {
                it.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }
}
