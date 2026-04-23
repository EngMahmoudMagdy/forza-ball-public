package com.forzaball.app.feature.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.PreferencesRepository
import com.forzaball.domain.repository.SignInResult
import timber.log.Timber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignInState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToHome: Boolean = false,
    val navigateToPersonalization: Boolean = false,
)

class SignInViewModel(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val feedRepository: FeedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun signInWithEmailOrPhone(emailOrPhone: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            if (emailOrPhone.contains("@")) {
                when (val result = authRepository.signInWithEmail(emailOrPhone, password)) {
                    is SignInResult.Success -> navigateAfterSignIn()
                    is SignInResult.Error -> {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = result.message,
                        )
                    }
                }
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Please use your email to sign in. Phone sign-in is not available yet.",
                )
            }
        }
    }

    private suspend fun navigateAfterSignIn() {
        runCatching { feedRepository.mergeTeamSearchHistoryFromRemote() }
            .onFailure { Timber.w(it, "mergeTeamSearchHistoryFromRemote") }
        val prefs = preferencesRepository.observeUserPreferences().first()
        val hasCompletedPersonalization = !prefs.favoriteTeamId.isNullOrBlank() &&
            !prefs.nickname.isNullOrBlank()
        _state.value = _state.value.copy(
            isLoading = false,
            errorMessage = null,
            navigateToHome = hasCompletedPersonalization,
            navigateToPersonalization = !hasCompletedPersonalization,
        )
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is SignInResult.Success -> navigateAfterSignIn()
                is SignInResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun clearNavigation() {
        _state.value = _state.value.copy(navigateToHome = false, navigateToPersonalization = false)
    }
}
