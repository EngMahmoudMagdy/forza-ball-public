package com.forzaball.app.feature.personalization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PersonalizationState(
    val step: Int = 1,
    val selectedLeagueIds: Set<String> = emptySet(),
    val selectedClubIds: Set<String> = emptySet(),
    val nickname: String = "",
    val profilePhotoUrl: String? = null,
    val navigateToHome: Boolean = false,
)

class PersonalizationViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PersonalizationState())
    val state: StateFlow<PersonalizationState> = _state.asStateFlow()

    fun toggleLeague(id: String) {
        _state.value = _state.value.copy(
            selectedLeagueIds = if (id in _state.value.selectedLeagueIds) {
                _state.value.selectedLeagueIds - id
            } else {
                _state.value.selectedLeagueIds + id
            },
        )
    }

    fun toggleClub(id: String) {
        val leagueId = defaultClubs.find { it.id == id }?.leagueId ?: return
        val selectedInLeague = defaultClubs.count { it.leagueId == leagueId && it.id in _state.value.selectedClubIds }
        val currentlySelected = id in _state.value.selectedClubIds
        if (currentlySelected) {
            _state.value = _state.value.copy(selectedClubIds = _state.value.selectedClubIds - id)
        } else if (selectedInLeague < 3) {
            _state.value = _state.value.copy(selectedClubIds = _state.value.selectedClubIds + id)
        }
    }

    fun setNickname(value: String) {
        _state.value = _state.value.copy(nickname = value)
    }

    fun setProfilePhotoUrl(url: String?) {
        _state.value = _state.value.copy(profilePhotoUrl = url)
    }

    fun nextStep() {
        val s = _state.value
        if (s.step < 3) _state.value = s.copy(step = s.step + 1)
    }

    fun previousStep() {
        val s = _state.value
        if (s.step > 1) _state.value = s.copy(step = s.step - 1)
    }

    fun finish() {
        viewModelScope.launch {
            val s = _state.value
            preferencesRepository.updateUserPreferences(
                UserPreferences(
                    countryCode = null,
                    favoriteLeagues = s.selectedLeagueIds.toList(),
                    favoriteClubs = s.selectedClubIds.toList(),
                    nickname = s.nickname.takeIf { it.isNotBlank() },
                    profilePhotoUrl = s.profilePhotoUrl,
                ),
            )
            _state.value = s.copy(navigateToHome = true)
        }
    }

    fun clearNavigation() {
        _state.value = _state.value.copy(navigateToHome = false)
    }
}
