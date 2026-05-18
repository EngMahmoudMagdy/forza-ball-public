package com.forzaball.notifications

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.forzaball.domain.repository.FeedRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import timber.log.Timber

private const val TAG = "FcmTokenRegistration"

/**
 * On each transition to started (cold start, returning from background), refreshes the FCM
 * registration token and writes it to `users/{uid}.fcmToken` in Firestore when signed in.
 */
@Composable
fun FcmTokenRegistrationEffect(
    feedRepository: FeedRepository = koinInject(),
) {
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as ComponentActivity

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_START) return@LifecycleEventObserver
            scope.launch {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid == null) {
                    Timber.tag(TAG).d("skip FCM registration (signed out)")
                    return@launch
                }
                val appContext = activity.applicationContext
                FcmRegistrar.subscribeFeedTopic(appContext)
                val registered = FcmRegistrar.registerToken(
                    context = appContext,
                    saveToken = { token -> feedRepository.saveMessagingToken(token) },
                )
                if (registered) {
                    Timber.tag(TAG).i("FCM token registered in Firestore")
                }
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }
}
