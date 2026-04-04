package com.forzaball.app.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Latest feed push for in-app UI (banner). Uses [StateFlow] so the Home screen always observes
 * the current value (SharedFlow with replay=0 was dropping events before collectors subscribed).
 */
object FeedNotificationBus {
    private val _pending = MutableStateFlow<FeedPushPayload?>(null)
    val pending: StateFlow<FeedPushPayload?> = _pending.asStateFlow()

    fun post(payload: FeedPushPayload) {
        _pending.value = payload
    }

    fun clear() {
        _pending.value = null
    }
}
