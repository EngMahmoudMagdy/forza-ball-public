package com.forzaball.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.model.UserPublicProfile
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.repository.AuthState
import com.forzaball.domain.repository.FeedPost
import com.forzaball.domain.repository.FeedRepository
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
)

class UserProfileViewModel(
    private val feedRepository: FeedRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(UserProfileUiState())
    val ui: StateFlow<UserProfileUiState> = _ui.asStateFlow()

    val authState = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    private var observeJob: Job? = null

    fun load(userId: String) {
        observeJob?.cancel()
        _ui.value = UserProfileUiState(isLoading = true)
        observeJob = viewModelScope.launch {
            launch {
                authState.collect { auth ->
                    val isOwn = auth is AuthState.SignedIn && auth.uid == userId
                    _ui.update { it.copy(isOwnProfile = isOwn) }
                }
            }
            launch {
                feedRepository.observeUserPublicProfile(userId).collect { profile ->
                    _ui.update { it.copy(profile = profile, isLoading = profile == null) }
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
}
