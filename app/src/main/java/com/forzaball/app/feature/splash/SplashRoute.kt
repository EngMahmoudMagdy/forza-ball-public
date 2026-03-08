package com.forzaball.app.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashRoute(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onNavigateToPersonalization: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.decideDestination()
    }
    LaunchedEffect(Unit) {
        viewModel.destination.collectLatest { dest ->
            when (dest) {
                is SplashDestination.Onboarding -> onNavigateToOnboarding()
                is SplashDestination.SignIn -> onNavigateToSignIn()
                is SplashDestination.Personalization -> onNavigateToPersonalization()
                is SplashDestination.Home -> onNavigateToHome()
            }
        }
    }
    SplashScreen()
}
