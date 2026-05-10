package com.forzaball.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.repository.AuthState
import com.forzaball.domain.repository.FeedRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationsViewModel(
    private val feedRepository: FeedRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    val notifications = feedRepository.observeUserNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            runCatching { feedRepository.markNotificationRead(notificationId) }
        }
    }
}
