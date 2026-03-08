package com.forzaball.app

import android.app.Application
import com.forzaball.app.di.appModule
import com.forzaball.data.di.dataModule
import com.forzaball.domain.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class ForzaBallApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@ForzaBallApp)
            modules(
                domainModule,
                dataModule,
                appModule,
            )
        }
    }
}

