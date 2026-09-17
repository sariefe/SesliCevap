package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class VoiceApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Timber logging initialization
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.i("VoiceApplication initialized successfully")

        // Initialize Dependency Injection container
        container = DefaultAppContainer(this)
    }
}
