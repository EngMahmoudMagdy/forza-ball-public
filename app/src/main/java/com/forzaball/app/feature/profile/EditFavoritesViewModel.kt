package com.forzaball.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.app.feature.personalization.ClubItem
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.PreferencesRepository
import com.forzaball.domain.repository.SoccerTeamsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

data class EditFavoritesState(
    val step: Int = 1,
    val selectedLeagueIds: Set<String> = emptySet(),
    val selectedClubIds: Set<String> = emptySet(),
    val teamsByLeague: Map<String, List<ClubItem>> = emptyMap(),
    val isLoadingTeams: Boolean = false,
    val isSaving: Boolean = false,
    val closed: Boolean = false,
)

class EditFavoritesViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val soccerTeamsRepository: SoccerTeamsRepository,
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditFavoritesState())
    val state: StateFlow<EditFavoritesState> = _state.asStateFlow()

    fun resetFromStorage() {
        viewModelScope.launch {
            val p = preferencesRepository.observeUserPreferences().first()
            _state.value = EditFavoritesState(
                selectedLeagueIds = p.favoriteLeagues.toSet(),
                selectedClubIds = p.favoriteClubs.toSet(),
            )
        }
    }

    fun toggleLeague(slug: String) {
        val s = _state.value
        val newLeagues = if (slug in s.selectedLeagueIds) {
            s.selectedLeagueIds - slug
        } else {
            s.selectedLeagueIds + slug
        }
        val newClubs = if (slug !in newLeagues) {
            s.selectedClubIds.filter { id ->
                s.teamsByLeague[slug]?.any { it.id == id } != true
            }.toSet()
        } else {
            s.selectedClubIds
        }
        _state.value = s.copy(selectedLeagueIds = newLeagues, selectedClubIds = newClubs)
    }

    fun toggleClub(id: String) {
        val s = _state.value
        val clubs = s.selectedLeagueIds.flatMap { slug -> s.teamsByLeague[slug].orEmpty() }
        val club = clubs.find { it.id == id } ?: return
        val slug = club.leagueSlug
        val countInLeague = clubs.count { it.leagueSlug == slug && it.id in s.selectedClubIds }
        val selected = id in s.selectedClubIds
        if (selected) {
            _state.value = s.copy(selectedClubIds = s.selectedClubIds - id)
        } else if (countInLeague < 3) {
            _state.value = s.copy(selectedClubIds = s.selectedClubIds + id)
        }
    }

    fun nextStep() {
        val s = _state.value
        if (s.step == 1) {
            viewModelScope.launch {
                _state.value = s.copy(isLoadingTeams = true)
                runCatching {
                    buildMap {
                        s.selectedLeagueIds.forEach { slug ->
                            val clubs = soccerTeamsRepository.teamsForLeague(slug).map { domain ->
                                ClubItem(
                                    id = domain.id,
                                    name = domain.name,
                                    leagueSlug = slug,
                                    crestUrl = domain.crestUrl,
                                )
                            }
                            put(slug, clubs)
                        }
                    }
                }.onSuccess { map ->
                    _state.value = _state.value.copy(
                        teamsByLeague = map,
                        isLoadingTeams = false,
                        step = 2,
                    )
                }.onFailure { e ->
                    Timber.e(e, "load teams for edit favorites")
                    _state.value = _state.value.copy(isLoadingTeams = false)
                }
            }
            return
        }
    }

    fun previousStep() {
        val s = _state.value
        if (s.step > 1) {
            _state.value = s.copy(step = s.step - 1)
        }
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            _state.value = s.copy(isSaving = true)
            val existing = preferencesRepository.observeUserPreferences().first()
            val prefs = existing.copy(
                favoriteLeagues = s.selectedLeagueIds.toList(),
                favoriteClubs = s.selectedClubIds.toList(),
            )
            preferencesRepository.updateUserPreferences(prefs)
            runCatching { feedRepository.syncUserProfilePreferences(prefs) }
                .onFailure { Timber.w(it, "syncUserProfilePreferences") }
            _state.value = _state.value.copy(isSaving = false, closed = true)
        }
    }

    fun clearClosed() {
        _state.value = _state.value.copy(closed = false)
    }
}
