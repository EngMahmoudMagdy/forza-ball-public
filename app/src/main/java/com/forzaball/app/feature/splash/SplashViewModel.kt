package com.forzaball.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.repository.AuthState
import com.forzaball.domain.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** One-time navigation event from splash. */
sealed class SplashDestination {
    data object Onboarding : SplashDestination()
    data object SignIn : SplashDestination()
    data object Personalization : SplashDestination()
    data object Home : SplashDestination()
}

class SplashViewModel(
    private val authRepository: AuthRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _destination = MutableSharedFlow<SplashDestination>(replay = 0)
    val destination: SharedFlow<SplashDestination> = _destination

    fun decideDestination() {
        viewModelScope.launch {
            // Optional: simulate loading / future API or DB calls here
            delay(1200)
            val auth = authRepository.authState().first()
            when (auth) {
                is AuthState.Loading, is AuthState.SignedOut -> {
                    _destination.emit(SplashDestination.Onboarding)
                }
                is AuthState.SignedIn -> {
                    val prefs = preferencesRepository.observeUserPreferences().first()
                    val hasCompletedPersonalization = !prefs.favoriteTeamId.isNullOrBlank() &&
                        !prefs.nickname.isNullOrBlank()
                    if (hasCompletedPersonalization) {
                        _destination.emit(SplashDestination.Home)
                    } else {
                        _destination.emit(SplashDestination.Personalization)
                    }
                }
            }
        }
    }
}
