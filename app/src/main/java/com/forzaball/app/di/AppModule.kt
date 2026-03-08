package com.forzaball.app.di

import com.forzaball.app.data.auth.AuthRepositoryImpl
import com.forzaball.app.feature.auth.signin.SignInViewModel
import com.forzaball.app.feature.auth.signup.SignUpViewModel
import com.forzaball.app.feature.home.HomeViewModel
import com.forzaball.app.feature.personalization.PersonalizationViewModel
import com.forzaball.app.feature.splash.SplashViewModel
import com.forzaball.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { FirebaseAuth.getInstance() }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { SplashViewModel(get(), get()) }
    viewModel { SignUpViewModel(get()) }
    viewModel { SignInViewModel(get(), get()) }
    viewModel { PersonalizationViewModel(get()) }
}

