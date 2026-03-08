package com.forzaball.domain.di

import com.forzaball.domain.usecase.ObserveFavoriteClubMatchUseCase
import com.forzaball.domain.usecase.ObserveFavoriteClubsNewsUseCase
import com.forzaball.domain.usecase.ObserveUserPreferencesUseCase
import org.koin.dsl.module

val domainModule = module {
    single { ObserveUserPreferencesUseCase(get()) }
    single { ObserveFavoriteClubMatchUseCase(get()) }
    single { ObserveFavoriteClubsNewsUseCase(get()) }
}

