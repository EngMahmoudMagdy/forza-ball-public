package com.forzaball.feature.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.repository.AuthResult
import com.forzaball.domain.repository.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignUpState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToPersonalization: Boolean = false,
)

class SignUpViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpState())
    val state: StateFlow<SignUpState> = _state.asStateFlow()

    fun signUpWithEmail(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String,
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val displayName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
            when (val result = authRepository.signUpWithEmail(email, password, displayName.ifBlank { null })) {
                is AuthResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        navigateToPersonalization = true,
                    )
                }
                is AuthResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun signUpWithGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is SignInResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        navigateToPersonalization = true,
                    )
                }
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
        _state.value = _state.value.copy(navigateToPersonalization = false)
    }
}
