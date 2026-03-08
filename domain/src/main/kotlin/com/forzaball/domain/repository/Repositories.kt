package com.forzaball.domain.repository

import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Result of sign-up with email/password or Google. */
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/** Result of sign-in. */
sealed class SignInResult {
    data object Success : SignInResult()
    data class Error(val message: String) : SignInResult()
}

/** Current auth state for UI. */
sealed class AuthState {
    data object Loading : AuthState()
    data object SignedOut : AuthState()
    data class SignedIn(val uid: String, val email: String?) : AuthState()
}

interface AuthRepository {
    fun authState(): Flow<AuthState>
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String?,
    ): AuthResult
    suspend fun signInWithEmail(email: String, password: String): SignInResult
    suspend fun signInWithGoogle(idToken: String): SignInResult
    suspend fun signOut()
    suspend fun getCurrentUserId(): String?
}

interface PreferencesRepository {
    fun observeUserPreferences(): Flow<UserPreferences>
    suspend fun updateUserPreferences(preferences: UserPreferences)
}

interface NewsRepository {
    fun observeClubNews(clubIds: List<String>): Flow<List<NewsArticle>>
}

interface MatchRepository {
    fun observeNextOrLiveMatchForFavoriteClub(): Flow<Match?>
}

interface FeedRepository {
    fun observeFeed(): Flow<List<FeedPost>>
}

data class FeedPost(
    val id: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val content: String,
    val likeCount: Int,
    val commentCount: Int,
    val repostCount: Int,
    val isLikedByUser: Boolean,
    val createdAtMillis: Long,
)

