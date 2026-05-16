package com.forzaball.data.feed

import com.forzaball.domain.model.TeamSearchHistoryEntry
import com.forzaball.domain.model.FeedContentLimits
import com.forzaball.domain.model.UserPreferences
import com.forzaball.domain.model.UserPublicProfile
import com.forzaball.domain.model.favoriteTeamIdsList
import com.forzaball.domain.model.leagueSlugsForEspnContent
import com.forzaball.domain.repository.FeedComment
import com.forzaball.domain.repository.FeedNotification
import com.forzaball.domain.repository.FeedNotificationTypes
import com.forzaball.domain.repository.FeedPost
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.PreferencesRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.max

class FeedRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val preferencesRepository: PreferencesRepository,
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
                    val docs = snapshot?.documents.orEmpty().filterActivePosts()
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

    override fun observeUserNotifications(): Flow<List<FeedNotification>> = callbackFlow {
        var notifReg: ListenerRegistration? = null
        var attachedUid: String? = null

        fun detach() {
            notifReg?.remove()
            notifReg = null
        }

        fun attach(uid: String) {
            if (uid == attachedUid && notifReg != null) return
            detach()
            attachedUid = uid
            notifReg = firestore.collection(COL_USERS).document(uid).collection(SUB_NOTIFICATIONS)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(NOTIFICATIONS_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag(TAG).e(error, "notifications listener")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents.orEmpty().mapNotNull { doc -> doc.toFeedNotification() }
                    trySend(list)
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { fa ->
            val uid = fa.currentUser?.uid
            if (uid == null) {
                detach()
                attachedUid = null
                trySend(emptyList())
            } else {
                attach(uid)
            }
        }
        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            detach()
            attachedUid = null
        }
    }

    override suspend fun markNotificationRead(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection(COL_USERS).document(uid).collection(SUB_NOTIFICATIONS).document(notificationId)
            .update(NF_READ, true)
            .await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toFeedNotification(): FeedNotification? {
        val postId = getString(NF_POST_ID) ?: return null
        return FeedNotification(
            id = id,
            type = getString(NF_TYPE).orEmpty(),
            postId = postId,
            commentId = getString(NF_COMMENT_ID)?.takeIf { it.isNotBlank() },
            actorUserId = getString(NF_ACTOR_USER_ID).orEmpty(),
            actorName = getString(NF_ACTOR_NAME).orEmpty(),
            read = getBoolean(NF_READ) ?: false,
            createdAtMillis = getTimestamp(FIELD_TIMESTAMP)?.toDate()?.time ?: 0L,
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.isPostDeleted(): Boolean =
        getBoolean(FIELD_IS_DELETED) == true

    private fun List<com.google.firebase.firestore.DocumentSnapshot>.filterActivePosts() =
        filter { !it.isPostDeleted() }

    private suspend fun buildFeedPostsFromDocuments(
        uid: String,
        docs: List<com.google.firebase.firestore.DocumentSnapshot>,
    ): List<FeedPost> {
        if (docs.isEmpty()) return emptyList()
        val userIds = docs.mapNotNull { it.getString(FIELD_USER_ID) }.distinct()
        val profiles = loadUserProfiles(userIds)
        val savedIds = loadSavedPostIds(uid)
        return coroutineScope {
            docs.map { doc ->
                async {
                    val authorId = doc.getString(FIELD_USER_ID) ?: return@async null
                    val profile = profiles[authorId]
                    val likeSnap = firestore.collection(COL_POSTS).document(doc.id)
                        .collection(SUB_LIKES).document(uid).get(Source.DEFAULT).await()
                    val dislikeSnap = firestore.collection(COL_POSTS).document(doc.id)
                        .collection(SUB_DISLIKES).document(uid).get(Source.DEFAULT).await()
                    toFeedPost(
                        doc,
                        profile,
                        likeSnap.exists(),
                        dislikeSnap.exists(),
                        savedIds.contains(doc.id),
                    )
                }
            }.mapNotNull { it.await() }
        }
    }

    private suspend fun loadSavedPostIds(uid: String): Set<String> = runCatching {
        firestore.collection(COL_USERS).document(uid).collection(SUB_SAVED_POSTS)
            .get(Source.DEFAULT)
            .await()
            .documents
            .map { it.id }
            .toSet()
    }.getOrElse { e ->
        Timber.tag(TAG).w(e, "loadSavedPostIds")
        emptySet()
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
        saved: Boolean = false,
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
        val avatar = resolveAvatarThumb(userDoc)
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
            isSavedByUser = saved,
            createdAtMillis = doc.getTimestamp(FIELD_TIMESTAMP)?.toDate()?.time ?: 0L,
        )
    }

    private fun resolveAvatarThumb(userDoc: com.google.firebase.firestore.DocumentSnapshot?): String? {
        if (userDoc == null || !userDoc.exists()) return null
        return userDoc.getString(FIELD_AVATAR_THUMB_URL)?.takeIf { it.isNotBlank() }
            ?: userDoc.getString(FIELD_AVATAR_URL)?.takeIf { it.isNotBlank() }
    }

    private fun resolveAvatarFull(userDoc: com.google.firebase.firestore.DocumentSnapshot?): String? {
        if (userDoc == null || !userDoc.exists()) return null
        return userDoc.getString(FIELD_AVATAR_URL)?.takeIf { it.isNotBlank() }
            ?: userDoc.getString(FIELD_AVATAR_THUMB_URL)?.takeIf { it.isNotBlank() }
    }

    private fun userDocToPublicProfile(
        userId: String,
        userDoc: com.google.firebase.firestore.DocumentSnapshot?,
    ): UserPublicProfile? {
        if (userDoc == null || !userDoc.exists()) return null
        val displayName = userDoc.getString(FIELD_DISPLAY_NAME)?.takeIf { it.isNotBlank() }
            ?: userDoc.getString(FIELD_USERNAME)?.takeIf { it.isNotBlank() }
            ?: userDoc.getString(FIELD_EMAIL)?.substringBefore("@")?.ifBlank { null }
            ?: "User"
        val handle = userDoc.getString(FIELD_HANDLE)?.takeIf { it.isNotBlank() }
            ?: sanitizeHandle(userDoc.getString(FIELD_EMAIL)?.substringBefore("@").orEmpty())
        return UserPublicProfile(
            userId = userId,
            displayName = displayName,
            handle = handle,
            avatarUrl = resolveAvatarFull(userDoc),
            avatarThumbUrl = resolveAvatarThumb(userDoc),
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
                if (snapshot == null || !snapshot.exists() || snapshot.isPostDeleted()) {
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
                        val savedIds = loadSavedPostIds(uid)
                        trySend(
                            toFeedPost(
                                snapshot,
                                profile,
                                likeSnap.exists(),
                                dislikeSnap.exists(),
                                savedIds.contains(postId),
                            ),
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
        FeedContentLimits.validatePost(text)?.let { error(it) }
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
                    FIELD_IS_DELETED to false,
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
            if (!post.exists() || post.getBoolean(FIELD_IS_DELETED) == true) return@runTransaction
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
            if (!post.exists() || post.getBoolean(FIELD_IS_DELETED) == true) return@runTransaction
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
            if (!post.exists() || post.getBoolean(FIELD_IS_DELETED) == true) return@runTransaction
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
            if (!post.exists() || post.getBoolean(FIELD_IS_DELETED) == true) return@runTransaction
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
        val avatar = resolveAvatarThumb(profile)
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
        FeedContentLimits.validateComment(text)?.let { error(it) }
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val commentRef = postRef.collection(SUB_COMMENTS).document()
        val newId = commentRef.id
        firestore.runTransaction { tx ->
            val post = tx.get(postRef)
            if (!post.exists() || post.getBoolean(FIELD_IS_DELETED) == true) error("Post missing")
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
        notifyPostAuthorOfComment(postRef, postId, newId, uid)
        newId
    }

    private suspend fun notifyPostAuthorOfComment(
        postRef: com.google.firebase.firestore.DocumentReference,
        postId: String,
        commentId: String,
        commenterUid: String,
    ) {
        try {
            val postSnap = postRef.get(Source.DEFAULT).await()
            val authorId = postSnap.getString(FIELD_USER_ID)?.takeIf { it.isNotBlank() } ?: return
            if (authorId == commenterUid) return
            val actorName = loadActorNameForNotification(commenterUid)
            firestore.collection(COL_USERS).document(authorId).collection(SUB_NOTIFICATIONS).add(
                mapOf(
                    NF_TYPE to FeedNotificationTypes.COMMENT,
                    NF_POST_ID to postId,
                    NF_COMMENT_ID to commentId,
                    NF_ACTOR_USER_ID to commenterUid,
                    NF_ACTOR_NAME to actorName,
                    NF_READ to false,
                    FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
                ),
            ).await()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "notifyPostAuthorOfComment failed")
        }
    }

    private suspend fun loadActorNameForNotification(uid: String): String {
        val doc = runCatching {
            firestore.collection(COL_USERS).document(uid).get(Source.DEFAULT).await()
        }.getOrNull() ?: return "Someone"
        return doc.getString(FIELD_DISPLAY_NAME)?.takeIf { it.isNotBlank() }
            ?: doc.getString(FIELD_USERNAME)?.takeIf { it.isNotBlank() }
            ?: doc.getString(FIELD_EMAIL)?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Someone"
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
        preferences.profilePhotoThumbUrl?.takeIf { it.isNotBlank() }?.let { url ->
            data[FIELD_AVATAR_THUMB_URL] = url
        }
        data[FIELD_TEAM_SEARCH_HISTORY] = preferences.teamSearchHistory
            .take(TEAM_SEARCH_HISTORY_MAX)
            .map { e ->
                mapOf(
                    "teamId" to e.teamId,
                    "leagueSlug" to e.leagueSlug,
                    "teamName" to e.teamName,
                    "leagueName" to e.leagueName,
                    "teamCrestUrl" to (e.teamCrestUrl ?: ""),
                    "searchedAtMillis" to e.searchedAtMillis,
                )
            }
        firestore.collection(COL_USERS).document(uid).set(data, SetOptions.merge()).await()
    }

    override suspend fun mergeTeamSearchHistoryFromRemote() {
        val uid = auth.currentUser?.uid ?: return
        val remote = runCatching {
            val snap = firestore.collection(COL_USERS).document(uid).get(Source.DEFAULT).await()
            if (!snap.exists()) return@runCatching emptyList()
            @Suppress("UNCHECKED_CAST")
            val raw = snap.get(FIELD_TEAM_SEARCH_HISTORY) as? List<*> ?: return@runCatching emptyList()
            raw.mapNotNull { item ->
                (item as? Map<*, *>)?.let { m ->
                    val sm = m.entries.associate { (k, v) -> k.toString() to v }
                    teamSearchEntryFromFirestoreMap(sm)
                }
            }
        }.getOrElse { e ->
            Timber.tag(TAG).w(e, "mergeTeamSearchHistoryFromRemote read")
            emptyList()
        }
        if (remote.isEmpty()) return
        val local = preferencesRepository.observeUserPreferences().first()
        val merged = mergeTeamSearchHistoryLists(local.teamSearchHistory, remote)
        if (merged == local.teamSearchHistory) return
        preferencesRepository.updateUserPreferences(local.copy(teamSearchHistory = merged))
    }

    private fun teamSearchEntryFromFirestoreMap(m: Map<String, Any?>): TeamSearchHistoryEntry? {
        val teamId = m["teamId"]?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val leagueSlug = m["leagueSlug"]?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val teamName = m["teamName"]?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val leagueName = m["leagueName"]?.toString().orEmpty()
        val crest = m["teamCrestUrl"]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val ts = when (val v = m["searchedAtMillis"]) {
            is Number -> v.toLong()
            else -> 0L
        }
        return TeamSearchHistoryEntry(
            teamId = teamId,
            leagueSlug = leagueSlug,
            teamName = teamName,
            leagueName = leagueName,
            teamCrestUrl = crest,
            searchedAtMillis = ts.takeIf { it > 0L } ?: System.currentTimeMillis(),
        )
    }

    private fun mergeTeamSearchHistoryLists(
        local: List<TeamSearchHistoryEntry>,
        remote: List<TeamSearchHistoryEntry>,
    ): List<TeamSearchHistoryEntry> {
        val byKey = LinkedHashMap<String, TeamSearchHistoryEntry>()
        fun key(e: TeamSearchHistoryEntry) = "${e.leagueSlug}|${e.teamId}"
        (local + remote).forEach { e ->
            val k = key(e)
            val cur = byKey[k]
            if (cur == null || e.searchedAtMillis > cur.searchedAtMillis) {
                byKey[k] = e
            }
        }
        return byKey.values
            .sortedByDescending { it.searchedAtMillis }
            .take(TEAM_SEARCH_HISTORY_MAX)
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

    override suspend fun deletePost(postId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val postRef = firestore.collection(COL_POSTS).document(postId)
        val post = postRef.get(Source.DEFAULT).await()
        if (!post.exists()) error("Post not found")
        if (post.getString(FIELD_USER_ID) != uid) error("Not allowed")
        if (post.isPostDeleted()) return@runCatching
        postRef.update(
            mapOf(
                FIELD_IS_DELETED to true,
                FIELD_DELETED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    override suspend fun savePost(postId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        firestore.collection(COL_USERS).document(uid).collection(SUB_SAVED_POSTS).document(postId)
            .set(
                mapOf(
                    FIELD_POST_ID to postId,
                    FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
                ),
            ).await()
    }

    override suspend fun unsavePost(postId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        firestore.collection(COL_USERS).document(uid).collection(SUB_SAVED_POSTS).document(postId)
            .delete()
            .await()
    }

    override suspend fun reportPost(
        postId: String,
        reasonId: String,
        reasonLabel: String,
        optionalComment: String?,
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val postSnap = firestore.collection(COL_POSTS).document(postId).get(Source.DEFAULT).await()
        if (!postSnap.exists() || postSnap.isPostDeleted()) error("Post not found")
        val postAuthorId = postSnap.getString(FIELD_USER_ID).orEmpty()
        val data = mutableMapOf<String, Any>(
            FIELD_POST_ID to postId,
            FIELD_POST_AUTHOR_ID to postAuthorId,
            FIELD_REPORTER_USER_ID to uid,
            FIELD_REPORT_REASON_ID to reasonId,
            FIELD_REPORT_REASON_LABEL to reasonLabel,
            FIELD_TIMESTAMP to FieldValue.serverTimestamp(),
        )
        optionalComment?.trim()?.takeIf { it.isNotEmpty() }?.let { data[FIELD_REPORT_COMMENT] = it }
        firestore.collection(COL_POST_REPORTS).add(data).await()
    }

    override fun observePostsByUser(userId: String): Flow<List<FeedPost>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = firestore.collection(COL_POSTS)
            .whereEqualTo(FIELD_USER_ID, userId)
            .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
            .limit(USER_POSTS_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.tag(TAG).e(error, "observePostsByUser")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents.orEmpty().filterActivePosts()
                ioScope.launch {
                    try {
                        trySend(buildFeedPostsFromDocuments(currentUid, docs))
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "observePostsByUser emit")
                        trySend(emptyList())
                    }
                }
            }
        awaitClose { reg.remove() }
    }

    override fun observeSavedPosts(): Flow<List<FeedPost>> = callbackFlow {
        var savedReg: ListenerRegistration? = null
        var attachedUid: String? = null

        fun detach() {
            savedReg?.remove()
            savedReg = null
        }

        fun attach(uid: String) {
            if (uid == attachedUid && savedReg != null) return
            detach()
            attachedUid = uid
            savedReg = firestore.collection(COL_USERS).document(uid).collection(SUB_SAVED_POSTS)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(SAVED_POSTS_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.tag(TAG).e(error, "observeSavedPosts")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val postIds = snapshot?.documents?.map { it.id }.orEmpty()
                    ioScope.launch {
                        try {
                            if (postIds.isEmpty()) {
                                trySend(emptyList())
                                return@launch
                            }
                            val docs = coroutineScope {
                                postIds.map { id ->
                                    async {
                                        firestore.collection(COL_POSTS).document(id).get(Source.DEFAULT).await()
                                    }
                                }.map { it.await() }.filter { it.exists() && !it.isPostDeleted() }
                            }
                            trySend(buildFeedPostsFromDocuments(uid, docs))
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, "observeSavedPosts emit")
                            trySend(emptyList())
                        }
                    }
                }
        }

        val authListener = FirebaseAuth.AuthStateListener { fa ->
            val uid = fa.currentUser?.uid
            if (uid == null) {
                detach()
                attachedUid = null
                trySend(emptyList())
            } else {
                attach(uid)
            }
        }
        auth.addAuthStateListener(authListener)
        awaitClose {
            auth.removeAuthStateListener(authListener)
            detach()
        }
    }

    override fun observeUserPublicProfile(userId: String): Flow<UserPublicProfile?> = callbackFlow {
        val reg = firestore.collection(COL_USERS).document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.tag(TAG).e(error, "observeUserPublicProfile")
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(userDocToPublicProfile(userId, snapshot))
            }
        awaitClose { reg.remove() }
    }

    companion object {
        private const val TAG = "FeedRepository"
        private const val TEAM_SEARCH_HISTORY_MAX = 30
        private const val COL_USERS = "users"
        private const val COL_POSTS = "posts"
        private const val COL_POST_REPORTS = "postReports"
        private const val SUB_SAVED_POSTS = "savedPosts"
        private const val USER_POSTS_LIMIT = 50L
        private const val SAVED_POSTS_LIMIT = 50L
        private const val SUB_NOTIFICATIONS = "notifications"
        private const val NOTIFICATIONS_LIMIT = 80L
        private const val SUB_LIKES = "likes"
        private const val SUB_DISLIKES = "dislikes"
        private const val SUB_COMMENTS = "comments"
        private const val SUB_COMMENT_LIKES = "commentLikes"
        private const val SUB_COMMENT_DISLIKES = "commentDislikes"
        /** Global feed: recent posts visible to all signed-in users (`posts` read is public). */
        private const val GLOBAL_FEED_LIMIT = 80L
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_IS_DELETED = "isDeleted"
        private const val FIELD_DELETED_AT = "deletedAt"
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
        private const val FIELD_AVATAR_THUMB_URL = "avatarThumbUrl"
        private const val FIELD_POST_ID = "postId"
        private const val FIELD_POST_AUTHOR_ID = "postAuthorId"
        private const val FIELD_REPORTER_USER_ID = "reporterUserId"
        private const val FIELD_REPORT_REASON_ID = "reasonId"
        private const val FIELD_REPORT_REASON_LABEL = "reasonLabel"
        private const val FIELD_REPORT_COMMENT = "comment"
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
        private const val FIELD_TEAM_SEARCH_HISTORY = "teamSearchHistory"

        private const val NF_TYPE = "type"
        private const val NF_POST_ID = "postId"
        private const val NF_COMMENT_ID = "commentId"
        private const val NF_ACTOR_USER_ID = "actorUserId"
        private const val NF_ACTOR_NAME = "actorName"
        private const val NF_READ = "read"
    }
}
