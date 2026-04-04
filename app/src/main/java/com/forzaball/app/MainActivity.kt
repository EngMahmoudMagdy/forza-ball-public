package com.forzaball.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.forzaball.app.notifications.FeedPushConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.forzaball.app.notifications.FcmTokenRegistrationEffect
import com.forzaball.app.notifications.FeedInAppNotificationBanner
import com.forzaball.app.notifications.FeedNotificationBus
import kotlinx.coroutines.delay
import com.forzaball.app.feature.home.HomeRoute
import com.forzaball.app.feature.onboarding.OnboardingScreen
import com.forzaball.app.feature.personalization.PersonalizationStep1Screen
import com.forzaball.app.feature.personalization.PersonalizationStep2Screen
import com.forzaball.app.feature.personalization.PersonalizationStep3Screen
import com.forzaball.app.feature.personalization.PersonalizationViewModel
import com.forzaball.app.feature.splash.SplashRoute
import com.forzaball.app.ui.theme.ForzaBallTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    /** Drives recomposition when a VIEW intent arrives while the activity is already running (singleTop). */
    private val deepLinkPostIdState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Lay out content between status bar and navigation/gesture bar (not behind system bars).
        WindowCompat.setDecorFitsSystemWindows(window, true)
        deepLinkPostIdState.value = intent.extractFeedPostId()
        setContent {
            val initialFeedPostId by deepLinkPostIdState
            ForzaBallTheme {
                ForzaBallAppCompose(initialFeedPostId = initialFeedPostId)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkPostIdState.value = intent.extractFeedPostId()
    }
}

private fun Intent?.extractFeedPostId(): String? {
    this?.getStringExtra(FeedPushConstants.EXTRA_OPEN_FEED_POST_ID)?.takeIf { it.isNotBlank() }?.let { return it }
    val uri = this?.data ?: return null
    // Custom scheme: forzaball://post/<id>
    if (uri.scheme == "forzaball" && uri.host == "post") {
        return uri.pathSegments.firstOrNull()?.takeIf { it.isNotBlank() }
    }
    // https://forzaball.app/post/<id> (and www)
    val host = uri.host?.lowercase() ?: return null
    if (host == "forzaball.app" || host == "www.forzaball.app") {
        val segments = uri.pathSegments
        val i = segments.indexOf("post")
        if (i >= 0 && i < segments.lastIndex) {
            return segments[i + 1].takeIf { it.isNotBlank() }
        }
        val path = uri.path ?: return null
        val prefix = "/post/"
        if (path.startsWith(prefix)) {
            return path.removePrefix(prefix).trim('/').substringBefore('/').takeIf { it.isNotBlank() }
        }
    }
    return null
}

