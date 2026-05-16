package com.forzaball.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.feature.personalization.ClubItem
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
    val selectedLeagueId: String? = null,
    val selectedClubId: String? = null,
    val teamsForLeague: List<ClubItem> = emptyList(),
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
                selectedLeagueId = p.favoriteTeamLeagueSlug,
                selectedClubId = p.favoriteTeamId,
            )
        }
    }

    fun selectLeague(slug: String) {
        val s = _state.value
        _state.value = s.copy(selectedLeagueId = slug, selectedClubId = null)
    }

    fun selectClub(id: String) {
        val s = _state.value
        _state.value = s.copy(selectedClubId = if (s.selectedClubId == id) null else id)
    }

    fun nextStep() {
        val s = _state.value
        if (s.step == 1) {
            val leagueId = s.selectedLeagueId ?: return
            viewModelScope.launch {
                _state.value = s.copy(isLoadingTeams = true)
                runCatching {
                    soccerTeamsRepository.teamsForLeague(leagueId).map { domain ->
                        ClubItem(
                            id = domain.id,
                            name = domain.name,
                            leagueSlug = leagueId,
                            crestUrl = domain.crestUrl,
                        )
                    }
                }.onSuccess { clubs ->
                    _state.value = _state.value.copy(
                        teamsForLeague = clubs,
                        isLoadingTeams = false,
                        step = 2,
                    )
                }.onFailure { e ->
                    Timber.e(e, "load teams for edit favorites")
                    _state.value = _state.value.copy(isLoadingTeams = false)
                }
            }
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
            val leagueId = s.selectedLeagueId
            val clubId = s.selectedClubId
            if (leagueId.isNullOrBlank() || clubId.isNullOrBlank()) return@launch
            _state.value = s.copy(isSaving = true)
            val existing = preferencesRepository.observeUserPreferences().first()
            val club = s.teamsForLeague.find { it.id == clubId }
            val prefs = existing.copy(
                favoriteTeamLeagueSlug = leagueId,
                favoriteTeamId = clubId,
                favoriteTeamName = club?.name,
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
