package com.forzaball.app.notifications

/**
 * Opens the feed post detail overlay, optionally scrolling to and highlighting a comment
 * (e.g. from an FCM tap).
 */
data class FeedOpenRequest(
    val postId: String,
    val highlightCommentId: String? = null,
) {
    /** Encoded key for [rememberSaveable] / overlay state. */
    val overlayKey: String
        get() = highlightCommentId?.let { "post:$postId:c:$it" } ?: "post:$postId"

    companion object {
        fun fromOverlayKey(key: String): FeedOpenRequest? {
            if (!key.startsWith("post:")) return null
            val rest = key.removePrefix("post:")
            val cIdx = rest.indexOf(":c:")
            if (cIdx == -1) return FeedOpenRequest(rest, null)
            val postId = rest.substring(0, cIdx)
            val commentId = rest.substring(cIdx + 3).trim().takeIf { it.isNotEmpty() }
            return FeedOpenRequest(postId, commentId)
        }
    }
}