@Composable
fun ForzaBallAppCompose(initialFeedPostId: String? = null) {
    val navController = rememberNavController()
    var bannerOpenPostId by remember { mutableStateOf<String?>(null) }
    val feedBanner by FeedNotificationBus.pending.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(feedBanner?.postId, feedBanner?.preview) {
        val current = feedBanner ?: return@LaunchedEffect
        delay(8_000)
        if (FeedNotificationBus.pending.value?.postId == current.postId) {
            FeedNotificationBus.clear()
        }
    }

    Box(Modifier.fillMaxSize()) {
        FcmTokenRegistrationEffect()

        NavHost(
            navController = navController,
            startDestination = "splash",
        ) {
        composable("splash") {
            SplashRoute(
                onNavigateToOnboarding = {
                    navController.navigate("onboarding") { popUpTo("splash") { inclusive = true } }
                },
                onNavigateToSignIn = {
                    navController.navigate("signin") { popUpTo("splash") { inclusive = true } }
                },
                onNavigateToPersonalization = {
                    navController.navigate("personalization") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("home") { popUpTo("splash") { inclusive = true } }
                },
            )
        }

        composable("onboarding") {
            OnboardingScreen(
                onLogIn = { navController.navigate("signin") },
                onSignUp = { navController.navigate("signup") },
                onChooseCountry = { /* TODO: show country picker */ },
                onGetStarted = { navController.navigate("signup") },
            )
        }

        composable("signup") {
            val viewModel: com.forzaball.app.feature.auth.signup.SignUpViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            LaunchedEffect(state.navigateToPersonalization) {
                if (state.navigateToPersonalization) {
                    viewModel.clearNavigation()
                    navController.navigate("personalization") {
                        popUpTo("onboarding") {
                            inclusive = true
                        }
                    }
                }
            }
            com.forzaball.app.feature.auth.signup.SignUpScreen(
                onBack = { navController.popBackStack() },
                onSignIn = {
                    navController.navigate("signin") {
                        popUpTo("signup") {
                            inclusive = true
                        }
                    }
                },
                onSignUpWithEmail = { firstName, lastName, email, _, password ->
                    viewModel.signUpWithEmail(firstName, lastName, email, "", password)
                },
                onSignUpWithGoogle = { /* TODO: launch Google Sign-In and pass idToken to viewModel.signUpWithGoogle(idToken) */ },
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
            )
        }

        composable("signin") {
            val viewModel: com.forzaball.app.feature.auth.signin.SignInViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            LaunchedEffect(state.navigateToHome, state.navigateToPersonalization) {
                if (state.navigateToHome) {
                    viewModel.clearNavigation()
                    navController.navigate("home") { popUpTo(0) { inclusive = true } }
                } else if (state.navigateToPersonalization) {
                    viewModel.clearNavigation()
                    navController.navigate("personalization") { popUpTo(0) { inclusive = true } }
                }
            }
            com.forzaball.app.feature.auth.signin.SignInScreen(
                onBack = { navController.popBackStack() },
                onSignUp = { navController.navigate("signup") },
                onSignInWithEmailOrPhone = { emailOrPhone, password ->
                    viewModel.signInWithEmailOrPhone(emailOrPhone, password)
                },
                onSignInWithGoogle = { /* TODO: launch Google Sign-In */ },
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
            )
        }

        composable("personalization") {
            val viewModel: PersonalizationViewModel = koinViewModel()
            val state by viewModel.state.collectAsState()
            LaunchedEffect(state.navigateToHome) {
                if (state.navigateToHome) {
                    viewModel.clearNavigation()
                    navController.navigate("home") { popUpTo(0) { inclusive = true } }
                }
            }
            when (state.step) {
                1 -> PersonalizationStep1Screen(
                    selectedLeagueIds = state.selectedLeagueIds,
                    onToggleLeague = viewModel::toggleLeague,
                    onBack = { navController.popBackStack() },
                    onNext = viewModel::nextStep,
                )

                2 -> PersonalizationStep2Screen(
                    selectedLeagueIds = state.selectedLeagueIds,
                    selectedClubIds = state.selectedClubIds,
                    onToggleClub = viewModel::toggleClub,
                    onBack = viewModel::previousStep,
                    onNext = viewModel::nextStep,
                )

                3 -> PersonalizationStep3Screen(
                    nickname = state.nickname,
                    onNicknameChange = viewModel::setNickname,
                    profileImageUrl = state.profilePhotoUrl,
                    onBack = viewModel::previousStep,
                    onFinish = viewModel::finish,
                )

                else -> PersonalizationStep1Screen(
                    selectedLeagueIds = state.selectedLeagueIds,
                    onToggleLeague = viewModel::toggleLeague,
                    onBack = { navController.popBackStack() },
                    onNext = viewModel::nextStep,
                )
            }
        }

        composable("home") {
            HomeRoute(
                initialFeedPostId = initialFeedPostId,
                bannerOpenPostId = bannerOpenPostId,
                onBannerOpenConsumed = { bannerOpenPostId = null },
                onNavigateToSignIn = {
                    navController.navigate("signin")
                },
                onNavigateToSignUp = {
                    navController.navigate("signup")
                },
            )
        }
    }

        feedBanner?.let { payload ->
            FeedInAppNotificationBanner(
                payload = payload,
                onOpen = {
                    FeedNotificationBus.clear()
                    bannerOpenPostId = payload.postId
                    navController.navigate("home") {
                        launchSingleTop = true
                    }
                },
                onDismiss = { FeedNotificationBus.clear() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .zIndex(1000f)
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ForzaBallTheme {
        ForzaBallAppCompose()
    }
}