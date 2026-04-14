package com.forzaball.app.notifications

import android.content.Intent

/**
 * Parses FCM **data** payloads for feed notifications.
 *
 * **Background + `notification` payload:** If the server (or Firebase Console) sends a message that
 * includes a notification block, Android may display it in the system tray **without** calling
 * [com.google.firebase.messaging.FirebaseMessagingService.onMessageReceived]. To always receive
 * messages in your service (including in background), send **data-only** messages with
 * [com.google.firebase.messaging.RemoteMessage] data keys — no `notification` field — and use
 * high priority on Android.
 */
object FeedPushFcmParser {

    private const val GENERIC_POST_ID_PREFIX = "campaign_"

    /** Requires [merged] non-empty (caller must guard empty maps). */
    fun parseForDelivery(merged: Map<String, String>): FeedPushPayload =
        parseStructured(merged) ?: buildGenericCampaign(merged)

    /**
     * Maps intent extras from opening the app via a notification tap into the same string map shape
     * as [com.google.firebase.messaging.RemoteMessage.getData], plus title/body fallbacks.
     */
    fun intentNotificationExtrasToDataMap(intent: Intent?): Map<String, String> {
        val extras = intent?.extras ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        for (key in extras.keySet()) {
            if (key.startsWith("google.") || key.startsWith("gcm.") || key == "from") continue
            val v = extras.getString(key) ?: continue
            out[key] = v
        }
        if (out["actorName"].isNullOrBlank()) {
            extras.getString("gcm.notification.title")?.trim()?.takeIf { it.isNotEmpty() }?.let {
                out["actorName"] = it
            }
        }
        if (out["preview"].isNullOrBlank()) {
            extras.getString("gcm.notification.body")?.trim()?.takeIf { it.isNotEmpty() }?.let {
                out["preview"] = it
            }
        }
        return out
    }

    fun isGenericCampaign(payload: FeedPushPayload): Boolean =
        payload.postId.startsWith(GENERIC_POST_ID_PREFIX)

    private fun parseStructured(data: Map<String, String>): FeedPushPayload? {
        val typeStr = data["type"] ?: return null
        val postId = data["postId"] ?: return null
        val commentId = data["commentId"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["comment_id"]?.trim()?.takeIf { it.isNotEmpty() }
        val type = when (typeStr) {
            "new_post" -> FeedPushType.NewPost
            "comment" -> FeedPushType.Comment
            "like" -> FeedPushType.Like
            "dislike" -> FeedPushType.Dislike
            "comment_like" -> FeedPushType.CommentLike
            else -> return null
        }
        val reaction = when (typeStr) {
            "like" -> FeedReactionKind.Like
            "dislike" -> FeedReactionKind.Dislike
            else -> null
        }
        val name = data["actorName"]?.trim().orEmpty().ifBlank { "Someone" }
        return FeedPushPayload(
            type = type,
            postId = postId,
            commentId = commentId,
            actorId = data["actorId"]?.takeIf { it.isNotBlank() },
            actorName = name,
            actorPhotoUrl = data["actorPhotoUrl"]?.takeIf { it.isNotBlank() },
            preview = data["preview"].orEmpty(),
            reaction = reaction,
        )
    }

    private fun buildGenericCampaign(data: Map<String, String>): FeedPushPayload {
        return FeedPushPayload(
            type = FeedPushType.NewPost,
            postId = "$GENERIC_POST_ID_PREFIX${System.currentTimeMillis()}",
            commentId = null,
            actorId = null,
            actorName = data["actorName"]?.trim().orEmpty().ifBlank { "ForzaBall" },
            actorPhotoUrl = null,
            preview = data["preview"].orEmpty(),
            reaction = null,
        )
    }

    /** String keys/values suitable for [android.content.Intent.putExtra] (FCM data shape). */
    fun payloadToIntentExtras(payload: FeedPushPayload): Map<String, String> {
        val typeWire = when (payload.type) {
            FeedPushType.NewPost -> "new_post"
            FeedPushType.Comment -> "comment"
            FeedPushType.Like -> "like"
            FeedPushType.Dislike -> "dislike"
            FeedPushType.CommentLike -> "comment_like"
        }
        val out = mutableMapOf(
            "type" to typeWire,
            "postId" to payload.postId,
            "actorName" to payload.actorName,
            "preview" to payload.preview,
        )
        payload.commentId?.let { out["commentId"] = it }
        payload.actorId?.let { out["actorId"] = it }
        payload.actorPhotoUrl?.let { out["actorPhotoUrl"] = it }
        return out
    }
}
