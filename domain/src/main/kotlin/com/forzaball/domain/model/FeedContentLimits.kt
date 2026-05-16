package com.forzaball.domain.model

object FeedContentLimits {
    const val MAX_POST_CHARS = 1000
    const val MAX_COMMENT_CHARS = 1000

    fun validatePost(text: String): String? {
        val trimmed = text.trim()
        return when {
            trimmed.isEmpty() -> "Post cannot be empty"
            trimmed.length > MAX_POST_CHARS -> "Post must be $MAX_POST_CHARS characters or fewer"
            else -> null
        }
    }

    fun validateComment(text: String): String? {
        val trimmed = text.trim()
        return when {
            trimmed.isEmpty() -> "Comment cannot be empty"
            trimmed.length > MAX_COMMENT_CHARS -> "Comment must be $MAX_COMMENT_CHARS characters or fewer"
            else -> null
        }
    }
}
