package com.forzaball.data.di

import com.forzaball.data.BuildConfig
import com.forzaball.data.feed.FeedRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import com.forzaball.data.match.MatchRepositoryImpl
import com.forzaball.data.network.ApiFootballAuthInterceptor
import com.forzaball.data.network.ApiFootballService
import com.forzaball.data.news.NewsRepositoryImpl
import com.forzaball.data.preferences.PreferencesRepositoryImpl
import com.forzaball.data.secrets.FootballSecrets
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.MatchRepository
import com.forzaball.domain.repository.NewsRepository
import com.forzaball.domain.repository.PreferencesRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit

val dataModule = module {
    single { FirebaseFirestore.getInstance() }

    single { FootballSecrets }

    single {
        val logging = HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(ApiFootballAuthInterceptor(get()))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(get<FootballSecrets>().baseUrl())
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    single<ApiFootballService> { get<Retrofit>().create(ApiFootballService::class.java) }

    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<NewsRepository> { NewsRepositoryImpl(get(), get()) }
    single<MatchRepository> { MatchRepositoryImpl(get(), get()) }
    single<FeedRepository> { FeedRepositoryImpl(get(), get()) }
}
