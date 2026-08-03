package com.starlive.app

import android.app.Application
import android.util.Log
import com.starlive.app.runtime.StripOrchestrator
import com.starlive.app.wallpaper.WallpaperRepository

class StarLiveApp : Application() {
    lateinit var orchestrator: StripOrchestrator
        private set

    override fun onCreate() {
        super.onCreate()
        orchestrator = StripOrchestrator(this)
        runCatching { WallpaperRepository.ensureSeeded(this) }
            .onFailure { Log.w(TAG, "seed failed", it) }
        Log.i(TAG, "StarLiveApp onCreate ${BuildConfig.VERSION_NAME}")
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
