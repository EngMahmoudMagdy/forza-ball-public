package com.forzaball.data.di

import com.forzaball.data.feed.FeedRepositoryImpl
import com.forzaball.data.match.MatchRepositoryImpl
import com.forzaball.data.news.NewsRepositoryImpl
import com.forzaball.data.preferences.PreferencesRepositoryImpl
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.MatchRepository
import com.forzaball.domain.repository.NewsRepository
import com.forzaball.domain.repository.PreferencesRepository
import org.koin.dsl.module

val dataModule = module {
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<NewsRepository> { NewsRepositoryImpl(get()) }
    single<MatchRepository> { MatchRepositoryImpl(get()) }
    single<FeedRepository> { FeedRepositoryImpl(get()) }
}

