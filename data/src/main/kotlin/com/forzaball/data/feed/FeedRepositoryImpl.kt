package com.forzaball.data.feed

import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.model.favoriteTeamIdsList
import com.forzaball.domain.model.leagueSlugsForEspnContent
import com.forzaball.domain.repository.FeedComment
import com.forzaball.domain.repository.FeedPost
import com.forzaball.domain.repository.FeedRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max

class FeedRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : FeedRepository {

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeFeedPosts(): Flow<List<FeedPost>> = callbackFlow {
        var postsReg: ListenerRegistration? = null
        var attachedUid: String? = null

        fun detachPostsListener() {
            postsReg?.remove()
            postsReg = null
        }

        fun attachForUser(uid: String) {
            if (uid == attachedUid && postsReg != null) return
            detachPostsListener()
            attachedUid = uid

            postsReg = firestore.collection(COL_POSTS)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(GLOBAL_FEED_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag(TAG).e(error, "global posts listener")
                        ioScope.launch {
                            trySend(emptyList())
                        }
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents.orEmpty()
                    ioScope.launch {
                        try {
                            val posts = buildFeedPostsFromDocuments(uid, docs)
                            trySend(posts)
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, "observeFeedPosts emit failed")
                            trySend(emptyList())
                        }
                    }
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { fa ->
            val uid = fa.currentUser?.uid
            if (uid == null) {
                detachPostsListener()
                attachedUid = null
                trySend(emptyList())
            } else {
                attachForUser(uid)
            }
        }
        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            detachPostsListener()
            attachedUid = null
        }
    }

    private suspend fun buildFeedPostsFromDocuments(
        uid: String,
        docs: List<com.google.firebase.firestore.DocumentSnapshot>,
    ): List<FeedPost> {
        if (docs.isEmpty()) return emptyList()
        val userIds = docs.mapNotNull { it.getString(FIELD_USER_ID) }.distinct()
        val profiles = loadUserProfiles(userIds)
        return coroutineScope {
            docs.map { doc ->
                async {
                    val authorId = doc.getString(FIELD_USER_ID) ?: return@async null
                    val profile = profiles[authorId]
                    val likeSnap = firestore.collection(COL_POSTS).document(doc.id)
                        .collection(SUB_LIKES).document(uid).get(Source.DEFAULT).await()
                    val dislikeSnap = firestore.collection(COL_POSTS).document(doc.id)
                        .collection(SUB_DISLIKES).document(uid).get(Source.DEFAULT).await()
                    toFeedPost(doc, profile, likeSnap.exists(), dislikeSnap.exists())
                }
            }.mapNotNull { it.await() }
        }
    }

    private suspend fun loadUserProfiles(userIds: List<String>): Map<String, com.google.firebase.firestore.DocumentSnapshot> {
        if (userIds.isEmpty()) return emptyMap()
        return coroutineScope {
            userIds.map { id ->
                async {
                    id to firestore.collection(COL_USERS).document(id).get(Source.DEFAULT).await()
                }
            }.associate { it.await() }
        }
    }

    private fun toFeedPost(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        userDoc: com.google.firebase.firestore.DocumentSnapshot?,
        liked: Boolean,
        disliked: Boolean = false,
    ): FeedPost {
        val userId = doc.getString(FIELD_USER_ID).orEmpty()
        val (displayName, handle) = if (userDoc != null && userDoc.exists()) {
            val name = userDoc.getString(FIELD_DISPLAY_NAME)?.takeIf { it.isNotBlank() }
                ?: userDoc.getString(FIELD_USERNAME)?.takeIf { it.isNotBlank() }
                ?: userDoc.getString(FIELD_EMAIL)?.substringBefore("@")?.ifBlank { null }
                ?: "User"
            val h = userDoc.getString(FIELD_HANDLE)?.takeIf { it.isNotBlank() }
                ?: sanitizeHandle(userDoc.getString(FIELD_EMAIL)?.substringBefore("@").orEmpty())
            name to h
        } else {
            "User" to "user"
        }
        val avatar = userDoc?.takeIf { it.exists() }?.getString(FIELD_AVATAR_URL)
        return FeedPost(
            id = doc.id,
            userId = userId,
            authorName = displayName,
            authorUsername = handle,
            authorAvatarUrl = avatar,
            text = doc.getString(FIELD_TEXT).orEmpty(),
            likeCount = (doc.getLong(FIELD_LIKE_COUNT) ?: 0L).toInt(),
            dislikeCount = (doc.getLong(FIELD_DISLIKE_COUNT) ?: 0L).toInt(),
            commentCount = (doc.getLong(FIELD_COMMENT_COUNT) ?: 0L).toInt(),
            isLikedByUser = liked,
            isDislikedByUser = disliked,
            createdAtMillis = doc.getTimestamp(FIELD_TIMESTAMP)?.toDate()?.time ?: 0L,
        )
    }

