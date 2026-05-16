package com.forzaball.shared.auth

import kotlinx.coroutines.flow.Flow

sealed interface SessionState {
    data object SignedOut : SessionState
    data class SignedIn(
        val userId: String,
        val email: String?,
    ) : SessionState
}

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String?,
)

interface SessionTokenStore {
    suspend fun readTokens(): SessionTokens?
    suspend fun writeTokens(tokens: SessionTokens?)
}

interface SessionUserStore {
    suspend fun readUser(): SessionState.SignedIn?
    suspend fun writeUser(user: SessionState.SignedIn?)
}

interface AuthApi {
    suspend fun refresh(tokens: SessionTokens): SessionTokens?
}

interface SessionOrchestrator {
    fun observeSession(): Flow<SessionState>
    suspend fun signIn(userId: String, email: String?, tokens: SessionTokens?)
    suspend fun signOut()
    suspend fun refreshIfNeeded()
}
