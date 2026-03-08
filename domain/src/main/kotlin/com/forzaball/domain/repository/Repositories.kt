package com.forzaball.domain.repository

import com.forzaball.domain.model.Match
import com.forzaball.domain.model.NewsArticle
import com.forzaball.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

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

