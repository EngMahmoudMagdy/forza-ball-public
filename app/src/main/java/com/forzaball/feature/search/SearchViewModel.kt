package com.forzaball.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.feature.personalization.LeagueItem
import com.forzaball.feature.personalization.catalogLeagues
import com.forzaball.domain.model.Club
import com.forzaball.domain.model.TeamSearchHistoryEntry
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.PreferencesRepository
import com.forzaball.domain.repository.SoccerTeamsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val MAX_HISTORY = 30

enum class SearchListLayout {
    List,
    Grid,
}

data class SearchUiState(
    val leagueQuery: String = "",
    val teamQuery: String = "",
    val selectedLeague: LeagueItem? = null,
    val teams: List<Club> = emptyList(),
    val isLoadingTeams: Boolean = false,
    val teamLoadError: String? = null,
    val history: List<TeamSearchHistoryEntry> = emptyList(),
    val leagueLayout: SearchListLayout = SearchListLayout.List,
    val teamLayout: SearchListLayout = SearchListLayout.List,
)

class SearchViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val soccerTeamsRepository: SoccerTeamsRepository,
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(SearchUiState())
    val ui: StateFlow<SearchUiState> = _ui.asStateFlow()

    private var lastKnownPrefs: UserPreferences? = null

    init {
        viewModelScope.launch {
            runCatching { feedRepository.mergeTeamSearchHistoryFromRemote() }
                .onFailure { Timber.w(it, "mergeTeamSearchHistoryFromRemote") }
        }
        viewModelScope.launch {
            preferencesRepository.observeUserPreferences().collect { prefs ->
                lastKnownPrefs = prefs
                _ui.update { it.copy(history = prefs.teamSearchHistory) }
            }
        }
    }

    fun onLeagueQueryChange(q: String) {
        _ui.update { it.copy(leagueQuery = q) }
    }

    fun onTeamQueryChange(q: String) {
        _ui.update { it.copy(teamQuery = q) }
    }

    fun setLeagueLayout(layout: SearchListLayout) {
        _ui.update { it.copy(leagueLayout = layout) }
    }

    fun setTeamLayout(layout: SearchListLayout) {
        _ui.update { it.copy(teamLayout = layout) }
    }

    fun selectLeague(league: LeagueItem) {
        _ui.update {
            it.copy(
                selectedLeague = league,
                teamQuery = "",
                teams = emptyList(),
                isLoadingTeams = true,
                teamLoadError = null,
            )
        }
        viewModelScope.launch {
            runCatching { soccerTeamsRepository.teamsForLeague(league.id) }
                .onSuccess { clubs ->
                    _ui.update {
                        it.copy(
                            teams = clubs,
                            isLoadingTeams = false,
                            teamLoadError = null,
                        )
                    }
                }
                .onFailure { e ->
                    Timber.w(e, "teamsForLeague %s", league.id)
                    _ui.update {
                        it.copy(
                            isLoadingTeams = false,
                            teamLoadError = e.message ?: "Could not load teams",
                        )
                    }
                }
        }
    }

    fun clearLeague() {
        _ui.update {
            it.copy(
                selectedLeague = null,
                teams = emptyList(),
                teamQuery = "",
                teamLoadError = null,
            )
        }
    }

    fun filteredLeagues(): List<LeagueItem> {
        val q = _ui.value.leagueQuery.trim().lowercase()
        if (q.isEmpty()) return catalogLeagues
        return catalogLeagues.filter { item ->
            item.name.lowercase().contains(q) ||
                item.country.lowercase().contains(q) ||
                item.id.lowercase().contains(q)
        }
    }

    fun filteredTeams(): List<Club> {
        val q = _ui.value.teamQuery.trim().lowercase()
        val teams = _ui.value.teams
        if (q.isEmpty()) return teams
        return teams.filter { it.name.lowercase().contains(q) }
    }

    fun removeHistoryEntry(entry: TeamSearchHistoryEntry) {
        viewModelScope.launch {
            val prefs = lastKnownPrefs ?: preferencesRepository.observeUserPreferences().first()
            val next = prefs.teamSearchHistory.filterNot { it.sameKey(entry) }
            val updated = prefs.copy(teamSearchHistory = next)
            preferencesRepository.updateUserPreferences(updated)
            runCatching { feedRepository.syncUserProfilePreferences(updated) }
                .onFailure { Timber.w(it, "syncUserProfilePreferences") }
        }
    }

    fun recordSearchAndGetEntry(
        club: Club,
        league: LeagueItem,
    ): TeamSearchHistoryEntry {
        val entry = TeamSearchHistoryEntry(
            teamId = club.id,
            leagueSlug = league.id,
            teamName = club.name,
            leagueName = league.name,
            teamCrestUrl = club.crestUrl,
            searchedAtMillis = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            val prefs = lastKnownPrefs ?: preferencesRepository.observeUserPreferences().first()
            val without = prefs.teamSearchHistory.filterNot { it.sameKey(entry) }
            val merged = (listOf(entry) + without).take(MAX_HISTORY)
            val updated = prefs.copy(teamSearchHistory = merged)
            preferencesRepository.updateUserPreferences(updated)
            runCatching { feedRepository.syncUserProfilePreferences(updated) }
                .onFailure { Timber.w(it, "syncUserProfilePreferences") }
        }
        return entry
    }
}

private fun TeamSearchHistoryEntry.sameKey(other: TeamSearchHistoryEntry): Boolean =
    teamId == other.teamId && leagueSlug == other.leagueSlug
