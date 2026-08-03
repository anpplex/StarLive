package com.starlive.app

import android.app.Application
import android.util.Log

/**
 * Application entry. Phase 0 skeleton — wallpaper seed / FGS land in later phases.
 */
class StarLiveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "StarLiveApp onCreate version=${BuildConfig.VERSION_NAME}")
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
