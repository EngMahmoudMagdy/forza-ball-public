package com.forzaball

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.forzaball.di.appModule
import com.forzaball.notifications.AppForegroundTracker
import com.forzaball.notifications.FeedNotificationChannels
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

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppForegroundTracker)
        FeedNotificationChannels.ensureFeedChannel(this)

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

