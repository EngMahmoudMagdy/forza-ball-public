package com.forzaball.shared.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DefaultSessionOrchestratorTest {
    @Test
    fun `sign in persists user and emits signed-in state`() = runTest {
        val tokenStore = InMemoryTokenStore()
        val userStore = InMemoryUserStore()
        val orchestrator = DefaultSessionOrchestrator(
            tokenStore = tokenStore,
            userStore = userStore,
            authApi = PassthroughAuthApi(),
        )

        orchestrator.signIn("uid-1", "test@forzaball.com", SessionTokens("a", "r"))
        val state = orchestrator.observeSession().first()

        assertIs<SessionState.SignedIn>(state)
        assertEquals("uid-1", state.userId)
        assertEquals("a", tokenStore.tokens?.accessToken)
    }

    @Test
    fun `sign out clears stored values`() = runTest {
        val tokenStore = InMemoryTokenStore()
        val userStore = InMemoryUserStore()
        val orchestrator = DefaultSessionOrchestrator(
            tokenStore = tokenStore,
            userStore = userStore,
            authApi = PassthroughAuthApi(),
        )

        orchestrator.signIn("uid-1", null, SessionTokens("a", null))
        orchestrator.signOut()

        assertNull(tokenStore.tokens)
        assertNull(userStore.user)
        assertIs<SessionState.SignedOut>(orchestrator.observeSession().first())
    }
}

private class PassthroughAuthApi : AuthApi {
    override suspend fun refresh(tokens: SessionTokens): SessionTokens? = tokens
}

private class InMemoryTokenStore : SessionTokenStore {
    var tokens: SessionTokens? = null

    override suspend fun readTokens(): SessionTokens? = tokens
    override suspend fun writeTokens(tokens: SessionTokens?) {
        this.tokens = tokens
    }
}

private class InMemoryUserStore : SessionUserStore {
    var user: SessionState.SignedIn? = null

    override suspend fun readUser(): SessionState.SignedIn? = user
    override suspend fun writeUser(user: SessionState.SignedIn?) {
        this.user = user
    }
}
