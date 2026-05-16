package com.forzaball.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.data.profile.ProfileImageRepository
import com.forzaball.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val nickname: String = "",
    val photoUrl: String? = null,
    val thumbUrl: String? = null,
    val photoCacheVersion: Long = 0L,
    val isSaving: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

class EditProfileViewModel(
    preferencesRepository: PreferencesRepository,
    private val profileImageRepository: ProfileImageRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(EditProfileUiState())
    val ui: StateFlow<EditProfileUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.observeUserPreferences().collect { prefs ->
                _ui.update {
                    it.copy(
                        nickname = prefs.nickname.orEmpty(),
                        photoUrl = prefs.profilePhotoUrl,
                        thumbUrl = prefs.profilePhotoThumbUrl,
                        photoCacheVersion = prefs.profilePhotoCacheVersion,
                    )
                }
            }
        }
    }

    fun setNickname(value: String) {
        _ui.update { it.copy(nickname = value, saved = false) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, errorMessage = null) }
            profileImageRepository.updateDisplayName(_ui.value.nickname)
                .onSuccess { _ui.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { e ->
                    _ui.update {
                        it.copy(isSaving = false, errorMessage = e.message ?: "Couldn't save")
                    }
                }
        }
    }

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _ui.update { it.copy(isUploadingPhoto = true, errorMessage = null) }
            profileImageRepository.uploadProfilePhoto(uri)
                .onSuccess { (full, thumb) ->
                    _ui.update {
                        it.copy(
                            isUploadingPhoto = false,
                            photoUrl = full,
                            thumbUrl = thumb,
                            photoCacheVersion = System.currentTimeMillis(),
                        )
                    }
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(
                            isUploadingPhoto = false,
                            errorMessage = e.message ?: "Couldn't upload photo",
                        )
                    }
                }
        }
    }

    fun consumeError() {
        _ui.update { it.copy(errorMessage = null) }
    }
}
