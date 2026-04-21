package com.forzaball.domain.di

import com.forzaball.domain.usecase.ObserveUserPreferencesUseCase
import org.koin.dsl.module

val domainModule = module {
    single { ObserveUserPreferencesUseCase(get()) }
}

