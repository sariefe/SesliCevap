package com.example

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class VoiceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Timber logging initialization — release builds plant no tree (no Logcat output)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.i("VoiceApplication initialized successfully")
        }
    }
}
