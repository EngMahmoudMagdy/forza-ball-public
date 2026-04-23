package com.forzaball.data.di

import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.forzaball.data.BuildConfig
import com.forzaball.data.feed.FeedRepositoryImpl
import com.forzaball.data.match.MatchRepositoryImpl
import com.forzaball.data.network.CurlLoggingInterceptor
import com.forzaball.data.network.EspnApiService
import com.forzaball.data.network.EspnTablesApiService
import com.forzaball.data.news.NewsRepositoryImpl
import com.forzaball.data.standings.StandingsRepositoryImpl
import com.forzaball.data.preferences.PreferencesRepositoryImpl
import com.forzaball.data.soccer.SoccerTeamsRepositoryImpl
import com.forzaball.domain.repository.FeedRepository
import com.forzaball.domain.repository.MatchRepository
import com.forzaball.domain.repository.NewsRepository
import com.forzaball.domain.repository.PreferencesRepository
import com.forzaball.domain.repository.SoccerTeamsRepository
import com.forzaball.domain.repository.StandingsRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit

val dataModule = module {
    single { FirebaseFirestore.getInstance() }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    single {
        val logging = HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                CurlLoggingInterceptor { line ->
                    Timber.tag("OkHttp").d("curl: %s", line)
                },
            )
        }
        builder
            .addInterceptor(logging)
            .addInterceptor(ChuckerInterceptor.Builder(androidContext()).build())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    single(qualifier = named("espnSiteSoccerRetrofit")) {
        val json: Json = get()
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://site.api.espn.com/apis/site/v2/sports/soccer/")
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    single(qualifier = named("espnSoccerTablesRetrofit")) {
        val json: Json = get()
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl("https://site.api.espn.com/apis/v2/sports/soccer/")
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    single<EspnApiService> {
        get<Retrofit>(named("espnSiteSoccerRetrofit")).create(EspnApiService::class.java)
    }

    single<EspnTablesApiService> {
        get<Retrofit>(named("espnSoccerTablesRetrofit")).create(EspnTablesApiService::class.java)
    }

    single<SoccerTeamsRepository> { SoccerTeamsRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get(), get()) }
    single<NewsRepository> { NewsRepositoryImpl(get()) }
    single<MatchRepository> { MatchRepositoryImpl(get()) }
    single<StandingsRepository> { StandingsRepositoryImpl(get()) }
    single<FeedRepository> { FeedRepositoryImpl(get(), get(), get()) }
}
