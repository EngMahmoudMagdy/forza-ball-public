package com.forzaball.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForzaBallTheme {
                ForzaBallAppCompose()
            }
        }
    }
}

@Composable
fun ForzaBallAppCompose() {
    val navController = rememberNavController()

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
            HomeRoute()
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