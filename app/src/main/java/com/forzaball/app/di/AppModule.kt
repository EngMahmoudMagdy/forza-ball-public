package com.forzaball.app.di

import com.forzaball.app.data.auth.AuthRepositoryImpl
import com.forzaball.app.feature.auth.signin.SignInViewModel
import com.forzaball.app.feature.auth.signup.SignUpViewModel
import com.forzaball.app.feature.feeds.FeedViewModel
import com.forzaball.app.feature.home.FixturesListViewModel
import com.forzaball.app.feature.home.HomeViewModel
import com.forzaball.app.feature.home.NewsListViewModel
import com.forzaball.app.feature.home.ScoresViewModel
import com.forzaball.app.feature.profile.EditFavoritesViewModel
import com.forzaball.app.feature.profile.ProfileViewModel
import com.forzaball.app.feature.personalization.PersonalizationViewModel
import com.forzaball.app.feature.splash.SplashViewModel
import com.forzaball.domain.diagnostics.HomeLoadTracer
import com.forzaball.domain.repository.AuthRepository
import com.forzaball.domain.usecase.LoadHomeContentUseCase
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import timber.log.Timber

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
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
    viewModel { SignInViewModel(get(), get()) }
    viewModel { PersonalizationViewModel(get(), get(), get()) }
    viewModel { EditFavoritesViewModel(get(), get(), get()) }
}

