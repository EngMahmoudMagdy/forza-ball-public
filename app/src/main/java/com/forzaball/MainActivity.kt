package com.forzaball

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import com.forzaball.notifications.FeedOpenRequest
import com.forzaball.notifications.FeedPushConstants
import com.forzaball.notifications.FeedPushFcmParser
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forzaball.feature.home.FixturesListRoute
import com.forzaball.feature.home.HomeRoute
import com.forzaball.feature.home.NavUrlCodec
import com.forzaball.feature.home.NewsListRoute
import com.forzaball.feature.home.NewsWebViewScreen
import com.forzaball.feature.notifications.NotificationsRoute
import com.forzaball.feature.search.SearchRoute
import com.forzaball.feature.search.TeamSearchProfileRoute
import com.forzaball.notifications.FcmTokenRegistrationEffect
import com.forzaball.notifications.FeedInAppNotificationBanner
import com.forzaball.notifications.FeedNotificationBus
import kotlinx.coroutines.delay
import com.forzaball.feature.onboarding.OnboardingScreen
import com.forzaball.feature.personalization.PersonalizationStep1Screen
import com.forzaball.feature.personalization.PersonalizationStep2Screen
import com.forzaball.feature.personalization.PersonalizationStep3Screen
import com.forzaball.feature.personalization.PersonalizationViewModel
import com.forzaball.feature.splash.SplashRoute
import com.forzaball.data.preferences.ThemePreferencesRepository
import com.forzaball.ui.theme.ForzaBallTheme
import com.forzaball.ui.theme.ThemeMode
import org.koin.android.ext.android.getKoin
import org.koin.androidx.compose.koinViewModel
import com.forzaball.R

class MainActivity : ComponentActivity() {

    /** Drives recomposition when a VIEW intent arrives while the activity is already running (singleTop). */
    private val feedOpenRequestState = mutableStateOf<FeedOpenRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ForzaBall)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        @Suppress("DEPRECATION")
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        feedOpenRequestState.value = intent.extractFeedOpenRequest()
        setContent {
            val initialFeedOpen by feedOpenRequestState
            val themeRepo: ThemePreferencesRepository = getKoin().get()
            val themeMode by themeRepo.observeThemeMode().collectAsState(initial = ThemeMode.Dark)
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> systemDark
            }
            ForzaBallTheme(darkTheme = useDarkTheme) {
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
    var notificationOpenRequest by remember { mutableStateOf<FeedOpenRequest?>(null) }
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
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
            val viewModel: com.forzaball.feature.auth.signup.SignUpViewModel = koinViewModel()
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
            com.forzaball.feature.auth.signup.SignUpScreen(
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
            val viewModel: com.forzaball.feature.auth.signin.SignInViewModel = koinViewModel()
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
            com.forzaball.feature.auth.signin.SignInScreen(
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
            BackHandler {
                if (state.step == 1) {
                    navController.popBackStack()
                } else {
                    viewModel.previousStep()
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

                3 -> {
                    val pickPhoto = com.forzaball.feature.profile.rememberProfilePhotoPicker(
                        onPhotoSelected = viewModel::uploadProfilePhoto,
                    )
                    PersonalizationStep3Screen(
                        nickname = state.nickname,
                        onNicknameChange = viewModel::setNickname,
                        profileImageUrl = state.profilePhotoUrl,
                        profileImageThumbUrl = state.profilePhotoThumbUrl,
                        profilePhotoCacheVersion = state.profilePhotoCacheVersion,
                        profileUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "me",
                        isUploadingPhoto = state.isUploadingPhoto,
                        onPickPhoto = pickPhoto,
                        onBack = viewModel::previousStep,
                        onFinish = viewModel::finish,
                    )
                }

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
                notificationOpenRequest = notificationOpenRequest,
                onNotificationOpenConsumed = { notificationOpenRequest = null },
                onNavigateToNotifications = { navController.navigate("notifications") },
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
                onNavigateToSearch = { navController.navigate("search") },
            )
        }

        composable("notifications") {
            NotificationsRoute(
                onBack = { navController.popBackStack() },
                onOpenPost = { req ->
                    notificationOpenRequest = req
                    navController.popBackStack()
                },
            )
        }

        composable("search") {
            SearchRoute(
                onBack = { navController.popBackStack() },
                onOpenTeamProfile = { leagueSlug, teamId ->
                    val encLeague = Uri.encode(leagueSlug)
                    navController.navigate("team_search_profile/$encLeague/$teamId")
                },
            )
        }

        composable(
            route = "team_search_profile/{leagueSlug}/{teamId}",
            arguments = listOf(
                navArgument("leagueSlug") { type = NavType.StringType },
                navArgument("teamId") { type = NavType.StringType },
            ),
        ) {
            TeamSearchProfileRoute(
                onBack = { navController.popBackStack() },
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
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                        .zIndex(1000f)
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
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