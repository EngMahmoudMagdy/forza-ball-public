package com.forzaball.feature.personalization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.model.UserPreferences
import android.net.Uri
import com.forzaball.data.profile.ProfileImageRepository
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.PreferencesRepository
import com.forzaball.domain.repository.SoccerTeamsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

data class PersonalizationState(
    val step: Int = 1,
    val selectedLeagueId: String? = null,
    val selectedClubId: String? = null,
    val teamsForLeague: List<ClubItem> = emptyList(),
    val isLoadingTeams: Boolean = false,
    val nickname: String = "",
    val profilePhotoUrl: String? = null,
    val profilePhotoThumbUrl: String? = null,
    val isUploadingPhoto: Boolean = false,
    val navigateToHome: Boolean = false,
)

class PersonalizationViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val soccerTeamsRepository: SoccerTeamsRepository,
    private val feedRepository: FeedRepository,
    private val profileImageRepository: ProfileImageRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PersonalizationState())
    val state: StateFlow<PersonalizationState> = _state.asStateFlow()

    fun selectLeague(slug: String) {
        val s = _state.value
        _state.value = s.copy(
            selectedLeagueId = slug,
            selectedClubId = null,
        )
    }

    fun selectClub(id: String) {
        val s = _state.value
        _state.value = s.copy(
            selectedClubId = if (s.selectedClubId == id) null else id,
        )
    }

    fun setNickname(value: String) {
        _state.value = _state.value.copy(nickname = value)
    }

    fun setProfilePhotoUrl(url: String?) {
        _state.value = _state.value.copy(profilePhotoUrl = url)
    }

    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploadingPhoto = true)
            profileImageRepository.uploadProfilePhoto(uri)
                .onSuccess { (full, thumb) ->
                    _state.value = _state.value.copy(
                        isUploadingPhoto = false,
                        profilePhotoUrl = full,
                        profilePhotoThumbUrl = thumb,
                    )
                }
                .onFailure { e ->
                    Timber.w(e, "uploadProfilePhoto personalization")
                    _state.value = _state.value.copy(isUploadingPhoto = false)
                }
        }
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
                    Timber.e(e, "load teams for personalization")
                    _state.value = _state.value.copy(isLoadingTeams = false)
                }
            }
            return
        }
        if (s.step < 3) {
            _state.value = s.copy(step = s.step + 1)
        }
    }

    fun previousStep() {
        val st = _state.value
        if (st.step > 1) {
            _state.value = st.copy(step = st.step - 1)
        }
    }

    fun finish() {
        viewModelScope.launch {
            val s = _state.value
            val club = s.teamsForLeague.find { it.id == s.selectedClubId }
            val existing = runCatching { preferencesRepository.observeUserPreferences().first() }
                .getOrNull()
            val prefs = UserPreferences(
                countryCode = null,
                favoriteTeamLeagueSlug = s.selectedLeagueId,
                favoriteTeamId = s.selectedClubId,
                favoriteTeamName = club?.name,
                nickname = s.nickname.takeIf { it.isNotBlank() },
                profilePhotoUrl = s.profilePhotoUrl,
                profilePhotoThumbUrl = s.profilePhotoThumbUrl,
                teamSearchHistory = existing?.teamSearchHistory.orEmpty(),
            )
            preferencesRepository.updateUserPreferences(prefs)
            runCatching { feedRepository.syncUserProfilePreferences(prefs) }
                .onFailure { Timber.w(it, "syncUserProfilePreferences") }
            _state.value = s.copy(navigateToHome = true)
        }
    }

    fun clearNavigation() {
        _state.value = _state.value.copy(navigateToHome = false)
    }
}
