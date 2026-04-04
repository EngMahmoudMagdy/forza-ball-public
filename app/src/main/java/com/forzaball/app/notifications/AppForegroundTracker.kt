package com.forzaball.app.notifications

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Used by [ForzaFirebaseMessagingService] to avoid showing system notifications while the app is open;
 * foreground events are shown as in-app snackbars instead.
 */
object AppForegroundTracker : DefaultLifecycleObserver {
    @Volatile
    var isInForeground: Boolean = false
        private set

    override fun onStart(owner: LifecycleOwner) {
        isInForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isInForeground = false
    }
}
