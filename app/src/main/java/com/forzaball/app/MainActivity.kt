package com.forzaball.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.forzaball.app.notifications.FeedOpenRequest
import com.forzaball.app.notifications.FeedPushConstants
import com.forzaball.app.notifications.FeedPushFcmParser
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forzaball.app.feature.home.FixturesListRoute
import com.forzaball.app.feature.home.HomeRoute
import com.forzaball.app.feature.home.NavUrlCodec
import com.forzaball.app.feature.home.NewsListRoute
import com.forzaball.app.feature.home.NewsWebViewScreen
import com.forzaball.app.notifications.FcmTokenRegistrationEffect
import com.forzaball.app.notifications.FeedInAppNotificationBanner
import com.forzaball.app.notifications.FeedNotificationBus
import kotlinx.coroutines.delay
import com.forzaball.app.feature.onboarding.OnboardingScreen
import com.forzaball.app.feature.personalization.PersonalizationStep1Screen
import com.forzaball.app.feature.personalization.PersonalizationStep2Screen
import com.forzaball.app.feature.personalization.PersonalizationStep3Screen
import com.forzaball.app.feature.personalization.PersonalizationViewModel
import com.forzaball.app.feature.splash.SplashRoute
import com.forzaball.app.ui.theme.ForzaBallTheme
import org.koin.androidx.compose.koinViewModel
import com.forzaball.app.R

class MainActivity : ComponentActivity() {

    /** Drives recomposition when a VIEW intent arrives while the activity is already running (singleTop). */
    private val feedOpenRequestState = mutableStateOf<FeedOpenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Use normal window (opaque system bars, not the splash/transparent window).
        setTheme(R.style.Theme_ForzaBall)
        super.onCreate(savedInstanceState)
        // Lay out content between status bar and navigation/gesture bar (not behind system bars).
        WindowCompat.setDecorFitsSystemWindows(window, true)
        feedOpenRequestState.value = intent.extractFeedOpenRequest()
        setContent {
            val initialFeedOpen by feedOpenRequestState
            ForzaBallTheme {
                ForzaBallAppCompose(initialFeedOpen = initialFeedOpen)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        feedOpenRequestState.value = intent.extractFeedOpenRequest()
    }
}

private fun Intent?.extractFeedOpenRequest(): FeedOpenRequest? {
    val explicitPost = this?.getStringExtra(FeedPushConstants.EXTRA_OPEN_FEED_POST_ID)?.takeIf { it.isNotBlank() }
    if (explicitPost != null) {
        val commentId = this.getStringExtra(FeedPushConstants.EXTRA_OPEN_FEED_COMMENT_ID)?.takeIf { it.isNotBlank() }
            ?: this.getStringExtra("commentId")?.takeIf { it.isNotBlank() }
            ?: this.getStringExtra("comment_id")?.takeIf { it.isNotBlank() }
        return FeedOpenRequest(explicitPost, commentId)
    }
    // FCM: background messages with a `notification` block are often not delivered to
    // FirebaseMessagingService; tapping the notification still delivers data keys via Activity extras.
    val fcmData = FeedPushFcmParser.intentNotificationExtrasToDataMap(this)
    if (fcmData.isNotEmpty()) {
        val payload = FeedPushFcmParser.parseForDelivery(fcmData)
        if (!FeedPushFcmParser.isGenericCampaign(payload)) {
            return FeedOpenRequest(payload.postId, payload.commentId)
        }
    }
    val uri = this?.data ?: return null
    val postIdFromUri = extractPostIdFromViewUri(uri) ?: return null
    return FeedOpenRequest(postIdFromUri, null)
}

private fun extractPostIdFromViewUri(uri: Uri): String? {
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
fun ForzaBallAppCompose(initialFeedOpen: FeedOpenRequest? = null) {
    val navController = rememberNavController()
    var bannerOpenRequest by remember { mutableStateOf<FeedOpenRequest?>(null) }
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
                    selectedLeagueId = state.selectedLeagueId,
                    onSelectLeague = viewModel::selectLeague,
                    onBack = { navController.popBackStack() },
                    onNext = viewModel::nextStep,
                    isLoadingTeams = state.isLoadingTeams,
                )

                2 -> PersonalizationStep2Screen(
                    clubs = state.teamsForLeague,
                    selectedClubId = state.selectedClubId,
                    onSelectClub = viewModel::selectClub,
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
                    selectedLeagueId = state.selectedLeagueId,
                    onSelectLeague = viewModel::selectLeague,
                    onBack = { navController.popBackStack() },
                    onNext = viewModel::nextStep,
                    isLoadingTeams = state.isLoadingTeams,
                )
            }
        }

        composable("home") {
            HomeRoute(
                initialFeedOpen = initialFeedOpen,
                bannerOpenRequest = bannerOpenRequest,
                onBannerOpenConsumed = { bannerOpenRequest = null },
                onNavigateToSignIn = {
                    navController.navigate("signin")
                },
                onNavigateToSignUp = {
                    navController.navigate("signup")
                },
                onNavigateToFixturesList = { navController.navigate("fixtures_list") },
                onOpenNewsArticle = { url, _ ->
                    val enc = NavUrlCodec.encode(url)
                    navController.navigate("news_web/$enc")
                },
            )
        }

        composable("news_list") {
            NewsListRoute(
                onBack = { navController.popBackStack() },
                onOpenArticle = { url, _ ->
                    val enc = NavUrlCodec.encode(url)
                    navController.navigate("news_web/$enc")
                },
            )
        }

        composable("fixtures_list") {
            FixturesListRoute(
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "news_web/{payload}",
            arguments = listOf(
                navArgument("payload") { type = NavType.StringType },
            ),
        ) { entry ->
            val payload = entry.arguments?.getString("payload").orEmpty()
            val url = runCatching { NavUrlCodec.decode(payload) }.getOrDefault("")
            NewsWebViewScreen(
                url = url,
                title = "Article",
                onClose = { navController.popBackStack() },
            )
        }
    }

        feedBanner?.let { payload ->
            FeedInAppNotificationBanner(
                payload = payload,
                onOpen = {
                    FeedNotificationBus.clear()
                    bannerOpenRequest = FeedOpenRequest(payload.postId, payload.commentId)
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