package com.forzaball.domain.repository

import com.forzaball.domain.model.HomeMatchContent
import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.TeamNextMatch
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
    suspend fun loadNewsForPreferences(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
        maxArticles: Int = 40,
    ): List<NewsArticle>
}

interface MatchRepository {
    suspend fun loadFavoriteHighlightAndLive(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
    ): HomeMatchContent

    /** Upcoming / live matches from scoreboards + team schedules, sorted by kickoff. */
    suspend fun loadMergedFixtures(
        favoriteLeagueSlugs: List<String>,
        favoriteTeamIds: List<String>,
    ): List<Match>

    /**
     * Earliest upcoming or live fixture for the favorite team, merging
     * `{domesticLeague}/teams/{id}/schedule` and `uefa.champions/teams/{id}/schedule` when applicable.
     */
    suspend fun loadNextMatchForFavoriteTeam(
        domesticLeagueSlug: String?,
        teamId: String?,
        fallbackTeamDisplayName: String?,
    ): TeamNextMatch?
}

/** Social feed post (text-only). */
data class FeedPost(
    val id: String,
    val userId: String,
    /** Display name (e.g. “Marcus Sterling”). */
    val authorName: String,
    /** Handle without “@” (e.g. “MarcusV_9”), shown on the line below the name. */
    val authorUsername: String,
    val authorAvatarUrl: String?,
    val text: String,
    val likeCount: Int,
    val dislikeCount: Int,
    val commentCount: Int,
    val isLikedByUser: Boolean,
    val isDislikedByUser: Boolean,
    val createdAtMillis: Long,
)

data class FeedComment(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val text: String,
    val likeCount: Int,
    val dislikeCount: Int,
    val isLikedByUser: Boolean,
    val isDislikedByUser: Boolean,
    val createdAtMillis: Long,
)

interface FeedRepository {
    fun observeFeedPosts(): Flow<List<FeedPost>>

    /** Single post for detail / deep links; emits null if missing or signed out. */
    fun observePost(postId: String): Flow<FeedPost?>

    suspend fun refreshFeed()

    suspend fun createPost(text: String): Result<Unit>

    suspend fun likePost(postId: String)

    suspend fun unlikePost(postId: String)

    suspend fun dislikePost(postId: String)

    suspend fun undislikePost(postId: String)

    fun observeComments(postId: String): Flow<List<FeedComment>>

    /** Returns the new comment document id. */
    suspend fun addComment(postId: String, text: String): Result<String>

    /** Deletes a comment if the current user wrote it or owns the post. */
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>

    suspend fun likeComment(postId: String, commentId: String)

    suspend fun unlikeComment(postId: String, commentId: String)

    suspend fun dislikeComment(postId: String, commentId: String)

    suspend fun undislikeComment(postId: String, commentId: String)

    /** Ensures `users/{uid}` exists for Firestore rules and profile display. */
    suspend fun ensureUserProfile()

    /** Stores FCM registration token on `users/{uid}` for Cloud Messaging (server-triggered pushes). */
    suspend fun saveMessagingToken(token: String)

    /** Merges nickname, avatar, and favorite team fields into `users/{uid}`. */
    suspend fun syncUserProfilePreferences(preferences: UserPreferences)
}

