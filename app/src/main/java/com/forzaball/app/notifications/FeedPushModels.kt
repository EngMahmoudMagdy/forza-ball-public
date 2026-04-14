package com.forzaball.app.notifications

/** Parsed FCM data payload for feed-related pushes (data-only messages from Cloud Functions). */
data class FeedPushPayload(
    val type: FeedPushType,
    val postId: String,
    /** Set for comment / comment_like notifications so the UI can scroll to this comment. */
    val commentId: String?,
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
    /** Someone liked the recipient’s post. */
    Like,
    Dislike,
    /** Someone liked the recipient’s comment. */
    CommentLike,
}

enum class FeedReactionKind {
    Like,
    Dislike,
}
