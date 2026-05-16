package com.forzaball.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.model.UserPublicProfile
import com.forzaball.domain.model.profileAvatarDisplayUrl
import com.forzaball.domain.model.profileAvatarFullUrl
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.repository.AuthState
import com.forzaball.domain.repository.FeedPost
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.PreferencesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val profile: UserPublicProfile? = null,
    val posts: List<FeedPost> = emptyList(),
    val savedPosts: List<FeedPost> = emptyList(),
    val isOwnProfile: Boolean = false,
    val isLoading: Boolean = true,
    val localPhotoCacheVersion: Long = 0L,
)

class UserProfileViewModel(
    private val feedRepository: FeedRepository,
    private val preferencesRepository: PreferencesRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(UserProfileUiState())
    val ui: StateFlow<UserProfileUiState> = _ui.asStateFlow()

    val authState = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    private var observeJob: Job? = null
    private var latestPrefs: UserPreferences? = null
    private var latestRemote: UserPublicProfile? = null

    fun load(userId: String) {
        observeJob?.cancel()
        latestPrefs = null
        latestRemote = null
        _ui.value = UserProfileUiState(isLoading = true)
        observeJob = viewModelScope.launch {
            launch {
                authState.collect { auth ->
                    val isOwn = auth is AuthState.SignedIn && auth.uid == userId
                    _ui.update { it.copy(isOwnProfile = isOwn) }
                }
            }
            launch {
                feedRepository.observeUserPublicProfile(userId).collect { remote ->
                    latestRemote = remote
                    emitMergedProfile()
                }
            }
            launch {
                authState.collect { auth ->
                    val isOwn = auth is AuthState.SignedIn && auth.uid == userId
                    if (isOwn) {
                        preferencesRepository.observeUserPreferences().collect { prefs ->
                            latestPrefs = prefs
                            _ui.update {
                                it.copy(localPhotoCacheVersion = prefs.profilePhotoCacheVersion)
                            }
                            emitMergedProfile()
                        }
                    }
                }
            }
            launch {
                feedRepository.observePostsByUser(userId).collect { posts ->
                    _ui.update { it.copy(posts = posts) }
                }
            }
            launch {
                authState.collect { auth ->
                    val isOwn = auth is AuthState.SignedIn && auth.uid == userId
                    if (isOwn) {
                        feedRepository.observeSavedPosts().collect { saved ->
                            _ui.update { it.copy(savedPosts = saved) }
                        }
                    }
                }
            }
        }
    }

    private fun emitMergedProfile() {
        val isOwn = _ui.value.isOwnProfile
        val merged = mergeWithLocalIfOwn(latestRemote, isOwn, latestPrefs)
        _ui.update {
            it.copy(profile = merged, isLoading = merged == null)
        }
    }

    private fun mergeWithLocalIfOwn(
        remote: UserPublicProfile?,
        isOwn: Boolean,
        prefs: UserPreferences?,
    ): UserPublicProfile? {
        if (remote == null) return null
        if (!isOwn || prefs == null) return remote
        val full = prefs.profileAvatarFullUrl() ?: remote.avatarUrl
        val thumb = prefs.profileAvatarDisplayUrl() ?: remote.avatarThumbUrl
        val name = prefs.nickname?.takeIf { it.isNotBlank() } ?: remote.displayName
        return remote.copy(
            displayName = name,
            avatarUrl = full,
            avatarThumbUrl = thumb,
        )
    }
}
