package com.forzaball.notifications

import android.content.Context
import android.os.SystemClock
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import timber.log.Timber

private const val TAG = "FcmRegistrar"
private const val MIN_TOKEN_REFRESH_MS = 30 * 60 * 1000L

/**
 * Registers FCM topics/tokens only when Google Play Services is available.
 * Avoids repeated broker calls that can spam logcat with
 * `SecurityException: Unknown calling package name 'com.google.android.gms'` on some emulators.
 */
object FcmRegistrar {

    @Volatile
    private var lastTokenRegisteredAtMs: Long = 0L

    suspend fun subscribeFeedTopic(context: Context): Boolean {
        if (!isPlayServicesReady(context)) return false
        return runCatching {
            FirebaseMessaging.getInstance().subscribeToTopic(FEED_BROADCAST_TOPIC).await()
            true
        }.onFailure { logFcmFailure("subscribeToTopic", it) }
            .getOrDefault(false)
    }

    suspend fun unsubscribeFeedTopic(context: Context): Boolean {
        if (!isPlayServicesReady(context)) return false
        return runCatching {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(FEED_BROADCAST_TOPIC).await()
            true
        }.onFailure { logFcmFailure("unsubscribeFromTopic", it) }
            .getOrDefault(false)
    }

    suspend fun registerToken(
        context: Context,
        saveToken: suspend (String) -> Unit,
        force: Boolean = false,
    ): Boolean {
        if (!isPlayServicesReady(context)) return false
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastTokenRegisteredAtMs < MIN_TOKEN_REFRESH_MS) {
            return true
        }
        return runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            saveToken(token)
            lastTokenRegisteredAtMs = now
            Timber.tag(TAG).i("FCM token registered")
            true
        }.onFailure { logFcmFailure("getToken", it) }
            .getOrDefault(false)
    }

    private fun isPlayServicesReady(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val code = availability.isGooglePlayServicesAvailable(context)
        if (code != ConnectionResult.SUCCESS) {
            Timber.tag(TAG).w(
                "Google Play Services unavailable (code=%s): %s",
                code,
                availability.getErrorString(code),
            )
            return false
        }
        return true
    }

    private fun logFcmFailure(operation: String, error: Throwable) {
        if (error is SecurityException) {
            Timber.tag(TAG).w(
                error,
                "%s failed: Play Services broker error — use a Google Play emulator image, " +
                    "update Google Play Services on device, or wipe emulator data",
                operation,
            )
        } else {
            Timber.tag(TAG).w(error, "%s failed", operation)
        }
    }
}
