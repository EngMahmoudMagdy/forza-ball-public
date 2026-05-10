package com.forzaball.di

import com.forzaball.data.auth.AuthRepositoryImpl
import com.forzaball.data.auth.AndroidSessionTokenStore
import com.forzaball.data.auth.AndroidSessionUserStore
import com.forzaball.data.auth.NoOpAuthRefreshApi
import com.forzaball.feature.auth.signin.SignInViewModel
import com.forzaball.feature.auth.signup.SignUpViewModel
import com.forzaball.feature.feeds.FeedViewModel
import com.forzaball.feature.home.FixturesListViewModel
import com.forzaball.feature.home.HomeViewModel
import com.forzaball.feature.home.NewsListViewModel
import com.forzaball.feature.home.ScoresViewModel
import com.forzaball.feature.profile.EditFavoritesViewModel
import com.forzaball.feature.profile.ProfileViewModel
import com.forzaball.feature.personalization.PersonalizationViewModel
import com.forzaball.feature.search.SearchViewModel
import com.forzaball.feature.search.TeamSearchProfileViewModel
import com.forzaball.data.preferences.ThemePreferencesRepository
import com.forzaball.data.preferences.ThemePreferencesRepositoryImpl
import com.forzaball.feature.notifications.NotificationsViewModel
import com.forzaball.feature.splash.SplashViewModel
import com.forzaball.domain.diagnostics.HomeLoadTracer
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.usecase.LoadHomeContentUseCase
import com.google.firebase.auth.FirebaseAuth
import com.forzaball.shared.auth.DefaultSessionOrchestrator
import com.forzaball.shared.auth.SessionOrchestrator
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

val appModule = module {
    single<ThemePreferencesRepository> { ThemePreferencesRepositoryImpl(get()) }
    single { FirebaseAuth.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single { AndroidSessionTokenStore(get()) }
    single { AndroidSessionUserStore(get()) }
    single { NoOpAuthRefreshApi() }
    single<SessionOrchestrator> { DefaultSessionOrchestrator(get(), get(), get()) }
    single {
        LoadHomeContentUseCase(
            matchRepository = get(),
            newsRepository = get(),
            tracer = HomeLoadTracer { message -> Timber.tag("HomePage").d(message) },
        )
    }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { NewsListViewModel(get(), get()) }
    viewModel { FixturesListViewModel(get(), get()) }
    viewModel { ScoresViewModel(get(), get(), get()) }
    viewModel { FeedViewModel(get(), get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { SplashViewModel(get(), get()) }
    viewModel { SignUpViewModel(get()) }
    viewModel { SignInViewModel(get(), get(), get()) }
    viewModel { PersonalizationViewModel(get(), get(), get()) }
    viewModel { EditFavoritesViewModel(get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get()) }
    viewModel { TeamSearchProfileViewModel(get(), get(), get()) }
    viewModel { NotificationsViewModel(get(), get()) }
}

