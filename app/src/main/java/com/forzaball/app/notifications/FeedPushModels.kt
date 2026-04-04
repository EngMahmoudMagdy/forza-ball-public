package com.forzaball.app.notifications

/** Parsed FCM data payload for feed-related pushes (data-only messages from Cloud Functions). */
data class FeedPushPayload(
    val type: FeedPushType,
    val postId: String,
    val actorId: String?,
    val actorName: String,
    val actorPhotoUrl: String?,
    /** Post excerpt or short comment preview. */
    val preview: String,
    /** For like/dislike: which reaction. */
    val reaction: FeedReactionKind?,
)

enum class FeedPushType {
    NewPost,
    Comment,
    Like,
    Dislike,
}

enum class FeedReactionKind {
    Like,
    Dislike,
}
