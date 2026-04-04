package com.forzaball.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.repository.AuthRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