    override fun observePost(postId: String): Flow<FeedPost?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val reg = firestore.collection(COL_POSTS).document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.tag(TAG).e(error, "observePost")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                ioScope.launch {
                    try {
                        val authorId = snapshot.getString(FIELD_USER_ID)
                        if (authorId == null) {
                            trySend(null)
                            return@launch
                        }
                        val profile = loadUserProfiles(listOf(authorId))[authorId]
                        val likeSnap = firestore.collection(COL_POSTS).document(postId)
                            .collection(SUB_LIKES).document(uid).get(Source.DEFAULT).await()
                        val dislikeSnap = firestore.collection(COL_POSTS).document(postId)
                            .collection(SUB_DISLIKES).document(uid).get(Source.DEFAULT).await()
                        trySend(
                            toFeedPost(snapshot, profile, likeSnap.exists(), dislikeSnap.exists()),
                        )
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "observePost map")
                        trySend(null)
                    }
                }
            }
        awaitClose { reg.remove() }
    }

    override suspend fun refreshFeed() {
        ensureUserProfile()
    }

    override suspend fun createPost(text: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        // Don’t block posting if `users/{uid}` rules fail; post write uses `posts` rules only.
        runCatching { ensureUserProfile() }
            .onFailure { Timber.tag(TAG).w(it, "ensureUserProfile before createPost") }
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Empty post" }
        require(trimmed.length <= MAX_POST_CHARS) { "Too long" }
        val ref = firestore.collection(COL_POSTS).document()
        try {
            ref.set(
                mapOf(
                    FIELD_USER_ID to uid,
                    FIELD_TEXT to trimmed,
                    FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
                    FIELD_LIKE_COUNT to 0,
                    FIELD_DISLIKE_COUNT to 0,
                    FIELD_COMMENT_COUNT to 0,
                ),
            ).await()
        } catch (e: FirebaseFirestoreException) {
            Timber.tag(TAG).e(e, "createPost Firestore code=%s", e.code)
            throw Exception(friendlyFirestoreMessage(e), e)
        }
    }

    private fun friendlyFirestoreMessage(e: FirebaseFirestoreException): String = when (e.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Post blocked: check Firestore rules (posts create must allow your userId)."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Network unavailable. Try again when you’re online."
        else -> e.message ?: "Couldn’t create post"
    }

    override suspend fun likePost(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val likeRef = postRef.collection(SUB_LIKES).document(uid)
        val dislikeRef = postRef.collection(SUB_DISLIKES).document(uid)
        firestore.runTransaction { tx ->
            val post = tx.get(postRef)
            if (!post.exists()) return@runTransaction
            val like = tx.get(likeRef)
            if (like.exists()) return@runTransaction
            var likeCount = post.getLong(FIELD_LIKE_COUNT) ?: 0L
            var dislikeCount = post.getLong(FIELD_DISLIKE_COUNT) ?: 0L
            val dislike = tx.get(dislikeRef)
            if (dislike.exists()) {
                tx.delete(dislikeRef)
                dislikeCount = max(0L, dislikeCount - 1)
            }
            tx.set(likeRef, mapOf(FIELD_TIMESTAMP to FieldValue.serverTimestamp()))
            likeCount += 1
            tx.update(
                postRef,
                mapOf(
                    FIELD_LIKE_COUNT to likeCount,
                    FIELD_DISLIKE_COUNT to dislikeCount,
                ),
            )
        }.await()
    }

    override suspend fun unlikePost(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val likeRef = postRef.collection(SUB_LIKES).document(uid)
        firestore.runTransaction { tx ->
            val post = tx.get(postRef)
            if (!post.exists()) return@runTransaction
            val like = tx.get(likeRef)
            if (!like.exists()) return@runTransaction
            val current = post.getLong(FIELD_LIKE_COUNT) ?: 0L
            tx.delete(likeRef)
            tx.update(postRef, FIELD_LIKE_COUNT, max(0L, current - 1))
        }.await()
    }

    override suspend fun dislikePost(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val dislikeRef = postRef.collection(SUB_DISLIKES).document(uid)
        val likeRef = postRef.collection(SUB_LIKES).document(uid)
        firestore.runTransaction { tx ->
            val post = tx.get(postRef)
            if (!post.exists()) return@runTransaction
            val dislike = tx.get(dislikeRef)
            if (dislike.exists()) return@runTransaction
            var likeCount = post.getLong(FIELD_LIKE_COUNT) ?: 0L
            var dislikeCount = post.getLong(FIELD_DISLIKE_COUNT) ?: 0L
            val like = tx.get(likeRef)
            if (like.exists()) {
                tx.delete(likeRef)
                likeCount = max(0L, likeCount - 1)
            }
            tx.set(dislikeRef, mapOf(FIELD_TIMESTAMP to FieldValue.serverTimestamp()))
            dislikeCount += 1
            tx.update(
                postRef,
                mapOf(
                    FIELD_LIKE_COUNT to likeCount,
                    FIELD_DISLIKE_COUNT to dislikeCount,
                ),
            )
        }.await()
    }

    override suspend fun undislikePost(postId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val dislikeRef = postRef.collection(SUB_DISLIKES).document(uid)
        firestore.runTransaction { tx ->
            val post = tx.get(postRef)
            if (!post.exists()) return@runTransaction
            val dislike = tx.get(dislikeRef)
            if (!dislike.exists()) return@runTransaction
            val current = post.getLong(FIELD_DISLIKE_COUNT) ?: 0L
            tx.delete(dislikeRef)
            tx.update(postRef, FIELD_DISLIKE_COUNT, max(0L, current - 1))
        }.await()
    }

    override fun observeComments(postId: String): Flow<List<FeedComment>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        suspend fun emitComments(comments: List<FeedComment>) {
            withContext(Dispatchers.Main.immediate) {
                trySend(comments)
            }
        }

        val reg = firestore.collection(COL_POSTS).document(postId).collection(SUB_COMMENTS)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.tag(TAG).e(error, "comments listener")
                    ioScope.launch {
                        emitComments(emptyList())
                    }
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty()
                ioScope.launch {
                    try {
                        val comments = buildComments(uid, postId, docs)
                        emitComments(comments)
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "map comments")
                        emitComments(emptyList())
                    }
                }
            }

        awaitClose { reg.remove() }
    }

    private suspend fun buildComments(
        uid: String,
        postId: String,
        docs: List<com.google.firebase.firestore.DocumentSnapshot>,
    ): List<FeedComment> {
        if (docs.isEmpty()) return emptyList()
        val userIds = docs.mapNotNull { it.getString(FIELD_USER_ID) }.distinct()
        val profiles = runCatching { loadUserProfiles(userIds) }
            .onFailure { Timber.tag(TAG).e(it, "loadUserProfiles for comments") }
            .getOrDefault(emptyMap())
        return coroutineScope {
            docs.map { doc ->
                async {
                    mapCommentDocument(uid, postId, doc, profiles)
                }
            }.mapNotNull { runCatching { it.await() }.getOrNull() }
        }
    }

    private suspend fun mapCommentDocument(
        uid: String,
        postId: String,
        doc: com.google.firebase.firestore.DocumentSnapshot,
        profiles: Map<String, com.google.firebase.firestore.DocumentSnapshot>,
    ): FeedComment? {
        val authorId = doc.getString(FIELD_USER_ID) ?: return null
        val profile = profiles[authorId]
        val name = profile?.takeIf { it.exists() }?.let { u ->
            u.getString(FIELD_USERNAME)?.takeIf { n -> n.isNotBlank() }
                ?: u.getString(FIELD_EMAIL)?.substringBefore("@")?.ifBlank { null }
        } ?: "User"
        val avatar = profile?.takeIf { it.exists() }?.getString(FIELD_AVATAR_URL)
        val commentBase = firestore.collection(COL_POSTS).document(postId)
            .collection(SUB_COMMENTS).document(doc.id)
        val liked = runCatching {
            commentBase.collection(SUB_COMMENT_LIKES).document(uid)
                .get(Source.DEFAULT).await()
                .exists()
        }.getOrElse { e ->
            Timber.tag(TAG).w(e, "comment like read")
            false
        }
        val disliked = runCatching {
            commentBase.collection(SUB_COMMENT_DISLIKES).document(uid)
                .get(Source.DEFAULT).await()
                .exists()
        }.getOrElse { e ->
            Timber.tag(TAG).w(e, "comment dislike read")
            false
        }
        return FeedComment(
            id = doc.id,
            userId = authorId,
            authorName = name,
            authorAvatarUrl = avatar,
            text = doc.getString(FIELD_TEXT).orEmpty(),
            likeCount = (doc.getLong(FIELD_LIKE_COUNT) ?: 0L).toInt(),
            dislikeCount = (doc.getLong(FIELD_DISLIKE_COUNT) ?: 0L).toInt(),
            isLikedByUser = liked,
            isDislikedByUser = disliked,
            createdAtMillis = doc.getTimestamp(FIELD_TIMESTAMP)?.toDate()?.time ?: 0L,
        )
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val commentRef = postRef.collection(SUB_COMMENTS).document(commentId)
        val likesSnap = commentRef.collection(SUB_COMMENT_LIKES).get(Source.DEFAULT).await()
        val dislikesSnap = commentRef.collection(SUB_COMMENT_DISLIKES).get(Source.DEFAULT).await()
        firestore.runTransaction { tx ->
            val post = tx.get(postRef)
            val comment = tx.get(commentRef)
            if (!post.exists() || !comment.exists()) error("Missing")
            val commentAuthor = comment.getString(FIELD_USER_ID)
            val postAuthor = post.getString(FIELD_USER_ID)
            val canDelete = uid == commentAuthor || uid == postAuthor
            if (!canDelete) error("Not allowed")
            val count = post.getLong(FIELD_COMMENT_COUNT) ?: 0L
            likesSnap.documents.forEach { doc -> tx.delete(doc.reference) }
            dislikesSnap.documents.forEach { doc -> tx.delete(doc.reference) }
            tx.delete(commentRef)
            tx.update(postRef, FIELD_COMMENT_COUNT, max(0L, count - 1))
        }.await()
    }

    override suspend fun addComment(postId: String, text: String): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Empty comment" }
        require(trimmed.length <= MAX_COMMENT_CHARS) { "Too long" }
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val commentRef = postRef.collection(SUB_COMMENTS).document()
        val newId = commentRef.id
        firestore.runTransaction { tx ->
            val post = tx.get(postRef)
            if (!post.exists()) error("Post missing")
            val count = post.getLong(FIELD_COMMENT_COUNT) ?: 0L
            tx.set(
                commentRef,
                mapOf(
                    FIELD_COMMENT_ID to newId,
                    FIELD_USER_ID to uid,
                    FIELD_TEXT to trimmed,
                    FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
                    FIELD_LIKE_COUNT to 0,
                    FIELD_DISLIKE_COUNT to 0,
                ),
            )
            tx.update(postRef, FIELD_COMMENT_COUNT, count + 1)
        }.await()
        newId
    }

    override suspend fun likeComment(postId: String, commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val commentRef = postRef.collection(SUB_COMMENTS).document(commentId)
        val likeRef = commentRef.collection(SUB_COMMENT_LIKES).document(uid)
        val dislikeRef = commentRef.collection(SUB_COMMENT_DISLIKES).document(uid)
        firestore.runTransaction { tx ->
            val comment = tx.get(commentRef)
            if (!comment.exists()) return@runTransaction
            val like = tx.get(likeRef)
            if (like.exists()) return@runTransaction
            var likeCount = comment.getLong(FIELD_LIKE_COUNT) ?: 0L
            var dislikeCount = comment.getLong(FIELD_DISLIKE_COUNT) ?: 0L
            val dislike = tx.get(dislikeRef)
            if (dislike.exists()) {
                tx.delete(dislikeRef)
                dislikeCount = max(0L, dislikeCount - 1)
            }
            tx.set(likeRef, mapOf(FIELD_TIMESTAMP to FieldValue.serverTimestamp()))
            likeCount += 1
            tx.update(
                commentRef,
                mapOf(
                    FIELD_LIKE_COUNT to likeCount,
                    FIELD_DISLIKE_COUNT to dislikeCount,
                ),
            )
        }.await()
    }

    override suspend fun unlikeComment(postId: String, commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val commentRef = postRef.collection(SUB_COMMENTS).document(commentId)
        val likeRef = commentRef.collection(SUB_COMMENT_LIKES).document(uid)
        firestore.runTransaction { tx ->
            val comment = tx.get(commentRef)
            if (!comment.exists()) return@runTransaction
            val like = tx.get(likeRef)
            if (!like.exists()) return@runTransaction
            val current = comment.getLong(FIELD_LIKE_COUNT) ?: 0L
            tx.delete(likeRef)
            tx.update(commentRef, FIELD_LIKE_COUNT, max(0L, current - 1))
        }.await()
    }

    override suspend fun dislikeComment(postId: String, commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val commentRef = postRef.collection(SUB_COMMENTS).document(commentId)
        val dislikeRef = commentRef.collection(SUB_COMMENT_DISLIKES).document(uid)
        val likeRef = commentRef.collection(SUB_COMMENT_LIKES).document(uid)
        firestore.runTransaction { tx ->
            val comment = tx.get(commentRef)
            if (!comment.exists()) return@runTransaction
            val dislike = tx.get(dislikeRef)
            if (dislike.exists()) return@runTransaction
            var likeCount = comment.getLong(FIELD_LIKE_COUNT) ?: 0L
            var dislikeCount = comment.getLong(FIELD_DISLIKE_COUNT) ?: 0L
            val like = tx.get(likeRef)
            if (like.exists()) {
                tx.delete(likeRef)
                likeCount = max(0L, likeCount - 1)
            }
            tx.set(dislikeRef, mapOf(FIELD_TIMESTAMP to FieldValue.serverTimestamp()))
            dislikeCount += 1
            tx.update(
                commentRef,
                mapOf(
                    FIELD_LIKE_COUNT to likeCount,
                    FIELD_DISLIKE_COUNT to dislikeCount,
                ),
            )
        }.await()
    }

    override suspend fun undislikeComment(postId: String, commentId: String) {
        val uid = auth.currentUser?.uid ?: return
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val commentRef = postRef.collection(SUB_COMMENTS).document(commentId)
        val dislikeRef = commentRef.collection(SUB_COMMENT_DISLIKES).document(uid)
        firestore.runTransaction { tx ->
            val comment = tx.get(commentRef)
            if (!comment.exists()) return@runTransaction
            val dislike = tx.get(dislikeRef)
            if (!dislike.exists()) return@runTransaction
            val current = comment.getLong(FIELD_DISLIKE_COUNT) ?: 0L
            tx.delete(dislikeRef)
            tx.update(commentRef, FIELD_DISLIKE_COUNT, max(0L, current - 1))
        }.await()
    }

    override suspend fun ensureUserProfile() {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val email = user.email.orEmpty()
        val local = email.substringBefore("@").ifBlank { "fan" }
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: local.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        val handle = sanitizeHandle(local)
        firestore.collection(COL_USERS).document(uid).set(
            mapOf(
                FIELD_DISPLAY_NAME to displayName,
                FIELD_HANDLE to handle,
                FIELD_USERNAME to displayName,
                FIELD_EMAIL to email,
                FIELD_AVATAR_URL to null,
                FIELD_JOINED_TIMESTAMP to FieldValue.serverTimestamp(),
                FIELD_FOLLOWER_COUNT to 0,
                FIELD_FOLLOWING_COUNT to 0,
            ),
            SetOptions.merge(),
        ).await()
    }

    override suspend fun saveMessagingToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return
        firestore.collection(COL_USERS).document(uid).set(
            mapOf(FIELD_FCM_TOKEN to trimmed),
            SetOptions.merge(),
        ).await()
    }

    override suspend fun syncUserProfilePreferences(preferences: UserPreferences) {
        val uid = auth.currentUser?.uid ?: return
        val data = mutableMapOf<String, Any>(
            FIELD_FAVORITE_LEAGUES to preferences.leagueSlugsForEspnContent(),
            FIELD_FAVORITE_CLUBS to preferences.favoriteTeamIdsList(),
            FIELD_FAVORITE_TEAM_ID to (preferences.favoriteTeamId ?: ""),
            FIELD_FAVORITE_TEAM_LEAGUE_SLUG to (preferences.favoriteTeamLeagueSlug ?: ""),
            FIELD_FAVORITE_TEAM_NAME to (preferences.favoriteTeamName ?: ""),
        )
        preferences.nickname?.takeIf { it.isNotBlank() }?.let { nick ->
            data[FIELD_USERNAME] = nick
            data[FIELD_DISPLAY_NAME] = nick
        }
        preferences.profilePhotoUrl?.takeIf { it.isNotBlank() }?.let { url ->
            data[FIELD_AVATAR_URL] = url
        }
        firestore.collection(COL_USERS).document(uid).set(data, SetOptions.merge()).await()
    }

    private fun sanitizeHandle(raw: String): String {
        val s = raw.lowercase().map { c ->
            when {
                c.isLetterOrDigit() -> c
                c == '_' || c == '.' -> c
                else -> '_'
            }
        }.joinToString("").trim('_').take(30)
        return s.ifBlank { "fan" }
    }

    companion object {
        private const val TAG = "FeedRepository"
        private const val COL_USERS = "users"
        private const val COL_POSTS = "posts"
        private const val SUB_LIKES = "likes"
        private const val SUB_DISLIKES = "dislikes"
        private const val SUB_COMMENTS = "comments"
        private const val SUB_COMMENT_LIKES = "commentLikes"
        private const val SUB_COMMENT_DISLIKES = "commentDislikes"
        /** Global feed: recent posts visible to all signed-in users (`posts` read is public). */
        private const val GLOBAL_FEED_LIMIT = 80L
        private const val MAX_POST_CHARS = 500
        private const val MAX_COMMENT_CHARS = 200

        private const val FIELD_USER_ID = "userId"
        private const val FIELD_TEXT = "text"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_LIKE_COUNT = "likeCount"
        private const val FIELD_DISLIKE_COUNT = "dislikeCount"
        private const val FIELD_COMMENT_COUNT = "commentCount"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_HANDLE = "handle"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_AVATAR_URL = "avatarUrl"
        private const val FIELD_JOINED_TIMESTAMP = "joinedTimestamp"
        private const val FIELD_FOLLOWER_COUNT = "followerCount"
        private const val FIELD_FOLLOWING_COUNT = "followingCount"
        private const val FIELD_COMMENT_ID = "commentId"
        private const val FIELD_FCM_TOKEN = "fcmToken"
        private const val FIELD_FAVORITE_LEAGUES = "favoriteLeagues"
        private const val FIELD_FAVORITE_CLUBS = "favoriteClubs"
        private const val FIELD_FAVORITE_TEAM_ID = "favoriteTeamId"
        private const val FIELD_FAVORITE_TEAM_LEAGUE_SLUG = "favoriteTeamLeagueSlug"
        private const val FIELD_FAVORITE_TEAM_NAME = "favoriteTeamName"
    }
}
