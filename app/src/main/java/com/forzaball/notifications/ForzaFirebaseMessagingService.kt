package com.forzaball.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.forzaball.MainActivity
import com.forzaball.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class ForzaFirebaseMessagingService : FirebaseMessagingService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("FCM new token received")
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Timber.d("FCM token skipped save (not signed in yet)")
            return
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).set(
            mapOf("fcmToken" to token),
            SetOptions.merge(),
        ).addOnSuccessListener { Timber.d("fcmToken saved to Firestore") }
            .addOnFailureListener { e -> Timber.w(e, "save fcmToken") }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val merged = mergedPayload(message)
        Timber.d(
            "FCM onMessageReceived keys=%s foreground=%s",
            merged.keys,
            AppForegroundTracker.isInForeground,
        )
        if (merged.isEmpty()) {
            Timber.w("FCM message has no data and no notification body; nothing to show")
            return
        }
        val payload = FeedPushFcmParser.parseForDelivery(merged)
        if (FeedPushFcmParser.isGenericCampaign(payload)) {
            Timber.i("FCM generic/campaign-style payload (notification-only or missing type/postId)")
        }

        val myUid = FirebaseAuth.getInstance().currentUser?.uid
        if (payload.type == FeedPushType.NewPost && payload.actorId != null && payload.actorId == myUid) {
            return
        }

        if (AppForegroundTracker.isInForeground) {
            deliverInApp(payload)
            return
        }

        showSystemNotification(payload)
    }

    private fun deliverInApp(payload: FeedPushPayload) {
        mainHandler.post { FeedNotificationBus.post(payload) }
    }

    /**
     * Combines `data` with [RemoteMessage.getNotification] so Console/tests that only set the
     * notification block still produce a preview; production Cloud Functions should send **data-only**
     * with `type`, `postId`, etc.
     */
    private fun mergedPayload(message: RemoteMessage): Map<String, String> {
        val out = message.data.toMutableMap()
        val n = message.notification
        if (n != null) {
            if (out["actorName"].isNullOrBlank()) {
                out["actorName"] = n.title?.trim().orEmpty().ifBlank { "ForzaBall" }
            }
            if (out["preview"].isNullOrBlank()) {
                out["preview"] = n.body?.trim().orEmpty()
            }
        }
        return out
    }

    private fun showSystemNotification(payload: FeedPushPayload) {
        FeedNotificationChannels.ensureFeedChannel(this)

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Timber.w("POST_NOTIFICATIONS denied; tray notification skipped (grant in system settings)")
                return
            }
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !nm.areNotificationsEnabled()) {
            Timber.w("Notifications disabled at OS level")
            return
        }

        val title = notificationTitle(payload)
        val text = payload.preview.ifBlank { "Tap to open" }

        val open = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!FeedPushFcmParser.isGenericCampaign(payload)) {
                putExtra(FeedPushConstants.EXTRA_OPEN_FEED_POST_ID, payload.postId)
                payload.commentId?.takeIf { it.isNotBlank() }?.let {
                    putExtra(FeedPushConstants.EXTRA_OPEN_FEED_COMMENT_ID, it)
                }
                // Duplicate FCM-shaped keys so taps work whether the system passes `data` or `notification` only.
                FeedPushFcmParser.payloadToIntentExtras(payload).forEach { (k, v) ->
                    putExtra(k, v)
                }
            }
        }
        val pending = PendingIntent.getActivity(
            this,
            payload.postId.hashCode() xor (payload.commentId?.hashCode() ?: 0) xor payload.type.ordinal,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, FeedPushConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_forza)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        nm.notify(payload.postId.hashCode(), notification)
    }

    private fun notificationTitle(payload: FeedPushPayload): String = when (payload.type) {
        FeedPushType.NewPost -> payload.actorName
        FeedPushType.Comment -> "${payload.actorName} commented"
        FeedPushType.Like -> "${payload.actorName} liked your post"
        FeedPushType.Dislike -> "${payload.actorName} disliked your post"
        FeedPushType.CommentLike -> "${payload.actorName} liked your comment"
    }

}
