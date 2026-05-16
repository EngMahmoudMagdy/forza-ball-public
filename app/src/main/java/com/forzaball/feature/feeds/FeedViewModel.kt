package com.forzaball.feature.feeds

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.repository.AuthState
import com.forzaball.domain.repository.FeedPost
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.notifications.FEED_BROADCAST_TOPIC
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

data class FeedUiState(
    val posts: List<FeedPost> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val isSignedIn: Boolean = false,
    val isPosting: Boolean = false,
)

class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(FeedUiState())
    val ui: StateFlow<FeedUiState> = _ui.asStateFlow()

    val authState: StateFlow<AuthState> = authRepository.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    /**
     * First Firestore emission is often an empty cache snapshot before server data arrives.
     * Keep loading until we have posts, or one refresh + short delay has settled an empty feed.
     */
    private var initialFeedSettleStarted: Boolean = false
    private var previousAuth: AuthState? = null

    init {
        viewModelScope.launch {
            authRepository.authState().collect { auth ->
                val signedIn = auth is AuthState.SignedIn
                val becameSignedIn = signedIn && previousAuth !is AuthState.SignedIn
                previousAuth = auth
                if (auth is AuthState.SignedOut || becameSignedIn) {
                    initialFeedSettleStarted = false
                }
                _ui.update {
                    it.copy(
                        isSignedIn = signedIn,
                        isInitialLoading = when {
                            auth is AuthState.SignedOut -> false
                            becameSignedIn -> true
                            else -> it.isInitialLoading
                        },
                        posts = if (auth is AuthState.SignedOut) emptyList() else it.posts,
                    )
                }
                if (signedIn) {
                    runCatching { feedRepository.ensureUserProfile() }
                        .onFailure { Timber.tag(TAG).w(it, "ensureUserProfile") }
                    runCatching {
                        FirebaseMessaging.getInstance().subscribeToTopic(FEED_BROADCAST_TOPIC)
                    }.onFailure { Timber.tag(TAG).w(it, "subscribe feed topic") }
                    viewModelScope.launch {
                        runCatching {
                            val token = FirebaseMessaging.getInstance().token.await()
                            feedRepository.saveMessagingToken(token)
                        }.onFailure { Timber.tag(TAG).w(it, "save FCM token") }
                    }
                } else {
                    runCatching {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(FEED_BROADCAST_TOPIC)
                    }.onFailure { Timber.tag(TAG).w(it, "unsubscribe feed topic") }
                }
            }
        }
        viewModelScope.launch {
            feedRepository.observeFeedPosts().collect { posts ->
                _ui.update { it.copy(posts = posts) }
                if (posts.isNotEmpty()) {
                    initialFeedSettleStarted = true
                    _ui.update { it.copy(isInitialLoading = false) }
                    return@collect
                }
                if (!initialFeedSettleStarted) {
                    initialFeedSettleStarted = true
                    viewModelScope.launch {
                        runCatching { feedRepository.refreshFeed() }
                            .onFailure { e -> Timber.tag(TAG).e(e, "initial refreshFeed") }
                        delay(400)
                        _ui.update { it.copy(isInitialLoading = false) }
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ui.update { it.copy(isRefreshing = true, errorMessage = null) }
            runCatching {
                feedRepository.refreshFeed()
                feedRepository.ensureUserProfile()
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "refresh")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn’t refresh") }
            }
            _ui.update { it.copy(isRefreshing = false) }
        }
    }

    fun consumeError() {
        _ui.update { it.copy(errorMessage = null) }
    }

    fun createPost(text: String, onDone: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(isPosting = true, errorMessage = null) }
            val result = feedRepository.createPost(text)
            _ui.update { it.copy(isPosting = false) }
            result.onFailure { e ->
                Timber.tag(TAG).e(e, "createPost")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn’t post") }
            }
            onDone(result)
        }
    }

    private var lastReactionElapsed = 0L

    fun toggleLike(post: FeedPost) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastReactionElapsed < 500L) return
        lastReactionElapsed = now
        viewModelScope.launch {
            runCatching {
                if (post.isLikedByUser) {
                    feedRepository.unlikePost(post.id)
                } else {
                    feedRepository.likePost(post.id)
                }
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "toggleLike")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn’t update like") }
            }
        }
    }

    fun toggleDislike(post: FeedPost) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastReactionElapsed < 500L) return
        lastReactionElapsed = now
        viewModelScope.launch {
            runCatching {
                if (post.isDislikedByUser) {
                    feedRepository.undislikePost(post.id)
                } else {
                    feedRepository.dislikePost(post.id)
                }
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "toggleDislike")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn’t update dislike") }
            }
        }
    }

    fun observePost(postId: String): Flow<FeedPost?> = feedRepository.observePost(postId)

    fun addComment(postId: String, text: String, onDone: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = feedRepository.addComment(postId, text)
            result.onFailure { e ->
                Timber.tag(TAG).e(e, "addComment")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn’t comment") }
            }
            onDone(result)
        }
    }

    private var lastCommentReactionElapsed = 0L

    fun toggleCommentLike(postId: String, commentId: String, liked: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCommentReactionElapsed < 500L) return
        lastCommentReactionElapsed = now
        viewModelScope.launch {
            runCatching {
                if (liked) {
                    feedRepository.unlikeComment(postId, commentId)
                } else {
                    feedRepository.likeComment(postId, commentId)
                }
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "toggleCommentLike")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn’t update like") }
            }
        }
    }

    fun toggleCommentDislike(postId: String, commentId: String, disliked: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCommentReactionElapsed < 500L) return
        lastCommentReactionElapsed = now
        viewModelScope.launch {
            runCatching {
                if (disliked) {
                    feedRepository.undislikeComment(postId, commentId)
                } else {
                    feedRepository.dislikeComment(postId, commentId)
                }
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "toggleCommentDislike")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn’t update dislike") }
            }
        }
    }

    fun observeComments(postId: String) = feedRepository.observeComments(postId)

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            runCatching {
                feedRepository.deleteComment(postId, commentId)
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "deleteComment")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn't delete comment") }
            }
        }
    }

    fun toggleSavePost(post: FeedPost) {
        viewModelScope.launch {
            runCatching {
                if (post.isSavedByUser) {
                    feedRepository.unsavePost(post.id)
                } else {
                    feedRepository.savePost(post.id)
                }
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "toggleSavePost")
                _ui.update { it.copy(errorMessage = e.message ?: "Couldn't update save") }
            }
        }
    }

    fun deletePost(postId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            feedRepository.deletePost(postId)
                .onSuccess { onDone() }
                .onFailure { e ->
                    Timber.tag(TAG).e(e, "deletePost")
                    _ui.update { it.copy(errorMessage = e.message ?: "Couldn't delete post") }
                }
        }
    }

    fun reportPost(
        postId: String,
        reasonId: String,
        reasonLabel: String,
        comment: String?,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            feedRepository.reportPost(postId, reasonId, reasonLabel, comment)
                .onSuccess { onDone() }
                .onFailure { e ->
                    Timber.tag(TAG).e(e, "reportPost")
                    _ui.update { it.copy(errorMessage = e.message ?: "Couldn't report post") }
                }
        }
    }

    companion object {
        private const val TAG = "FeedViewModel"
    }
}
