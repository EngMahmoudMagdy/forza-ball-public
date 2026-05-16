package com.forzaball.shared.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultSessionOrchestrator(
    private val tokenStore: SessionTokenStore,
    private val userStore: SessionUserStore,
    private val authApi: AuthApi,
) : SessionOrchestrator {
    private val sessionState = MutableStateFlow<SessionState>(SessionState.SignedOut)

    override fun observeSession(): Flow<SessionState> = sessionState.asStateFlow()

    override suspend fun signIn(userId: String, email: String?, tokens: SessionTokens?) {
        val user = SessionState.SignedIn(userId = userId, email = email)
        userStore.writeUser(user)
        tokenStore.writeTokens(tokens)
        sessionState.value = user
    }

    override suspend fun signOut() {
        tokenStore.writeTokens(null)
        userStore.writeUser(null)
        sessionState.value = SessionState.SignedOut
    }

    override suspend fun refreshIfNeeded() {
        val currentTokens = tokenStore.readTokens() ?: return
        val refreshed = authApi.refresh(currentTokens) ?: return
        tokenStore.writeTokens(refreshed)
        val currentUser = userStore.readUser()
        sessionState.value = currentUser ?: SessionState.SignedOut
    }
}
