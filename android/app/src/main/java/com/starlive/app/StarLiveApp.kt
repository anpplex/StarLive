package com.starlive.app

import android.app.Application
import android.util.Log
import com.starlive.app.runtime.BootRecoverScheduler
import com.starlive.app.runtime.StripOrchestrator
import com.starlive.app.service.KeepAliveService
import com.starlive.app.wallpaper.WallpaperRepository

class StarLiveApp : Application() {
    lateinit var orchestrator: StripOrchestrator
        private set
    lateinit var bootScheduler: BootRecoverScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        orchestrator = StripOrchestrator(this)
        bootScheduler = BootRecoverScheduler(this, orchestrator)
        runCatching { WallpaperRepository.ensureSeeded(this) }
            .onFailure { Log.w(TAG, "seed failed", it) }
        if (WallpaperRepository.idlePrefer(this)) {
            runCatching { KeepAliveService.start(this) }
        }
        Log.i(TAG, "StarLiveApp onCreate ${BuildConfig.VERSION_NAME}")
    }

    companion object {
        private const val TAG = "StarLive"
    }
}
